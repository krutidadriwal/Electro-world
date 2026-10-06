// Same shape as fcm.js, but for the staff app's OWN Firebase project
// ("electro-world-staff"), registered separately from the customer app's
// ("ew-app-gondia") -- device tokens are only ever valid within the project
// that issued them, so sending to staff tokens needs its own service account
// and its own named Admin SDK app instance (a plain admin.initializeApp()
// would collide with fcm.js's default app if both ran in the same process).
const admin = require('firebase-admin');

const APP_NAME = 'staffApp';
let app;

function getMessaging() {
  if (!app) {
    const raw = process.env.STAFF_FIREBASE_SERVICE_ACCOUNT_JSON;
    if (!raw) {
      throw new Error('STAFF_FIREBASE_SERVICE_ACCOUNT_JSON environment variable is not set.');
    }
    let serviceAccount;
    try {
      serviceAccount = JSON.parse(raw);
    } catch (err) {
      throw new Error(
        `STAFF_FIREBASE_SERVICE_ACCOUNT_JSON is not valid JSON: ${err.message}. ` +
        `length=${raw.length} firstCharCode=${raw.charCodeAt(0)} lastCharCode=${raw.charCodeAt(raw.length - 1)}`
      );
    }
    const existing = admin.apps.find((a) => a?.name === APP_NAME);
    app = existing || admin.initializeApp({ credential: admin.credential.cert(serviceAccount) }, APP_NAME);
  }
  return admin.messaging(app);
}

// FCM caps multicast sends at 500 tokens per call.
const BATCH_SIZE = 500;

// See fcm.js's sendToTokens for why this is data-only.
async function sendToTokens(tokens, payload) {
  if (tokens.length === 0) {
    return { deadTokens: [] };
  }

  const messaging = getMessaging();
  const deadTokens = [];
  const data = Object.fromEntries(
    Object.entries(payload)
      .filter(([, value]) => value !== undefined && value !== null)
      .map(([key, value]) => [key, String(value)])
  );

  for (let i = 0; i < tokens.length; i += BATCH_SIZE) {
    const batch = tokens.slice(i, i + BATCH_SIZE);
    const response = await messaging.sendEachForMulticast({
      tokens: batch,
      data,
      android: { priority: 'high' }
    });
    response.responses.forEach((result, index) => {
      if (result.success) return;
      if (result.error?.code === 'messaging/registration-token-not-registered') {
        deadTokens.push(batch[index]);
      } else {
        // Anything else (e.g. a sender/project mismatch) previously failed
        // silently -- logged here so a bad token or credential shows up
        // instead of just "no notification arrived".
        console.error('staff FCM send failed', result.error?.code, result.error?.message);
      }
    });
  }

  return { deadTokens };
}

module.exports = { sendToTokens };
