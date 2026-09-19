const http = require('http');

const user = require('../api/user');
const invoices = require('../api/invoices');
const invoiceFile = require('../api/invoice-file');
const deviceTokens = require('../api/device-tokens');
const auth = require('../api/auth/[...path]');
const staff = require('../api/staff/[...path]');
const notifications = require('../api/notifications/[...path]');

// Exact-path routes, same as Vercel's static api/*.js file routing.
const routes = {
  '/api/user': user,
  '/api/invoices': invoices,
  '/api/invoice-file': invoiceFile,
  '/api/device-tokens': deviceTokens
};

// Catch-all routes, mirroring Vercel's api/<prefix>/[...path].js dynamic
// routing: any request under one of these prefixes is handed to the same
// function. The handler itself parses req.url to figure out which sub-route
// it is (see lib/routeSegments.js) -- not req.query.path, which turned out
// not to be reliably populated by Vercel's Node runtime for these dynamic
// routes -- so this file only needs to pick the right function, not compute
// the segments.
const catchAllRoutes = {
  '/api/auth': auth,
  '/api/staff': staff,
  '/api/notifications': notifications
};

function resolveHandler(pathname) {
  if (routes[pathname]) {
    return routes[pathname];
  }
  for (const [prefix, handler] of Object.entries(catchAllRoutes)) {
    if (pathname === prefix || pathname.startsWith(`${prefix}/`)) {
      return handler;
    }
  }
  return null;
}

const server = http.createServer((req, res) => {
  const parsed = new URL(req.url, `http://${req.headers.host ?? 'localhost'}`);
  const handler = resolveHandler(parsed.pathname);
  if (!handler) {
    res.writeHead(404, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ error: 'Not found' }));
    return;
  }

  req.query = Object.fromEntries(parsed.searchParams);

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

    Promise.resolve(resolved.handler(req, res)).catch((err) => {
      console.error(err);
      res.statusCode = 500;
      res.end(JSON.stringify({ error: 'Internal server error' }));
    });
  });
});

const PORT = 3001;
server.listen(PORT, () => console.log(`Dev server listening on http://localhost:${PORT}`));
