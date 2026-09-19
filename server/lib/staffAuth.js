// Verifies the staff app's Supabase-issued access token and loads the
// caller's role from public.staff_profiles. Supabase mints access tokens as
// HS256 JWTs signed with the project's JWT secret, so verifying one needs no
// network call -- unlike getSupabaseAdmin(), which is only for privileged
// actions (creating/listing staff).

const jwt = require('jsonwebtoken');
const { getPool } = require('./db');

class StaffAuthError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

function getSupabaseJwtSecret() {
  const secret = process.env.SUPABASE_JWT_SECRET;
  if (!secret) {
    throw new Error('SUPABASE_JWT_SECRET environment variable is not set.');
  }
  return secret;
}

function extractBearerToken(req) {
  const header = req.headers?.authorization;
  if (typeof header !== 'string' || !header.startsWith('Bearer ')) {
    return null;
  }
  return header.slice('Bearer '.length).trim();
}

// Returns the caller's { id, email, role } from staff_profiles, or throws a
// StaffAuthError (401 for missing/invalid token or unknown staff id, 403 if
// minRole is 'admin' and the caller is an employee).
async function requireStaff(req, { minRole } = {}) {
  const token = extractBearerToken(req);
  if (!token) {
    throw new StaffAuthError(401, 'Missing bearer token');
  }

  let payload;
  try {
    payload = jwt.verify(token, getSupabaseJwtSecret(), { algorithms: ['HS256'] });
  } catch (err) {
    throw new StaffAuthError(401, 'Invalid or expired token');
  }

  const pool = getPool();
  const result = await pool.query(
    'select id, email, role from public.staff_profiles where id = $1',
    [payload.sub]
  );
  if (result.rowCount === 0) {
    throw new StaffAuthError(401, 'No staff account for this token');
  }

  const staff = result.rows[0];
  if (minRole === 'admin' && staff.role !== 'admin') {
    throw new StaffAuthError(403, 'Admin role required');
  }
  return staff;
}

function requireAdmin(req) {
  return requireStaff(req, { minRole: 'admin' });
}

module.exports = { requireStaff, requireAdmin, StaffAuthError };
