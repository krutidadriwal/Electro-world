const test = require('node:test');
const assert = require('node:assert/strict');
const { parseInvoiceAmount, pointsForAmount, wordsToNumber } = require('../lib/invoicePoints');

test('pointsForAmount gives 50 points per 50,000, pro rata and rounded down', () => {
  assert.equal(pointsForAmount(50000), 50);
  assert.equal(pointsForAmount(100000), 100);
  assert.equal(pointsForAmount(84500), 84);
  assert.equal(pointsForAmount(17000), 17);
  assert.equal(pointsForAmount(999), 0);
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
