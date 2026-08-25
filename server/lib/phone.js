const PHONE_REGEX = /^\d{10}$/;

function normalizePhone(phone, countryCode) {
  if (typeof phone !== 'string' || !PHONE_REGEX.test(phone)) {
    return null;
  }
  return `${typeof countryCode === 'string' ? countryCode : '+91'}${phone}`;
}

module.exports = { PHONE_REGEX, normalizePhone };
