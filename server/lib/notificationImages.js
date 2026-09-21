// Stores notification images in Supabase Storage rather than Google Drive.
// Drive uploads need either a Shared Drive or domain-wide delegation --
// a bare service account has no storage quota of its own to own a file in a
// regular "My Drive" folder (see lib/drive.js's uploadFile, still used for
// invoices, which only ever reads/lists files staff upload manually).
// Supabase is already this project's auth + DB provider, so its Storage
// (accessed here with the service role key, bypassing RLS) needs no new
// infrastructure -- just the "notification-images" public bucket.

const BUCKET = 'notification-images';

const EXTENSION_BY_MIME_TYPE = {
  'image/jpeg': 'jpg',
  'image/png': 'png',
  'image/webp': 'webp',
  'image/gif': 'gif'
};

function getConfig() {
  const supabaseUrl = process.env.SUPABASE_URL;
  const serviceRoleKey = process.env.SUPABASE_SERVICE_ROLE_KEY;
  if (!supabaseUrl) {
    throw new Error('SUPABASE_URL environment variable is not set.');
  }
  if (!serviceRoleKey) {
    throw new Error('SUPABASE_SERVICE_ROLE_KEY environment variable is not set.');
  }
  return { supabaseUrl, serviceRoleKey };
}

// Uploads buffer to the bucket and returns { path, url } -- url is the
// bucket's public URL (the bucket itself is public; no signed URL needed).
async function uploadNotificationImage(filename, mimeType, buffer) {
  const { supabaseUrl, serviceRoleKey } = getConfig();
  const extension = EXTENSION_BY_MIME_TYPE[mimeType];
  const objectPath = `${Date.now()}-${filename}${extension ? `.${extension}` : ''}`;

  const response = await fetch(`${supabaseUrl}/storage/v1/object/${BUCKET}/${objectPath}`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${serviceRoleKey}`,
      apikey: serviceRoleKey,
      'Content-Type': mimeType
    },
    body: buffer
  });
  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`Supabase Storage upload failed: ${response.status} ${errText}`);
  }

  return {
    path: objectPath,
    url: `${supabaseUrl}/storage/v1/object/public/${BUCKET}/${objectPath}`
  };
}

module.exports = { uploadNotificationImage };
