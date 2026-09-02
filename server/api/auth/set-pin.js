const { getPool } = require('../../lib/db');
const { verifyOtpVerification, signSession } = require('../../lib/jwt');
const bcrypt = require('bcryptjs');

const PIN_REGEX = /^\d{4}$/;
const SALT_ROUNDS = 10;

// Finishes both signup (new phone, name required) and forgot-PIN (existing
// phone, name optional) -- in both cases the caller has just proven phone
// ownership via /api/auth/verify-otp and is handing us the resulting
// verification token instead of the raw phone number.
module.exports = async function handler(req, res) {
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
};
