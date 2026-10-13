// Purchase points: every invoice PDF in a user's Drive folder earns points
// from its invoice total, credited to public.points_ledger exactly once per
// file (tracked in public.invoice_points). There's no upload hook to hang
// this on -- staff drop PDFs straight into Drive -- so it runs lazily, from
// GET /api/points, picking up whatever's new since the last time.

const { listPdfFiles, downloadFile } = require('./drive');
const { resolveInvoiceFolderId } = require('./invoiceFolder');
const { notifyStaff } = require('./staffNotify');

// 50 points per Rs 50,000 spent, pro rata (so Rs 1,000 = 1 point), rounded
// down to a whole point.
const POINTS_PER_BLOCK = 50;
const AMOUNT_PER_BLOCK = 50000;

// Each new invoice costs a Drive download plus a PDF parse, all inside one
// request -- a backlog bigger than this is worked through over the next
// few calls rather than risking the function timeout.
const MAX_INVOICES_PER_SYNC = 10;

function pointsForAmount(amount) {
  if (typeof amount !== 'number' || !Number.isFinite(amount) || amount <= 0) {
    return 0;
  }
  return Math.floor((amount * POINTS_PER_BLOCK) / AMOUNT_PER_BLOCK);
}

const SMALL_NUMBERS = {
  zero: 0, one: 1, two: 2, three: 3, four: 4, five: 5, six: 6, seven: 7, eight: 8, nine: 9,
  ten: 10, eleven: 11, twelve: 12, thirteen: 13, fourteen: 14, fifteen: 15, sixteen: 16,
  seventeen: 17, eighteen: 18, nineteen: 19,
  twenty: 20, thirty: 30, forty: 40, fourty: 40, fifty: 50, sixty: 60, seventy: 70, eighty: 80, ninety: 90
};

// Indian numbering, as Tally writes it out ("One Lakh Twenty Thousand").
const MULTIPLIERS = { thousand: 1000, lakh: 100000, lac: 100000, lakhs: 100000, crore: 10000000, crores: 10000000 };

// "Eighty Four Thousand Five Hundred" -> 84500. Null on any word it doesn't
// recognise, rather than a guess.
function wordsToNumber(words) {
  const tokens = words.toLowerCase().split(/[\s-]+/).filter((token) => token && token !== 'and');
  if (tokens.length === 0) {
    return null;
  }

  let total = 0;
  let current = 0;
  for (const token of tokens) {
    if (token in SMALL_NUMBERS) {
      current += SMALL_NUMBERS[token];
    } else if (token === 'hundred') {
      current = (current || 1) * 100;
    } else if (token in MULTIPLIERS) {
      total += (current || 1) * MULTIPLIERS[token];
      current = 0;
    } else {
      return null;
    }
  }
  return total + current;
}

// Pulls the invoice total out of an invoice PDF's extracted text. The store
// prints several layouts (invoice series RS2-/EW-/PS2-), and where a table
// cell's text lands relative to its label isn't stable from one to the next,
// so this only trusts the two fields that are always a single run of text:
//
//   "Invoice Total (Rounded) 84,500.00"   -- RS2-/EW- layouts only
//   "Amount in words : Eighty Four Thousand Five Hundred INR Only."  -- all
//
// When both are present they must agree; a mismatch means the text didn't
// extract the way this expects, and no points beats wrong points.
function parseInvoiceAmount(text) {
  if (typeof text !== 'string') {
    return null;
  }

  let numeric = null;
  const numericMatch = text.match(/Invoice\s+Total\s*\(Rounded\)\s*:?\s*(\d[\d,\s]*\.\s?\d{2})/i);
  if (numericMatch) {
    const parsed = Number(numericMatch[1].replace(/[,\s]/g, ''));
    numeric = Number.isFinite(parsed) ? parsed : null;
  }

  let fromWords = null;
  const wordsMatch = text.match(/Amount\s+in\s+words\s*:?\s*([A-Za-z\s-]+?)\s*INR\s+Only/i);
  if (wordsMatch) {
    fromWords = wordsToNumber(wordsMatch[1]);
  }

  if (numeric !== null && fromWords !== null && Math.round(numeric) !== fromWords) {
    return null;
  }
  const amount = numeric ?? fromWords;
  return amount !== null && amount > 0 ? amount : null;
}

