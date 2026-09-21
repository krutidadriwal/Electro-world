const admin = require('firebase-admin');

let app;

// Its own service account, in its own Firebase project -- separate from
// lib/drive.js's GOOGLE_SERVICE_ACCOUNT_JSON (a different GCP project used
// only for Drive access) and unrelated to the customer app's old, now-removed
// ew-app-48acf project. Must match whatever Firebase project the customer
// app's current google-services.json is registered under, since FCM device
// tokens are only valid within their own project.
function getMessaging() {
  if (!app) {
    const raw = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
    if (!raw) {
      throw new Error('FIREBASE_SERVICE_ACCOUNT_JSON environment variable is not set.');
    }
    let serviceAccount;
    try {
      serviceAccount = JSON.parse(raw);
    } catch (err) {
      // Length + boundary char codes (never the content) to diagnose paste
      // mistakes -- e.g. a wrapping quote pair or smart quotes -- without
      // logging the private key itself.
      throw new Error(
        `FIREBASE_SERVICE_ACCOUNT_JSON is not valid JSON: ${err.message}. ` +
        `length=${raw.length} firstCharCode=${raw.charCodeAt(0)} lastCharCode=${raw.charCodeAt(raw.length - 1)}`
      );
    }
    app = admin.apps.length > 0 ? admin.app() : admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
  }
  return admin.messaging(app);
}

// FCM caps multicast sends at 500 tokens per call.
const BATCH_SIZE = 500;

// Sends the same notification to every token, batching as needed. Returns
// the subset of tokens FCM reports as no-longer-registered, so the caller
// can prune them from device_tokens.
//
// Deliberately data-only (no top-level `notification` field): a message
// carrying both `notification` and `data` gets auto-displayed by the OS
// itself whenever the app isn't in the foreground, using Android's own
// fallback channel -- bypassing EWFirebaseMessagingService.onMessageReceived
// entirely, which broke this app's own channel, its stable per-notification
// tray id (needed to cancel a tray entry when read in-app, for badge-count
// accuracy), and image rendering. Data-only messages always reach
// onMessageReceived, foreground or not.
async function sendToTokens(tokens, { title, body, imageUrl, notificationId }) {
  if (tokens.length === 0) {
    return { deadTokens: [] };
  }

  const messaging = getMessaging();
  const deadTokens = [];
  const data = Object.fromEntries(
    Object.entries({ title, body, imageUrl, notificationId })
      .filter(([, value]) => value !== undefined && value !== null)
      .map(([key, value]) => [key, String(value)])
  );

  for (let i = 0; i < tokens.length; i += BATCH_SIZE) {
    const batch = tokens.slice(i, i + BATCH_SIZE);
    const response = await messaging.sendEachForMulticast({
      tokens: batch,
      data,
      // Data-only messages default to normal priority; without this,
      // delivery to a backgrounded/doze'd device can be significantly
      // delayed compared to how `notification` messages behaved.
      android: { priority: 'high' }
    });
    response.responses.forEach((result, index) => {
      if (!result.success && result.error?.code === 'messaging/registration-token-not-registered') {
        deadTokens.push(batch[index]);
      }
    });
  }

  return { deadTokens };
}

module.exports = { sendToTokens };
