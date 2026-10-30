const test = require('node:test');
const assert = require('node:assert/strict');
const handler = require('../api/staff/[...path]');
const { mockReq, mockRes } = require('../testUtils/mockReqRes');

// Same approach as auth.test.js: only the validation branches that return
// before any DB/network call are exercised here.

function post(body) {
  return mockReq({ method: 'POST', url: '/api/staff/customer-create', body });
}

test('GET /api/staff/customer-create is not allowed', async () => {
  const res = mockRes();
  await handler(mockReq({ method: 'GET', url: '/api/staff/customer-create' }), res);
  assert.equal(res.statusCode, 405);
});

test('customer-create with a bad phone returns 400', async () => {
  const res = mockRes();
  await handler(post({ phone: '12345', name: 'Ravi Kumar', pin: '1234' }), res);
  assert.equal(res.statusCode, 400);
  assert.match(res.body.error, /phone/i);
});

test('customer-create without a name returns 400', async () => {
  const res = mockRes();
  await handler(post({ phone: '9326763315', name: '   ', pin: '1234' }), res);
  assert.equal(res.statusCode, 400);
  assert.match(res.body.error, /name/i);
});

test('customer-create with a non-4-digit pin returns 400', async () => {
  for (const pin of ['123', '12345', 'abcd', 1234]) {
    const res = mockRes();
    await handler(post({ phone: '9326763315', name: 'Ravi Kumar', pin }), res);
    assert.equal(res.statusCode, 400);
    assert.match(res.body.error, /pin/i);
  }
});
