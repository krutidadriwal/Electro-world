// Derives the sub-path segments matched by a [...path].js catch-all route,
// parsed directly from req.url rather than trusting req.query.path -- on
// Vercel's Node runtime, the dynamic segment isn't reliably populated into
// req.query for these bracket-named files, so parsing the URL ourselves is
// the one approach that works the same in production and in the local
// scripts/dev-server.js.
//
// prefixSegmentCount is how many segments to drop from the front (e.g. 2 for
// "/api/staff/me" -> drop "api","staff" -> ["me"]).
function routeSegmentsAfter(req, prefixSegmentCount) {
  const pathname = req.url.split('?')[0];
  const allSegments = pathname.split('/').filter(Boolean);
  return allSegments.slice(prefixSegmentCount);
}

module.exports = { routeSegmentsAfter };
