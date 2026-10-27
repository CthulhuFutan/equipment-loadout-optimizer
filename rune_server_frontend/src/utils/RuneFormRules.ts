/**
 * RuneForm 数值规则
 * 1. 词条属性范围上限
 * 2. 各槽位词条白名单
 * 3. 图片路径
 * 4. 活动图相对大小、位置系数
 *  5. 研磨资格
 */
import { RUNE_SET_TYPES, SET_EFFECTS, STAT_TYPES } from '@/utils/constants'

export interface SubValue {
  statType: string
  value: number
}

export interface RuneFormState {
  slotNumber: number
  setType: string
  quality: string
  starRating: number
  levels: number
  locked: boolean
  runeName: string
  actImage: string
  mainType: string
  mainValue: number
  extra: SubValue | null
  subs: SubValue[]
  grind: number
}

export const QUALITIES = [
  { label: '灰色', key: 'gray' },
  { label: '蓝色', key: 'blue' },
  { label: '紫色', key: 'purple' },
  { label: '橙色', key: 'orange' },
] as const

/* 套组图片颜色后缀以 RUNE_SET_TYPES.image 为唯一来源 */
const SET_COLOR_RE = /01_([a-z]+)_0[67]\.png$/

export function setColor(setType: string) {
  const image = RUNE_SET_TYPES.find((s) => s.value === setType)?.image
  return image?.match(SET_COLOR_RE)?.[1] ?? 'blackblue'
}

/** 槽位-词条约束，与后端 RuneConstants.SLOT_MAIN_STATS 同源 */
export const MAIN_BY_SLOT: Record<number, string[]> = {
  1: ['攻击力+'],
  2: ['攻击力%', '防御力%', '生命力%', '攻击速度%'],
  3: ['防御力+'],
  4: ['攻击力%', '防御力%', '生命力%', '致命率%', '致命伤害%'],
  5: ['生命力+'],
  6: ['攻击力%', '防御力%', '生命力%', '效果命中%', '效果抵抗%'],
}

export interface RuneCaps {
  mainPct: number
  mainAtkDef: number
  mainHp: number
  subPct: number
  subAtkDef: number
  subHp: number
  subPctDefault?: number
}

export const CAPS: Record<number, RuneCaps> = {
  6: { mainPct: 46, mainAtkDef: 492, mainHp: 34440, subPct: 10, subAtkDef: 25, subHp: 1500 },
  7: { mainPct: 55, mainAtkDef: 984, mainHp: 50340, subPct: 25, subAtkDef: 30, subHp: 1800, subPctDefault: 12 },
}

export const ACT_IMAGES = [
  '01_sword.png', '01_arms.png', '01_essence.png', '01_jewelry.png', '01_life.png', '01_mark.png',
  '02_fairystone.png', '02_necklace.png', '03_colosseum.png', '03_mark_hallucination.png',
  '04_colosseum.png', '04_necklace.png', '04_wardcube.png', '05_spear.png', '06_necklace.png',
]

const RUNE_IMAGE_SCALE = 0.925

/**
 * 活动图最终渲染参数：
 * scale = 普通基准 0.925 × 图片归一化系数 × 人工 s
 * dy    = 相对 39.5% 的纵向像素位移
 */
export const ACT_IMAGE_META: Record<string, { scale: number; dy?: number }> = {
  '01_sword': { scale: RUNE_IMAGE_SCALE * 0.808 * 1.08, dy: -3 },
  '01_arms': { scale: RUNE_IMAGE_SCALE * 0.897 * 1.08, dy: -2 },
  '01_essence': { scale: RUNE_IMAGE_SCALE * 0.882 },
  '01_jewelry': { scale: RUNE_IMAGE_SCALE * 0.729 * 1.08, dy: 4 },
  '01_life': { scale: RUNE_IMAGE_SCALE * 0.854 },
  '01_mark': { scale: RUNE_IMAGE_SCALE * 0.937 },
  '02_fairystone': { scale: RUNE_IMAGE_SCALE * 0.778 },
  '02_necklace': { scale: RUNE_IMAGE_SCALE * 0.814 * 1.08, dy: -1 },
  '03_colosseum': { scale: RUNE_IMAGE_SCALE * 0.921 },
  '03_mark_hallucination': { scale: RUNE_IMAGE_SCALE * 0.882 * 1.08, dy: 3 },
  '04_colosseum': { scale: RUNE_IMAGE_SCALE * 0.921 },
  '04_necklace': { scale: RUNE_IMAGE_SCALE * 0.820 * 1.08, dy: -2 },
  '04_wardcube': { scale: RUNE_IMAGE_SCALE * 0.700 * 1.15, dy: 0 },
  '05_spear': { scale: RUNE_IMAGE_SCALE * 0.795 * 1.15, dy: 3 },
  '06_necklace': { scale: RUNE_IMAGE_SCALE * 0.875 * 1.15, dy: 3 },
}

