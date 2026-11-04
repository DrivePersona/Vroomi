const fs = require('node:fs');

const VERSION = /^\d+\.\d+\.\d+(?:[-+][0-9A-Za-z.-]+)?$/;
const PROPERTY_BY_PACKAGE = {
  'voice-pack-sdk': 'drivepersona.voicepack.version',
  'character-pack-sdk': 'drivepersona.characterpack.version',
};

function normalizeDispatch(action, payload = {}) {
  const packages = {};
  if (payload.packages && typeof payload.packages === 'object' && !Array.isArray(payload.packages)) {
    for (const [name, version] of Object.entries(payload.packages)) packages[name] = version;
  } else if (payload.package && payload.version) {
    packages[payload.package] = payload.version;
  } else {
    const legacyPackage = {
      'voicepack-published': 'voice-pack-sdk',
      'characterpack-published': 'character-pack-sdk',
    }[action];
    if (legacyPackage) packages[legacyPackage] = payload.version;
  }

  const selected = Object.entries(packages);
  if (selected.length === 0) throw new Error('dispatch must include package/version or a non-empty packages object');
  if (selected.some(([name]) => !(name in PROPERTY_BY_PACKAGE))) throw new Error('package must be voice-pack-sdk or character-pack-sdk');
  if (selected.some(([, version]) => typeof version !== 'string' || !VERSION.test(version))) {
    throw new Error('every package version must use numeric semantic version syntax');
  }
  return {
    versions: Object.fromEntries(selected),
    metadata: sanitizeMetadata(payload.contentMetadata || payload.metadata || {}),
  };
}

function sanitizeMetadata(metadata) {
  if (!metadata || typeof metadata !== 'object' || Array.isArray(metadata)) return {};
  const allowed = [
    'releaseName', 'component', 'runtimeVersion', 'contentVersion', 'contentPackIds',
    'contentPackCount', 'matrixVersion', 'characterIds', 'voicePackIds', 'notes',
  ];
  return Object.fromEntries(allowed
    .filter((key) => metadata[key] !== undefined)
    .map((key) => [key, String(Array.isArray(metadata[key]) ? metadata[key].join(', ') : metadata[key])
      .replace(/[\r\n\u0000-\u001f]/g, ' ').slice(0, 1000)]));
}

function readVersions(properties) {
  const versions = {};
  for (const [packageName, key] of Object.entries(PROPERTY_BY_PACKAGE)) {
    const match = properties.match(new RegExp(`^${key.replaceAll('.', '\\.')}=(.+)$`, 'm'));
    if (!match) throw new Error(`missing ${key} in android/gradle.properties`);
    versions[packageName] = match[1].trim();
  }
  return versions;
}

function planVersions(current, requested) {
  const target = { ...current };
  for (const [packageName, version] of Object.entries(requested.versions)) target[packageName] = version;
  const changed = Object.keys(requested.versions).filter((name) => current[name] !== target[name]);
  return { current, target, changed, isNoop: changed.length === 0 };
}

function updateProperties(properties, target, changed) {
  let result = properties;
  for (const packageName of changed) {
    const key = PROPERTY_BY_PACKAGE[packageName];
    result = result.replace(new RegExp(`^${key.replaceAll('.', '\\.')}=.*$`, 'm'), `${key}=${target[packageName]}`);
  }
  return result;
}

function prepareFromEnvironment() {
  const action = process.env.RELEASE_ACTION || '';
  const payload = JSON.parse(process.env.RELEASE_PAYLOAD || '{}');
  const path = process.env.GRADLE_PROPERTIES_PATH || 'android/gradle.properties';
  const properties = fs.readFileSync(path, 'utf8');
  const requested = normalizeDispatch(action, payload);
  const plan = planVersions(readVersions(properties), requested);
  if (!plan.isNoop && process.env.PLAN_ONLY !== 'true') {
    fs.writeFileSync(path, updateProperties(properties, plan.target, plan.changed));
  }

  const output = process.env.GITHUB_OUTPUT;
  if (output && process.argv[2] !== '--apply') {
    const branch = `automation/packs-v${plan.target['voice-pack-sdk']}-c${plan.target['character-pack-sdk']}`;
    const lines = [
      `changed=${plan.isNoop ? 'false' : 'true'}`,
      `branch=${branch}`,
      `voice_version=${plan.target['voice-pack-sdk']}`,
      `character_version=${plan.target['character-pack-sdk']}`,
      `changed_packages=${plan.changed.join(',')}`,
      `metadata=${JSON.stringify(requested.metadata)}`,
    ];
    fs.appendFileSync(output, `${lines.join('\n')}\n`);
  }
  process.stdout.write(plan.isNoop ? 'No package version changes are needed.\n' : `Prepared ${plan.changed.join(', ')} version update.\n`);
}

if (require.main === module) {
  try {
    if (process.argv[2] === '--apply') process.env.PLAN_ONLY = 'false';
    prepareFromEnvironment();
  } catch (error) {
    process.stderr.write(`${error.message}\n`);
    process.exitCode = 1;
  }
}

module.exports = { normalizeDispatch, planVersions, readVersions, updateProperties };
