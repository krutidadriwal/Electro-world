// Pushes a staff-app alert (new wishlist/complaint/installation/invoice-
// request) to staff device tokens, via the staff app's OWN Firebase project
// (see staffFcm.js) -- registered separately from the customer app's, so it
// needs its own service account/credentials, not fcm.js's. Best-effort:
// called after the triggering row is already committed, so a push failure
// here is logged and swallowed rather than failing the request that created it.
const { getPool } = require('./db');
const { sendToTokens } = require('./staffFcm');

// target is an opaque string the staff app's notification tap handler
// understands, e.g. "wishlist:not_contacted", "complaints:open",
// "installations:open", "invoice_requests:open".
async function notifyStaff({ adminOnly = false, title, body, target }) {
  try {
    const pool = getPool();
    const result = adminOnly
      ? await pool.query(
          `select sdt.fcm_token from public.staff_device_tokens sdt
           join public.staff_profiles sp on sp.id = sdt.staff_id
           where sp.role = 'admin'`
        )
      : await pool.query(`select fcm_token from public.staff_device_tokens`);

    const tokens = result.rows.map((row) => row.fcm_token);
    if (tokens.length === 0) return;

    const { deadTokens } = await sendToTokens(tokens, { title, body, target });
    if (deadTokens.length > 0) {
      await pool.query('delete from public.staff_device_tokens where fcm_token = any($1)', [deadTokens]);
    }
  } catch (err) {
    console.error('notifyStaff error', err);
  }
}

module.exports = { notifyStaff };
