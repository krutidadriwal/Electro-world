// Purchase points: every invoice PDF in a user's Drive folder is read once
// -- its total and its invoice date -- and recorded in public.invoice_points,
// crediting public.points_ledger exactly once per file when it earns points.
// There's no upload hook to hang this on -- staff drop PDFs straight into
// Drive -- so it runs lazily, picking up whatever's new since the last time,
// and only when the customer opens the app's Points tab (GET /api/points
// with ?sync=true): reading PDFs is the slow, costly part, and every other
// screen just reads what's already stored. The stored invoice date is
// also what the app's invoice list shows, instead of the Drive upload date
// (staff often upload old invoices long after the purchase).

const { listPdfFiles, downloadFile } = require('./drive');
const { resolveInvoiceFolderId } = require('./invoiceFolder');
const { notifyStaff } = require('./staffNotify');

// 50 points for every full Rs 5,000 of the invoice total -- Rs 4,999 earns
// nothing, Rs 7,000 earns 50, Rs 10,000 earns 100.
const POINTS_PER_BLOCK = 50;
const AMOUNT_PER_BLOCK = 5000;

// Only purchases on or after this invoice date earn points. Earlier
// invoices are still read and recorded (for their date), just never credited.
const POINTS_START_DATE = '2026-10-11';

// Each new invoice costs a Drive download plus a PDF parse, all inside one
// request -- a backlog bigger than this is worked through over the next
// few calls rather than risking the function timeout.
const MAX_INVOICES_PER_SYNC = 10;

function pointsForAmount(amount) {
  if (typeof amount !== 'number' || !Number.isFinite(amount) || amount < AMOUNT_PER_BLOCK) {
    return 0;
  }
  return POINTS_PER_BLOCK * Math.floor(amount / AMOUNT_PER_BLOCK);
}

// invoiceDate is "YYYY-MM-DD" (or null if it couldn't be read, which earns
// nothing -- there's no telling whether the purchase qualifies).
function pointsForInvoice(amount, invoiceDate) {
  if (typeof invoiceDate !== 'string' || invoiceDate < POINTS_START_DATE) {
    return 0;
  }
  return pointsForAmount(amount);
}

