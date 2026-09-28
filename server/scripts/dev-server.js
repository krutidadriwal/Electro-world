const http = require('http');
const fs = require('fs');
const path = require('path');

const account = require('../api/account');
const catalog = require('../api/catalog');
const serviceRequests = require('../api/service-requests');
const invoices = require('../api/invoices');
const auth = require('../api/auth/[...path]');
const staff = require('../api/staff/[...path]');
const notifications = require('../api/notifications/[...path]');

// Exact-path routes, same as Vercel's static api/*.js file routing.
const routes = {
  '/api/account': { handler: account },
  '/api/catalog': { handler: catalog },
  '/api/service-requests': { handler: serviceRequests },
  '/api/invoices': { handler: invoices }
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

// Consolidated endpoints (categories, wishlist, complaints, installations,
// invoice-requests, user, device-tokens, invoice-file) live under grouped
// files (catalog/service-requests/account/invoices) to stay under Vercel
// Hobby's 12-serverless-function cap -- server/vercel.json's `rewrites`
// preserve their original URLs by mapping to the grouped file plus a
// ?resource= query param. Reading that file here (rather than duplicating
// the mapping) keeps this dev server from drifting out of sync with it.
const { rewrites } = JSON.parse(fs.readFileSync(path.join(__dirname, '../vercel.json'), 'utf8'));
for (const { source, destination } of rewrites) {
  const [destPath, destQuery] = destination.split('?');
  const target = routes[destPath];
  if (!target) {
    throw new Error(`vercel.json rewrite destination ${destPath} has no matching route in dev-server.js`);
  }
  routes[source] = {
    handler: target.handler,
    resource: destQuery ? new URLSearchParams(destQuery).get('resource') : undefined
  };
}

function resolveRoute(pathname) {
  if (routes[pathname]) {
    return routes[pathname];
  }
  for (const [prefix, handler] of Object.entries(catchAllRoutes)) {
    if (pathname === prefix || pathname.startsWith(`${prefix}/`)) {
      return { handler };
    }
  }
  return null;
}

const server = http.createServer((req, res) => {
  const parsed = new URL(req.url, `http://${req.headers.host ?? 'localhost'}`);
  const resolved = resolveRoute(parsed.pathname);
  if (!resolved) {
    res.writeHead(404, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ error: 'Not found' }));
    return;
  }

  req.query = Object.fromEntries(parsed.searchParams);
  if (resolved.resource) {
    req.query.resource = resolved.resource;
  }

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
