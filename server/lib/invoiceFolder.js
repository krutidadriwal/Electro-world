const { findUserFolder } = require('./drive');

// Resolves the Drive folder holding a user's invoice PDFs: the subfolder of
// DRIVE_ROOT_FOLDER_ID named after their 10-digit phone number. Looked up on
// Drive the first time and cached on the users row from then on.
//
// Returns undefined if there's no such user, null if they have no invoice
// folder (yet), otherwise the folder id.
async function resolveInvoiceFolderId(pool, normalizedPhone) {
  const userResult = await pool.query(
    `select drive_folder_id from public.users where phone = $1`,
    [normalizedPhone]
  );
  if (userResult.rowCount === 0) {
    return undefined;
  }

  const cachedFolderId = userResult.rows[0].drive_folder_id;
  if (cachedFolderId) {
    return cachedFolderId;
  }

  const rootFolderId = process.env.DRIVE_ROOT_FOLDER_ID;
  if (!rootFolderId) {
    throw new Error('DRIVE_ROOT_FOLDER_ID environment variable is not set.');
  }
  const folder = await findUserFolder(rootFolderId, normalizedPhone.slice(-10));
  if (!folder) {
    return null;
  }
  await pool.query(`update public.users set drive_folder_id = $1 where phone = $2`, [folder.id, normalizedPhone]);
  return folder.id;
}

module.exports = { resolveInvoiceFolderId };
