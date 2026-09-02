const jwt = require('jsonwebtoken');

const SESSION_EXPIRY = '30d';
const OTP_VERIFICATION_EXPIRY = '10m';
const OTP_VERIFICATION_PURPOSE = 'otp_verified';

function getSecret() {
  const secret = process.env.JWT_SECRET;
  if (!secret) {
    throw new Error('JWT_SECRET environment variable is not set.');
  }
  return secret;
}

function signSession(phone) {
  return jwt.sign({ phone }, getSecret(), { expiresIn: SESSION_EXPIRY });
}

// Returns the decoded { phone } payload, or null if the token is missing/invalid/expired,
// or if it's actually a purpose-tagged token of a different kind (e.g. an OTP-verification token).
function verifySession(token) {
  if (typeof token !== 'string' || token.length === 0) {
    return null;
  }
  try {
    const decoded = jwt.verify(token, getSecret());
    return decoded.purpose === undefined ? decoded : null;
  } catch (err) {
    return null;
  }
}

// Proof that a phone number just completed OTP verification (see
// /api/auth/verify-otp), handed to /api/auth/set-pin in place of the phone
// number itself. Short-lived and purpose-tagged so it can't be reused as a
// session token or vice versa.
function signOtpVerification(phone) {
  return jwt.sign({ phone, purpose: OTP_VERIFICATION_PURPOSE }, getSecret(), { expiresIn: OTP_VERIFICATION_EXPIRY });
}

// Returns the decoded { phone } payload, or null if the token is missing/invalid/expired/wrong-purpose.
function verifyOtpVerification(token) {
  if (typeof token !== 'string' || token.length === 0) {
    return null;
  }
  try {
    const decoded = jwt.verify(token, getSecret());
    return decoded.purpose === OTP_VERIFICATION_PURPOSE ? decoded : null;
  } catch (err) {
    return null;
  }
}

module.exports = { signSession, verifySession, signOtpVerification, verifyOtpVerification };
