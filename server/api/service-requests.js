// Combined into one function to stay under Vercel Hobby's 12-serverless-
// function cap (see server/vercel.json) -- /api/installations and
// /api/invoice-requests are rewritten here with ?resource=... appended,
// everything else (the bare /api/service-requests path, which nothing calls
// directly) falls through to complaints. External URLs are unchanged:
// /api/complaints, /api/installations, /api/invoice-requests.

const { getPool } = require('../lib/db');
const { normalizePhone } = require('../lib/phone');

module.exports = async function handler(req, res) {
  switch (req.query.resource) {
    case 'installations':
      return installations(req, res);
    case 'invoice-requests':
      return invoiceRequests(req, res);
    default:
      return complaints(req, res);
  }
};

async function complaints(req, res) {
  if (req.method === 'GET') {
    return complaintsGet(req, res);
  }
  if (req.method === 'POST') {
    return complaintsPost(req, res);
  }
  res.setHeader('Allow', 'GET, POST');
  return res.status(405).json({ error: 'Method not allowed' });
}

async function complaintsGet(req, res) {
  const { phone, countryCode } = req.query;
  const normalizedPhone = normalizePhone(phone, countryCode);
  if (!normalizedPhone) {
    return res.status(400).json({ error: 'phone query param must be a 10-digit number' });
  }

  try {
    const pool = getPool();
    const result = await pool.query(
      `select id, invoice_file_id, invoice_file_name, category_icon_key, category_name,
              subcategory_id, subcategory_name, description, address,
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

async function complaintsPost(req, res) {
  const {
    phone,
    countryCode,
    invoiceFileId,
    invoiceFileName,
    categoryIconKey,
    subcategoryId,
    description,
    address,
    contactPhone
  } = req.body ?? {};

  const normalizedPhone = normalizePhone(phone, countryCode);
  if (!normalizedPhone) {
    return res.status(400).json({ error: 'phone must be a 10-digit number' });
  }
  if (typeof categoryIconKey !== 'string' || categoryIconKey.trim().length === 0) {
    return res.status(400).json({ error: 'categoryIconKey is required' });
  }
  if (typeof description !== 'string' || description.trim().length === 0) {
    return res.status(400).json({ error: 'description is required' });
  }
  if (typeof address !== 'string' || address.trim().length === 0) {
    return res.status(400).json({ error: 'address is required' });
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
          subcategory_id, subcategory_name, description, address, contact_phone)
       values ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
       returning id, invoice_file_id, invoice_file_name, category_icon_key, category_name,
                 subcategory_id, subcategory_name, description, address,
                 contact_phone, status, created_at, updated_at, resolved_at`,
      [
        normalizedPhone,
        typeof invoiceFileId === 'string' && invoiceFileId.trim().length > 0 ? invoiceFileId.trim() : null,
        typeof invoiceFileName === 'string' && invoiceFileName.trim().length > 0 ? invoiceFileName.trim() : null,
        category.icon_key,
        category.name,
        subcategory ? subcategory.id : null,
        subcategory ? subcategory.name : null,
        description.trim(),
        address.trim(),
        typeof contactPhone === 'string' && contactPhone.trim().length > 0 ? contactPhone.trim() : null
      ]
    );
    return res.status(201).json(result.rows[0]);
  } catch (err) {
    console.error('create complaint error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

async function installations(req, res) {
  if (req.method === 'GET') {
    return installationsGet(req, res);
  }
  if (req.method === 'POST') {
    return installationsPost(req, res);
  }
  res.setHeader('Allow', 'GET, POST');
  return res.status(405).json({ error: 'Method not allowed' });
}

async function installationsGet(req, res) {
  const { phone, countryCode } = req.query;
  const normalizedPhone = normalizePhone(phone, countryCode);
  if (!normalizedPhone) {
    return res.status(400).json({ error: 'phone query param must be a 10-digit number' });
  }

  try {
    const pool = getPool();
    const result = await pool.query(
      `select id, invoice_file_id, invoice_file_name, category_icon_key, category_name,
              item_name, wants_demo, wants_installation, address,
              contact_phone, status, created_at, updated_at, resolved_at
       from public.installations
       where phone = $1
       order by created_at desc`,
      [normalizedPhone]
    );
    return res.status(200).json({ installations: result.rows });
  } catch (err) {
    console.error('list installations error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

async function installationsPost(req, res) {
  const {
    phone,
    countryCode,
    invoiceFileId,
    invoiceFileName,
    categoryIconKey,
    itemName,
    wantsDemo,
    wantsInstallation,
    address,
    contactPhone
  } = req.body ?? {};

  const normalizedPhone = normalizePhone(phone, countryCode);
  if (!normalizedPhone) {
    return res.status(400).json({ error: 'phone must be a 10-digit number' });
  }
  if (typeof categoryIconKey !== 'string' || categoryIconKey.trim().length === 0) {
    return res.status(400).json({ error: 'categoryIconKey is required' });
  }
  if (typeof itemName !== 'string' || itemName.trim().length === 0) {
    return res.status(400).json({ error: 'itemName is required' });
  }
  if (typeof address !== 'string' || address.trim().length === 0) {
    return res.status(400).json({ error: 'address is required' });
  }
  const demo = wantsDemo === true;
  const installation = wantsInstallation === true;
  if (!demo && !installation) {
    return res.status(400).json({ error: 'At least one of wantsDemo or wantsInstallation must be true' });
  }

  try {
    const pool = getPool();
    const userResult = await pool.query(`select 1 from public.users where phone = $1`, [normalizedPhone]);
    if (userResult.rowCount === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    const categoryResult = await pool.query(
      `select icon_key, name, can_install, can_demo from public.categories where icon_key = $1`,
      [categoryIconKey]
    );
    if (categoryResult.rowCount === 0) {
      return res.status(400).json({ error: 'categoryIconKey does not match a known category' });
    }
    const category = categoryResult.rows[0];
    if (installation && !category.can_install) {
      return res.status(400).json({ error: 'Installation is not available for this category' });
    }
    if (demo && !category.can_demo) {
      return res.status(400).json({ error: 'Demo is not available for this category' });
    }

    const result = await pool.query(
      `insert into public.installations
         (phone, invoice_file_id, invoice_file_name, category_icon_key, category_name,
          item_name, wants_demo, wants_installation, address, contact_phone)
       values ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
       returning id, invoice_file_id, invoice_file_name, category_icon_key, category_name,
                 item_name, wants_demo, wants_installation, address,
                 contact_phone, status, created_at, updated_at, resolved_at`,
      [
        normalizedPhone,
        typeof invoiceFileId === 'string' && invoiceFileId.trim().length > 0 ? invoiceFileId.trim() : null,
        typeof invoiceFileName === 'string' && invoiceFileName.trim().length > 0 ? invoiceFileName.trim() : null,
        category.icon_key,
        category.name,
        itemName.trim(),
        demo,
        installation,
        address.trim(),
        typeof contactPhone === 'string' && contactPhone.trim().length > 0 ? contactPhone.trim() : null
      ]
    );
    return res.status(201).json(result.rows[0]);
  } catch (err) {
    console.error('create installation error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

async function invoiceRequests(req, res) {
  if (req.method === 'GET') {
    return invoiceRequestsGet(req, res);
  }
  if (req.method === 'POST') {
    return invoiceRequestsPost(req, res);
  }
  res.setHeader('Allow', 'GET, POST');
  return res.status(405).json({ error: 'Method not allowed' });
}

async function invoiceRequestsGet(req, res) {
  const { phone, countryCode } = req.query;
  const normalizedPhone = normalizePhone(phone, countryCode);
  if (!normalizedPhone) {
    return res.status(400).json({ error: 'phone query param must be a 10-digit number' });
  }

  try {
    const pool = getPool();
    const result = await pool.query(
      `select id, description, status, created_at, updated_at, resolved_at
       from public.invoice_requests
       where phone = $1
       order by created_at desc`,
      [normalizedPhone]
    );
    return res.status(200).json({ requests: result.rows });
  } catch (err) {
    console.error('list invoice requests error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

async function invoiceRequestsPost(req, res) {
  const { phone, countryCode, description } = req.body ?? {};

  const normalizedPhone = normalizePhone(phone, countryCode);
  if (!normalizedPhone) {
    return res.status(400).json({ error: 'phone must be a 10-digit number' });
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

    const result = await pool.query(
      `insert into public.invoice_requests (phone, description)
       values ($1, $2)
       returning id, description, status, created_at, updated_at, resolved_at`,
      [normalizedPhone, description.trim()]
    );
    return res.status(201).json(result.rows[0]);
  } catch (err) {
    console.error('create invoice request error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}
