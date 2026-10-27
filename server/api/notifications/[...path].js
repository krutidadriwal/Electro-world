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

const crypto = require('crypto');
const { getPool } = require('../../lib/db');
const { requireStaff, requireAdmin, StaffAuthError } = require('../../lib/staffAuth');
const { sendToTokens } = require('../../lib/fcm');
const { uploadNotificationImage } = require('../../lib/notificationImages');
const { routeSegmentsAfter } = require('../../lib/routeSegments');
const { broadcastDescription, logActivity } = require('../../lib/activityLog');
const { normalizePhone } = require('../../lib/phone');
const { sendExpiryReminders } = require('../../lib/points');

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
    case 'points-expiry-reminders':
      return pointsExpiryReminders(req, res);
    default:
      return res.status(404).json({ error: 'Not found' });
  }
};

// Public (no staff auth) -- the customer app's in-app notification center.
// Unlike list(), this only returns not-yet-expired notifications, and
// customers have no login of their own to gate this behind (same trust
// model as the other customer-facing endpoints, e.g. GET /api/invoices).
//
// With ?phone=, that customer's own notifications (public.
// customer_notifications, e.g. points expiry reminders) are listed too, in
// the same shape -- duration_minutes worked out from their expires_at --
// so the app treats both alike. Older app versions don't send it and just
// get the broadcasts.
async function active(req, res) {
  if (req.method !== 'GET') {
    res.setHeader('Allow', 'GET');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const phone = req.query.phone === undefined ? null : normalizePhone(req.query.phone, req.query.countryCode);
  if (req.query.phone !== undefined && !phone) {
    return res.status(400).json({ error: 'phone must be a 10-digit number' });
  }

  try {
    const pool = getPool();
    const result = await pool.query(
      `select id, message, image_url, duration_minutes, created_at
       from public.notifications
       where (created_at + (duration_minutes || ' minutes')::interval) >= now()
       union all
       select id, message, null, ceil(extract(epoch from expires_at - created_at) / 60)::int, created_at
       from public.customer_notifications
       where phone = $1 and expires_at >= now()
       order by created_at desc`,
      [phone]
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
          notificationId: notification.id
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

    await logActivity(admin, { workType: 'broadcast', description: broadcastDescription(notification.message) });
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

// Called daily at 10:00 IST by the pg_cron job in schema.sql, authenticated
// with the CRON_SECRET env var (kept in Supabase Vault on the database
// side). Safe to call again by hand: each reminder is only ever sent once.
async function pointsExpiryReminders(req, res) {
  if (req.method !== 'POST') {
    res.setHeader('Allow', 'POST');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const secret = process.env.CRON_SECRET;
  if (!secret) {
    console.error('points expiry reminders: CRON_SECRET is not set');
    return res.status(503).json({ error: 'Reminders are not configured' });
  }
  const expected = Buffer.from(`Bearer ${secret}`);
  const given = Buffer.from(String(req.headers?.authorization ?? ''));
  if (given.length !== expected.length || !crypto.timingSafeEqual(given, expected)) {
    return res.status(401).json({ error: 'Unauthorized' });
  }

  try {
    const result = await sendExpiryReminders(getPool());
    return res.status(200).json(result);
  } catch (err) {
    console.error('points expiry reminders error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}
