'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');

const CANONICAL_EVENTS = [
  'GREETING', 'BRAKE', 'TURN', 'MODE', 'TIRE', 'WARNING', 'DOOR', 'WINDOW',
  'AMBIENT_LIGHT', 'HVAC', 'WIPER', 'CHARGE', 'ENERGY_LOW',
];
const REVIEW_STATES = new Set([
  'DRAFT', 'TEXT_APPROVED', 'AUDIO_GENERATED', 'AUDIO_APPROVED', 'PACK_READY', 'RELEASE_READY',
]);

function parseCsv(source) {
  const rows = [];
  let row = [];
  let field = '';
  let quoted = false;
  source = source.replace(/^\uFEFF/, '');

  for (let i = 0; i < source.length; i++) {
    const char = source[i];
    if (quoted) {
      if (char === '"' && source[i + 1] === '"') {
        field += '"';
        i++;
      } else if (char === '"') {
        quoted = false;
      } else {
        field += char;
      }
    } else if (char === '"' && field.length === 0) {
      quoted = true;
    } else if (char === ',') {
      row.push(field);
      field = '';
    } else if (char === '\n') {
      row.push(field.replace(/\r$/, ''));
      if (row.some((cell) => cell.length > 0)) rows.push(row);
      row = [];
      field = '';
    } else {
      field += char;
    }
  }
  if (field.length || row.length) {
    row.push(field.replace(/\r$/, ''));
    rows.push(row);
  }
  assert.equal(quoted, false, 'CSV contains an unterminated quoted field');
  const [headers, ...data] = rows;
  return data.map((cells) => Object.fromEntries(headers.map((header, index) => [header, cells[index] ?? ''])));
}

const matrixPath = path.resolve(__dirname, '../docs/DIALOGUE_MATRIX_V1.csv');
const matrix = parseCsv(fs.readFileSync(matrixPath, 'utf8'));

test('each candidate has all 13 canonical, non-empty draft dialogue slots', () => {
  const requiredFields = ['characterId', 'displayName', 'voicePersona', 'eventId', 'text', 'drivingPolicy', 'sourceCapability', 'reviewStatus'];
  assert.ok(matrix.length > 0, 'matrix must contain rows');
  for (const row of matrix) {
    for (const field of requiredFields) assert.ok(row[field]?.trim(), `${field} is required for ${row.characterId}/${row.eventId}`);
    assert.ok(CANONICAL_EVENTS.includes(row.eventId), `unknown eventId ${row.eventId}`);
    assert.ok(REVIEW_STATES.has(row.reviewStatus), `unknown reviewStatus ${row.reviewStatus}`);
  }

  const byCharacter = Map.groupBy(matrix, (row) => row.characterId);
  for (const [characterId, rows] of byCharacter) {
    assert.equal(rows.length, CANONICAL_EVENTS.length, `${characterId} must have exactly 13 rows`);
    assert.deepEqual(rows.map((row) => row.eventId).sort(), [...CANONICAL_EVENTS].sort(), `${characterId} must cover each event once`);
    assert.equal(new Set(rows.map((row) => row.text)).size, rows.length, `${characterId} has repeated dialogue`);
    assert.equal(new Set(rows.map((row) => row.displayName)).size, 1, `${characterId} has inconsistent display names`);
    assert.equal(new Set(rows.map((row) => row.voicePersona)).size, 1, `${characterId} has inconsistent voice persona`);
  }
});

test('neutral safety lines are shared, short, and avoid dynamic values or safety guarantees', () => {
  for (const eventId of ['WARNING', 'ENERGY_LOW']) {
    const rows = matrix.filter((row) => row.eventId === eventId);
    assert.equal(new Set(rows.map((row) => row.text)).size, 1, `${eventId} wording must be common across candidates`);
    assert.ok(rows[0].text.length <= 24, `${eventId} should remain a short utterance`);
  }

  for (const row of matrix) {
    assert.doesNotMatch(row.text, /\d|안전합니다|안전해요|잘했어요|문제없어요|보장/);
    if (!['stationary-only', 'silent-by-default-until-policy-approved'].some((policy) => row.drivingPolicy.includes(policy))) {
      assert.ok(row.text.length >= 8 && row.text.length <= 24, `${row.characterId}/${row.eventId} should fit a short driving utterance`);
    }
  }
});
