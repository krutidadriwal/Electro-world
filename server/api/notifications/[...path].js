// Combined into one function for the same reason as api/auth/[...path].js --
// staying under Vercel Hobby's 12-Serverless-Function cap.
//
// Routes live under /api/notifications/<segment> (list, create, image)
// rather than at the bare /api/notifications path: Vercel's [...path] catch-
// all only matches one-or-more segments, not the prefix itself, so a bare
// /api/notifications request never even reaches this function (confirmed by
// testing against the deployed server -- it 404s at the Vercel routing layer,
// before req.query.path or req.url parsing ever runs). Giving every route an
// explicit segment sidesteps that entirely.

const { getPool } = require('../../lib/db');
const { requireStaff, requireAdmin, StaffAuthError } = require('../../lib/staffAuth');
const { sendToTokens } = require('../../lib/fcm');
const { uploadNotificationImage } = require('../../lib/notificationImages');
const { routeSegmentsAfter } = require('../../lib/routeSegments');

const MAX_IMAGE_BYTES = 3 * 1024 * 1024; // base64 inflates ~33%; Vercel caps request bodies at 4.5MB

module.exports = async function handler(req, res) {
  // .../api/notifications/<...> -- drop "api", "notifications".
  const route = routeSegmentsAfter(req, 2).join('/');

  switch (route) {
    case 'list':
      return list(req, res);
    case 'active':
      return active(req, res);
    case 'create':
      return create(req, res);
    case 'image':
      return uploadImage(req, res);
    default:
      return res.status(404).json({ error: 'Not found' });
  }
};

// Public (no staff auth) -- the customer app's in-app notification center.
// Unlike list(), this only returns not-yet-expired notifications, and
// customers have no login of their own to gate this behind (same trust
// model as the other customer-facing endpoints, e.g. GET /api/invoices).
async function active(req, res) {
  if (req.method !== 'GET') {
    res.setHeader('Allow', 'GET');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  try {
    const pool = getPool();
    const result = await pool.query(
      `select id, message, image_url, duration_minutes, created_at
       from public.notifications
       where (created_at + (duration_minutes || ' minutes')::interval) >= now()
       order by created_at desc`
    );
    return res.status(200).json({ notifications: result.rows });
  } catch (err) {
    console.error('list active notifications error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

async function list(req, res) {
  if (req.method !== 'GET') {
    res.setHeader('Allow', 'GET');
    return res.status(405).json({ error: 'Method not allowed' });
  }

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
  if (req.method !== 'POST') {
    res.setHeader('Allow', 'POST');
    return res.status(405).json({ error: 'Method not allowed' });
  }

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
          imageUrl: notification.image_url ?? undefined,
          // Lets the client use a stable tray-notification id (tied to this
          // row) instead of a random one, so opening/reading it in-app can
          // cancel that specific tray notification -- keeping the launcher's
          // unread badge count (where the OEM launcher supports one) in
          // sync with what the in-app notification list shows as read.
          data: { notificationId: notification.id }
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

    // driveFileId is a legacy name from when this uploaded to Google Drive
    // (a bare service account has no storage quota to own a file there --
    // see lib/notificationImages.js); it now holds the Supabase Storage
    // object path instead, kept as-is to avoid a client + column rename.
    const file = await uploadNotificationImage(fileName.trim(), mimeType, buffer);
    return res.status(201).json({ driveFileId: file.path, url: file.url });
  } catch (err) {
    if (err instanceof StaffAuthError) {
      return res.status(err.status).json({ error: err.message });
    }
    console.error('upload notification image error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}
