const { getPool } = require('../lib/db');
const { normalizePhone } = require('../lib/phone');
const { getFileMetadata, downloadFile } = require('../lib/drive');

module.exports = async function handler(req, res) {
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
};
