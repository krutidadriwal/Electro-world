// Combined into one function to stay under Vercel Hobby's 12-serverless-
// function cap (see server/vercel.json) -- /api/invoice-file is rewritten
// here with ?resource=invoice-file appended, everything else (the bare
// /api/invoices path) falls through to the list handler. External URLs are
// unchanged: /api/invoices, /api/invoice-file.

const { getPool } = require('../lib/db');
const { normalizePhone } = require('../lib/phone');
const { listPdfFiles, getFileMetadata, downloadFile } = require('../lib/drive');
const { resolveInvoiceFolderId } = require('../lib/invoiceFolder');

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
    const folderId = await resolveInvoiceFolderId(pool, normalizedPhone);
    if (folderId === undefined) {
      return res.status(404).json({ error: 'User not found' });
    }
    if (!folderId) {
      return res.status(200).json({ invoices: [] });
    }

    const files = await listPdfFiles(folderId);

    // Printed invoice dates, for whichever invoices have been read so far --
    // that only happens when the customer opens the Points tab (see
    // lib/invoicePoints.js), not here, so this stays a quick list. The rest
    // fall back to the Drive upload date until then.
    const invoiceDates = new Map();
    try {
      // ::text keeps it a plain "YYYY-MM-DD" -- node-pg would otherwise turn
      // a date into a local-midnight Date, which can serialize as the day before.
      const datesResult = await pool.query(
        `select file_id, invoice_date::text as invoice_date from public.invoice_points
         where file_id = any($1) and invoice_date is not null`,
        [files.map((f) => f.id)]
      );
      for (const row of datesResult.rows) {
        invoiceDates.set(row.file_id, row.invoice_date);
      }
    } catch (datesErr) {
      console.error('invoice dates lookup error', datesErr);
    }

    // invoiceDate is the date printed on the invoice -- what the app shows,
    // when known. createdAt (the Drive upload date) stays as the fallback.
    const invoices = files
      .map((f) => ({
        id: f.id,
        name: f.name,
        createdAt: f.createdTime,
        invoiceDate: invoiceDates.get(f.id) ?? null,
        sizeBytes: f.size ? Number(f.size) : null
      }))
      .sort((a, b) => (b.invoiceDate ?? b.createdAt).localeCompare(a.invoiceDate ?? a.createdAt));

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
