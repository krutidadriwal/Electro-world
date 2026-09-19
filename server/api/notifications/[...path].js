// Combined into one function for the same reason as api/auth/[...path].js --
// staying under Vercel Hobby's 12-Serverless-Function cap. External URLs are
// unchanged: /api/notifications (GET/POST) and /api/notifications/image
// (POST) both still work, just served by this one function.

const { getPool } = require('../../lib/db');
const { requireStaff, requireAdmin, StaffAuthError } = require('../../lib/staffAuth');
const { sendToTokens } = require('../../lib/fcm');
const { uploadFile } = require('../../lib/drive');

const MAX_IMAGE_BYTES = 3 * 1024 * 1024; // base64 inflates ~33%; Vercel caps request bodies at 4.5MB

module.exports = async function handler(req, res) {
  const segments = Array.isArray(req.query.path) ? req.query.path : req.query.path ? [req.query.path] : [];
  const route = segments.join('/');

  if (route === '') {
    if (req.method === 'GET') return list(req, res);
    if (req.method === 'POST') return create(req, res);
    res.setHeader('Allow', 'GET, POST');
    return res.status(405).json({ error: 'Method not allowed' });
  }
  if (route === 'image') {
    return uploadImage(req, res);
  }
  return res.status(404).json({ error: 'Not found' });
};

async function list(req, res) {
  try {
    await requireStaff(req);

    const pool = getPool();
    const result = await pool.query(
      `select id, message, image_drive_file_id, image_url, duration_minutes, created_by, created_at,
              (created_at + (duration_minutes || ' minutes')::interval) < now() as expired
       from public.notifications
       order by created_at desc`
    );
    return res.status(200).json({ notifications: result.rows });
  } catch (err) {
    if (err instanceof StaffAuthError) {
      return res.status(err.status).json({ error: err.message });
    }
    console.error('list notifications error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

async function create(req, res) {
  const { message, durationMinutes, imageDriveFileId, imageUrl } = req.body ?? {};
  if (typeof message !== 'string' || message.trim().length === 0) {
    return res.status(400).json({ error: 'message is required' });
  }
  if (!Number.isInteger(durationMinutes) || durationMinutes <= 0) {
    return res.status(400).json({ error: 'durationMinutes must be a positive integer' });
  }

  try {
    const admin = await requireAdmin(req);

    const pool = getPool();
    const insertResult = await pool.query(
      `insert into public.notifications (message, image_drive_file_id, image_url, duration_minutes, created_by)
       values ($1, $2, $3, $4, $5)
       returning id, message, image_drive_file_id, image_url, duration_minutes, created_by, created_at`,
      [
        message.trim(),
        typeof imageDriveFileId === 'string' && imageDriveFileId.trim().length > 0 ? imageDriveFileId.trim() : null,
        typeof imageUrl === 'string' && imageUrl.trim().length > 0 ? imageUrl.trim() : null,
        durationMinutes,
        admin.id
      ]
    );
    const notification = insertResult.rows[0];

    const tokensResult = await pool.query('select fcm_token from public.device_tokens');
    const tokens = tokensResult.rows.map((row) => row.fcm_token);
    if (tokens.length > 0) {
      try {
        const { deadTokens } = await sendToTokens(tokens, {
          title: 'Electro World',
          body: notification.message,
          imageUrl: notification.image_url ?? undefined
        });
        if (deadTokens.length > 0) {
          await pool.query('delete from public.device_tokens where fcm_token = any($1)', [deadTokens]);
        }
      } catch (pushErr) {
        // The notification row is already saved -- a push delivery failure
        // shouldn't fail the whole request, just get logged for follow-up.
        console.error('fcm send error', pushErr);
      }
    }

    return res.status(201).json(notification);
  } catch (err) {
    if (err instanceof StaffAuthError) {
      return res.status(err.status).json({ error: err.message });
    }
    console.error('create notification error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

async function uploadImage(req, res) {
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
}
