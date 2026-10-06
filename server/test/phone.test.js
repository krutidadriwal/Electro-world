const test = require('node:test');
const assert = require('node:assert/strict');
const { normalizePhone, PHONE_REGEX } = require('../lib/phone');

test('normalizePhone prefixes a valid 10-digit number with +91 by default', () => {
  assert.equal(normalizePhone('9326763315'), '+919326763315');
});

test('normalizePhone uses the given country code', () => {
  assert.equal(normalizePhone('9326763315', '+1'), '+19326763315');
});

test('normalizePhone rejects non-10-digit input', () => {
  assert.equal(normalizePhone('123'), null);
  assert.equal(normalizePhone('abcdefghij'), null);
  assert.equal(normalizePhone(''), null);
});

test('normalizePhone rejects non-string input', () => {
  assert.equal(normalizePhone(undefined), null);
  assert.equal(normalizePhone(9326763315), null);
});

test('PHONE_REGEX matches exactly 10 digits', () => {
  assert.equal(PHONE_REGEX.test('9326763315'), true);
  assert.equal(PHONE_REGEX.test('93267633155'), false);
  assert.equal(PHONE_REGEX.test('93267-6331'), false);
});
