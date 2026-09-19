const http = require('http');
const url = require('url');

const user = require('../api/user');
const invoices = require('../api/invoices');
const invoiceFile = require('../api/invoice-file');
const deviceTokens = require('../api/device-tokens');
const staffMe = require('../api/staff/me');
const staffList = require('../api/staff/list');
const staffCreate = require('../api/staff/create');
const staffRole = require('../api/staff/role');
const notifications = require('../api/notifications/index');
const notificationImage = require('../api/notifications/image');

const routes = {
  '/api/user': user,
  '/api/invoices': invoices,
  '/api/invoice-file': invoiceFile,
  '/api/device-tokens': deviceTokens,
  '/api/staff/me': staffMe,
  '/api/staff/list': staffList,
  '/api/staff/create': staffCreate,
  '/api/staff/role': staffRole,
  '/api/notifications': notifications,
  '/api/notifications/image': notificationImage
};

const server = http.createServer((req, res) => {
  const parsed = url.parse(req.url, true);
  const handler = routes[parsed.pathname];
  if (!handler) {
    res.writeHead(404, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ error: 'Not found' }));
    return;
  }

  req.query = parsed.query;

  let body = '';
  req.on('data', (chunk) => (body += chunk));
  req.on('end', () => {
    try {
      req.body = body ? JSON.parse(body) : {};
    } catch {
      req.body = {};
    }

    res.status = (code) => {
      res.statusCode = code;
      return res;
    };
    res.json = (obj) => {
      res.setHeader('Content-Type', 'application/json');
      res.end(JSON.stringify(obj));
    };

    Promise.resolve(handler(req, res)).catch((err) => {
      console.error(err);
      res.statusCode = 500;
      res.end(JSON.stringify({ error: 'Internal server error' }));
    });
  });
});

const PORT = 3001;
server.listen(PORT, () => console.log(`Dev server listening on http://localhost:${PORT}`));
