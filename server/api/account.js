// Combined into one function to stay under Vercel Hobby's 12-serverless-
// function cap (see server/vercel.json) -- /api/device-tokens is rewritten
// here with ?resource=device-tokens appended, everything else (the bare
// /api/account path, which nothing calls directly) falls through to user.
// External URLs are unchanged: /api/user, /api/device-tokens.

const { getPool } = require('../lib/db');
const { normalizePhone } = require('../lib/phone');
const { verifySession } = require('../lib/jwt');
const spin = require('../lib/spin');

const PHONE_REGEX = /^\d{10}$/;

module.exports = async function handler(req, res) {
  if (req.query.resource === 'device-tokens') {
    return deviceTokens(req, res);
  }
  if (req.query.resource === 'points') {
    return points(req, res);
  }
  if (req.query.resource === 'spin') {
    return spin(req, res);
  }
  return user(req, res);
};

async function user(req, res) {
  if (req.method === 'GET') {
    return userGet(req, res);
  }
  if (req.method === 'POST') {
    return userPost(req, res);
  }
  if (req.method === 'PATCH') {
    return userPatch(req, res);
  }
  if (req.method === 'DELETE') {
    return userDelete(req, res);
  }
  res.setHeader('Allow', 'GET, POST, PATCH, DELETE');
  return res.status(405).json({ error: 'Method not allowed' });
}

async function userGet(req, res) {
  const { phone, countryCode } = req.query;
  if (typeof phone !== 'string' || !PHONE_REGEX.test(phone)) {
    return res.status(400).json({ error: 'phone query param must be a 10-digit number' });
  }

  const normalizedPhone = `${typeof countryCode === 'string' ? countryCode : '+91'}${phone}`;

  try {
    const pool = getPool();
    const result = await pool.query(
      `select phone, name, created_at, last_login_at from public.users where phone = $1`,
      [normalizedPhone]
    );
    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'User not found' });
    }
    return res.status(200).json(result.rows[0]);
  } catch (err) {
    console.error('get user error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

async function userPost(req, res) {
  const { name, phone, countryCode, howHeardAboutUs } = req.body ?? {};

  if (typeof name !== 'string' || name.trim().length === 0) {
    return res.status(400).json({ error: 'name is required' });
  }
  if (typeof phone !== 'string' || !PHONE_REGEX.test(phone)) {
    return res.status(400).json({ error: 'phone must be a 10-digit number' });
  }

  const normalizedPhone = `${typeof countryCode === 'string' ? countryCode : '+91'}${phone}`;
  const normalizedHowHeard = typeof howHeardAboutUs === 'string' && howHeardAboutUs.trim().length > 0
    ? howHeardAboutUs.trim()
    : null;

  try {
    const pool = getPool();
    const result = await pool.query(
      `insert into public.users (phone, name, how_heard_about_us, last_login_at)
       values ($1, $2, $3, now())
       on conflict (phone)
       do update set name = excluded.name, last_login_at = now()
       returning phone, name, created_at, last_login_at`,
      [normalizedPhone, name.trim(), normalizedHowHeard]
    );
    return res.status(200).json(result.rows[0]);
  } catch (err) {
    console.error('create/update user error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

// Edit Profile: name is the only field the app currently lets a signed-in
// user change (phone is the login identifier and PIN has its own OTP-backed
// flow, so neither belongs here).
async function userPatch(req, res) {
  const { phone, countryCode, name } = req.body ?? {};

  if (typeof phone !== 'string' || !PHONE_REGEX.test(phone)) {
    return res.status(400).json({ error: 'phone must be a 10-digit number' });
  }
  if (typeof name !== 'string' || name.trim().length === 0) {
    return res.status(400).json({ error: 'name is required' });
  }

  const normalizedPhone = `${typeof countryCode === 'string' ? countryCode : '+91'}${phone}`;

  try {
    const pool = getPool();
    const result = await pool.query(
      `update public.users set name = $2 where phone = $1
       returning phone, name, created_at, last_login_at`,
      [normalizedPhone, name.trim()]
    );
    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'User not found' });
    }
    return res.status(200).json(result.rows[0]);
  } catch (err) {
    console.error('update user error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

// Delete Account: removes the user row outright. complaints/wishlist_items/
// installations reference users(phone) with ON DELETE CASCADE (see
// schema.sql), so this also clears the user's history in those tables.
// Their invoice PDFs on Google Drive are untouched -- deleting arbitrary
// files on someone's connected Drive from an account-deletion request is a
// separate, deliberate decision, not a side effect.
//
// Requires the caller's own session token (set at login/signup) so a
// request can only ever delete the account it's authenticated as -- phone
// alone is not proof of ownership.
async function userDelete(req, res) {
  const { phone, countryCode } = req.body ?? {};

  if (typeof phone !== 'string' || !PHONE_REGEX.test(phone)) {
    return res.status(400).json({ error: 'phone must be a 10-digit number' });
  }

  const authHeader = req.headers.authorization ?? '';
  const token = authHeader.startsWith('Bearer ') ? authHeader.slice(7) : null;
  const session = verifySession(token);
  if (!session) {
    return res.status(401).json({ error: 'Missing or invalid session token' });
  }

  const normalizedPhone = `${typeof countryCode === 'string' ? countryCode : '+91'}${phone}`;

  if (session.phone !== normalizedPhone) {
    return res.status(403).json({ error: 'Session does not match the account being deleted' });
  }

  try {
    const pool = getPool();
    const result = await pool.query('delete from public.users where phone = $1', [normalizedPhone]);
    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'User not found' });
    }
    return res.status(200).json({ deleted: true });
  } catch (err) {
    console.error('delete user error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

async function points(req, res) {
  if (req.method !== 'GET') {
    res.setHeader('Allow', 'GET');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { phone, countryCode } = req.query;
  if (typeof phone !== 'string' || !PHONE_REGEX.test(phone)) {
    return res.status(400).json({ error: 'phone query param must be a 10-digit number' });
  }

  const normalizedPhone = `${typeof countryCode === 'string' ? countryCode : '+91'}${phone}`;

  try {
    const pool = getPool();
    const balanceResult = await pool.query(
      `select coalesce(sum(points), 0)::int as balance from public.points_ledger where phone = $1`,
      [normalizedPhone]
    );
    const historyResult = await pool.query(
      `select id, points, reason, created_at from public.points_ledger where phone = $1 order by created_at desc limit 50`,
      [normalizedPhone]
    );
    return res.status(200).json({ balance: balanceResult.rows[0].balance, history: historyResult.rows });
  } catch (err) {
    console.error('get points error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

async function deviceTokens(req, res) {
  if (req.method !== 'POST') {
    res.setHeader('Allow', 'POST');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { phone, countryCode, fcmToken } = req.body ?? {};
  const normalizedPhone = normalizePhone(phone, countryCode);
  if (!normalizedPhone) {
    return res.status(400).json({ error: 'phone must be a 10-digit number' });
  }
  if (typeof fcmToken !== 'string' || fcmToken.trim().length === 0) {
    return res.status(400).json({ error: 'fcmToken is required' });
  }

  try {
    const pool = getPool();
    const userResult = await pool.query('select 1 from public.users where phone = $1', [normalizedPhone]);
    if (userResult.rowCount === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    await pool.query(
      `insert into public.device_tokens (phone, fcm_token)
       values ($1, $2)
       on conflict (fcm_token) do update set phone = excluded.phone, updated_at = now()`,
      [normalizedPhone, fcmToken.trim()]
    );
    return res.status(204).end();
  } catch (err) {
    console.error('register device token error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}