async function extractPdfText(buffer) {
  // Required lazily (only a request that actually has a new invoice pays for
  // loading pdf.js), and from lib/ rather than the package root: pdf-parse's
  // index.js runs a debug self-test that reads a sample PDF off disk whenever
  // it decides it's the entry module, which it does under a bundler.
  const pdfParse = require('pdf-parse/lib/pdf-parse.js');
  const { text } = await pdfParse(buffer);
  return text;
}

async function creditInvoice(pool, normalizedPhone, file) {
  const text = await extractPdfText(await downloadFile(file.id));
  const amount = parseInvoiceAmount(text);
  const points = pointsForAmount(amount);
  const invoiceNumber = file.name.replace(/\.pdf$/i, '');

  if (amount === null) {
    console.error('could not read an invoice total from', file.name, file.id);
  }

  // One statement, so the "already processed" marker and the ledger credit
  // can't come apart: two overlapping requests both reach here for the same
  // file, only one insert wins, and only that one credits. An unreadable
  // invoice is still recorded (amount null, no credit) so it isn't
  // re-downloaded on every call -- admins see those in the staff app's
  // Notifications tab and can have one checked again from there (see
  // unreadableInvoicesRetry in api/staff/[...path].js).
  const result = await pool.query(
    `with recorded as (
       insert into public.invoice_points (file_id, phone, invoice_name, amount, points)
       values ($1, $2, $3, $4, $5)
       on conflict (file_id) do nothing
       returning phone, points
     ),
     credited as (
       insert into public.points_ledger (phone, points, reason)
       select phone, points, $6 from recorded where points > 0
     )
     select count(*)::int as recorded from recorded`,
    [file.id, normalizedPhone, file.name, amount, points, `Purchase - Invoice ${invoiceNumber}`]
  );

  // True only for the request that actually recorded this invoice as
  // unreadable, so admins are pushed about it once, not once per racer.
  return amount === null && result.rows[0].recorded === 1;
}

// notify: false is for an admin-triggered re-check, where the admin is
// already looking at the result and doesn't need a push about it too.
async function syncInvoicePoints(pool, normalizedPhone, { notify = true } = {}) {
  const folderId = await resolveInvoiceFolderId(pool, normalizedPhone);
  if (!folderId) {
    return;
  }

  const files = await listPdfFiles(folderId);
  if (files.length === 0) {
    return;
  }

  const processedResult = await pool.query(
    `select file_id from public.invoice_points where file_id = any($1)`,
    [files.map((file) => file.id)]
  );
  const processed = new Set(processedResult.rows.map((row) => row.file_id));
  const pending = files.filter((file) => !processed.has(file.id)).slice(0, MAX_INVOICES_PER_SYNC);

  // A file that fails here (Drive hiccup, corrupt PDF) is left unrecorded,
  // so it's simply retried on the next call.
  const newlyUnreadable = await Promise.all(
    pending.map((file) =>
      creditInvoice(pool, normalizedPhone, file).catch((err) => {
        console.error('invoice points credit error', file.id, err);
        return false;
      })
    )
  );

  if (notify && newlyUnreadable.some(Boolean)) {
    const countResult = await pool.query(
      `select count(*)::int as count from public.invoice_points where amount is null`
    );
    const unreadableCount = countResult.rows[0].count;
    // No "tab:status" target of its own -- the staff app sends anything it
    // doesn't recognise to its Notifications tab, which is where these are
    // listed (and it means app builds from before this existed land there too).
    await notifyStaff({
      adminOnly: true,
      title: 'EW Ops Hub',
      body: `${unreadableCount} invoice${unreadableCount === 1 ? '' : 's'} could not be read for points`,
      target: 'unreadable_invoices'
    });
  }
}

module.exports = { syncInvoicePoints, parseInvoiceAmount, pointsForAmount, wordsToNumber };
