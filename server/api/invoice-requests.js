const { getPool } = require('../lib/db');
const { normalizePhone } = require('../lib/phone');

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
  const normalizedPhone = normalizePhone(phone, countryCode);
  if (!normalizedPhone) {
    return res.status(400).json({ error: 'phone query param must be a 10-digit number' });
  }

  try {
    const pool = getPool();
    const result = await pool.query(
      `select id, description, status, created_at, updated_at, resolved_at
       from public.invoice_requests
       where phone = $1
       order by created_at desc`,
      [normalizedPhone]
    );
    return res.status(200).json({ requests: result.rows });
  } catch (err) {
    console.error('list invoice requests error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

async function handlePost(req, res) {
  const { phone, countryCode, description } = req.body ?? {};

  const normalizedPhone = normalizePhone(phone, countryCode);
  if (!normalizedPhone) {
    return res.status(400).json({ error: 'phone must be a 10-digit number' });
  }
  if (typeof description !== 'string' || description.trim().length === 0) {
    return res.status(400).json({ error: 'description is required' });
  }

  try {
    const pool = getPool();
    const userResult = await pool.query(`select 1 from public.users where phone = $1`, [normalizedPhone]);
    if (userResult.rowCount === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    const result = await pool.query(
      `insert into public.invoice_requests (phone, description)
       values ($1, $2)
       returning id, description, status, created_at, updated_at, resolved_at`,
      [normalizedPhone, description.trim()]
    );
    return res.status(201).json(result.rows[0]);
  } catch (err) {
    console.error('create invoice request error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}
