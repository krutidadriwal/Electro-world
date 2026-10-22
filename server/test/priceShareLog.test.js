const test = require('node:test');
const assert = require('node:assert/strict');
const handler = require('../api/staff/[...path]');
const { mockReq, mockRes } = require('../testUtils/mockReqRes');

// Same approach as staffSpin.test.js: only the validation branches that
// return before any auth/DB call are exercised here.

async function post(body) {
  const res = mockRes();
  await handler(mockReq({ method: 'POST', url: '/api/staff/price-share-log', body }), res);
  return res;
}

test('GET /api/staff/price-share-log is not allowed', async () => {
  const res = mockRes();
  await handler(mockReq({ method: 'GET', url: '/api/staff/price-share-log' }), res);
  assert.equal(res.statusCode, 405);
  assert.equal(res.headers.Allow, 'POST');
});

test('price-share-log rejects a malformed phone', async () => {
  const res = await post({ customerPhone: '98765', customerName: 'Asha' });
  assert.equal(res.statusCode, 400);
  assert.match(res.body.error, /customerPhone/);
});

test('price-share-log requires a customer name', async () => {
  const res = await post({ customerPhone: '9876543210', customerName: '  ' });
  assert.equal(res.statusCode, 400);
  assert.match(res.body.error, /customerName/);
});

test('price-share-log rejects an unparseable sharedAt', async () => {
  const res = await post({ customerPhone: '9876543210', customerName: 'Asha', sharedAt: 'yesterday' });
  assert.equal(res.statusCode, 400);
  assert.match(res.body.error, /sharedAt/);
});
