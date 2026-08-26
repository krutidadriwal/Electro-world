const { getPool } = require('../lib/db');
const { normalizePhone } = require('../lib/phone');

const ISSUE_TYPES = ['Not working', 'Damaged on delivery', 'Installation issue', 'Missing parts', 'Wrong item', 'Other'];

module.exports = async function handler(req, res) {
  if (req.method === 'GET') {
    return handleGet(req, res);
  }
  if (req.method === 'POST') {
    return handlePost(req, res);
  }
  res.setHeader('Allow', 'GET, POST');
  return res.status(405).json({ error: 'Method not allowed' });
};

async function handleGet(req, res) {
  const { phone, countryCode } = req.query;
  const normalizedPhone = normalizePhone(phone, countryCode);
  if (!normalizedPhone) {
    return res.status(400).json({ error: 'phone query param must be a 10-digit number' });
  }

  try {
    const pool = getPool();
    const result = await pool.query(
      `select id, invoice_file_id, invoice_file_name, category_icon_key, category_name,
              subcategory_id, subcategory_name, issue_type, description,
              contact_phone, status, created_at, updated_at, resolved_at
       from public.complaints
       where phone = $1
       order by created_at desc`,
      [normalizedPhone]
    );
    return res.status(200).json({ complaints: result.rows });
  } catch (err) {
    console.error('list complaints error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

async function handlePost(req, res) {
  const {
    phone,
    countryCode,
    invoiceFileId,
    invoiceFileName,
    categoryIconKey,
    subcategoryId,
    issueType,
    description,
    contactPhone
  } = req.body ?? {};

  const normalizedPhone = normalizePhone(phone, countryCode);
  if (!normalizedPhone) {
    return res.status(400).json({ error: 'phone must be a 10-digit number' });
  }
  if (typeof categoryIconKey !== 'string' || categoryIconKey.trim().length === 0) {
    return res.status(400).json({ error: 'categoryIconKey is required' });
  }
  if (typeof issueType !== 'string' || !ISSUE_TYPES.includes(issueType)) {
    return res.status(400).json({ error: `issueType must be one of: ${ISSUE_TYPES.join(', ')}` });
  }
  if (typeof description !== 'string' || description.trim().length === 0) {
    return res.status(400).json({ error: 'description is required' });
  }

  try {
    const pool = getPool();
    const userResult = await pool.query(`select 1 from public.users where phone = $1`, [normalizedPhone]);
    if (userResult.rowCount === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    const categoryResult = await pool.query(
      `select icon_key, name from public.categories where icon_key = $1`,
      [categoryIconKey]
    );
    if (categoryResult.rowCount === 0) {
      return res.status(400).json({ error: 'categoryIconKey does not match a known category' });
    }
    const category = categoryResult.rows[0];

    let subcategory = null;
    if (typeof subcategoryId === 'string' && subcategoryId.trim().length > 0) {
      const subcategoryResult = await pool.query(
        `select id, name from public.subcategories where id = $1 and category_icon_key = $2`,
        [subcategoryId, categoryIconKey]
      );
      if (subcategoryResult.rowCount === 0) {
        return res.status(400).json({ error: 'subcategoryId does not match a subcategory of categoryIconKey' });
      }
      subcategory = subcategoryResult.rows[0];
    }

    const result = await pool.query(
      `insert into public.complaints
         (phone, invoice_file_id, invoice_file_name, category_icon_key, category_name,
          subcategory_id, subcategory_name, issue_type, description, contact_phone)
       values ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
       returning id, invoice_file_id, invoice_file_name, category_icon_key, category_name,
                 subcategory_id, subcategory_name, issue_type, description,
                 contact_phone, status, created_at, updated_at, resolved_at`,
      [
        normalizedPhone,
        typeof invoiceFileId === 'string' && invoiceFileId.trim().length > 0 ? invoiceFileId.trim() : null,
        typeof invoiceFileName === 'string' && invoiceFileName.trim().length > 0 ? invoiceFileName.trim() : null,
        category.icon_key,
        category.name,
        subcategory ? subcategory.id : null,
        subcategory ? subcategory.name : null,
        issueType,
        description.trim(),
        typeof contactPhone === 'string' && contactPhone.trim().length > 0 ? contactPhone.trim() : null
      ]
    );
    return res.status(201).json(result.rows[0]);
  } catch (err) {
    console.error('create complaint error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}
