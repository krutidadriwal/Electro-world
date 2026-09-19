const { getPool } = require('../../lib/db');
const { requireStaff, requireAdmin, StaffAuthError } = require('../../lib/staffAuth');
const { sendToTokens } = require('../../lib/fcm');

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

async function handlePost(req, res) {
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
