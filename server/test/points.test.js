const test = require('node:test');
const assert = require('node:assert/strict');
const { planFifo, nextExpiry, balanceOf, reminderText } = require('../lib/points');
const staffHandler = require('../api/staff/[...path]');
const notificationsHandler = require('../api/notifications/[...path]');
const { mockReq, mockRes } = require('../testUtils/mockReqRes');

const lots = [
  { id: 'a', remaining: 200, expires_on: '2026-11-01' },
  { id: 'b', remaining: 150, expires_on: '2026-11-01' },
  { id: 'c', remaining: 400, expires_on: '2027-03-15' }
];

test('planFifo takes from the soonest-expiring lots first', () => {
  assert.deepEqual(planFifo(lots, 100), [{ id: 'a', points: 100 }]);
  assert.deepEqual(planFifo(lots, 200), [{ id: 'a', points: 200 }]);
  assert.deepEqual(planFifo(lots, 500), [
    { id: 'a', points: 200 },
    { id: 'b', points: 150 },
    { id: 'c', points: 150 }
  ]);
  assert.deepEqual(planFifo(lots, 750).map((a) => a.points), [200, 150, 400]);
});

test('planFifo refuses more than the lots hold', () => {
  assert.throws(() => planFifo(lots, 751));
});

test('balanceOf and nextExpiry sum what is left', () => {
  assert.equal(balanceOf(lots), 750);
  assert.deepEqual(nextExpiry(lots), { points: 350, expiresOn: '2026-11-01' });
  assert.equal(nextExpiry([]), null);
});

test('reminderText names the date, or the range of dates', () => {
  assert.match(reminderText(7, 200, '2026-11-03', '2026-11-03').message, /^200 .* expire on 3 Nov 2026\./);
  assert.match(reminderText(3, 350, '2026-11-01', '2026-11-03').message, /between 1 Nov 2026 and 3 Nov 2026/);
  assert.match(reminderText(3, 1, '2026-11-01', '2026-11-01').title, /3 days/);
});

test('POST /api/staff/points-redeem with a malformed phone returns 400', async () => {
  const req = mockReq({ method: 'POST', url: '/api/staff/points-redeem', body: { phone: '123', points: 500 } });
  const res = mockRes();
  await staffHandler(req, res);
  assert.equal(res.statusCode, 400);
  assert.match(res.body.error, /phone/);
});

test('POST /api/staff/points-redeem with non-integer points returns 400', async () => {
  for (const points of [0, -5, 12.5, '500', undefined]) {
    const req = mockReq({ method: 'POST', url: '/api/staff/points-redeem', body: { phone: '9326763315', points } });
    const res = mockRes();
    await staffHandler(req, res);
    assert.equal(res.statusCode, 400);
    assert.match(res.body.error, /points/);
  }
});

test('GET /api/staff/points-customer without a phone returns 400', async () => {
  const req = mockReq({ method: 'GET', url: '/api/staff/points-customer', query: {} });
  const res = mockRes();
  await staffHandler(req, res);
  assert.equal(res.statusCode, 400);
});

test('points expiry reminders refuse to run without CRON_SECRET', async () => {
  const previous = process.env.CRON_SECRET;
  delete process.env.CRON_SECRET;
  try {
    const req = mockReq({ method: 'POST', url: '/api/notifications/points-expiry-reminders' });
    const res = mockRes();
    await notificationsHandler(req, res);
    assert.equal(res.statusCode, 503);
  } finally {
    if (previous !== undefined) process.env.CRON_SECRET = previous;
  }
});

test('points expiry reminders reject a wrong secret', async () => {
  const previous = process.env.CRON_SECRET;
  process.env.CRON_SECRET = 'right-secret';
  try {
    const req = { ...mockReq({ method: 'POST', url: '/api/notifications/points-expiry-reminders' }), headers: { authorization: 'Bearer wrong-secret' } };
    const res = mockRes();
    await notificationsHandler(req, res);
    assert.equal(res.statusCode, 401);
  } finally {
    if (previous === undefined) delete process.env.CRON_SECRET;
    else process.env.CRON_SECRET = previous;
  }
});

test('GET /api/notifications/active with a malformed phone returns 400', async () => {
  const req = mockReq({ method: 'GET', url: '/api/notifications/active', query: { phone: '12' } });
  const res = mockRes();
  await notificationsHandler(req, res);
  assert.equal(res.statusCode, 400);
});
