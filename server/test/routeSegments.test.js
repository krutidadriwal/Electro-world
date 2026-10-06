const test = require('node:test');
const assert = require('node:assert/strict');
const { routeSegmentsAfter } = require('../lib/routeSegments');

test('routeSegmentsAfter drops the prefix and keeps the rest', () => {
  assert.deepEqual(
    routeSegmentsAfter({ url: '/api/staff/me' }, 2),
    ['me']
  );
});

test('routeSegmentsAfter strips the query string', () => {
  assert.deepEqual(
    routeSegmentsAfter({ url: '/api/auth/login?foo=bar' }, 2),
    ['login']
  );
});

test('routeSegmentsAfter returns an empty array when nothing is left', () => {
  assert.deepEqual(
    routeSegmentsAfter({ url: '/api/auth' }, 2),
    []
  );
});
