const admin = require('firebase-admin');

let app;

// Reused across warm serverless invocations, mirroring the lazy pool in db.js.
function getApp() {
  if (!app) {
    const projectId = process.env.FIREBASE_PROJECT_ID;
    const serviceAccountJson = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
    if (!projectId || !serviceAccountJson) {
      throw new Error('FIREBASE_PROJECT_ID / FIREBASE_SERVICE_ACCOUNT_JSON environment variables are not set.');
    }
    app = admin.initializeApp({
      credential: admin.credential.cert(JSON.parse(serviceAccountJson)),
      projectId
    });
  }
  return app;
}

// Verifies a Firebase Phone Auth ID token and returns its decoded claims,
// including phone_number in E.164 format (e.g. +919876543210).
async function verifyIdToken(idToken) {
  return admin.auth(getApp()).verifyIdToken(idToken);
}

module.exports = { verifyIdToken };
