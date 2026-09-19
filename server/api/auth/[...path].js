// Vercel's Hobby plan caps a deployment at 12 Serverless Functions, and each
// file under api/ counts as one -- so the four auth endpoints (each of which
// used to be its own file) are combined into a single catch-all function
// here. External URLs are unchanged (/api/auth/login, /api/auth/send-otp,
// etc.) -- Vercel's [...path] dynamic route just means this one function now
// handles all of them, with req.query.path giving the matched segments.

const crypto = require('crypto');
const bcrypt = require('bcryptjs');
const { getPool } = require('../../lib/db');
const { normalizePhone } = require('../../lib/phone');
const { sendSms } = require('../../lib/textbee');
const { signSession, signOtpVerification, verifyOtpVerification } = require('../../lib/jwt');

const SALT_ROUNDS = 10;
const CODE_EXPIRY_MS = 5 * 60 * 1000;
const CODE_REGEX = /^\d{6}$/;
const PIN_REGEX = /^\d{4}$/;
const MAX_OTP_ATTEMPTS = 5;

module.exports = async function handler(req, res) {
  const segments = Array.isArray(req.query.path) ? req.query.path : req.query.path ? [req.query.path] : [];
  const route = segments.join('/');

  switch (route) {
    case 'login':
      return login(req, res);
    case 'send-otp':
      return sendOtp(req, res);
    case 'verify-otp':
      return verifyOtp(req, res);
    case 'set-pin':
      return setPin(req, res);
    default:
      return res.status(404).json({ error: 'Not found' });
  }
};

async function login(req, res) {
  if (req.method !== 'POST') {
    res.setHeader('Allow', 'POST');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { phone, countryCode, pin } = req.body ?? {};
  const normalizedPhone = normalizePhone(phone, countryCode);
  if (!normalizedPhone) {
    return res.status(400).json({ error: 'phone must be a 10-digit number' });
  }
  if (typeof pin !== 'string' || pin.length === 0) {
    return res.status(400).json({ error: 'pin is required' });
  }

  try {
    const pool = getPool();
    const result = await pool.query(
      'select phone, name, pin_hash from public.users where phone = $1',
      [normalizedPhone]
    );
    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'No account found with this number' });
    }

    const user = result.rows[0];
    if (!user.pin_hash) {
      return res.status(400).json({ error: 'This account has not set up a PIN yet' });
    }

    const pinMatches = await bcrypt.compare(pin, user.pin_hash);
    if (!pinMatches) {
      return res.status(401).json({ error: 'Incorrect PIN' });
    }

    await pool.query('update public.users set last_login_at = now() where phone = $1', [normalizedPhone]);

    return res.status(200).json({ token: signSession(user.phone), phone: user.phone, name: user.name });
  } catch (err) {
    console.error('login error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

async function sendOtp(req, res) {
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
}

async function verifyOtp(req, res) {
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
    if (row.attempts >= MAX_OTP_ATTEMPTS) {
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
}

// Finishes both signup (new phone, name required) and forgot-PIN (existing
// phone, name optional) -- in both cases the caller has just proven phone
// ownership via verify-otp and is handing us the resulting verification
// token instead of the raw phone number.
async function setPin(req, res) {
  if (req.method !== 'POST') {
    res.setHeader('Allow', 'POST');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { verificationToken, pin, name, howHeardAboutUs } = req.body ?? {};

  if (typeof verificationToken !== 'string' || verificationToken.length === 0) {
    return res.status(400).json({ error: 'verificationToken is required' });
  }
  if (typeof pin !== 'string' || !PIN_REGEX.test(pin)) {
    return res.status(400).json({ error: 'pin must be a 4-digit number' });
  }

  const decoded = verifyOtpVerification(verificationToken);
  if (!decoded) {
    return res.status(401).json({ error: 'Invalid or expired verification code' });
  }
  const phone = decoded.phone;

  const trimmedName = typeof name === 'string' && name.trim().length > 0 ? name.trim() : null;
  const normalizedHowHeard = typeof howHeardAboutUs === 'string' && howHeardAboutUs.trim().length > 0
    ? howHeardAboutUs.trim()
    : null;

  const salt = await bcrypt.genSalt(SALT_ROUNDS);
  const pinHash = await bcrypt.hash(pin, salt);

  try {
    const pool = getPool();
    const existing = await pool.query('select phone from public.users where phone = $1', [phone]);

    let result;
    if (existing.rowCount === 0) {
      if (!trimmedName) {
        return res.status(400).json({ error: 'name is required to create an account' });
      }
      result = await pool.query(
        `insert into public.users (phone, name, how_heard_about_us, pin_hash, pin_salt, last_login_at)
         values ($1, $2, $3, $4, $5, now())
         returning phone, name`,
        [phone, trimmedName, normalizedHowHeard, pinHash, salt]
      );
    } else {
      result = await pool.query(
        `update public.users
         set pin_hash = $2, pin_salt = $3, last_login_at = now(),
             name = coalesce($4, name),
             how_heard_about_us = coalesce($5, how_heard_about_us)
         where phone = $1
         returning phone, name`,
        [phone, pinHash, salt, trimmedName, normalizedHowHeard]
      );
    }

    const user = result.rows[0];
    return res.status(200).json({ token: signSession(user.phone), phone: user.phone, name: user.name });
  } catch (err) {
    console.error('set-pin error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}
