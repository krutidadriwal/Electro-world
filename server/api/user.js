const { getPool } = require('../lib/db');
// trial one for deployment
const PHONE_REGEX = /^\d{10}$/;

module.exports = async function handler(req, res) {
  if (req.method === 'GET') {
    return handleGet(req, res);
  }
  if (req.method === 'POST') {
    return handlePost(req, res);
  }
  if (req.method === 'PATCH') {
    return handlePatch(req, res);
  }
  if (req.method === 'DELETE') {
    return handleDelete(req, res);
  }
  res.setHeader('Allow', 'GET, POST, PATCH, DELETE');
  return res.status(405).json({ error: 'Method not allowed' });
};

async function handleGet(req, res) {
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

async function handlePost(req, res) {
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
async function handlePatch(req, res) {
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
async function handleDelete(req, res) {
  const { phone, countryCode } = req.body ?? {};

  if (typeof phone !== 'string' || !PHONE_REGEX.test(phone)) {
    return res.status(400).json({ error: 'phone must be a 10-digit number' });
  }

  const normalizedPhone = `${typeof countryCode === 'string' ? countryCode : '+91'}${phone}`;

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
