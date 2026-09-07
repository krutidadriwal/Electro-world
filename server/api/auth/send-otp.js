const crypto = require('crypto');
const { getPool } = require('../../lib/db');
const { normalizePhone } = require('../../lib/phone');
const { sendSms } = require('../../lib/textbee');
const bcrypt = require('bcryptjs');

const CODE_EXPIRY_MS = 5 * 60 * 1000;
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

    // One OTP per phone per calendar day (IST). This single statement is
    // atomic (the unique constraint on phone serializes concurrent requests
    // for the same number), so it both enforces and records the limit --
    // send_count coming back > 1 means today's row already existed before
    // this request, i.e. the limit was already used.
    const limitResult = await pool.query(
      `insert into public.otp_daily_limits (phone, send_date, send_count)
       values ($1, (now() at time zone 'Asia/Kolkata')::date, 1)
       on conflict (phone) do update set
         send_count = case
           when otp_daily_limits.send_date = (now() at time zone 'Asia/Kolkata')::date
             then otp_daily_limits.send_count + 1
           else 1
         end,
         send_date = (now() at time zone 'Asia/Kolkata')::date
       returning send_count`,
      [normalizedPhone]
    );
    if (limitResult.rows[0].send_count > 1) {
      return res.status(429).json({ error: 'You have already requested an OTP today. Please try again tomorrow.' });
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

    try {
      await sendSms(normalizedPhone, `Your Electro World code is ${code}. It expires in 5 minutes.`);
    } catch (smsErr) {
      // The SMS never actually went out, so don't let this attempt count
      // against today's limit -- otherwise a transient gateway failure
      // would lock the user out for the rest of the day.
      await pool.query(
        `update public.otp_daily_limits set send_count = send_count - 1
         where phone = $1 and send_date = (now() at time zone 'Asia/Kolkata')::date`,
        [normalizedPhone]
      );
      throw smsErr;
    }

    return res.status(200).json({ sent: true });
  } catch (err) {
    console.error('send-otp error', err);
    return res.status(500).json({ error: 'Unable to send verification code. Please try again.' });
  }
};
