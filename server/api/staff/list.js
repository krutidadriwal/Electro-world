const { getPool } = require('../../lib/db');
const { requireAdmin, StaffAuthError } = require('../../lib/staffAuth');

module.exports = async function handler(req, res) {
  if (req.method !== 'GET') {
    res.setHeader('Allow', 'GET');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  try {
    await requireAdmin(req);

    const pool = getPool();
    const result = await pool.query(
      `select id, email, role, created_at from public.staff_profiles order by created_at desc`
    );
    return res.status(200).json({ staff: result.rows });
  } catch (err) {
    if (err instanceof StaffAuthError) {
      return res.status(err.status).json({ error: err.message });
    }
    console.error('list staff error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
};
