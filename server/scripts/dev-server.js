const http = require('http');
const url = require('url');

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
// function, with req.query.path set to the remaining segments (an array,
// same shape Vercel gives the handler).
const catchAllRoutes = {
  '/api/auth': auth,
  '/api/staff': staff,
  '/api/notifications': notifications
};

function resolveHandler(pathname) {
  if (routes[pathname]) {
    return { handler: routes[pathname], pathSegments: [] };
  }
  for (const [prefix, handler] of Object.entries(catchAllRoutes)) {
    if (pathname === prefix) {
      return { handler, pathSegments: [] };
    }
    if (pathname.startsWith(`${prefix}/`)) {
      return { handler, pathSegments: pathname.slice(prefix.length + 1).split('/') };
    }
  }
  return null;
}

const server = http.createServer((req, res) => {
  const parsed = url.parse(req.url, true);
  const resolved = resolveHandler(parsed.pathname);
  if (!resolved) {
    res.writeHead(404, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ error: 'Not found' }));
    return;
  }

  req.query = { ...parsed.query, ...(resolved.pathSegments.length > 0 ? { path: resolved.pathSegments } : {}) };

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
