// Spin to Win -- staff side. Dispatched from api/staff/[...path].js as
// single-segment routes (spin-overview, spin-prize, spin-purchase-item,
// spin-grant), see the note there on why they can't be nested. The customer
// side (reading state, actually spinning) is lib/spin.js.
//
// Ported from the old standalone spinner's admin.html + add-number.html.
// Everything here is admin-only for now.

const { getPool } = require('./db');
const { requireAdmin, StaffAuthError } = require('./staffAuth');
const { normalizePhone } = require('./phone');
const { sendToTokens } = require('./fcm');
const { logActivity } = require('./activityLog');

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const UNIQUE_VIOLATION = '23505';
const GRANT_LOG_LIMIT = 200;

function isUuid(value) {
  return typeof value === 'string' && UUID_REGEX.test(value);
}

function trimmedName(value) {
  return typeof value === 'string' ? value.trim() : '';
}

function isQuantity(value) {
  return Number.isInteger(value) && value >= 0;
}

function isUuidArray(value) {
  return Array.isArray(value) && value.every(isUuid);
}

function methodNotAllowed(res, allow) {
  res.setHeader('Allow', allow);
  return res.status(405).json({ error: 'Method not allowed' });
}

function handleError(res, err, label) {
  if (err instanceof StaffAuthError) {
    return res.status(err.status).json({ error: err.message });
  }
  console.error(label, err);
  return res.status(500).json({ error: 'Internal server error' });
}

// Everything the admin screen shows, in one call: prize stock, the purchase
// items and their prize mapping, and the most recent spins (pending and
// played) with pending/played totals.
async function spinOverview(req, res) {
  if (req.method !== 'GET') {
    return methodNotAllowed(res, 'GET');
  }

  try {
    await requireAdmin(req);

    const pool = getPool();
    const prizes = await pool.query(
      `select id, name, quantity from public.spin_prizes order by name`
    );
    const purchaseItems = await pool.query(
      `select id, name, mapped_prize_ids as "mappedPrizeIds" from public.spin_purchase_items order by name`
    );
    const grants = await pool.query(
      `select g.id,
              g.phone,
              u.name as "customerName",
              g.bought_item as "boughtItem",
              g.created_at as "createdAt",
              g.used_at as "usedAt",
              g.prize_name as "prizeName",
              g.delivered_at as "deliveredAt",
              s.email as "grantedByEmail"
       from public.spin_grants g
       left join public.users u on u.phone = g.phone
       left join public.staff_profiles s on s.id = g.granted_by
       order by coalesce(g.used_at, g.created_at) desc
       limit ${GRANT_LOG_LIMIT}`
    );
    const counts = await pool.query(
      `select count(*) filter (where used_at is null)::int as pending,
              count(*) filter (where used_at is not null)::int as played,
              count(*) filter (where delivered_at is not null)::int as delivered
       from public.spin_grants`
    );
    return res.status(200).json({
      prizes: prizes.rows,
      purchaseItems: purchaseItems.rows,
      grants: grants.rows,
      pendingCount: counts.rows[0].pending,
      playedCount: counts.rows[0].played,
      deliveredCount: counts.rows[0].delivered
    });
  } catch (err) {
    return handleError(res, err, 'spin overview error');
  }
}

