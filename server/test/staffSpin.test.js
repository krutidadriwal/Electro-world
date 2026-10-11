const test = require('node:test');
const assert = require('node:assert/strict');
const handler = require('../api/staff/[...path]');
const { mockReq, mockRes } = require('../testUtils/mockReqRes');

// Same approach as account.test.js: only the validation branches that return
// before any auth/DB call are exercised here.

test('POST /api/staff/spin-overview is not allowed', async () => {
  const req = mockReq({ method: 'POST', url: '/api/staff/spin-overview' });
  const res = mockRes();
  await handler(req, res);
  assert.equal(res.statusCode, 405);
  assert.equal(res.headers.Allow, 'GET');
});

test('POST /api/staff/spin-prize without a name returns 400', async () => {
  const req = mockReq({ method: 'POST', url: '/api/staff/spin-prize', body: { quantity: 3 } });
  const res = mockRes();
  await handler(req, res);
  assert.equal(res.statusCode, 400);
  assert.match(res.body.error, /name/);
});

test('PATCH /api/staff/spin-prize with a negative quantity returns 400', async () => {
  const req = mockReq({
    method: 'PATCH',
    url: '/api/staff/spin-prize',
    body: { id: '7b1f3c1e-5f0a-4c59-9d0b-2f4f3a6f9c11', quantity: -1 }
  });
  const res = mockRes();
  await handler(req, res);
  assert.equal(res.statusCode, 400);
  assert.match(res.body.error, /quantity/);
});

test('POST /api/staff/spin-purchase-item with malformed prize ids returns 400', async () => {
  const req = mockReq({
    method: 'POST',
    url: '/api/staff/spin-purchase-item',
    body: { name: 'Washing Machine', mappedPrizeIds: ['not-a-uuid'] }
  });
  const res = mockRes();
  await handler(req, res);
  assert.equal(res.statusCode, 400);
  assert.match(res.body.error, /mappedPrizeIds/);
});

test('POST /api/staff/spin-grant with a bad phone returns 400', async () => {
  const req = mockReq({
    method: 'POST',
    url: '/api/staff/spin-grant',
    body: { phone: '123', purchaseItemId: '7b1f3c1e-5f0a-4c59-9d0b-2f4f3a6f9c11' }
  });
  const res = mockRes();
  await handler(req, res);
  assert.equal(res.statusCode, 400);
  assert.match(res.body.error, /phone/);
});

test('DELETE /api/staff/spin-grant without an id returns 400', async () => {
  const req = mockReq({ method: 'DELETE', url: '/api/staff/spin-grant', query: {} });
  const res = mockRes();
  await handler(req, res);
  assert.equal(res.statusCode, 400);
});
