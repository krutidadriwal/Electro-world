// Spin to Win -- customer side. Dispatched from api/account.js (via the
// /api/spin rewrite in vercel.json) rather than living in its own api/ file,
// to stay under Vercel Hobby's 12-function cap.
//
// Ported from the old standalone spinner page, where the browser itself
// picked the prize and wrote the stock/used-flag updates with the anon key.
// Here the server is the only thing that ever decides or records a win:
//   GET  /api/spin?phone=  -> how many spins are unlocked, what's on the
//                             wheel, and what this customer has already won
//   POST /api/spin         -> consume one unlocked spin and return the prize

const { getPool } = require('./db');
const { normalizePhone } = require('./phone');
const { verifySession } = require('./jwt');

module.exports = async function spin(req, res) {
  if (req.method === 'GET') {
    return spinGet(req, res);
  }
  if (req.method === 'POST') {
    return spinPost(req, res);
  }
  res.setHeader('Allow', 'GET, POST');
  return res.status(405).json({ error: 'Method not allowed' });
};

async function spinGet(req, res) {
  const normalizedPhone = normalizePhone(req.query.phone, req.query.countryCode);
  if (!normalizedPhone) {
    return res.status(400).json({ error: 'phone query param must be a 10-digit number' });
  }

  try {
    const pool = getPool();
    const pendingResult = await pool.query(
      `select count(*)::int as count from public.spin_grants where phone = $1 and used_at is null`,
      [normalizedPhone]
    );
    // Everything currently in stock goes on the wheel, whether or not this
    // customer's purchase is mapped to a narrower set -- the mapping only
    // limits what they can actually land on (see spinPost).
    const prizesResult = await pool.query(
      `select id, name from public.spin_prizes where quantity > 0 order by name`
    );
    const winsResult = await pool.query(
      `select id, prize_name as "prizeName", bought_item as "boughtItem", used_at as "wonAt"
       from public.spin_grants
       where phone = $1 and used_at is not null
       order by used_at desc
       limit 50`,
      [normalizedPhone]
    );
    return res.status(200).json({
      availableSpins: pendingResult.rows[0].count,
      prizes: prizesResult.rows,
      wins: winsResult.rows
    });
  } catch (err) {
    console.error('get spin state error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

// Requires the caller's own session token, same as DELETE /api/user -- a
// spin hands out a real prize, so phone alone is not proof of ownership.
async function spinPost(req, res) {
  const { phone, countryCode } = req.body ?? {};
  const normalizedPhone = normalizePhone(phone, countryCode);
  if (!normalizedPhone) {
    return res.status(400).json({ error: 'phone must be a 10-digit number' });
  }

  const authHeader = req.headers?.authorization ?? '';
  const token = authHeader.startsWith('Bearer ') ? authHeader.slice(7) : null;
  const session = verifySession(token);
  if (!session) {
    return res.status(401).json({ error: 'Missing or invalid session token' });
  }
  if (session.phone !== normalizedPhone) {
    return res.status(403).json({ error: 'Session does not match the account spinning' });
  }

  let client;
  try {
    client = await getPool().connect();
    await client.query('begin');

    // Oldest unlocked spin first. FOR UPDATE so two simultaneous taps (or
    // two devices) can't both consume the same grant.
    const grantResult = await client.query(
      `select id, bought_item, mapped_prize_ids
       from public.spin_grants
       where phone = $1 and used_at is null
       order by created_at
       limit 1
       for update`,
      [normalizedPhone]
    );
    if (grantResult.rowCount === 0) {
      await client.query('rollback');
      return res.status(403).json({ error: 'No spin available. Make a purchase to unlock one.' });
    }
    const grant = grantResult.rows[0];
    const mappedPrizeIds = grant.mapped_prize_ids ?? [];

    // The purchase's mapped prizes if it has any, otherwise anything in
    // stock -- picked uniformly at random either way. A mapped purchase
    // never falls back to the wider pool: if its prizes are all out of
    // stock the spin fails (and stays unlocked) instead.
    const prizeResult = await client.query(
      `select id, name
       from public.spin_prizes
       where quantity > 0
         and (cardinality($1::uuid[]) = 0 or id = any($1::uuid[]))
       order by random()
       limit 1
       for update`,
      [mappedPrizeIds]
    );
    if (prizeResult.rowCount === 0) {
      await client.query('rollback');
      return res.status(409).json({
        error: mappedPrizeIds.length > 0
          ? 'The gifts for your purchase are out of stock right now. Please ask our staff.'
          : 'No prizes are available right now. Please try again later.'
      });
    }
    const prize = prizeResult.rows[0];

    await client.query(`update public.spin_prizes set quantity = quantity - 1 where id = $1`, [prize.id]);
    await client.query(
      `update public.spin_grants set used_at = now(), prize_id = $2, prize_name = $3 where id = $1`,
      [grant.id, prize.id, prize.name]
    );
    const remainingResult = await client.query(
      `select count(*)::int as count from public.spin_grants where phone = $1 and used_at is null`,
      [normalizedPhone]
    );
    await client.query('commit');

    return res.status(200).json({
      prize,
      boughtItem: grant.bought_item,
      availableSpins: remainingResult.rows[0].count
    });
  } catch (err) {
    if (client) {
      await client.query('rollback').catch(() => {});
    }
    console.error('spin error', err);
    return res.status(500).json({ error: 'Internal server error' });
  } finally {
    if (client) {
      client.release();
    }
  }
}
