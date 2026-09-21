// Minimal Google Drive v3 REST wrapper authenticated as a service account
// (JWT bearer grant, RFC 7523) -- no user consent screen, no refresh token,
// no expiry to babysit. The service account's own email must be added as a
// collaborator (Editor) on the Drive folder this app reads/writes.

const jwt = require('jsonwebtoken');

const TOKEN_URL = 'https://oauth2.googleapis.com/token';
const DRIVE_API = 'https://www.googleapis.com/drive/v3';
const DRIVE_SCOPE = 'https://www.googleapis.com/auth/drive';
const JWT_BEARER_GRANT = 'urn:ietf:params:oauth:grant-type:jwt-bearer';

let cachedToken = null;
let cachedTokenExpiresAt = 0;

function loadServiceAccount() {
  const raw = process.env.GOOGLE_SERVICE_ACCOUNT_JSON;
  if (!raw) {
    throw new Error('GOOGLE_SERVICE_ACCOUNT_JSON must be set to the service account key JSON.');
  }
  let parsed;
  try {
    parsed = JSON.parse(raw);
  } catch (err) {
    throw new Error('GOOGLE_SERVICE_ACCOUNT_JSON is not valid JSON.');
  }
  if (!parsed.client_email || !parsed.private_key) {
    throw new Error('GOOGLE_SERVICE_ACCOUNT_JSON is missing client_email or private_key.');
  }
  return parsed;
}

async function getAccessToken() {
  if (cachedToken && Date.now() < cachedTokenExpiresAt) {
    return cachedToken;
  }

  const { client_email: clientEmail, private_key: privateKey } = loadServiceAccount();
  const now = Math.floor(Date.now() / 1000);

  // Self-signed JWT asserting this service account's identity, exchanged
  // below for a short-lived (1hr) Drive access token. This assertion step
  // needs no stored, long-lived credential beyond the key itself, which
  // Google never expires or rotates out from under you.
  const assertion = jwt.sign(
    {
      iss: clientEmail,
      scope: DRIVE_SCOPE,
      aud: TOKEN_URL,
      iat: now,
      exp: now + 3600
    },
    privateKey,
    { algorithm: 'RS256' }
  );

  const response = await fetch(TOKEN_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: JWT_BEARER_GRANT,
      assertion
    })
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`Failed to obtain Google service account access token: ${response.status} ${errText}`);
  }

  const data = await response.json();
  cachedToken = data.access_token;
  // Refresh a minute early to avoid using a token that expires mid-request.
  cachedTokenExpiresAt = Date.now() + (data.expires_in - 60) * 1000;
  return cachedToken;
}

async function driveRequest(path, { params, ...init } = {}) {
  const token = await getAccessToken();
  const url = new URL(`${DRIVE_API}${path}`);
  // Drive API v3 ignores Shared Drive content by default -- these two flags
  // opt every call into seeing it too. Harmless for plain My Drive files
  // (the invoices folder); required for the notifications folder, which
  // lives in a Shared Drive (a bare service account has no storage quota of
  // its own, so file *uploads* only succeed inside a Shared Drive).
  const mergedParams = { supportsAllDrives: 'true', includeItemsFromAllDrives: 'true', ...params };
  for (const [key, value] of Object.entries(mergedParams)) {
    url.searchParams.set(key, value);
  }

  const response = await fetch(url, {
    ...init,
    headers: {
      ...init.headers,
      Authorization: `Bearer ${token}`
    }
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`Drive API request failed: ${response.status} ${errText}`);
  }

  return response;
}

// Escapes a value for safe interpolation into a Drive API `q` string literal.
function escapeQueryValue(value) {
  return value.replace(/\\/g, '\\\\').replace(/'/g, "\\'");
}

async function findUserFolder(rootFolderId, folderName) {
  const q = `'${rootFolderId}' in parents and mimeType = 'application/vnd.google-apps.folder' and name = '${escapeQueryValue(folderName)}' and trashed = false`;
  const response = await driveRequest('/files', {
    params: { q, fields: 'files(id, name)', pageSize: '1' }
  });
  const data = await response.json();
  return data.files && data.files.length > 0 ? data.files[0] : null;
}

async function listPdfFiles(folderId) {
  const q = `'${folderId}' in parents and mimeType = 'application/pdf' and trashed = false`;
  const response = await driveRequest('/files', {
    params: { q, fields: 'files(id, name, createdTime, size)', orderBy: 'createdTime desc', pageSize: '100' }
  });
  const data = await response.json();
  return data.files || [];
}

async function getFileMetadata(fileId) {
  const response = await driveRequest(`/files/${fileId}`, {
    params: { fields: 'id, name, parents, mimeType' }
  });
  return response.json();
}

async function downloadFile(fileId) {
  const response = await driveRequest(`/files/${fileId}`, { params: { alt: 'media' } });
  const arrayBuffer = await response.arrayBuffer();
  return Buffer.from(arrayBuffer);
}

// Uploads a file into folderId via the multipart upload endpoint (metadata +
// content in one request -- simplest option for the image sizes a
// notification image realistically needs). Makes the file readable by
// anyone with the link, since notification images are shown in the
// customer app, not gated per-user like invoices.
async function uploadFile(folderId, filename, mimeType, buffer) {
  const boundary = `drive-upload-${Date.now()}-${Math.random().toString(16).slice(2)}`;
  const metadata = JSON.stringify({ name: filename, parents: [folderId] });

  const body = Buffer.concat([
    Buffer.from(
      `--${boundary}\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n${metadata}\r\n` +
        `--${boundary}\r\nContent-Type: ${mimeType}\r\n\r\n`
    ),
    buffer,
    Buffer.from(`\r\n--${boundary}--`)
  ]);

  const token = await getAccessToken();
  const response = await fetch(
    `https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name&supportsAllDrives=true`,
    {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': `multipart/related; boundary=${boundary}`
      },
      body
    }
  );
  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`Drive upload failed: ${response.status} ${errText}`);
  }
  const file = await response.json();

  await driveRequest(`/files/${file.id}/permissions`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ role: 'reader', type: 'anyone' })
  });

  return { id: file.id, name: file.name, url: `https://drive.google.com/uc?id=${file.id}` };
}

module.exports = { findUserFolder, listPdfFiles, getFileMetadata, downloadFile, uploadFile };
