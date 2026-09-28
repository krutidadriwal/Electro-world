// Parses the Tally-exported retail price list and syncs it into
// public.price_list_items (see server/api/staff/[...path].js's
// price-list/list and price-list/sync routes).
//
// The source file (named *.json, Drive file id PRICE_LIST_DRIVE_FILE_ID) is
// NOT valid JSON -- it's a raw Tally TDL export: UTF-16LE encoded with a BOM,
// duplicate "EWSRNO" keys back to back with no array wrapper, and some
// numeric fields (e.g. "EWDSCMRP": 41 %,) left unquoted with a trailing "%".
// Rather than fight a JSON.parse into accepting this, each record is
// extracted field-by-field with tolerant regexes.

const { getPool } = require('./db');
const { downloadFile } = require('./drive');

function decodeText(buffer) {
  if (buffer[0] === 0xff && buffer[1] === 0xfe) {
    return buffer.toString('utf16le').slice(1); // drop the BOM
  }
  if (buffer[0] === 0xef && buffer[1] === 0xbb && buffer[2] === 0xbf) {
    return buffer.toString('utf8').slice(1);
  }
  return buffer.toString('utf8');
}

// Undoes the backslash-escaping inside a quoted field (e.g. \" -> ", \\ -> \).
function unescapeQuoted(value) {
  return value.replace(/\\(.)/g, '$1');
}

function extractString(record, key) {
  const match = record.match(new RegExp(`"${key}"\\s*:\\s*"((?:[^"\\\\]|\\\\.)*)"`));
  return match ? unescapeQuoted(match[1]) : null;
}

function extractNumber(record, key) {
  const match = record.match(new RegExp(`"${key}"\\s*:\\s*(-?[0-9.]+)`));
  return match ? Number(match[1]) : null;
}

// Returns { itemName, category, groupName, finalPrice, stockLabel }[].
// Records with no item name are skipped (nothing to display); a missing
// final price defaults to 0 rather than dropping the item, since a
// zero/unpriced item should still show up in the price list.
function parsePriceListDocument(buffer) {
  const text = decodeText(buffer);
  const records = text.split(/(?="EWSRNO":)/g).slice(1);

  const items = [];
  for (const record of records) {
    const itemName = extractString(record, 'EWItemName');
    if (!itemName) continue;
    items.push({
      itemName,
      category: extractString(record, 'EWCategory') || '',
      groupName: extractString(record, 'EWGroup') || '',
      finalPrice: extractNumber(record, 'EWFINAL') ?? 0,
      stockLabel: extractString(record, 'EWCLSTK') || ''
    });
  }
  return items;
}

// Re-downloads and re-parses the source file, then atomically replaces the
// entire price_list_items table with the new snapshot (full delete + bulk
// insert rather than an upsert -- there's no stable external id to match on
// across syncs, and it's just a browsable catalog with nothing else
// referencing it by row id).
async function syncPriceList() {
  const driveFileId = process.env.PRICE_LIST_DRIVE_FILE_ID;
  if (!driveFileId) {
    throw new Error('PRICE_LIST_DRIVE_FILE_ID environment variable is not set.');
  }

  const buffer = await downloadFile(driveFileId);
  const items = parsePriceListDocument(buffer);

  const pool = getPool();
  await pool.query('begin');
  try {
    await pool.query('delete from public.price_list_items');

    const rowsPerStatement = 500; // keep parameter counts comfortably under Postgres' 65535 limit
    for (let offset = 0; offset < items.length; offset += rowsPerStatement) {
      const batch = items.slice(offset, offset + rowsPerStatement);
      const params = [];
      const rows = batch.map((item, i) => {
        const base = i * 5;
        params.push(item.category, item.groupName, item.itemName, item.finalPrice, item.stockLabel);
        return `($${base + 1}, $${base + 2}, $${base + 3}, $${base + 4}, $${base + 5})`;
      });
      await pool.query(
        `insert into public.price_list_items (category, group_name, item_name, final_price, stock_label)
         values ${rows.join(', ')}`,
        params
      );
    }

    await pool.query('commit');
  } catch (err) {
    await pool.query('rollback');
    throw err;
  }

  return { count: items.length, syncedAt: new Date().toISOString() };
}

module.exports = { parsePriceListDocument, syncPriceList };