export function isActivity(setType: string) {
  return setType === '活动'
}

export function pad2(n: number) {
  return String(n).padStart(2, '0')
}

/** 普通符文：槽位/套组色/星档推导；活动：静态图 */
export function imagePath(slot: number, setType: string, star: number, actImage = '01_sword.png') {
  if (isActivity(setType)) return `/runes/special/${actImage}`
  const s = pad2(slot)
  return `/runes/normal/${s}/${s}_${setColor(setType)}_${star >= 7 ? '07' : '06'}.png`
}

/** 活动图缩放：RuneVisual 的 imgScale */
export function previewImgScale(setType: string, actImage: string) {
  if (!isActivity(setType)) return RUNE_IMAGE_SCALE
  const key = actImage.replace('.png', '')
  return ACT_IMAGE_META[key]?.scale ?? RUNE_IMAGE_SCALE
}

/** 活动图纵向定位：.frame.act .art { top: 39.5% }，再叠加逐图 dy */
export function previewImgTop(setType: string, actImage: string) {
  if (!isActivity(setType)) return undefined
  const key = actImage.replace('.png', '')
  const dy = ACT_IMAGE_META[key]?.dy || 0
  if (!dy) return '39.5%'
  return `calc(39.5% ${dy > 0 ? '+' : '-'} ${Math.abs(dy)}px)`
}

export function capsFor(star: number): RuneCaps {
  return CAPS[star] ?? CAPS[6]!
}

export function isHpStat(stat: string) {
  return String(stat).indexOf('生命力') === 0
}

export function mainCapOf(stat: string, star: number) {
  const c = capsFor(star)
  if (String(stat).endsWith('%')) return c.mainPct
  return isHpStat(stat) ? c.mainHp : c.mainAtkDef
}

export function subCapOf(stat: string, star: number) {
  const c = capsFor(star)
  if (String(stat).endsWith('%')) return c.subPct
  return isHpStat(stat) ? c.subHp : c.subAtkDef
}

/** 7星符文百分比副词条默认 12（兜底上限 25），其余默认 = 上限 */
export function subDefaultOf(stat: string, star: number) {
  const c = capsFor(star)
  if (String(stat).endsWith('%') && c.subPctDefault != null) return c.subPctDefault
  return subCapOf(stat, star)
}

export function clampNum(v: unknown, min: number, max: number) {
  if (v === '' || v === null || v === undefined) return min
  const n = Number(v)
  if (!Number.isFinite(n)) return min
  return Math.min(max, Math.max(min, Math.round(n)))
}

export function allowedMain(slot: number) {
  return MAIN_BY_SLOT[slot] || []
}

export function effectiveName(state: RuneFormState) {
  if (!isActivity(state.setType)) return ''
  return state.runeName.trim() || `活动${state.slotNumber}号符文`
}

export function grindInfo(state: RuneFormState) {
  if (isActivity(state.setType)) return { ok: false, reason: '活动符文不可研磨', candidates: [] as number[] }
  if (state.levels < 12) return { ok: false, reason: '强化等级需 ≥ 12 才能研磨', candidates: [] as number[] }
  const candidates = state.subs.map((s, i) => (String(s.value) !== '' ? i : -1)).filter((i) => i >= 0)
  if (!candidates.length) return { ok: false, reason: '没有可研磨的副词条', candidates: [] as number[] }
  return { ok: true, reason: '', candidates }
}

export function statLabel(statType: string) {
  return String(statType).replace(/[%+]$/, '')
}

export function fmtValue(statType: string, value: number | string) {
  if (String(value) === '') return '—'
  return `+${value}${String(statType).endsWith('%') ? '%' : ''}`
}

export function setEffect(setType: string) {
  return SET_EFFECTS[setType] || { count: 2, desc: '' }
}

/* 品质 */
export function qualityTagType(quality: string): 'info' | 'primary' | 'warning' | 'success' {
  if (quality === '蓝色') return 'primary'
  if (quality === '橙色') return 'warning'
  if (quality === '紫色') return 'success'
  return 'info'
}