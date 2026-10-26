// One-off: copies the staff activity that was already being recorded
// elsewhere, from before staff_activity_log existed, into it so the staff
// app's Activity Log tab isn't empty on day one:
//   - wishlist calls          (wishlist_call_logs)
//   - price list shares       (price_share_log)
//   - spins unlocked          (spin_grants.granted_by)
//   - wishlist status changes (wishlist_items.status_updated_by -- only the
//                              latest change per item was ever kept)
//   - broadcasts sent         (notifications.created_by)
//   - staff accounts added    (staff_profiles.invited_by)
// Complaint/installation work, invoice requests and spin deliveries weren't
// recorded with a staff member before, so there's nothing to copy for them.
//
// Only copies activity from before the first live log entry, so it can be
// run after the new server is deployed without doubling anything up, and
// refuses to run a second time.
//
// Dry run by default -- prints what would be added and writes nothing:
//   node scripts/backfill-activity-log.js
// Then, to write it:
//   node scripts/backfill-activity-log.js --apply
//
// Needs DATABASE_URL, and schema.sql applied first.

const { Pool } = require('pg');
const { WISHLIST_STATUS_LABELS, categoryText, broadcastDescription } = require('../lib/activityLog');

const apply = process.argv.includes('--apply');

// Who to show for an entry whose staff member has since been deleted, when
// the old table didn't keep their email either.
const UNKNOWN_STAFF = 'Removed staff member';

const STAFF_NAME_SQL = `coalesce(nullif(sp.name, ''), sp.email, '${UNKNOWN_STAFF}')`;

async function collect(pool, cutoff) {
  const rows = [];

  const calls = await pool.query(
    `select wcl.staff_id, coalesce(nullif(sp.name, ''), sp.email, wcl.staff_email) as staff_name,
            wcl.customer_phone, wcl.customer_name, wcl.called_at as at,
            wi.category_name, wi.subcategory_name
     from public.wishlist_call_logs wcl
     left join public.staff_profiles sp on sp.id = wcl.staff_id
     left join public.wishlist_items wi on wi.id = wcl.wishlist_item_id
     where wcl.called_at < $1`,
    [cutoff]
  );
  for (const r of calls.rows) {
    rows.push({
      ...r,
      work_type: 'wishlist',
      description: r.category_name
        ? `called about ${categoryText(r.category_name, r.subcategory_name)} wishlist`
        : 'called about a wishlist item'
    });
  }

  const shares = await pool.query(
    `select psl.staff_id, ${STAFF_NAME_SQL} as staff_name,
            psl.customer_phone, psl.customer_name, psl.shared_at as at
     from public.price_share_log psl
     left join public.staff_profiles sp on sp.id = psl.staff_id
     where psl.shared_at < $1`,
    [cutoff]
  );
  for (const r of shares.rows) {
    rows.push({ ...r, work_type: 'price_share', description: 'shared prices' });
  }

  const spins = await pool.query(
    `select g.granted_by as staff_id, ${STAFF_NAME_SQL} as staff_name,
            g.phone as customer_phone, u.name as customer_name, g.created_at as at, g.bought_item
     from public.spin_grants g
     left join public.staff_profiles sp on sp.id = g.granted_by
     left join public.users u on u.phone = g.phone
     where g.granted_by is not null and g.created_at < $1`,
    [cutoff]
  );
  for (const r of spins.rows) {
    rows.push({ ...r, work_type: 'spin', description: `unlocked a spin for buying ${r.bought_item}` });
  }

  const statuses = await pool.query(
    `select wi.status_updated_by as staff_id, ${STAFF_NAME_SQL} as staff_name,
            wi.phone as customer_phone, u.name as customer_name, wi.status_updated_at as at,
            wi.category_name, wi.subcategory_name, wi.status
     from public.wishlist_items wi
     left join public.staff_profiles sp on sp.id = wi.status_updated_by
     left join public.users u on u.phone = wi.phone
     where wi.status_updated_by is not null and wi.status_updated_at < $1`,
    [cutoff]
  );
  for (const r of statuses.rows) {
    rows.push({
      ...r,
      work_type: 'wishlist',
      description: `changed ${categoryText(r.category_name, r.subcategory_name)} wishlist to ${WISHLIST_STATUS_LABELS[r.status] ?? r.status}`
    });
  }

  const broadcasts = await pool.query(
    `select n.created_by as staff_id, ${STAFF_NAME_SQL} as staff_name, n.created_at as at, n.message
     from public.notifications n
     left join public.staff_profiles sp on sp.id = n.created_by
     where n.created_by is not null and n.created_at < $1`,
    [cutoff]
  );
  for (const r of broadcasts.rows) {
    rows.push({ ...r, work_type: 'broadcast', description: broadcastDescription(r.message) });
  }

  const invites = await pool.query(
    `select s.invited_by as staff_id, ${STAFF_NAME_SQL} as staff_name, s.created_at as at, s.email, s.role
     from public.staff_profiles s
     left join public.staff_profiles sp on sp.id = s.invited_by
     where s.invited_by is not null and s.created_at < $1`,
    [cutoff]
  );
  for (const r of invites.rows) {
    rows.push({ ...r, work_type: 'staff', description: `added ${r.email} as ${r.role}` });
  }

  return rows.sort((a, b) => a.at - b.at);
}

async function main() {
  const connectionString = process.env.DATABASE_URL;
  if (!connectionString) {
    throw new Error('DATABASE_URL environment variable is not set.');
  }
  const pool = new Pool({ connectionString, ssl: { rejectUnauthorized: false } });

  try {
    const already = await pool.query(`select 1 from public.staff_activity_log where backfilled limit 1`);
    if (already.rowCount > 0) {
      console.log('Already backfilled -- nothing to do.');
      return;
    }

    const firstLive = await pool.query(`select min(created_at) as at from public.staff_activity_log`);
    const cutoff = firstLive.rows[0].at ?? new Date();
    console.log(`Copying activity from before ${cutoff.toISOString()}.`);

    const rows = await collect(pool, cutoff);
    const counts = {};
    for (const r of rows) counts[r.work_type] = (counts[r.work_type] ?? 0) + 1;
    console.log(`${rows.length} entries:`, counts);
    for (const r of rows.slice(-10)) {
      console.log(`  ${r.at.toISOString()}  ${r.staff_name} ${r.description}${r.customer_phone ? `  [${r.customer_phone}]` : ''}`);
    }

    if (!apply) {
      console.log('\nDry run -- nothing written. Re-run with --apply to write.');
      return;
    }

    const client = await pool.connect();
    try {
      await client.query('begin');
      await client.query(
        `insert into public.staff_activity_log
           (staff_id, staff_name, work_type, description, customer_phone, customer_name, created_at, backfilled)
         select * , true
         from unnest($1::uuid[], $2::text[], $3::text[], $4::text[], $5::text[], $6::text[], $7::timestamptz[])`,
        [
          rows.map((r) => r.staff_id ?? null),
          rows.map((r) => r.staff_name),
          rows.map((r) => r.work_type),
          rows.map((r) => r.description),
          rows.map((r) => r.customer_phone ?? null),
          rows.map((r) => r.customer_name ?? null),
          rows.map((r) => r.at)
        ]
      );
      await client.query('commit');
      console.log(`\nWrote ${rows.length} entries.`);
    } catch (err) {
      await client.query('rollback').catch(() => {});
      throw err;
    } finally {
      client.release();
    }
  } finally {
    await pool.end();
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
