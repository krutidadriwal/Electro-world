const { getPool } = require('../../lib/db');
const { requireAdmin, StaffAuthError } = require('../../lib/staffAuth');

const VALID_ROLES = ['admin', 'employee'];

module.exports = async function handler(req, res) {
  if (req.method !== 'PATCH') {
    res.setHeader('Allow', 'PATCH');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { staffId, role } = req.body ?? {};
  if (typeof staffId !== 'string' || staffId.trim().length === 0) {
    return res.status(400).json({ error: 'staffId is required' });
  }
  if (!VALID_ROLES.includes(role)) {
    return res.status(400).json({ error: `role must be one of: ${VALID_ROLES.join(', ')}` });
  }

  try {
    const admin = await requireAdmin(req);
    if (staffId === admin.id) {
      return res.status(400).json({ error: 'You cannot change your own role' });
    }

    const pool = getPool();
    const result = await pool.query(
      `update public.staff_profiles set role = $1 where id = $2
       returning id, email, role, created_at`,
      [role, staffId]
    );
    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'Staff member not found' });
    }
    return res.status(200).json(result.rows[0]);
  } catch (err) {
    if (err instanceof StaffAuthError) {
      return res.status(err.status).json({ error: err.message });
    }
    console.error('update staff role error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
};