// POST   { name, quantity }  add a prize
// PATCH  { id, quantity }    set its stock
// DELETE ?id=                remove it
async function spinPrize(req, res) {
  if (req.method === 'POST') {
    const name = trimmedName(req.body?.name);
    const quantity = req.body?.quantity ?? 0;
    if (name.length === 0) {
      return res.status(400).json({ error: 'name is required' });
    }
    if (!isQuantity(quantity)) {
      return res.status(400).json({ error: 'quantity must be a whole number, 0 or more' });
    }
    try {
      await requireAdmin(req);
      const result = await getPool().query(
        `insert into public.spin_prizes (name, quantity) values ($1, $2) returning id, name, quantity`,
        [name, quantity]
      );
      return res.status(201).json(result.rows[0]);
    } catch (err) {
      if (err.code === UNIQUE_VIOLATION) {
        return res.status(409).json({ error: 'A prize with this name already exists' });
      }
      return handleError(res, err, 'create spin prize error');
    }
  }

  if (req.method === 'PATCH') {
    const { id, quantity } = req.body ?? {};
    if (!isUuid(id)) {
      return res.status(400).json({ error: 'id is required' });
    }
    if (!isQuantity(quantity)) {
      return res.status(400).json({ error: 'quantity must be a whole number, 0 or more' });
    }
    try {
      await requireAdmin(req);
      const result = await getPool().query(
        `update public.spin_prizes set quantity = $2 where id = $1 returning id, name, quantity`,
        [id, quantity]
      );
      if (result.rowCount === 0) {
        return res.status(404).json({ error: 'Prize not found' });
      }
      return res.status(200).json(result.rows[0]);
    } catch (err) {
      return handleError(res, err, 'update spin prize error');
    }
  }

  if (req.method === 'DELETE') {
    const { id } = req.query;
    if (!isUuid(id)) {
      return res.status(400).json({ error: 'id query param is required' });
    }
    try {
      await requireAdmin(req);
      const pool = getPool();
      // A purchase item whose mapped prizes have all been deleted must not
      // quietly turn into "any prize" (an empty mapping), so the mapping has
      // to be removed deliberately first. Setting quantity to 0 takes a
      // prize off the wheel without any of this.
      const mapped = await pool.query(
        `select name from public.spin_purchase_items where $1 = any(mapped_prize_ids) order by name`,
        [id]
      );
      if (mapped.rowCount > 0) {
        return res.status(409).json({
          error: `Still mapped to: ${mapped.rows.map((row) => row.name).join(', ')}. Remove it from those purchase items first.`
        });
      }
      const result = await pool.query(`delete from public.spin_prizes where id = $1`, [id]);
      if (result.rowCount === 0) {
        return res.status(404).json({ error: 'Prize not found' });
      }
      return res.status(204).end();
    } catch (err) {
      return handleError(res, err, 'delete spin prize error');
    }
  }

  return methodNotAllowed(res, 'POST, PATCH, DELETE');
}

// POST   { name, mappedPrizeIds }      add a purchase item
// PATCH  { id, name, mappedPrizeIds }  rename / re-map it
// DELETE ?id=                          remove it
//
// Editing or deleting one never touches spins already unlocked with it --
// each grant carries its own copy of the item name and mapping.
async function spinPurchaseItem(req, res) {
  if (req.method === 'POST' || req.method === 'PATCH') {
    const isUpdate = req.method === 'PATCH';
    const { id } = req.body ?? {};
    const name = trimmedName(req.body?.name);
    const mappedPrizeIds = req.body?.mappedPrizeIds ?? [];
    if (isUpdate && !isUuid(id)) {
      return res.status(400).json({ error: 'id is required' });
    }
    if (name.length === 0) {
      return res.status(400).json({ error: 'name is required' });
    }
    if (!isUuidArray(mappedPrizeIds)) {
      return res.status(400).json({ error: 'mappedPrizeIds must be a list of prize ids' });
    }
    try {
      await requireAdmin(req);
      const pool = getPool();
      const result = isUpdate
        ? await pool.query(
            `update public.spin_purchase_items set name = $2, mapped_prize_ids = $3::uuid[] where id = $1
             returning id, name, mapped_prize_ids as "mappedPrizeIds"`,
            [id, name, mappedPrizeIds]
          )
        : await pool.query(
            `insert into public.spin_purchase_items (name, mapped_prize_ids) values ($1, $2::uuid[])
             returning id, name, mapped_prize_ids as "mappedPrizeIds"`,
            [name, mappedPrizeIds]
          );
      if (result.rowCount === 0) {
        return res.status(404).json({ error: 'Purchase item not found' });
      }
      return res.status(isUpdate ? 200 : 201).json(result.rows[0]);
    } catch (err) {
      if (err.code === UNIQUE_VIOLATION) {
        return res.status(409).json({ error: 'A purchase item with this name already exists' });
      }
      return handleError(res, err, 'save spin purchase item error');
    }
  }

  if (req.method === 'DELETE') {
    const { id } = req.query;
    if (!isUuid(id)) {
      return res.status(400).json({ error: 'id query param is required' });
    }
    try {
      await requireAdmin(req);
      const result = await getPool().query(`delete from public.spin_purchase_items where id = $1`, [id]);
      if (result.rowCount === 0) {
        return res.status(404).json({ error: 'Purchase item not found' });
      }
      return res.status(204).end();
    } catch (err) {
      return handleError(res, err, 'delete spin purchase item error');
    }
  }

  return methodNotAllowed(res, 'POST, PATCH, DELETE');
}

