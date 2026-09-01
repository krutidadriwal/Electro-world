const jwt = require('jsonwebtoken');

const SESSION_EXPIRY = '30d';

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

// Returns the decoded { phone } payload, or null if the token is missing/invalid/expired.
function verifySession(token) {
  if (typeof token !== 'string' || token.length === 0) {
    return null;
  }
  try {
    return jwt.verify(token, getSecret());
  } catch (err) {
    return null;
  }
}

module.exports = { signSession, verifySession };
