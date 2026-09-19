const { uploadFile } = require('../../lib/drive');
const { requireAdmin, StaffAuthError } = require('../../lib/staffAuth');

// Base64 inflates size by ~33%, and Vercel serverless functions cap request
// bodies at 4.5MB -- 3MB raw keeps the encoded body comfortably under that.
const MAX_IMAGE_BYTES = 3 * 1024 * 1024;

module.exports = async function handler(req, res) {
  if (req.method !== 'POST') {
    res.setHeader('Allow', 'POST');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { fileName, mimeType, base64Data } = req.body ?? {};
  if (typeof fileName !== 'string' || fileName.trim().length === 0) {
    return res.status(400).json({ error: 'fileName is required' });
  }
  if (typeof mimeType !== 'string' || !mimeType.startsWith('image/')) {
    return res.status(400).json({ error: 'mimeType must be an image/* type' });
  }
  if (typeof base64Data !== 'string' || base64Data.length === 0) {
    return res.status(400).json({ error: 'base64Data is required' });
  }

  const folderId = process.env.DRIVE_NOTIFICATIONS_FOLDER_ID;
  if (!folderId) {
    return res.status(500).json({ error: 'DRIVE_NOTIFICATIONS_FOLDER_ID is not configured' });
  }

  let buffer;
  try {
    buffer = Buffer.from(base64Data, 'base64');
  } catch (err) {
    return res.status(400).json({ error: 'base64Data is not valid base64' });
  }
  if (buffer.length === 0 || buffer.length > MAX_IMAGE_BYTES) {
    return res.status(400).json({ error: `Image must be between 1 byte and ${MAX_IMAGE_BYTES} bytes` });
  }

  try {
    await requireAdmin(req);

    const file = await uploadFile(folderId, fileName.trim(), mimeType, buffer);
    return res.status(201).json({ driveFileId: file.id, url: file.url });
  } catch (err) {
    if (err instanceof StaffAuthError) {
      return res.status(err.status).json({ error: err.message });
    }
    console.error('upload notification image error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
};
