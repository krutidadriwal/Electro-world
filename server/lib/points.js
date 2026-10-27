// Points balance, redemption and expiry reminders. Earned points (purchase
// credits, the welcome bonus) are positive points_ledger rows -- "lots" --
// each expiring 6 months after it was credited. A redemption is a negative
// row whose points are taken from the customer's unexpired lots, the one
// expiring soonest first (FIFO), recorded in points_redemption_allocations.
// See schema.sql for the tables.

const { sendToTokens } = require('./fcm');

// The points T&C: "A minimum of 500 points is needed to redeem" -- read as
// the customer's valid balance, not the size of each redemption (the 5%
// cap would make a 500-point redemption impossible on any invoice under
// Rs 10,000).
const MIN_BALANCE_TO_REDEEM = 500;

const REDEMPTION_REASON = 'Redeemed at store';

// How much of lot `l` hasn't been redeemed yet (an index lookup per lot,
// see points_redemption_allocations_credit_idx).
const LOT_REMAINING_SQL = `
  l.points - coalesce((select sum(a.points) from public.points_redemption_allocations a where a.credit_ledger_id = l.id), 0)`;

// The customer's unexpired lots that still have points left, in the order
// redemptions take from them.
async function availableLots(db, phone) {
  const result = await db.query(
    `select id, remaining::int as remaining, expires_at,
            (expires_at at time zone 'Asia/Kolkata')::date::text as expires_on
     from (
       select l.id, l.created_at, l.expires_at, ${LOT_REMAINING_SQL} as remaining
       from public.points_ledger l
       where l.phone = $1 and l.points > 0 and l.expires_at > now()
     ) lots
     where remaining > 0
     order by expires_at, created_at, id`,
    [phone]
  );
  return result.rows;
}

function balanceOf(lots) {
  return lots.reduce((sum, lot) => sum + lot.remaining, 0);
}

// The soonest expiry date and how many points lapse on it, or null.
function nextExpiry(lots) {
  if (lots.length === 0) {
    return null;
  }
  const expiresOn = lots[0].expires_on;
  const points = lots.filter((lot) => lot.expires_on === expiresOn).reduce((sum, lot) => sum + lot.remaining, 0);
  return { points, expiresOn };
}

// Splits a redemption across lots already in FIFO order: [{ id, points }].
// Throws if the lots don't cover it -- callers check the balance first.
function planFifo(lots, points) {
  const allocations = [];
  let left = points;
  for (const lot of lots) {
    if (left === 0) break;
    const take = Math.min(lot.remaining, left);
    allocations.push({ id: lot.id, points: take });
    left -= take;
  }
  if (left > 0) {
    throw new Error('Not enough points to cover the redemption');
  }
  return allocations;
}

class RedemptionError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

// Redeems `points` from the customer's valid points, in one transaction.
// The advisory lock serialises redemptions for the same phone, so two
// admins redeeming at once can't both spend the same lot. Returns the
// customer's balance afterwards.
async function redeemPoints(pool, phone, points, staffId) {
  const client = await pool.connect();
  try {
    await client.query('begin');
    await client.query('select pg_advisory_xact_lock(hashtextextended($1, 0))', [`points:${phone}`]);

    const lots = await availableLots(client, phone);
    const balance = balanceOf(lots);
    if (balance < MIN_BALANCE_TO_REDEEM) {
      throw new RedemptionError(409, `At least ${MIN_BALANCE_TO_REDEEM} valid points are needed to redeem (customer has ${balance})`);
    }
    if (points > balance) {
      throw new RedemptionError(409, `Customer only has ${balance} valid points`);
    }

    const allocations = planFifo(lots, points);
    const redemption = await client.query(
      `insert into public.points_ledger (phone, points, reason, expires_at, created_by)
       values ($1, $2, $3, now(), $4)
       returning id`,
      [phone, -points, REDEMPTION_REASON, staffId]
    );
    await client.query(
      `insert into public.points_redemption_allocations (redemption_ledger_id, credit_ledger_id, points)
       select $1, unnest($2::uuid[]), unnest($3::int[])`,
      [redemption.rows[0].id, allocations.map((a) => a.id), allocations.map((a) => a.points)]
    );

    await client.query('commit');
    return { balance: balance - points };
  } catch (err) {
    await client.query('rollback').catch(() => {});
    throw err;
  } finally {
    client.release();
  }
}