// POST   { phone, purchaseItemId }  unlock a spin for a customer
// PATCH  { id }                     undo a played spin
// PATCH  { id, action: 'deliver' }  mark a won prize as handed over
// DELETE ?id=                       revoke a spin that hasn't been played
async function spinGrant(req, res) {
  if (req.method === 'POST') {
    return spinGrantCreate(req, res);
  }
  if (req.method === 'PATCH') {
    // No action means undo -- what staff app builds from before "deliver"
    // existed send.
    if ((req.body ?? {}).action === 'deliver') {
      return spinGrantDeliver(req, res);
    }
    return spinGrantUndo(req, res);
  }
  if (req.method === 'DELETE') {
    return spinGrantRevoke(req, res);
  }
  return methodNotAllowed(res, 'POST, PATCH, DELETE');
}

async function spinGrantCreate(req, res) {
  const { phone, countryCode, purchaseItemId } = req.body ?? {};
  const normalizedPhone = normalizePhone(phone, countryCode);
  if (!normalizedPhone) {
    return res.status(400).json({ error: 'phone must be a 10-digit number' });
  }
  if (!isUuid(purchaseItemId)) {
    return res.status(400).json({ error: 'purchaseItemId is required' });
  }

  try {
    const admin = await requireAdmin(req);

    const pool = getPool();
    const itemResult = await pool.query(
      `select name, mapped_prize_ids from public.spin_purchase_items where id = $1`,
      [purchaseItemId]
    );
    if (itemResult.rowCount === 0) {
      return res.status(404).json({ error: 'Purchase item not found' });
    }
    const item = itemResult.rows[0];

    const result = await pool.query(
      `insert into public.spin_grants (phone, bought_item, mapped_prize_ids, granted_by)
       values ($1, $2, $3::uuid[], $4)
       returning id`,
      [normalizedPhone, item.name, item.mapped_prize_ids, admin.id]
    );

    await notifyCustomer(pool, normalizedPhone, item.name);
    await logActivity(admin, {
      workType: 'spin',
      description: `unlocked a spin for buying ${item.name}`,
      customerPhone: normalizedPhone
    });
    return res.status(201).json({ id: result.rows[0].id });
  } catch (err) {
    return handleError(res, err, 'create spin grant error');
  }
}

// Best-effort: the spin is unlocked whether or not the push goes out (the
// customer may not have the app yet, or FCM may not be configured).
async function notifyCustomer(pool, normalizedPhone, boughtItem) {
  try {
    const tokens = await pool.query(`select fcm_token from public.device_tokens where phone = $1`, [normalizedPhone]);
    const { deadTokens } = await sendToTokens(
      tokens.rows.map((row) => row.fcm_token),
      {
        title: 'Your spin is unlocked!',
        body: `Thanks for buying ${boughtItem}. Open Spin to Win in the app to play.`
      }
    );
    if (deadTokens.length > 0) {
      await pool.query(`delete from public.device_tokens where fcm_token = any($1::text[])`, [deadTokens]);
    }
  } catch (err) {
    console.error('spin unlock push error', err);
  }
}

