const test = require('node:test');
const assert = require('node:assert/strict');
const { normalizeDispatch, planVersions, readVersions, updateProperties } = require('./pack-release');

const currentProperties = `drivepersona.voicepack.version=0.2.0\ndrivepersona.characterpack.version=0.1.1\n`;
const current = readVersions(currentProperties);

test('voice-only dispatch plans only the voice package', () => {
  const release = normalizeDispatch('voicepack-published', { version: '0.3.0' });
  const plan = planVersions(current, release);
  assert.deepEqual(plan.changed, ['voice-pack-sdk']);
  assert.equal(plan.target['character-pack-sdk'], '0.1.1');
  assert.deepEqual(readVersions(updateProperties(currentProperties, plan.target, plan.changed)), {
    'voice-pack-sdk': '0.3.0',
    'character-pack-sdk': '0.1.1',
  });
});

test('character-only dispatch plans only the character package', () => {
  const release = normalizeDispatch('characterpack-published', { version: '0.4.0' });
  const plan = planVersions(current, release);
  assert.deepEqual(plan.changed, ['character-pack-sdk']);
  assert.equal(plan.target['voice-pack-sdk'], '0.2.0');
});

test('one dispatch can update both packages and retain release metadata', () => {
  const release = normalizeDispatch('packs-published', {
    packages: { 'voice-pack-sdk': '0.3.0', 'character-pack-sdk': '0.4.0' },
    contentMetadata: { matrixVersion: 'v1', characterIds: ['hero.a', 'hero.b'], notes: 'row one\nrow two', ignored: 'not copied' },
  });
  const plan = planVersions(current, release);
  assert.deepEqual(plan.changed, ['voice-pack-sdk', 'character-pack-sdk']);
  assert.deepEqual(release.metadata, { matrixVersion: 'v1', characterIds: 'hero.a, hero.b', notes: 'row one row two' });
});

test('already applied package versions are a no-op', () => {
  const release = normalizeDispatch('voicepack-published', { package: 'voice-pack-sdk', version: '0.2.0' });
  const plan = planVersions(current, release);
  assert.equal(plan.isNoop, true);
  assert.deepEqual(plan.changed, []);
});

test('unknown packages and malformed versions are rejected', () => {
  assert.throws(() => normalizeDispatch('packs-published', { package: 'other', version: '1.0.0' }));
  assert.throws(() => normalizeDispatch('voicepack-published', { version: 'main' }));
  assert.throws(() => normalizeDispatch('packs-published', { packages: {} }));
});
