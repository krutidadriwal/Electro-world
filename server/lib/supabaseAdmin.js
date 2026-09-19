const { createClient } = require('@supabase/supabase-js');

let client;

// Server-only client using the service role key -- bypasses RLS and can
// manage auth users (create staff accounts). Never expose this key to
// either Android app.
function getSupabaseAdmin() {
  if (!client) {
    const url = process.env.SUPABASE_URL;
    const serviceRoleKey = process.env.SUPABASE_SERVICE_ROLE_KEY;
    if (!url || !serviceRoleKey) {
      throw new Error('SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY environment variables must be set.');
    }
    client = createClient(url, serviceRoleKey, {
      auth: { autoRefreshToken: false, persistSession: false }
    });
  }
  return client;
}

module.exports = { getSupabaseAdmin };
