const { getPool } = require('../lib/db');
const { normalizePhone } = require('../lib/phone');

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

async function handlePost(req, res) {
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
