const test = require('node:test');
const assert = require('node:assert/strict');
const handler = require('../api/account');
const { mockReq, mockRes } = require('../testUtils/mockReqRes');

// These exercise only the input-validation branches, which return before the
// handler ever calls getPool() -- so no DATABASE_URL/DB connection is needed.

test('GET /api/user without a phone query param returns 400', async () => {
  const req = mockReq({ method: 'GET', query: {} });
  const res = mockRes();
  await handler(req, res);
  assert.equal(res.statusCode, 400);
  assert.match(res.body.error, /phone/i);
});

test('GET /api/user with a malformed phone returns 400', async () => {
  const req = mockReq({ method: 'GET', query: { phone: '123' } });
  const res = mockRes();
  await handler(req, res);
  assert.equal(res.statusCode, 400);
});

test('POST /api/user without a name returns 400', async () => {
  const req = mockReq({ method: 'POST', body: { phone: '9326763315' } });
  const res = mockRes();
  await handler(req, res);
  assert.equal(res.statusCode, 400);
  assert.match(res.body.error, /name/i);
});

test('unsupported method on /api/user returns 405', async () => {
  const req = mockReq({ method: 'PUT', query: {} });
  const res = mockRes();
  await handler(req, res);
  assert.equal(res.statusCode, 405);
  assert.equal(res.headers.Allow, 'GET, POST, PATCH, DELETE');
});

test('POST /api/device-tokens without fcmToken returns 400', async () => {
  const req = mockReq({
    method: 'POST',
    query: { resource: 'device-tokens' },
    body: { phone: '9326763315' }
  });
  const res = mockRes();
  await handler(req, res);
  assert.equal(res.statusCode, 400);
  assert.match(res.body.error, /fcmToken/);
});

test('GET /api/spin without a phone query param returns 400', async () => {
  const req = mockReq({ method: 'GET', query: { resource: 'spin' } });
  const res = mockRes();
  await handler(req, res);
  assert.equal(res.statusCode, 400);
  assert.match(res.body.error, /phone/i);
});

test('POST /api/spin without a session token returns 401', async () => {
  const req = mockReq({ method: 'POST', query: { resource: 'spin' }, body: { phone: '9326763315' } });
  const res = mockRes();
  await handler(req, res);
  assert.equal(res.statusCode, 401);
});

test('unsupported method on /api/spin returns 405', async () => {
  const req = mockReq({ method: 'DELETE', query: { resource: 'spin' } });
  const res = mockRes();
  await handler(req, res);
  assert.equal(res.statusCode, 405);
  assert.equal(res.headers.Allow, 'GET, POST');
});

test('GET /api/device-tokens is not allowed', async () => {
  const req = mockReq({ method: 'GET', query: { resource: 'device-tokens' } });
  const res = mockRes();
  await handler(req, res);
  assert.equal(res.statusCode, 405);
});
