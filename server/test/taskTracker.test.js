const test = require('node:test');
const assert = require('node:assert/strict');
const handler = require('../api/tasks/[...path]');
const { mockReq, mockRes } = require('../testUtils/mockReqRes');

// EXPERIMENTAL task tracker -- delete this file along with
// api/tasks/[...path].js. Same approach as staffSpin.test.js: only the
// branches that return before any auth/DB call are exercised here.

const TASK_ID = '7b1f3c1e-5f0a-4c59-9d0b-2f4f3a6f9c11';

async function call(opts) {
  const res = mockRes();
  await handler(mockReq(opts), res);
  return res;
}

function withFlag(value, fn) {
  return async () => {
    const previous = process.env.TASK_TRACKER_MODE;
    if (value === undefined) delete process.env.TASK_TRACKER_MODE;
    else process.env.TASK_TRACKER_MODE = value;
    try {
      await fn();
    } finally {
      if (previous === undefined) delete process.env.TASK_TRACKER_MODE;
      else process.env.TASK_TRACKER_MODE = previous;
    }
  };
}

for (const value of [undefined, 'false', 'TRUE', '1']) {
  test(`every route 404s when TASK_TRACKER_MODE is ${JSON.stringify(value)}`, withFlag(value, async () => {
    for (const [method, route] of [['GET', 'list'], ['GET', 'staff'], ['POST', 'create'], ['PATCH', 'update'], ['DELETE', 'delete']]) {
      const res = await call({ method, url: `/api/tasks/${route}` });
      assert.equal(res.statusCode, 404, `${method} ${route}`);
    }
  }));
}

test('unknown route returns 404', withFlag('true', async () => {
  const res = await call({ method: 'GET', url: '/api/tasks/nope' });
  assert.equal(res.statusCode, 404);
}));

test('POST /api/tasks/list is not allowed', withFlag('true', async () => {
  const res = await call({ method: 'POST', url: '/api/tasks/list' });
  assert.equal(res.statusCode, 405);
  assert.equal(res.headers.Allow, 'GET');
}));

test('POST /api/tasks/create without a title returns 400', withFlag('true', async () => {
  const res = await call({ method: 'POST', url: '/api/tasks/create', body: { title: '   ' } });
  assert.equal(res.statusCode, 400);
  assert.match(res.body.error, /title/);
}));

test('POST /api/tasks/create with a bad priority returns 400', withFlag('true', async () => {
  const res = await call({ method: 'POST', url: '/api/tasks/create', body: { title: 'x', priority: 'asap' } });
  assert.equal(res.statusCode, 400);
  assert.match(res.body.error, /priority/);
}));

test('PATCH /api/tasks/update without an id returns 400', withFlag('true', async () => {
  const res = await call({ method: 'PATCH', url: '/api/tasks/update', body: { status: 'done' } });
  assert.equal(res.statusCode, 400);
  assert.match(res.body.error, /id/);
}));

test('PATCH /api/tasks/update with nothing to change returns 400', withFlag('true', async () => {
  const res = await call({ method: 'PATCH', url: '/api/tasks/update', body: { id: TASK_ID } });
  assert.equal(res.statusCode, 400);
  assert.match(res.body.error, /Nothing to update/);
}));

test('PATCH /api/tasks/update with a bad status returns 400', withFlag('true', async () => {
  const res = await call({ method: 'PATCH', url: '/api/tasks/update', body: { id: TASK_ID, status: 'shipped' } });
  assert.equal(res.statusCode, 400);
  assert.match(res.body.error, /status/);
}));

test('PATCH /api/tasks/update with a malformed assignee returns 400', withFlag('true', async () => {
  const res = await call({ method: 'PATCH', url: '/api/tasks/update', body: { id: TASK_ID, assignedTo: 'bob' } });
  assert.equal(res.statusCode, 400);
  assert.match(res.body.error, /assignedTo/);
}));

test('DELETE /api/tasks/delete without an id returns 400', withFlag('true', async () => {
  const res = await call({ method: 'DELETE', url: '/api/tasks/delete' });
  assert.equal(res.statusCode, 400);
}));

test('valid request without a token reaches auth and returns 401', withFlag('true', async () => {
  const req = mockReq({ method: 'GET', url: '/api/tasks/list' });
  req.headers = {};
  const res = mockRes();
  await handler(req, res);
  assert.equal(res.statusCode, 401);
}));
