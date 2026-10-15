const test = require('node:test');
const assert = require('node:assert/strict');
const {
  parseInvoiceAmount,
  parseInvoiceDate,
  pointsForAmount,
  pointsForInvoice,
  needsReview,
  wordsToNumber
} = require('../lib/invoicePoints');

test('pointsForAmount gives 50 points per full 5,000', () => {
  assert.equal(pointsForAmount(5000), 50);
  assert.equal(pointsForAmount(5001), 50);
  assert.equal(pointsForAmount(7000), 50);
  assert.equal(pointsForAmount(9999), 50);
  assert.equal(pointsForAmount(10000), 100);
  assert.equal(pointsForAmount(22000), 200);
  assert.equal(pointsForAmount(120000), 1200);
});

test('pointsForInvoice only credits invoices dated on or after 11 Oct 2026', () => {
  assert.equal(pointsForInvoice(22000, '2026-10-10'), 0);
  assert.equal(pointsForInvoice(22000, '2026-10-11'), 200);
  assert.equal(pointsForInvoice(22000, '2026-10-12'), 200);
  assert.equal(pointsForInvoice(22000, '2024-09-17'), 0);
  assert.equal(pointsForInvoice(22000, null), 0);
  assert.equal(pointsForInvoice(4999, '2026-10-12'), 0);
});

test('needsReview flags an unreadable date, or an unreadable total on an eligible invoice', () => {
  assert.equal(needsReview(22000, null), true);
  assert.equal(needsReview(null, '2026-10-12'), true);
  assert.equal(needsReview(null, '2026-09-01'), false);
  assert.equal(needsReview(22000, '2026-09-01'), false);
  assert.equal(needsReview(22000, '2026-10-12'), false);
});

test('parseInvoiceDate splits a date glued onto the invoice number', () => {
  const text = 'TAX INVOICE\nInvoice No. :\nRS2-024813-Oct-26\nDate:\nParty:\n... (Applicable From 13-Oct-26)';
  assert.equal(parseInvoiceDate(text, 'RS2-0248.pdf'), '2026-10-13');
});

test('parseInvoiceDate handles single-digit days (no leading zero)', () => {
  assert.equal(parseInvoiceDate('Invoice No. :\nRS2-02378-Oct-26\nDate:', 'RS2-0237.pdf'), '2026-10-08');
  assert.equal(parseInvoiceDate('Invoice No. :\nRS2-01981-Sep-26\nDate:', 'RS2-0198.pdf'), '2026-09-01');
});

test('parseInvoiceDate takes the serial length from the file name, else assumes 4 digits', () => {
  // A 3-digit serial named in the file: "RS2-123" + "15-Oct-26".
  assert.equal(parseInvoiceDate('Invoice No. :\nRS2-12315-Oct-26\nDate:', 'Copy of RS2-123.pdf'), '2026-10-15');
  // No usable file name: "RS2-0237" + "8-Oct-26".
  assert.equal(parseInvoiceDate('Invoice No. :\nRS2-02378-Oct-26\nDate:', '1.pdf'), '2026-10-08');
});

test('parseInvoiceDate reads the spaced-out header layout', () => {
  assert.equal(parseInvoiceDate('Invoice No. : RS2-0246 Date : 12-Oct-26', 'RS2-0246.pdf'), '2026-10-12');
});

test('parseInvoiceDate falls back to the warranty box when the header has no invoice number', () => {
  const text = 'Invoice No. :\nTAX INVOICE24-Sep-26\nDate:\n... Manufacturer’s Warrantee Details (Applicable From 24-Sep-26)';
  assert.equal(parseInvoiceDate(text, 'RS2-0224.pdf'), '2026-09-24');
});

test('parseInvoiceDate returns null when the header and warranty box disagree', () => {
  const text = 'Invoice No. :\nRS2-024813-Oct-26\nDate:\n... (Applicable From 14-Oct-26)';
  assert.equal(parseInvoiceDate(text, 'RS2-0248.pdf'), null);
});

test('parseInvoiceDate returns null when there is no real date to read', () => {
  assert.equal(parseInvoiceDate('Invoice No. :\nRS2-024831-Feb-26\nDate:', 'RS2-0248.pdf'), null);
  assert.equal(parseInvoiceDate('no dates here', 'RS2-0248.pdf'), null);
  assert.equal(parseInvoiceDate(undefined, 'RS2-0248.pdf'), null);
});

test('pointsForAmount gives nothing for an invoice under 5,000', () => {
  assert.equal(pointsForAmount(4999), 0);
  assert.equal(pointsForAmount(4999.99), 0);
  assert.equal(pointsForAmount(100), 0);
});

test('pointsForAmount gives nothing for a missing or non-positive amount', () => {
  assert.equal(pointsForAmount(null), 0);
  assert.equal(pointsForAmount(0), 0);
  assert.equal(pointsForAmount(-5000), 0);
  assert.equal(pointsForAmount(NaN), 0);
});

test('wordsToNumber reads Indian-style amounts in words', () => {
  assert.equal(wordsToNumber('Eighty Four Thousand Five Hundred'), 84500);
  assert.equal(wordsToNumber('Seventeen Thousand'), 17000);
  assert.equal(wordsToNumber('Six Thousand Two Hundred'), 6200);
  assert.equal(wordsToNumber('One Lakh Twenty Thousand'), 120000);
  assert.equal(wordsToNumber('Two Lakh Five Thousand Nine Hundred and Ninety Nine'), 205999);
  assert.equal(wordsToNumber('One Crore'), 10000000);
  assert.equal(wordsToNumber('Nine Hundred Ninety-Nine'), 999);
});

test('wordsToNumber returns null for words it does not recognise', () => {
  assert.equal(wordsToNumber('Eighty Four Thousand Bananas'), null);
  assert.equal(wordsToNumber(''), null);
});

test('parseInvoiceAmount reads the RS2-/EW- layout (rounded total + words)', () => {
  const text = [
    'Total 1 Pcs 84,500.23',
    'Amount in words : Eighty Four Thousand Five Hundred INR Only. Invoice Total (Rounded) 84,500.00',
    'Manufacturer’s Warrantee Details (Applicable From 12-Oct-26)'
  ].join('\n');
  assert.equal(parseInvoiceAmount(text), 84500);
});

test('parseInvoiceAmount reads the PS2- layout (words only)', () => {
  const text = ['CGST 1296.61', 'SGST 1296.61', 'Rounded Off (-)0.04', 'Total 17000.00', 'Amount in words : Seventeen Thousand INR Only.'].join('\n');
  assert.equal(parseInvoiceAmount(text), 17000);
});

test('parseInvoiceAmount tolerates run-together and spaced-out text', () => {
  assert.equal(parseInvoiceAmount('Amount in words: Forty One Thousand INR Only.Invoice Total (Rounded)41,000.00'), 41000);
  assert.equal(parseInvoiceAmount('Invoice Total (Rounded)\n47, 500. 00'), 47500);
  assert.equal(parseInvoiceAmount('Amount in words : One Lakh Twenty\nThousand INR Only.'), 120000);
});

test('parseInvoiceAmount returns null when the two totals disagree', () => {
  const text = 'Amount in words : Forty One Thousand INR Only. Invoice Total (Rounded) 14,000.00';
  assert.equal(parseInvoiceAmount(text), null);
});

test('parseInvoiceAmount returns null when there is no total to read', () => {
  assert.equal(parseInvoiceAmount('Total 1 Pcs 84,500.23'), null);
  assert.equal(parseInvoiceAmount(''), null);
  assert.equal(parseInvoiceAmount(undefined), null);
});
