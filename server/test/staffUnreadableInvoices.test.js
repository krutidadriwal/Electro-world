const test = require('node:test');
const assert = require('node:assert/strict');
const handler = require('../api/staff/[...path]');
const { mockReq, mockRes } = require('../testUtils/mockReqRes');

// Same approach as staffSpin.test.js: only the validation branches that
// return before any auth/DB call are exercised here.

test('POST /api/staff/unreadable-invoices-list is not allowed', async () => {
  const req = mockReq({ method: 'POST', url: '/api/staff/unreadable-invoices-list' });
  const res = mockRes();
  await handler(req, res);
  assert.equal(res.statusCode, 405);
  assert.equal(res.headers.Allow, 'GET');
});

test('GET /api/staff/unreadable-invoices-retry is not allowed', async () => {
  const req = mockReq({ method: 'GET', url: '/api/staff/unreadable-invoices-retry' });
  const res = mockRes();
  await handler(req, res);
  assert.equal(res.statusCode, 405);
  assert.equal(res.headers.Allow, 'POST');
});

test('POST /api/staff/unreadable-invoices-retry without a fileId returns 400', async () => {
  const req = mockReq({ method: 'POST', url: '/api/staff/unreadable-invoices-retry', body: {} });
  const res = mockRes();
  await handler(req, res);
  assert.equal(res.statusCode, 400);
  assert.match(res.body.error, /fileId/);
});
