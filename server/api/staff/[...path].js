// Combined into one function for the same reason as api/auth/[...path].js --
// staying under Vercel Hobby's 12-Serverless-Function cap. External URLs are
// unchanged (/api/staff/me, /api/staff/list, etc).

const { getPool } = require('../../lib/db');
const { getSupabaseAdmin } = require('../../lib/supabaseAdmin');
const { requireStaff, requireAdmin, StaffAuthError } = require('../../lib/staffAuth');

const VALID_ROLES = ['admin', 'employee'];

module.exports = async function handler(req, res) {
  const segments = Array.isArray(req.query.path) ? req.query.path : req.query.path ? [req.query.path] : [];
  const route = segments.join('/');

  switch (route) {
    case 'me':
      return me(req, res);
    case 'list':
      return list(req, res);
    case 'create':
      return create(req, res);
    case 'role':
      return role(req, res);
    default:
      return res.status(404).json({ error: 'Not found' });
  }
};

async function me(req, res) {
  if (req.method !== 'GET') {
    res.setHeader('Allow', 'GET');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  try {
    const staff = await requireStaff(req);
    return res.status(200).json({ id: staff.id, email: staff.email, role: staff.role });
  } catch (err) {
    if (err instanceof StaffAuthError) {
      return res.status(err.status).json({ error: err.message });
    }
    console.error('get staff me error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

async function list(req, res) {
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
}

async function create(req, res) {
  if (req.method !== 'POST') {
    res.setHeader('Allow', 'POST');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { email, password, role: newRole } = req.body ?? {};
  if (typeof email !== 'string' || !email.includes('@')) {
    return res.status(400).json({ error: 'A valid email is required' });
  }
  if (typeof password !== 'string' || password.length < 8) {
    return res.status(400).json({ error: 'password must be at least 8 characters' });
  }
  if (!VALID_ROLES.includes(newRole)) {
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
      [data.user.id, email.trim(), newRole, admin.id]
    );
    return res.status(201).json(result.rows[0]);
  } catch (err) {
    if (err instanceof StaffAuthError) {
      return res.status(err.status).json({ error: err.message });
    }
    console.error('create staff error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

async function role(req, res) {
  if (req.method !== 'PATCH') {
    res.setHeader('Allow', 'PATCH');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { staffId, role: newRole } = req.body ?? {};
  if (typeof staffId !== 'string' || staffId.trim().length === 0) {
    return res.status(400).json({ error: 'staffId is required' });
  }
  if (!VALID_ROLES.includes(newRole)) {
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
      [newRole, staffId]
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
}
