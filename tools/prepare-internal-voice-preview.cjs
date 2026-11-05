#!/usr/bin/env node
/*
 * Assemble local, internal-only VoiceDesign candidates for emulator testing.
 * This imports generated Qwen WAVs and candidate GLBs from the private SDK
 * worktrees into app/src/internal/assets. It refuses to overwrite pack dirs.
 */
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');

const appRoot = path.join(__dirname, '..');
const voiceRoot = valueAfter('--voice-repo');
const characterRoot = valueAfter('--character-repo');
if (!voiceRoot || !characterRoot) {
  throw new Error('Usage: node tools/prepare-internal-voice-preview.cjs --voice-repo <path> --character-repo <path>');
}

const events = ['BRAKE', 'TURN', 'MODE', 'TIRE', 'DOOR', 'WINDOW', 'AMBIENT_LIGHT', 'HVAC', 'WIPER', 'CHARGE'];
const expectedTargetCharacterCount = 33;
const expectedBaselinePackCount = 10;
const refsRoot = path.join(voiceRoot, 'generated', 'voice-design-references');
const eventRoot = path.join(voiceRoot, 'generated', 'voicedesign-event-clips');
const matrix = JSON.parse(fs.readFileSync(path.join(voiceRoot, 'docs', 'RELEASE_DIALOGUE_MATRIX_V1.json'), 'utf8'));
const candidatesRoot = path.join(characterRoot, 'candidates');
const contentRoot = path.join(characterRoot, 'characterpack-content', 'src', 'main', 'assets', 'characterpacks');
const baselineVoiceRoot = path.join(voiceRoot, 'voicepack-content', 'src', 'main', 'assets', 'voicepacks');
const outVoiceRoot = path.join(appRoot, 'android', 'app', 'src', 'internal', 'assets', 'voicepacks');
const outCharacterRoot = path.join(appRoot, 'android', 'app', 'src', 'internal', 'assets', 'characterpacks');

function valueAfter(flag) {
  const index = process.argv.indexOf(flag);
  return index < 0 ? null : path.resolve(process.argv[index + 1]);
}

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, 'utf8'));
}

function sha256(file) {
  return crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
}

function copyTree(source, target) {
  fs.mkdirSync(target, { recursive: true });
  for (const entry of fs.readdirSync(source, { withFileTypes: true })) {
    const from = path.join(source, entry.name);
    const to = path.join(target, entry.name);
    if (entry.isDirectory()) copyTree(from, to);
    else fs.copyFileSync(from, to);
  }
}

function findCharacter(characterId) {
  const roots = [candidatesRoot, contentRoot];
  for (const root of roots) {
    if (!fs.existsSync(root)) continue;
    const pending = [root];
    while (pending.length) {
      const dir = pending.pop();
      for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
        const file = path.join(dir, entry.name);
        if (entry.isDirectory()) pending.push(file);
        else if (entry.name === 'manifest.json') {
          const manifest = readJson(file);
          if (manifest.id === characterId) return { dir: path.dirname(file), manifest };
        }
      }
    }
  }
  return null;
}

function preflight(pack) {
  const id = pack.id;
  if (!fs.existsSync(path.join(refsRoot, id, 'greeting.wav')) || !fs.existsSync(path.join(refsRoot, id, 'reference.json'))) {
    throw new Error(`Missing VoiceDesign reference: ${id}`);
  }
  for (const event of events) {
    const stem = event.toLowerCase();
    if (!fs.existsSync(path.join(eventRoot, id, 'clips', `${stem}.wav`)) || !fs.existsSync(path.join(eventRoot, id, 'clips', `${stem}.json`))) {
      throw new Error(`Missing ${event} VoiceDesign clone for ${id}`);
    }
  }
  if (!findCharacter(pack.characterId)) throw new Error(`Missing character asset candidate: ${pack.characterId}`);
  if (fs.existsSync(path.join(outVoiceRoot, id))) throw new Error(`Refusing to overwrite existing preview pack: ${path.join(outVoiceRoot, id)}`);
  const characterIsInSdkContent = fs.existsSync(path.join(contentRoot, pack.characterId, 'manifest.json'));
  if (!characterIsInSdkContent && fs.existsSync(path.join(outCharacterRoot, pack.characterId))) {
    throw new Error(`Refusing to overwrite existing preview pack: ${path.join(outCharacterRoot, pack.characterId)}`);
  }
}

function ensureNewDirectory(dir) {
  if (fs.existsSync(dir)) throw new Error(`Refusing to overwrite existing preview pack: ${dir}`);
  fs.mkdirSync(dir, { recursive: true });
}

