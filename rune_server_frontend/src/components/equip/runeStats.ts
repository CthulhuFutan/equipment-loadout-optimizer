/* 装备调整页相关计算 */
import { SET_EFFECTS, STAT_DISPLAY_ORDER } from '@/utils/constants'

export interface RuneSubStat {
  statType: string
  statValue: number
  sortOrder: number
}

// 符文相关使用字段
export interface Rune {
  runeId: number
  runeName?: string | null
  setType: string
  slotNumber: number
  quality: string
  starRating: number
  levels?: number | null
  imageUrl?: string | null
  mainStatType: string
  mainStatValue: number
  subStats?: RuneSubStat[] | null
  grindSortOrder?: number | null
  equipped?: boolean
  equippedDragonId?: number | null
  locked?: boolean
}

// 龙相关使用字段
export interface Dragon {
  dragonId: number
  dragonName: string
  dragonImage?: string | null
  element: string
  category: string
}

// 槽位 → 符文 ID。缺键表示该槽为空
export type Loadout = Record<number, number>
// 龙 ID → Loadout
export type LoadoutMap = Record<number, Loadout>

// 参与 “属性总和” 的数值套组
export const SET_BONUS: Record<string, { stat: string; value: number }> = {
  体力: { stat: '生命力%', value: 20 },
  机会: { stat: '致命率%', value: 20 },
  坚固: { stat: '防御力%', value: 20 },
  强烈: { stat: '攻击力%', value: 40 },
  伤害: { stat: '致命伤害%', value: 20 },
  快速: { stat: '攻击速度%', value: 40 },
}

// 词条名去掉末尾的 % / + 号
export function statLabel(statType: string): string {
  return (statType || '').replace(/[%+]$/, '')
}
// 词条值带 + 与 %
export function fmtStat(statType: string, value: number): string {
  return `+${value}${String(statType).endsWith('%') ? '%' : ''}`
}

export function fmtNum(v: unknown): string {
  const n = Number(v)
  if (!Number.isFinite(n)) return String(v ?? '')
  return Number.isInteger(n) ? String(n) : String(Math.round(n * 100) / 100)
}

// Loadout → 符文 ID
export function equippedIdAt(loadout: Loadout | undefined, slot: number): number | null {
  return loadout?.[slot] ?? null
}

// 符文 ID → 符文
export function indexRunes(runes: Rune[]): Map<number, Rune> {
  const m = new Map<number, Rune>()
  for (const r of runes) m.set(r.runeId, r)
  return m
}

// 占用表：符文 ID → 装备它的龙 ID。
export function buildOwnerMap(dragons: Dragon[], loadouts: LoadoutMap): Map<number, number> {
  const m = new Map<number, number>()
  for (const d of dragons) {
    const lo = loadouts[d.dragonId]
    if (!lo) continue
    for (const slot of Object.keys(lo)) {
      const rid = lo[Number(slot)]
      if (rid != null && !m.has(rid)) m.set(rid, d.dragonId)
    }
  }
  return m
}

// 把后端返回的符文列表按「装备关系」还原成 LoadoutMap
export function buildLoadoutMap(runes: Rune[]): LoadoutMap {
  const map: LoadoutMap = {}
  for (const r of runes) {
    if (!r.equipped || r.equippedDragonId == null) continue
    const lo = (map[r.equippedDragonId] ??= {})
    lo[r.slotNumber] = r.runeId
  }
  return map
}

// 按槽位排序取出该龙的符文
export function loadoutRunes(loadout: Loadout | undefined, runeMap: Map<number, Rune>): Rune[] {
  if (!loadout) return []
  return Object.keys(loadout)
    .map(Number)
    .sort((a, b) => a - b)
    .map((slot) => runeMap.get(loadout[slot] as number))
    .filter((r): r is Rune => !!r)
}

// 额外属性
export function extraStatOf(rune: Rune | null | undefined): RuneSubStat | null {
  return rune?.subStats?.find((s) => s.sortOrder === 0) ?? null
}
// 副属性
export function normalSubStatsOf(rune: Rune | null | undefined): RuneSubStat[] {
  return (rune?.subStats ?? [])
    .filter((s) => s.sortOrder !== 0)
    .sort((a, b) => a.sortOrder - b.sortOrder)
}

export interface StatLine {
  label: string
  base: number
  setBonus: number
  total: number
  isPercent: boolean
}

export interface SetLine {
  setType: string
  count: number
  threshold: number
  stacks: number
  active: boolean
  desc: string | null
}

export interface AggregateResult {
  stats: StatLine[]
  sets: SetLine[]
  setCount: Record<string, number>
}

