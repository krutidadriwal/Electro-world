// Minimal Google Drive v3 REST wrapper authenticated via a long-lived OAuth
// refresh token (no googleapis dependency -- just the token endpoint + REST API).

const TOKEN_URL = 'https://oauth2.googleapis.com/token';
const DRIVE_API = 'https://www.googleapis.com/drive/v3';

let cachedToken = null;
let cachedTokenExpiresAt = 0;

async function getAccessToken() {
  if (cachedToken && Date.now() < cachedTokenExpiresAt) {
    return cachedToken;
  }

  const clientId = process.env.GOOGLE_OAUTH_CLIENT_ID;
  const clientSecret = process.env.GOOGLE_OAUTH_CLIENT_SECRET;
  const refreshToken = process.env.GOOGLE_OAUTH_REFRESH_TOKEN;
  if (!clientId || !clientSecret || !refreshToken) {
    throw new Error('GOOGLE_OAUTH_CLIENT_ID, GOOGLE_OAUTH_CLIENT_SECRET and GOOGLE_OAUTH_REFRESH_TOKEN must be set.');
  }

  const response = await fetch(TOKEN_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      client_id: clientId,
      client_secret: clientSecret,
      refresh_token: refreshToken,
      grant_type: 'refresh_token'
    })
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`Failed to refresh Google OAuth access token: ${response.status} ${errText}`);
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
  if (params) {
    for (const [key, value] of Object.entries(params)) {
      url.searchParams.set(key, value);
    }
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

module.exports = { findUserFolder, listPdfFiles, getFileMetadata, downloadFile };
