const test = require('node:test');
const assert = require('node:assert/strict');
const { visibleInAppSql, wishlistVisibleInAppSql } = require('../lib/closedRequests');

test('wishlistVisibleInAppSql hides only deals closed more than 7 days ago', () => {
  assert.equal(
    wishlistVisibleInAppSql('wi'),
    "not (wi.status = 'purchased' and coalesce(wi.status_updated_at, wi.created_at) < now() - interval '7 days')"
  );
});

test('visibleInAppSql hides only requests closed more than 7 days ago', () => {
  assert.equal(
    visibleInAppSql(),
    "not (status = 'closed' and coalesce(resolved_at, updated_at) < now() - interval '7 days')"
  );
});

test('visibleInAppSql prefixes columns with the table alias', () => {
  assert.equal(
    visibleInAppSql('c'),
    "not (c.status = 'closed' and coalesce(c.resolved_at, c.updated_at) < now() - interval '7 days')"
  );
});
