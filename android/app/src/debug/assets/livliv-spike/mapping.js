// 차량 이벤트 → 캐릭터 동작 매핑 (사용자 확정 2026-11-16). 웹 실험실과 Pleos 프로브가 같은 파일을 쓴다.
// 반응은 소리가 시작된 뒤 지연만큼 기다렸다가 시작하고, 클립이 끝나면(done) 표정·시선·idle을 되돌린다.
// 크기(amp)·지연은 시험용 임시값이다. 앱 manifest 형식은 아직 없다(설계: harness/design-livliv-overlay-2026-11-16.md).

export const PERSONAS = {
  seori: { name: '서리', model: 'd02', amp: 0.6, delay: 300 },
  gomji: { name: '곰지', model: 'd01', amp: 1.4, delay: 0 },
  pin: { name: '핀', model: 'q03', amp: 1.0, delay: 0 },
}
export const EVENTS = { BRAKE: '브레이크', TURN: '방향지시등', MODE: '드라이브 모드', TIRE: '타이어', TOUCH: '터치(약·중·강 = 1·3·4번 줄)', WARNING: '차량 경고(몸짓 없음)' }

const hop = (p, a, ms, amp) => setTimeout(() => p.act('hop', amp), ms)
// motion(player, amp, freeze) — freeze(true)는 idle을 멈춘다(done에서 풀림)
const MOTION = {
  BRAKE: { seori: (p, a) => p.act('nod', a), gomji: (p, a) => p.act('hop', a), pin: (p, a) => { p.act('nod', a); hop(p, a, 250, a * 0.5) } },
  TURN: { seori: (p, a) => p.act('tilt', a), gomji: (p, a) => p.act('tilt', a), pin: (p, a) => { p.act('tilt', a); hop(p, a, 600, a * 0.4) } },
  MODE: { seori: (p, a) => p.act('nod', a), gomji: (p, a) => { p.act('nod', a); setTimeout(() => p.act('nod', a), 450) }, pin: (p, a) => p.act('nod', a) },
  TIRE: { seori: (p, a) => { p.setExpression('sad'); p.act('nod', a) }, gomji: (p, a) => { p.setExpression('sad'); p.act('shake', a) }, pin: (p, a) => { p.setExpression('sad'); p.act('nod', a) } },
  TOUCH: {
    seori: { 1: (p, a) => p.act('shy', a * 0.8), 3: (p, a, f) => { p.setExpression('shy'); f(true) }, 4: (p, a, f) => { p.lookAt(0, 0); f(true) } },
    gomji: { 1: (p, a) => { p.setExpression('shy'); p.act('shy', a * 0.6) }, 3: (p, a) => { p.setExpression('shy'); p.act('hop', a) }, 4: (p, a) => { p.setExpression('angry'); p.act('shake', a) } },
    pin: { 1: (p, a) => p.act('shy', a * 0.6), 3: (p, a) => { p.setExpression('shy'); p.act('hop', a * 0.5) }, 4: (p, a, f) => { p.setExpression('angry'); f(true) } },
  },
}
const FILE = { BRAKE: 'brake', TURN: 'turn', MODE: 'mode', TIRE: 'tire' }
const wait = (ms) => new Promise((r) => setTimeout(r, ms))

/** 이벤트·줄에 맞는 클립 파일 이름(확장자 없음). */
export function clipName(ev, line) {
  if (ev === 'TOUCH') return { 1: 'touch', 3: 'touch-3', 4: 'touch-4' }[line]
  return FILE[ev] + (line > 1 ? '-' + line : '')
}

/**
 * 반응기. say(name, who, hooks) → Promise<boolean>(클립을 시작했는지), hooks = { onStart, onEnd }.
 * 이벤트마다 줄을 순환한다(BRAKE·TURN·MODE·TIRE 1→2→3, TOUCH 1→3→4).
 */
export function createReactor({ player, say, log = () => {}, getBaseIdle = () => true }) {
  const cursor = {}
  const freeze = (on) => { player.idle = on ? false : getBaseIdle() }
  return async function react(ev, who) {
    if (ev === 'WARNING') { log('이벤트 WARNING → 몸짓 없음(중립 음성)'); return { ev, line: 0, played: false } }
    const P = PERSONAS[who]
    const lines = ev === 'TOUCH' ? [1, 3, 4] : [1, 2, 3]
    const key = ev + who
    const line = lines[(cursor[key] = ((cursor[key] ?? -1) + 1) % lines.length)]
    const motion = ev === 'TOUCH' ? MOTION.TOUCH[who][line] : MOTION[ev][who]
    log(`이벤트 ${ev} 줄${line} · ${P.name} (크기 ${P.amp}, 지연 ${P.delay}ms)`)
    const done = () => { player.setExpression(null); player.lookAt(null); freeze(false); log('done → 기본 자세') }
    const played = await say(clipName(ev, line), who, {
      onStart: async () => { await wait(P.delay); motion(player, P.amp, freeze) },
      onEnd: done,
    })
    if (!played) { motion(player, P.amp, freeze); setTimeout(done, 1600) } // 소리를 못 불러오면 1.6초 뒤 복귀(시험용)
    return { ev, line, played }
  }
}
