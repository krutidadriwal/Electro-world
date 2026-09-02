const crypto = require('crypto');
const { getPool } = require('../../lib/db');
const { normalizePhone } = require('../../lib/phone');
const { sendSms } = require('../../lib/textbee');
const bcrypt = require('bcryptjs');

const CODE_EXPIRY_MS = 5 * 60 * 1000;
const RESEND_COOLDOWN_MS = 30 * 1000;
const SALT_ROUNDS = 10;

module.exports = async function handler(req, res) {
  if (req.method !== 'POST') {
    res.setHeader('Allow', 'POST');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { phone, countryCode } = req.body ?? {};
  const normalizedPhone = normalizePhone(phone, countryCode);
  if (!normalizedPhone) {
    return res.status(400).json({ error: 'phone must be a 10-digit number' });
  }

  try {
    const pool = getPool();

    const existing = await pool.query(
      'select created_at from public.otp_codes where phone = $1',
      [normalizedPhone]
    );
    if (existing.rowCount > 0) {
      const ageMs = Date.now() - new Date(existing.rows[0].created_at).getTime();
      if (ageMs < RESEND_COOLDOWN_MS) {
        return res.status(429).json({ error: 'Please wait before requesting another code.' });
      }
    }

    const code = crypto.randomInt(100000, 1000000).toString();
    const codeHash = await bcrypt.hash(code, await bcrypt.genSalt(SALT_ROUNDS));
    const expiresAt = new Date(Date.now() + CODE_EXPIRY_MS);

    await pool.query(
      `insert into public.otp_codes (phone, code_hash, expires_at, attempts, created_at)
       values ($1, $2, $3, 0, now())
       on conflict (phone) do update set
         code_hash = excluded.code_hash,
         expires_at = excluded.expires_at,
         attempts = 0,
         created_at = now()`,
      [normalizedPhone, codeHash, expiresAt]
    );

    await sendSms(normalizedPhone, `Your Electro World verification code is ${code}. It expires in 5 minutes.`);

    return res.status(200).json({ sent: true });
  } catch (err) {
    console.error('send-otp error', err);
    return res.status(500).json({ error: 'Unable to send verification code. Please try again.' });
  }
};
