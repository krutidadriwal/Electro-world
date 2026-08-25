const { getPool } = require('../lib/db');
const { normalizePhone } = require('../lib/phone');
const { findUserFolder, listPdfFiles } = require('../lib/drive');

module.exports = async function handler(req, res) {
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
};