// "3 Nov 2026" from "2026-11-03".
const MONTH_NAMES = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

function formatDate(isoDate) {
  const [year, month, day] = isoDate.split('-').map(Number);
  return `${day} ${MONTH_NAMES[month - 1]} ${year}`;
}

// The reminder text for one customer's lots in one stage.
function reminderText(stage, points, firstExpiresOn, lastExpiresOn) {
  const when = firstExpiresOn === lastExpiresOn
    ? `on ${formatDate(firstExpiresOn)}`
    : `between ${formatDate(firstExpiresOn)} and ${formatDate(lastExpiresOn)}`;
  return {
    title: stage === 3 ? 'Your points expire in 3 days' : 'Your points expire next week',
    message: `${points} of your Electro World points expire ${when}. Visit the store to redeem them before they're gone.`
  };
}

// Run daily at 10:00 IST by pg_cron (see schema.sql). Every unexpired lot
// with points left gets a reminder once it's within 7 days of expiring and
// again within 3 days -- counted in calendar days in the store's time zone.
// A lot already inside 3 days when first seen (say a run was missed) gets
// only the 3-day one. Lots are claimed in points_expiry_reminders before
// anything is sent, so they're never reminded twice for the same stage.
// One notification per customer per stage, summing their lots.
async function sendExpiryReminders(pool) {
  const claimed = await pool.query(
    `with lots as (
       select l.id, l.phone, ${LOT_REMAINING_SQL} as remaining,
              (l.expires_at at time zone 'Asia/Kolkata')::date as expires_on,
              l.expires_at,
              (l.expires_at at time zone 'Asia/Kolkata')::date - (now() at time zone 'Asia/Kolkata')::date as days_left
       from public.points_ledger l
       where l.points > 0 and l.expires_at > now() and l.expires_at < now() + interval '8 days'
     ),
     candidates as (
       select id, phone, remaining, expires_on, expires_at, case when days_left <= 3 then 3 else 7 end as stage
       from lots
       where remaining > 0 and days_left <= 7
     ),
     inserted as (
       insert into public.points_expiry_reminders (ledger_id, stage)
       select id, stage from candidates
       on conflict do nothing
       returning ledger_id, stage
     )
     select c.phone, c.stage, sum(c.remaining)::int as points,
            min(c.expires_on)::text as first_expires_on, max(c.expires_on)::text as last_expires_on,
            max(c.expires_at) as last_expires_at
     from candidates c
     join inserted i on i.ledger_id = c.id and i.stage = c.stage
     group by c.phone, c.stage`
  );

  for (const group of claimed.rows) {
    try {
      await remindCustomer(pool, group);
    } catch (err) {
      console.error('points expiry reminder error', group.phone, err);
    }
  }
  return { reminded: claimed.rows.length };
}

async function remindCustomer(pool, group) {
  const { title, message } = reminderText(group.stage, group.points, group.first_expires_on, group.last_expires_on);
  const notification = await pool.query(
    `insert into public.customer_notifications (phone, title, message, expires_at)
     values ($1, $2, $3, $4)
     returning id`,
    [group.phone, title, message, group.last_expires_at]
  );

  const tokens = await pool.query('select fcm_token from public.device_tokens where phone = $1', [group.phone]);
  const { deadTokens } = await sendToTokens(
    tokens.rows.map((row) => row.fcm_token),
    { title, body: message, notificationId: notification.rows[0].id }
  );
  if (deadTokens.length > 0) {
    await pool.query('delete from public.device_tokens where fcm_token = any($1::text[])', [deadTokens]);
  }
}

module.exports = {
  MIN_BALANCE_TO_REDEEM,
  REDEMPTION_REASON,
  LOT_REMAINING_SQL,
  RedemptionError,
  availableLots,
  balanceOf,
  nextExpiry,
  planFifo,
  redeemPoints,
  reminderText,
  sendExpiryReminders
};