// Whether an invoice needs an admin to look at it: its date couldn't be
// read, or it's recent enough to earn points but its total couldn't be. An
// old invoice with an unreadable total doesn't -- it earns nothing either way.
function needsReview(amount, invoiceDate) {
  return invoiceDate === null || (amount === null && invoiceDate >= POINTS_START_DATE);
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

const MONTHS = { jan: 0, feb: 1, mar: 2, apr: 3, may: 4, jun: 5, jul: 6, aug: 7, sep: 8, oct: 9, nov: 10, dec: 11 };

// "8", "Oct", "26" -> "2026-10-08", or null if that's not a real date.
function toIsoDate(day, month, year) {
  const monthIndex = MONTHS[month.slice(0, 3).toLowerCase()];
  const fullYear = year.length === 2 ? 2000 + Number(year) : Number(year);
  const dayNumber = Number(day);
  if (monthIndex === undefined || !Number.isInteger(dayNumber)) {
    return null;
  }
  const date = new Date(Date.UTC(fullYear, monthIndex, dayNumber));
  if (date.getUTCFullYear() !== fullYear || date.getUTCMonth() !== monthIndex || date.getUTCDate() !== dayNumber) {
    return null;
  }
  return date.toISOString().slice(0, 10);
}

const DATE = /(\d{1,2})-([A-Za-z]{3})[A-Za-z]*-(\d{4}|\d{2})/.source;
// The invoice number on its own, wherever it turns up (file name or PDF):
// a series code, a hyphen, a numeric serial -- "RS2-0248", "POS-0119".
const INVOICE_NUMBER = /([A-Za-z0-9]+)-(\d+)/;

// Pulls the invoice date (as "YYYY-MM-DD") out of an invoice PDF's text.
//
// The header prints "Invoice No. : RS2-0248   Date : 13-Oct-26", but in the
// extracted text the date usually lands glued straight onto the invoice
// number, with no leading zero on the day: "RS2-02378-Oct-26" is invoice
// RS2-0237 of 8-Oct-26. Where the serial ends and the day begins is taken
// from the file name when it carries the same invoice number (staff name
// the PDFs after it), otherwise from the store's serials being 4 digits.
//
// The warranty box repeats the invoice date ("Applicable From 13-Oct-26")
// as a cross-check, and as the fallback if the header can't be read. Like
// the amount, a disagreement means no date rather than a guess.
function parseInvoiceDate(text, fileName) {
  if (typeof text !== 'string') {
    return null;
  }

  let headerDate = null;
  const spaced = text.match(new RegExp(/Invoice\s*No\.?\s*:?\s*[A-Za-z0-9]+-\d+\s+(?:Date\s*:?\s*)?/.source + DATE, 'i'));
  if (spaced) {
    headerDate = toIsoDate(spaced[1], spaced[2], spaced[3]);
  } else {
    const glued = text.match(/Invoice\s*No\.?\s*:?\s*([A-Za-z0-9]+)-(\d+)-([A-Za-z]{3})[A-Za-z]*-(\d{4}|\d{2})/i);
    if (glued) {
      const [, series, digits, month, year] = glued;
      const fromName = typeof fileName === 'string' ? fileName.match(INVOICE_NUMBER) : null;
      const serialLength = fromName && fromName[1].toUpperCase() === series.toUpperCase() && digits.startsWith(fromName[2])
        ? fromName[2].length
        : 4;
      const day = digits.slice(serialLength);
      if (day.length >= 1 && day.length <= 2) {
        headerDate = toIsoDate(day, month, year);
      }
    }
  }

  let warrantyDate = null;
  const warranty = text.match(new RegExp(/Applicable\s+From\s*/.source + DATE, 'i'));
  if (warranty) {
    warrantyDate = toIsoDate(warranty[1], warranty[2], warranty[3]);
  }

  if (headerDate && warrantyDate && headerDate !== warrantyDate) {
    return null;
  }
  return headerDate ?? warrantyDate;
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

// Downloads and reads one invoice PDF. Shared with
// scripts/backfill-invoice-points.js, which re-reads invoices recorded
// before dates were.
async function readInvoice(file) {
  const text = await extractPdfText(await downloadFile(file.id));
  const amount = parseInvoiceAmount(text);
  const invoiceDate = parseInvoiceDate(text, file.name);
  return {
    amount,
    invoiceDate,
    points: pointsForInvoice(amount, invoiceDate),
    unreadable: needsReview(amount, invoiceDate)
  };
}

// The ledger reason for an invoice's credit -- also how the backfill script
// finds that credit again, so the two must stay in step.
function purchaseReason(fileName) {
  return `Purchase - Invoice ${fileName.replace(/\.pdf$/i, '')}`;
}

async function creditInvoice(pool, normalizedPhone, file) {
  const { amount, invoiceDate, points, unreadable } = await readInvoice(file);

  if (unreadable) {
    console.error('could not read an invoice', amount === null ? 'total' : 'date', 'from', file.name, file.id);
  }

  // One statement, so the "already processed" marker and the ledger credit
  // can't come apart: two overlapping requests both reach here for the same
  // file, only one insert wins, and only that one credits. An unreadable
  // invoice is still recorded (no credit) so it isn't re-downloaded on every
  // call -- admins see those in the staff app's Notifications tab and can
  // have one checked again from there (see unreadableInvoicesRetry in
  // api/staff/[...path].js).
  //
  // The credit is dated with the purchase (midnight UTC of the invoice
  // date, so its date part reads as that day), not with when it was read.
  const result = await pool.query(
    `with recorded as (
       insert into public.invoice_points (file_id, phone, invoice_name, amount, invoice_date, points, unreadable)
       values ($1, $2, $3, $4, $5, $6, $7)
       on conflict (file_id) do nothing
       returning phone, points, invoice_date
     ),
     credited as (
       insert into public.points_ledger (phone, points, reason, created_at)
       select phone, points, $8, invoice_date::timestamp at time zone 'UTC' from recorded where points > 0
     )
     select count(*)::int as recorded from recorded`,
    [file.id, normalizedPhone, file.name, amount, invoiceDate, points, unreadable, purchaseReason(file.name)]
  );

  // True only for the request that actually recorded this invoice as
  // unreadable, so admins are pushed about it once, not once per racer.
  return unreadable && result.rows[0].recorded === 1;
}

// For an invoice recorded before invoice dates were read: stores its date
// (and its total, if that wasn't readable then) and nothing else. Its
// points and any ledger credit are left exactly as they are -- bringing
// those in line with the current rules is scripts/backfill-invoice-points.js,
// a separate, deliberate step, since it can lower a customer's balance.
//
// Only an invoice that was never credited can be flagged for admin review:
// the staff app's "check again" deletes the row and reads the invoice as
// new, which would credit it a second time on top of an existing credit.
async function fillInvoiceDate(pool, file) {
  const { amount, invoiceDate } = await readInvoice(file);
  const result = await pool.query(
    `update public.invoice_points
     set invoice_date = $2,
         amount = coalesce(amount, $3),
         unreadable = (points = 0 and $4)
     where file_id = $1 and invoice_date is null and not unreadable
     returning unreadable`,
    [file.id, invoiceDate, amount, needsReview(amount, invoiceDate)]
  );
  if (invoiceDate === null) {
    console.error('could not read an invoice date from', file.name, file.id);
  }
  return result.rows[0]?.unreadable === true;
}

// files: the user's invoice PDFs, if the caller has already listed them --
// saves listing the Drive folder twice.
// notify: false is for an admin-triggered re-check, where the admin is
// already looking at the result and doesn't need a push about it too.
async function syncInvoicePoints(pool, normalizedPhone, { notify = true, files: listedFiles } = {}) {
  let files = listedFiles;
  if (!files) {
    const folderId = await resolveInvoiceFolderId(pool, normalizedPhone);
    if (!folderId) {
      return;
    }
    files = await listPdfFiles(folderId);
  }
  if (files.length === 0) {
    return;
  }

  // needs_date: recorded before invoice dates were read at all, so it's
  // re-read just for its date (see fillInvoiceDate). Not for a row already
  // flagged unreadable -- that's waiting on an admin, not on a re-read.
  const processedResult = await pool.query(
    `select file_id, (invoice_date is null and not unreadable) as needs_date
     from public.invoice_points where file_id = any($1)`,
    [files.map((file) => file.id)]
  );
  const needsDate = new Map(processedResult.rows.map((row) => [row.file_id, row.needs_date]));
  // New invoices first -- they're the ones that can earn points.
  const pending = [
    ...files.filter((file) => !needsDate.has(file.id)),
    ...files.filter((file) => needsDate.get(file.id) === true)
  ].slice(0, MAX_INVOICES_PER_SYNC);

  // A file that fails here (Drive hiccup, corrupt PDF) is left as it was,
  // so it's simply retried on the next call.
  const newlyUnreadable = await Promise.all(
    pending.map((file) => {
      const work = needsDate.has(file.id) ? fillInvoiceDate(pool, file) : creditInvoice(pool, normalizedPhone, file);
      return work.catch((err) => {
        console.error('invoice points sync error', file.id, err);
        return false;
      });
    })
  );

  if (notify && newlyUnreadable.some(Boolean)) {
    const countResult = await pool.query(
      `select count(*)::int as count from public.invoice_points where unreadable`
    );
    const unreadableCount = countResult.rows[0].count;
    // No "tab:status" target of its own -- the staff app sends anything it
    // doesn't recognise to its Notifications tab, which is where these are
    // listed (and it means app builds from before this existed land there too).
    await notifyStaff({
      adminOnly: true,
      title: 'Electro World Staff',
      body: `${unreadableCount} invoice${unreadableCount === 1 ? '' : 's'} could not be read for points`,
      target: 'unreadable_invoices'
    });
  }
}

module.exports = {
  syncInvoicePoints,
  readInvoice,
  purchaseReason,
  parseInvoiceAmount,
  parseInvoiceDate,
  pointsForAmount,
  pointsForInvoice,
  needsReview,
  wordsToNumber,
  POINTS_START_DATE
};
