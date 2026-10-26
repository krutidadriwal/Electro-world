// Companies (product brands) a complaint/installation request is for -- see
// public.companies in schema.sql. Shared by the customer endpoints
// (api/service-requests.js, api/catalog.js) and the staff ones
// (api/staff/[...path].js).

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

async function listCompanies(pool) {
  const result = await pool.query(`select id, name from public.companies order by lower(name)`);
  return result.rows;
}

// Looks up the company a new request names. Returns { company } on success
// or { error } with the 400 message to send back -- a company is required on
// every new complaint/installation request.
async function resolveCompany(pool, companyId) {
  if (typeof companyId !== 'string' || companyId.trim().length === 0) {
    return { error: 'companyId is required' };
  }
  if (!UUID_REGEX.test(companyId)) {
    return { error: 'companyId does not match a known company' };
  }
  const result = await pool.query(`select id, name from public.companies where id = $1`, [companyId]);
  if (result.rowCount === 0) {
    return { error: 'companyId does not match a known company' };
  }
  return { company: result.rows[0] };
}

module.exports = { UUID_REGEX, listCompanies, resolveCompany };
