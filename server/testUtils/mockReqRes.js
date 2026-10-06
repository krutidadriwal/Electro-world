// Minimal req/res fakes for exercising Vercel-style handlers without a real
// HTTP server or database connection. Handlers that validate input before
// touching the DB (the common pattern in api/) can be tested fully with
// these; handlers need a live DB are out of scope for these tests.
function mockReq({ method = 'GET', url = '/', query = {}, body } = {}) {
  return { method, url, query, body };
}

function mockRes() {
  const res = {
    statusCode: 200,
    headers: {},
    body: undefined,
    ended: false,
    status(code) {
      this.statusCode = code;
      return this;
    },
    setHeader(name, value) {
      this.headers[name] = value;
      return this;
    },
    json(payload) {
      this.body = payload;
      return this;
    },
    end() {
      this.ended = true;
      return this;
    }
  };
  return res;
}

module.exports = { mockReq, mockRes };
