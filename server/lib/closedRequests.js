// Closed complaints/installation requests only stay visible in the apps
// (customer and staff) for a week after they were closed, so the lists
// don't fill up with finished work. They're only hidden from the list
// endpoints -- the rows themselves are never deleted from the database.
//
// "When it was closed" is resolved_at, which the staff status change stamps
// when a request is closed (see complaintsStatus/installationsStatus in
// api/staff/[...path].js); updated_at covers any older row without one.
// closed_date isn't used: it's the date staff type in, which can be backdated.
const CLOSED_VISIBLE_DAYS = 7;

// A where-clause condition keeping everything except requests closed more
// than CLOSED_VISIBLE_DAYS ago. alias is the table's alias in the query
// ('' for none).
function visibleInAppSql(alias = '') {
  const col = (name) => (alias ? `${alias}.${name}` : name);
  return `not (${col('status')} = 'closed' and coalesce(${col('resolved_at')}, ${col('updated_at')}) < now() - interval '${CLOSED_VISIBLE_DAYS} days')`;
}

// The same week-long window for the staff wishlist: a 'purchased' (deal
// closed) entry stays listed for CLOSED_VISIBLE_DAYS after it was marked,
// then drops off. Never deleted. status_updated_at is stamped by every
// staff status change; created_at covers a row without one.
function wishlistVisibleInAppSql(alias = '') {
  const col = (name) => (alias ? `${alias}.${name}` : name);
  return `not (${col('status')} = 'purchased' and coalesce(${col('status_updated_at')}, ${col('created_at')}) < now() - interval '${CLOSED_VISIBLE_DAYS} days')`;
}

module.exports = { visibleInAppSql, wishlistVisibleInAppSql, CLOSED_VISIBLE_DAYS };
