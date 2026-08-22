const { getPool } = require('../lib/db');

const PHONE_REGEX = /^\d{10}$/;

module.exports = async function handler(req, res) {
  if (req.method === 'GET') {
    return handleGet(req, res);
  }
  if (req.method === 'POST') {
    return handlePost(req, res);
  }
  res.setHeader('Allow', 'GET, POST');
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
  const { name, phone, countryCode } = req.body ?? {};

  if (typeof name !== 'string' || name.trim().length === 0) {
    return res.status(400).json({ error: 'name is required' });
  }
  if (typeof phone !== 'string' || !PHONE_REGEX.test(phone)) {
    return res.status(400).json({ error: 'phone must be a 10-digit number' });
  }

  const normalizedPhone = `${typeof countryCode === 'string' ? countryCode : '+91'}${phone}`;

  try {
    const pool = getPool();
    const result = await pool.query(
      `insert into public.users (phone, name, last_login_at)
       values ($1, $2, now())
       on conflict (phone)
       do update set name = excluded.name, last_login_at = now()
       returning phone, name, created_at, last_login_at`,
      [normalizedPhone, name.trim()]
    );
    return res.status(200).json(result.rows[0]);
  } catch (err) {
    console.error('create/update user error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}
