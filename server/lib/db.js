const { Pool } = require('pg');

let pool;

// Reused across warm serverless invocations; a fresh one is created per cold start.
// max: 1 because Supabase's transaction pooler (port 6543) already pools connections
// upstream -- this function only ever needs one connection at a time.
function getPool() {
  if (!pool) {
    const connectionString = process.env.DATABASE_URL;
    if (!connectionString) {
      throw new Error('DATABASE_URL environment variable is not set.');
    }
    pool = new Pool({
      connectionString,
      ssl: { rejectUnauthorized: false },
      max: 1
    });
  }
  return pool;
}

module.exports = { getPool };