// Records that a won prize was handed over to the customer, stamped with
// the time it was marked. Only a played spin can be delivered, and only once.
async function spinGrantDeliver(req, res) {
  const { id } = req.body ?? {};
  if (!isUuid(id)) {
    return res.status(400).json({ error: 'id is required' });
  }

  try {
    const admin = await requireAdmin(req);

    const pool = getPool();
    const result = await pool.query(
      `update public.spin_grants set delivered_at = now()
       where id = $1 and used_at is not null and delivered_at is null
       returning delivered_at as "deliveredAt", phone, prize_name`,
      [id]
    );
    if (result.rowCount === 0) {
      const existing = await pool.query(`select used_at, delivered_at from public.spin_grants where id = $1`, [id]);
      if (existing.rowCount === 0) {
        return res.status(404).json({ error: 'Spin not found' });
      }
      return res.status(409).json({
        error: existing.rows[0].used_at === null
          ? 'This spin has not been played yet'
          : 'This prize has already been marked as delivered'
      });
    }
    const delivered = result.rows[0];
    await logActivity(admin, {
      workType: 'spin',
      description: `handed over spin prize ${delivered.prize_name}`,
      customerPhone: delivered.phone
    });
    return res.status(200).json({ id, deliveredAt: delivered.deliveredAt });
  } catch (err) {
    return handleError(res, err, 'deliver spin prize error');
  }
}

// Reverses a win: the prize goes back into stock and the spin becomes
// playable again (the customer spins afresh -- they aren't handed the same
// prize back). Revoke it afterwards to take the spin away entirely. Not
// once the prize is delivered: it has physically left the store, so putting
// it back into stock would overstate what's there.
async function spinGrantUndo(req, res) {
  const { id } = req.body ?? {};
  if (!isUuid(id)) {
    return res.status(400).json({ error: 'id is required' });
  }

  let client;
  try {
    const admin = await requireAdmin(req);

    client = await getPool().connect();
    await client.query('begin');
    const grantResult = await client.query(
      `select prize_id, prize_name, phone, used_at, delivered_at from public.spin_grants where id = $1 for update`,
      [id]
    );
    if (grantResult.rowCount === 0) {
      await client.query('rollback');
      return res.status(404).json({ error: 'Spin not found' });
    }
    const grant = grantResult.rows[0];
    if (grant.used_at === null) {
      await client.query('rollback');
      return res.status(409).json({ error: 'This spin has not been played yet' });
    }
    if (grant.delivered_at !== null) {
      await client.query('rollback');
      return res.status(409).json({ error: 'This prize has already been delivered, so the win can no longer be undone' });
    }
    // prize_id is null if the prize was deleted since -- nothing to restock.
    if (grant.prize_id !== null) {
      await client.query(`update public.spin_prizes set quantity = quantity + 1 where id = $1`, [grant.prize_id]);
    }
    await client.query(
      `update public.spin_grants set used_at = null, prize_id = null, prize_name = null where id = $1`,
      [id]
    );
    await client.query('commit');
    // Released before logging -- the pool holds a single connection
    // (lib/db.js), so logActivity would otherwise wait on it forever.
    client.release();
    client = null;
    await logActivity(admin, {
      workType: 'spin',
      description: `undid a spin win (${grant.prize_name ?? 'prize since deleted'}), so the customer can spin again`,
      customerPhone: grant.phone
    });
    return res.status(204).end();
  } catch (err) {
    if (client) {
      await client.query('rollback').catch(() => {});
    }
    return handleError(res, err, 'undo spin error');
  } finally {
    if (client) {
      client.release();
    }
  }
}

async function spinGrantRevoke(req, res) {
  const { id } = req.query;
  if (!isUuid(id)) {
    return res.status(400).json({ error: 'id query param is required' });
  }

  try {
    const admin = await requireAdmin(req);

    // Only ever deletes an unplayed spin -- a played one is the record of a
    // prize that left stock, so it has to be undone first (spinGrantUndo).
    const pool = getPool();
    const result = await pool.query(
      `delete from public.spin_grants where id = $1 and used_at is null returning phone, bought_item`,
      [id]
    );
    if (result.rowCount === 0) {
      const exists = await pool.query(`select 1 from public.spin_grants where id = $1`, [id]);
      return exists.rowCount === 0
        ? res.status(404).json({ error: 'Spin not found' })
        : res.status(409).json({ error: 'This spin has already been played. Undo the win first.' });
    }
    const revoked = result.rows[0];
    await logActivity(admin, {
      workType: 'spin',
      description: `took back an unplayed spin (for buying ${revoked.bought_item})`,
      customerPhone: revoked.phone
    });
    return res.status(204).end();
  } catch (err) {
    return handleError(res, err, 'revoke spin error');
  }
}

module.exports = { spinOverview, spinPrize, spinPurchaseItem, spinGrant };
