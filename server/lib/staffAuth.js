// Verifies the staff app's Supabase-issued access token and loads the
// caller's role from public.staff_profiles.
//
// This project's Supabase instance signs access tokens asymmetrically
// (ES256, confirmed from a real token's header: {"alg":"ES256",...}) rather
// than with the older shared HS256 JWT secret -- so verification has to go
// through Supabase's published JWKS (its public keys), not a local secret.
// jose's createRemoteJWKSet handles fetching + caching those keys (and
// re-fetching on an unrecognized "kid") for us.

const { createRemoteJWKSet, jwtVerify } = require('jose');
const { getPool } = require('./db');

class StaffAuthError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

let jwks;

function getJwks() {
  if (!jwks) {
    const supabaseUrl = process.env.SUPABASE_URL;
    if (!supabaseUrl) {
      throw new Error('SUPABASE_URL environment variable is not set.');
    }
    jwks = createRemoteJWKSet(new URL(`${supabaseUrl}/auth/v1/.well-known/jwks.json`));
  }
  return jwks;
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
    ({ payload } = await jwtVerify(token, getJwks()));
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
