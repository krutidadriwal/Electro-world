// Combined into one function to stay under Vercel Hobby's 12-serverless-
// function cap (see server/vercel.json) -- /api/invoice-file is rewritten
// here with ?resource=invoice-file appended, everything else (the bare
// /api/invoices path) falls through to the list handler. External URLs are
// unchanged: /api/invoices, /api/invoice-file.

const { getPool } = require('../lib/db');
const { normalizePhone } = require('../lib/phone');
const { findUserFolder, listPdfFiles, getFileMetadata, downloadFile } = require('../lib/drive');

module.exports = async function handler(req, res) {
  if (req.query.resource === 'invoice-file') {
    return invoiceFile(req, res);
  }
  return invoicesList(req, res);
};

async function invoicesList(req, res) {
  if (req.method !== 'GET') {
    res.setHeader('Allow', 'GET');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { phone, countryCode } = req.query;
  const normalizedPhone = normalizePhone(phone, countryCode);
  if (!normalizedPhone) {
    return res.status(400).json({ error: 'phone query param must be a 10-digit number' });
  }

  try {
    const pool = getPool();
    const userResult = await pool.query(
      `select drive_folder_id from public.users where phone = $1`,
      [normalizedPhone]
    );
    if (userResult.rowCount === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    let folderId = userResult.rows[0].drive_folder_id;

    if (!folderId) {
      const rootFolderId = process.env.DRIVE_ROOT_FOLDER_ID;
      if (!rootFolderId) {
        throw new Error('DRIVE_ROOT_FOLDER_ID environment variable is not set.');
      }
      const folder = await findUserFolder(rootFolderId, phone);
      if (!folder) {
        return res.status(200).json({ invoices: [] });
      }
      folderId = folder.id;
      await pool.query(`update public.users set drive_folder_id = $1 where phone = $2`, [folderId, normalizedPhone]);
    }

    const files = await listPdfFiles(folderId);
    const invoices = files.map((f) => ({
      id: f.id,
      name: f.name,
      createdAt: f.createdTime,
      sizeBytes: f.size ? Number(f.size) : null
    }));

    return res.status(200).json({ invoices });
  } catch (err) {
    console.error('list invoices error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

async function invoiceFile(req, res) {
  if (req.method !== 'GET') {
    res.setHeader('Allow', 'GET');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { id, phone, countryCode } = req.query;
  if (typeof id !== 'string' || id.trim().length === 0) {
    return res.status(400).json({ error: 'id query param is required' });
  }
  const normalizedPhone = normalizePhone(phone, countryCode);
  if (!normalizedPhone) {
    return res.status(400).json({ error: 'phone query param must be a 10-digit number' });
  }

  try {
    const pool = getPool();
    const userResult = await pool.query(
      `select drive_folder_id from public.users where phone = $1`,
      [normalizedPhone]
    );
    const folderId = userResult.rows[0]?.drive_folder_id;
    if (!folderId) {
      return res.status(404).json({ error: 'No invoices found for this account' });
    }

    const metadata = await getFileMetadata(id);
    const belongsToUser = metadata.mimeType === 'application/pdf' && (metadata.parents || []).includes(folderId);
    if (!belongsToUser) {
      return res.status(403).json({ error: 'File does not belong to this account' });
    }

    const fileBuffer = await downloadFile(id);
    res.setHeader('Content-Type', 'application/pdf');
    res.setHeader('Content-Disposition', `inline; filename="${metadata.name.replace(/"/g, '')}"`);
    return res.status(200).end(fileBuffer);
  } catch (err) {
    console.error('download invoice file error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}
