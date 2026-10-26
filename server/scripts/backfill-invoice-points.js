// One-off: brings every recorded invoice's points in line with the current
// rules -- 50 per full Rs 5,000, only for invoices dated on or after
// POINTS_START_DATE (see lib/invoicePoints.js). Invoices credited before
// those rules (or before invoice dates were read) can carry the wrong
// amount; the regular sync deliberately never touches an existing credit,
// since fixing one can lower a customer's balance.
//
// Uses each invoice's stored date and total. One recorded before dates
// were read, whose customer hasn't opened the Points tab since (which
// fills the date in), is read from its PDF here instead.
//
// Credits are rewritten in place rather than offset with adjustment
// entries, so a customer's history just shows the correct credit (dated
// with the purchase), or none.
//
// Dry run by default -- prints what would change and writes nothing:
//   node scripts/backfill-invoice-points.js
// Then, to write it:
//   node scripts/backfill-invoice-points.js --apply
//
// Needs DATABASE_URL and the Drive env vars (GOOGLE_SERVICE_ACCOUNT_JSON),
// same as the server, and schema.sql applied first.

const { Pool } = require('pg');
const { readInvoice, purchaseReason, pointsForInvoice, needsReview } = require('../lib/invoicePoints');

const apply = process.argv.includes('--apply');

// pdf.js logs a "Warning: ..." line for every quirk in a PDF's encoding --
// harmless, and it would bury the report.
const log = console.log;
console.log = (...args) => {
  if (typeof args[0] === 'string' && args[0].startsWith('Warning:')) return;
  log(...args);
};

async function main() {
  const connectionString = process.env.DATABASE_URL;
  if (!connectionString) {
    throw new Error('DATABASE_URL environment variable is not set.');
  }
  const pool = new Pool({ connectionString, ssl: { rejectUnauthorized: false }, max: 4 });

  try {
    // Flagged (unreadable) invoices are an admin's to sort out from the
    // staff app, and are never credited, so they're left alone here.
    const rows = (
      await pool.query(
        `select ip.file_id, ip.phone, ip.invoice_name, ip.amount::float8 as amount,
                ip.invoice_date::text as invoice_date, ip.points,
                coalesce(sum(pl.points), 0)::int as credited
         from public.invoice_points ip
         left join public.points_ledger pl
           on pl.phone = ip.phone and pl.reason = 'Purchase - Invoice ' || regexp_replace(ip.invoice_name, '\\.pdf$', '', 'i')
         where not ip.unreadable
         group by ip.file_id
         order by ip.phone, ip.invoice_name`
      )
    ).rows;
    log(`${rows.length} recorded invoice(s)${apply ? '' : ' (dry run -- nothing will be written)'}\n`);

    let changed = 0;
    let failed = 0;
    for (const row of rows) {
      let { amount, invoice_date: invoiceDate } = row;
      const readNow = invoiceDate === null;
      if (readNow) {
        try {
          const read = await readInvoice({ id: row.file_id, name: row.invoice_name });
          invoiceDate = read.invoiceDate;
          amount = amount ?? read.amount;
        } catch (err) {
          failed++;
          log(`SKIP   ${row.invoice_name} (${row.phone}): could not download/read -- ${err.message.slice(0, 100)}`);
          continue;
        }
      }

      const points = pointsForInvoice(amount, invoiceDate);
      // A date still unreadable on a credited invoice: no telling whether it
      // qualifies, so its credit is left as it is rather than guessed at.
      const undecidable = invoiceDate === null && row.credited > 0;
      const willChange = !undecidable && (row.credited !== points || row.points !== points);
      if (willChange) changed++;

      log(
        `${willChange ? 'CHANGE' : '      '} ${row.invoice_name.padEnd(24)} ${row.phone}  ` +
          `date ${invoiceDate ?? 'UNREADABLE'}${readNow ? ' (read now)' : ''}  amount ${amount ?? 'UNREADABLE'}  ` +
          `points ${row.credited} -> ${undecidable ? `${row.credited} (left as is: date unreadable)` : points}`
      );

      if (!apply || (!willChange && !readNow)) continue;

      const client = await pool.connect();
      try {
        await client.query('begin');
        await client.query(
          `update public.invoice_points
           set amount = $2, invoice_date = $3, points = $4, unreadable = $5
           where file_id = $1`,
          [
            row.file_id,
            amount,
            invoiceDate,
            undecidable ? row.points : points,
            !undecidable && points === 0 && needsReview(amount, invoiceDate)
          ]
        );
        if (willChange) {
          const reason = purchaseReason(row.invoice_name);
          await client.query(`delete from public.points_ledger where phone = $1 and reason = $2`, [row.phone, reason]);
          if (points > 0) {
            // Expiry counts from when the invoice was first credited (its
            // processed_at), same as schema.sql's backfill -- a correction
            // here mustn't extend it.
            await client.query(
              `insert into public.points_ledger (phone, points, reason, created_at, expires_at)
               select $1, $2, $3, $4::date::timestamp at time zone 'UTC', processed_at + interval '6 months'
               from public.invoice_points where file_id = $5`,
              [row.phone, points, reason, invoiceDate, row.file_id]
            );
          }
        }
        await client.query('commit');
      } catch (err) {
        await client.query('rollback');
        throw err;
      } finally {
        client.release();
      }
    }

    log(`\n${rows.length - failed} checked, ${changed} with a points change, ${failed} skipped.${apply ? ' Written.' : ' Dry run: re-run with --apply to write.'}`);
  } finally {
    await pool.end();
  }
}

main().catch((err) => {
  console.error('Backfill failed:', err);
  process.exit(1);
});