// 属性数值汇总：符文主/副属性为基础数值，只保留 total ≠ 0 的行
export function aggregate(runes: Rune[]): AggregateResult {
  const statSum: Record<string, number> = {}
  const setCount: Record<string, number> = {}

  for (const r of runes) {
    if (!r) continue
    const add = (t: string, v: unknown) => {
      if (!t) return
      statSum[t] = (statSum[t] ?? 0) + (Number(v) || 0)
    }
    add(r.mainStatType, r.mainStatValue)
    for (const s of r.subStats ?? []) add(s.statType, s.statValue)
    setCount[r.setType] = (setCount[r.setType] ?? 0) + 1
  }

  const setBonusSum: Record<string, number> = {}
  for (const [setType, count] of Object.entries(setCount)) {
    const info = SET_BONUS[setType]
    const eff = SET_EFFECTS[setType]
    if (!info || !eff) continue
    const stacks = Math.floor(count / eff.count)
    if (stacks > 0) setBonusSum[info.stat] = (setBonusSum[info.stat] ?? 0) + stacks * info.value
  }

  const labels = new Set([...Object.keys(statSum), ...Object.keys(setBonusSum)])
  const stats: StatLine[] = [...labels]
    .map((label) => {
      const base = statSum[label] ?? 0
      const setBonus = setBonusSum[label] ?? 0
      return { label, base, setBonus, total: base + setBonus, isPercent: label.endsWith('%') }
    })
    .filter((s) => s.total !== 0)
    .sort((a, b) => {
      const ia = STAT_DISPLAY_ORDER.indexOf(a.label as (typeof STAT_DISPLAY_ORDER)[number])
      const ib = STAT_DISPLAY_ORDER.indexOf(b.label as (typeof STAT_DISPLAY_ORDER)[number])
      return (ia < 0 ? 999 : ia) - (ib < 0 ? 999 : ib)
    })

  const sets: SetLine[] = Object.entries(setCount)
    .map(([setType, count]) => {
      const eff = SET_EFFECTS[setType] ?? { count: 2, desc: null }
      const isNumeric = !!SET_BONUS[setType]
      const stacks = isNumeric ? Math.floor(count / eff.count) : 0
      return {
        setType,
        count,
        threshold: eff.count,
        stacks,
        active: isNumeric ? stacks > 0 : count >= eff.count,
        desc: eff.desc,
      }
    })
    .sort((a, b) => b.count - a.count)

  return { stats, sets, setCount }
}

// 属性变化行
export interface DeltaRow {
  label: string
  from: number
  to: number
  diff: number
}

// 逐条属性的变化：只保留有变化的行，按 STAT_DISPLAY_ORDER 排序。展示为装上去之后：N → M（±差）
export function statDelta(before: Rune[], after: Rune[]): DeltaRow[] {
  const a = aggregate(before).stats
  const b = aggregate(after).stats
  const map = new Map<string, { from: number; to: number }>()
  for (const s of a) map.set(s.label, { from: s.total, to: 0 })
  for (const s of b) {
    const cur = map.get(s.label)
    if (cur) cur.to = s.total
    else map.set(s.label, { from: 0, to: s.total })
  }
  return [...map.entries()]
    .map(([label, v]) => ({ label, from: v.from, to: v.to, diff: v.to - v.from }))
    .filter((r) => r.diff !== 0)
    .sort((x, y) => {
      const ix = STAT_DISPLAY_ORDER.indexOf(x.label as (typeof STAT_DISPLAY_ORDER)[number])
      const iy = STAT_DISPLAY_ORDER.indexOf(y.label as (typeof STAT_DISPLAY_ORDER)[number])
      return (ix < 0 ? 999 : ix) - (iy < 0 ? 999 : iy)
    })
}

// 套组变化行：1 激活↑ / -1 失效↓ / 0 无
export interface SetDeltaRow {
  setType: string
  from: number
  to: number
  mark: 1 | -1 | 0
}

// 套组件数变化：只保留件数有变化的套组
export function setDelta(before: Rune[], after: Rune[]): SetDeltaRow[] {
  const count = (arr: Rune[]) => {
    const c: Record<string, number> = {}
    for (const r of arr) if (r) c[r.setType] = (c[r.setType] ?? 0) + 1
    return c
  }
  const c0 = count(before)
  const c1 = count(after)
  const keys = [...new Set([...Object.keys(c0), ...Object.keys(c1)])]
  return keys
    .map((setType) => {
      const from = c0[setType] ?? 0
      const to = c1[setType] ?? 0
      if (from === to) return null
      const threshold = SET_EFFECTS[setType]?.count ?? 2
      const isNumeric = !!SET_BONUS[setType]
      const t0 = isNumeric ? Math.floor(from / threshold) : from >= threshold ? 1 : 0
      const t1 = isNumeric ? Math.floor(to / threshold) : to >= threshold ? 1 : 0
      const mark: 1 | -1 | 0 = t1 > t0 ? 1 : t1 < t0 ? -1 : 0
      return { setType, from, to, mark } as SetDeltaRow
    })
    .filter((r): r is SetDeltaRow => !!r)
    .sort((a, b) => b.to - a.to)
}
