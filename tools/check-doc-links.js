// Fails when a relative link/image in a Markdown or HTML doc points to a missing file,
// or a doc under docs/ is not reachable from docs/README.md. Run: node tools/check-doc-links.js
const fs = require('fs'), path = require('path'), cp = require('child_process');
const files = cp.execSync('git ls-files -co --exclude-standard', { encoding: 'utf8' }).trim().split('\n')
  .filter((f) => /\.(md|html)$/.test(f) && !/^android\/.*\/(assets|build)\//.test(f) && fs.existsSync(f));
const linksOf = (f) => {
  const s = fs.readFileSync(f, 'utf8'); const out = [];
  for (const m of s.matchAll(/\]\(([^)\s]+)\)/g)) out.push(m[1]);
  for (const m of s.matchAll(/(?:href|src)="([^"]+)"/g)) out.push(m[1]);
  return out.filter((t) => !/^(https?:|mailto:|#|\/|data:)/.test(t)).map((t) => t.split('#')[0]).filter(Boolean);
};
const errors = [], graph = {};
for (const f of files) {
  graph[f] = [];
  for (const t of linksOf(f)) {
    const abs = path.posix.normalize(path.posix.join(path.posix.dirname(f), decodeURI(t)));
    if (!fs.existsSync(abs)) errors.push(`${f}: broken link -> ${t}`);
    else graph[f].push(abs);
  }
}
const seen = new Set(['docs/README.md']), q = ['docs/README.md'];
while (q.length) for (const n of graph[q.pop()] || []) if (!seen.has(n)) { seen.add(n); q.push(n); }
for (const f of files) if (f.startsWith('docs/') && f.endsWith('.md') && !seen.has(f)) errors.push(`${f}: not reachable from docs/README.md`);
if (errors.length) { console.error(errors.join('\n')); process.exit(1); }
console.log(`doc links OK (${files.length} files)`);
