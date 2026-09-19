const { getPool } = require('../../lib/db');
const { getSupabaseAdmin } = require('../../lib/supabaseAdmin');
const { requireAdmin, StaffAuthError } = require('../../lib/staffAuth');

const VALID_ROLES = ['admin', 'employee'];

module.exports = async function handler(req, res) {
  if (req.method !== 'POST') {
    res.setHeader('Allow', 'POST');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { email, password, role } = req.body ?? {};
  if (typeof email !== 'string' || !email.includes('@')) {
    return res.status(400).json({ error: 'A valid email is required' });
  }
  if (typeof password !== 'string' || password.length < 8) {
    return res.status(400).json({ error: 'password must be at least 8 characters' });
  }
  if (!VALID_ROLES.includes(role)) {
    return res.status(400).json({ error: `role must be one of: ${VALID_ROLES.join(', ')}` });
  }

  try {
    const admin = await requireAdmin(req);

    const supabase = getSupabaseAdmin();
    const { data, error } = await supabase.auth.admin.createUser({
      email: email.trim(),
      password,
      email_confirm: true
    });
    if (error) {
      return res.status(400).json({ error: error.message });
    }

    const pool = getPool();
    const result = await pool.query(
      `insert into public.staff_profiles (id, email, role, invited_by)
       values ($1, $2, $3, $4)
       returning id, email, role, created_at`,
      [data.user.id, email.trim(), role, admin.id]
    );
    return res.status(201).json(result.rows[0]);
  } catch (err) {
    if (err instanceof StaffAuthError) {
      return res.status(err.status).json({ error: err.message });
    }
    console.error('create staff error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
};
