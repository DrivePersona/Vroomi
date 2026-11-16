#!/usr/bin/env node
// LivLiv 공개 샘플 캐릭터(d01·d02·q03)와 LICENSE를 디버그 스파이크 에셋 폴더로 내려받는다.
// 샘플 캐릭터 라이선스(LivLiv Featured Characters License)는 이 저장소로의 재배포를 허용하지 않으므로
// 파일을 커밋하지 않고 각자 받는다(대상 폴더는 .gitignore). 의존성 없음, Node 18+.
const fs = require('node:fs')
const path = require('node:path')

const REPO = 'livliv-dev/player'
const REF = process.argv[2] || 'main'
const IDS = ['d01', 'd02', 'q03']
const OUT = path.join(__dirname, '..', 'android', 'app', 'src', 'debug', 'assets', 'livliv-spike', 'characters')

async function json(url) {
  const res = await fetch(url, { headers: { 'User-Agent': 'fetch-livliv-samples' } })
  if (!res.ok) throw new Error(`${url} → HTTP ${res.status}`)
  return res.json()
}

async function main() {
  const tree = await json(`https://api.github.com/repos/${REPO}/git/trees/${REF}?recursive=1`)
  const wanted = tree.tree.filter((e) => e.type === 'blob' &&
    (e.path === 'characters/LICENSE.md' || IDS.some((id) => e.path.startsWith(`characters/${id}/`))))
  if (tree.truncated || wanted.length === 0) throw new Error('파일 목록을 읽지 못했어요(truncated 또는 비어 있음)')
  for (const entry of wanted) {
    const dest = path.join(OUT, entry.path.replace(/^characters\//, ''))
    fs.mkdirSync(path.dirname(dest), { recursive: true })
    const res = await fetch(`https://raw.githubusercontent.com/${REPO}/${REF}/${entry.path}`)
    if (!res.ok) throw new Error(`${entry.path} → HTTP ${res.status}`)
    fs.writeFileSync(dest, Buffer.from(await res.arrayBuffer()))
  }
  console.log(`${wanted.length}개 파일을 ${OUT}에 받았어요. 라이선스: characters/LICENSE.md (조직 상업 이용은 사전 동의 필요).`)
}

main().catch((e) => { console.error(e.message); process.exit(1) })