function createVoicePack(pack) {
  const id = pack.id;
  const referenceDir = path.join(refsRoot, id);
  const referenceFile = path.join(referenceDir, 'greeting.wav');
  const referenceMetaFile = path.join(referenceDir, 'reference.json');
  if (!fs.existsSync(referenceFile) || !fs.existsSync(referenceMetaFile)) throw new Error(`Missing VoiceDesign reference: ${id}`);
  const referenceMeta = readJson(referenceMetaFile);
  const target = path.join(outVoiceRoot, id);
  ensureNewDirectory(target);
  const clipsDir = path.join(target, 'clips');
  fs.mkdirSync(clipsDir, { recursive: true });

  const greetingTarget = path.join(clipsDir, 'greeting.wav');
  fs.copyFileSync(referenceFile, greetingTarget);
  const fileEntries = [{ path: 'clips/greeting.wav', size: fs.statSync(greetingTarget).size, sha256: sha256(greetingTarget) }];
  const clips = {};
  for (const event of events) {
    const source = path.join(eventRoot, id, 'clips', `${event.toLowerCase()}.wav`);
    const metadata = path.join(eventRoot, id, 'clips', `${event.toLowerCase()}.json`);
    if (!fs.existsSync(source) || !fs.existsSync(metadata)) throw new Error(`Missing ${event} VoiceDesign clone for ${id}`);
    const eventMeta = readJson(metadata);
    if (eventMeta.status !== 'audition' || eventMeta.text !== pack.slots[event].text) throw new Error(`Metadata mismatch at ${id}/${event}`);
    const relative = `clips/${event.toLowerCase()}.wav`;
    const copied = path.join(target, relative);
    fs.copyFileSync(source, copied);
    fileEntries.push({ path: relative, size: fs.statSync(copied).size, sha256: sha256(copied) });
    clips[event] = [{ file: relative, text: eventMeta.text }];
  }

  const manifest = {
    schema: 1,
    id,
    characterId: pack.characterId,
    name: pack.name,
    version: '0.1.0-audition.1',
    kind: 'clip',
    language: 'ko-KR',
    audio: { format: 'wav-pcm16', sampleRate: 24000, channels: 1 },
    greeting: { file: 'clips/greeting.wav', text: referenceMeta.text },
    clips,
    files: fileEntries,
    prototype: {
      workflow: 'VoiceDesign reference -> Qwen Base voice clone',
      voiceDesignModel: referenceMeta.model,
      cloneModel: eventRoot && (readJson(path.join(eventRoot, id, 'clips', 'brake.json')).model),
      instruct: referenceMeta.instruct,
      status: 'local internal audition only; not a commercial release pack',
    },
    provenance: {
      character: 'original',
      voiceSource: 'synthetic',
      consent: true,
      license: 'internal audition only',
      commercialUse: false,
      attribution: 'Qwen3-TTS VoiceDesign/Base · internal candidate preview',
    },
    dialogueMatrix: {
      version: '1.0.0-text',
      source: 'docs/RELEASE_DIALOGUE_MATRIX_V1.json',
      slotCount: 13,
      readyAudioSlots: ['__GREETING', ...events],
      pendingAudioSlots: ['WARNING', 'ENERGY_LOW'],
    },
  };
  fs.writeFileSync(path.join(target, 'manifest.json'), `${JSON.stringify(manifest, null, 2)}\n`, 'utf8');
}

function createCharacterPack(pack) {
  // These two new characters already ship in the local character-content AAR;
  // its preview link is patched and published to Maven Local separately.
  if (fs.existsSync(path.join(contentRoot, pack.characterId, 'manifest.json'))) return;
  const source = findCharacter(pack.characterId);
  if (!source) throw new Error(`Missing character asset candidate: ${pack.characterId}`);
  const target = path.join(outCharacterRoot, pack.characterId);
  ensureNewDirectory(target);
  copyTree(source.dir, target);
  const manifestPath = path.join(target, 'manifest.json');
  const manifest = readJson(manifestPath);
  manifest.previewVoicePackId = pack.id;
  manifest.voicePackId = null;
  fs.writeFileSync(manifestPath, `${JSON.stringify(manifest, null, 2)}\n`, 'utf8');
}

const characterIds = new Set(matrix.packs.map((pack) => pack.characterId));
const voicePackIds = new Set(matrix.packs.map((pack) => pack.id));
if (matrix.packs.length !== expectedTargetCharacterCount || characterIds.size !== expectedTargetCharacterCount || voicePackIds.size !== expectedTargetCharacterCount) {
  throw new Error(`Expected ${expectedTargetCharacterCount} unique character/voice pairs in the matrix, found ${matrix.packs.length} rows, ${characterIds.size} characters, and ${voicePackIds.size} voice packs`);
}

const baselinePacks = matrix.packs.filter((pack) => fs.existsSync(path.join(baselineVoiceRoot, pack.id, 'manifest.json')));
if (baselinePacks.length !== expectedBaselinePackCount) {
  throw new Error(`Expected ${expectedBaselinePackCount} baseline SDK voice packs, found ${baselinePacks.length}`);
}

const packs = matrix.packs.filter((pack) => fs.existsSync(path.join(refsRoot, pack.id, 'greeting.wav')));
if (packs.length !== expectedTargetCharacterCount - expectedBaselinePackCount) {
  throw new Error(`Expected ${expectedTargetCharacterCount - expectedBaselinePackCount} new VoiceDesign packs, found ${packs.length}`);
}
const coveredVoicePackIds = new Set([...baselinePacks.map((pack) => pack.id), ...packs.map((pack) => pack.id)]);
if (coveredVoicePackIds.size !== expectedTargetCharacterCount) {
  throw new Error(`Baseline and new VoiceDesign assets cover ${coveredVoicePackIds.size}/${expectedTargetCharacterCount} target voice packs`);
}
const missingCharacters = matrix.packs.filter((pack) => !findCharacter(pack.characterId)).map((pack) => pack.characterId);
if (missingCharacters.length) throw new Error(`Missing target character assets: ${missingCharacters.join(', ')}`);

for (const pack of packs) preflight(pack);
fs.mkdirSync(outVoiceRoot, { recursive: true });
fs.mkdirSync(outCharacterRoot, { recursive: true });
for (const pack of packs) {
  createVoicePack(pack);
  createCharacterPack(pack);
  console.log(`prepared ${pack.characterId} -> ${pack.id}`);
}
console.log(`Target inventory: ${coveredVoicePackIds.size} pairs (${baselinePacks.length} baseline + ${packs.length} new). Prepared ${packs.length} new internal VoiceDesign pairs under android/app/src/internal/assets.`);
