const { getPool } = require('../../lib/db');
const { normalizePhone } = require('../../lib/phone');
const { signOtpVerification } = require('../../lib/jwt');
const bcrypt = require('bcryptjs');

const CODE_REGEX = /^\d{6}$/;
const MAX_ATTEMPTS = 5;

module.exports = async function handler(req, res) {
  if (req.method !== 'POST') {
    res.setHeader('Allow', 'POST');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { phone, countryCode, code } = req.body ?? {};
  const normalizedPhone = normalizePhone(phone, countryCode);
  if (!normalizedPhone) {
    return res.status(400).json({ error: 'phone must be a 10-digit number' });
  }
  if (typeof code !== 'string' || !CODE_REGEX.test(code)) {
    return res.status(400).json({ error: 'code must be a 6-digit number' });
  }

  try {
    const pool = getPool();
    const result = await pool.query(
      'select code_hash, expires_at, attempts from public.otp_codes where phone = $1',
      [normalizedPhone]
    );

    if (result.rowCount === 0) {
      return res.status(400).json({ error: 'No verification code was requested for this number.' });
    }

    const row = result.rows[0];
    if (new Date(row.expires_at).getTime() < Date.now()) {
      await pool.query('delete from public.otp_codes where phone = $1', [normalizedPhone]);
      return res.status(400).json({ error: 'This code has expired. Please request a new one.' });
    }
    if (row.attempts >= MAX_ATTEMPTS) {
      return res.status(429).json({ error: 'Too many incorrect attempts. Please request a new code.' });
    }

    const codeMatches = await bcrypt.compare(code, row.code_hash);
    if (!codeMatches) {
      await pool.query('update public.otp_codes set attempts = attempts + 1 where phone = $1', [normalizedPhone]);
      return res.status(401).json({ error: 'Incorrect verification code.' });
    }

    await pool.query('delete from public.otp_codes where phone = $1', [normalizedPhone]);

    return res.status(200).json({ verificationToken: signOtpVerification(normalizedPhone) });
  } catch (err) {
    console.error('verify-otp error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
};
