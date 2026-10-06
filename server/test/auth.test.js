const test = require('node:test');
const assert = require('node:assert/strict');
const handler = require('../api/auth/[...path]');
const { mockReq, mockRes } = require('../testUtils/mockReqRes');

// Same approach as account.test.js: only the validation branches that return
// before any DB/network call are exercised here.

test('unknown /api/auth/<route> returns 404', async () => {
  const req = mockReq({ method: 'POST', url: '/api/auth/does-not-exist', body: {} });
  const res = mockRes();
  await handler(req, res);
  assert.equal(res.statusCode, 404);
});

test('GET /api/auth/login is not allowed', async () => {
  const req = mockReq({ method: 'GET', url: '/api/auth/login' });
  const res = mockRes();
  await handler(req, res);
  assert.equal(res.statusCode, 405);
});

test('POST /api/auth/login with a bad phone returns 400', async () => {
  const req = mockReq({
    method: 'POST',
    url: '/api/auth/login',
    body: { phone: 'not-a-phone', pin: '1234' }
  });
  const res = mockRes();
  await handler(req, res);
  assert.equal(res.statusCode, 400);
  assert.match(res.body.error, /phone/i);
});

test('POST /api/auth/login without a pin returns 400', async () => {
  const req = mockReq({
    method: 'POST',
    url: '/api/auth/login',
    body: { phone: '9326763315' }
  });
  const res = mockRes();
  await handler(req, res);
  assert.equal(res.statusCode, 400);
  assert.match(res.body.error, /pin/i);
});

test('POST /api/auth/send-otp with a bad phone returns 400', async () => {
  const req = mockReq({
    method: 'POST',
    url: '/api/auth/send-otp',
    body: { phone: '123' }
  });
  const res = mockRes();
  await handler(req, res);
  assert.equal(res.statusCode, 400);
});
