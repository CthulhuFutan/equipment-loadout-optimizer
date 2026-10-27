import dealerImg  from '@/assets/category/role_dealer.png'
import tankerImg  from '@/assets/category/role_tanker.png'
import healerImg  from '@/assets/category/role_healer.png'
import supportImg from '@/assets/category/role_supporter.png'

import waterImg from '@/assets/element/attr_water_02.png'
import fireImg from '@/assets/element/attr_fire_02.png'
import earthImg from '@/assets/element/attr_earth_02.png'
import lightImg from '@/assets/element/attr_light_02.png'
import darkImg from '@/assets/element/attr_dark_02.png'

// 类别
/** label：前端显示内容；value：数据库参数内容 */
export const DRAGON_CATEGORIES = [
    { label: '攻击', value: '攻击', image: dealerImg },
    { label: '防御', value: '防御', image: tankerImg },
    { label: '辅助', value: '辅助', image: supportImg },
    { label: '治疗', value: '治疗', image: healerImg }
] as const

// 属性
export const DRAGON_ELEMENTS = [
    { label: '火', value: '火', image: fireImg },
    { label: '水', value: '水', image: waterImg },
    { label: '地', value: '地', image: earthImg },
    { label: '光', value: '光', image: lightImg },
    { label: '暗', value: '暗', image: darkImg }
] as const

export function getCategoryImage(category: string): string | undefined {
    return DRAGON_CATEGORIES.find(item => item.value === category)?.image
}
export function getElementImage(element: string): string | undefined {
    return DRAGON_ELEMENTS.find(item => item.value === element)?.image
}

// 预设队长技能
export const LEADER_SKILLS = [
  { name: '光之守护', icon: '/leader_skill/skill_common_icon_0.png', desc: '我军全队的攻击力增加50%' },
  { name: '增加命中', icon: '/leader_skill/skill_common_icon_14.png', desc: '我军全队的命中增加50%' },
  { name: '增加命中', icon: '/leader_skill/skill_common_icon_15.png', desc: '我军全队的命中增加50%' },
  { name: '增加命中', icon: '/leader_skill/skill_common_icon_16.png', desc: '我军全队的命中增加50%' },
  { name: '增加命中', icon: '/leader_skill/skill_common_icon_17.png', desc: '我军全队的命中增加50%' },
  { name: '增加命中', icon: '/leader_skill/skill_common_icon_18.png', desc: '我军全队的命中增加50%' },
  { name: '攻击力增加', icon: '/leader_skill/skill_common_icon_21.png', desc: '在头目战斗中，我军攻击力增加50%' },
  { name: '攻击力增加', icon: '/leader_skill/skill_common_icon_22.png', desc: '在头目战斗中，我军攻击力增加50%' },
  { name: '攻击力增加', icon: '/leader_skill/skill_common_icon_23.png', desc: '在头目战斗中，我军攻击力增加50%' },
  { name: '无队长技能', icon: '/leader_skill/skill_empty.png', desc: '' },
] as const

export const RUNE_SET_TYPES = [
    { label: '活动', value: '活动', image: '/runes/special/05_spear.png' },
    { label: '体力', value: '体力', image: '/runes/normal/01/01_yellow_07.png' },
    { label: '机会', value: '机会', image: '/runes/normal/01/01_purple_07.png' },
    { label: '坚固', value: '坚固', image: '/runes/normal/01/01_orange_07.png' },
    { label: '强烈', value: '强烈', image: '/runes/normal/01/01_red_07.png' },
    { label: '伤害', value: '伤害', image: '/runes/normal/01/01_pink_07.png' },
    { label: '快速', value: '快速', image: '/runes/normal/01/01_blue_07.png' },
    { label: '吸血', value: '吸血', image: '/runes/normal/01/01_blackred_07.png' },
    { label: '冥想', value: '冥想', image: '/runes/normal/01/01_blackblue_07.png' },
    { label: '反击', value: '反击', image: '/runes/normal/01/01_blackyellow_07.png' },
    { label: '生存', value: '生存', image: '/runes/normal/01/01_blackwhite_07.png' },
    { label: '决心', value: '决心', image: '/runes/normal/01/01_blackpurple_07.png' },
    { label: '仇恨', value: '仇恨', image: '/runes/normal/01/01_blackgreen_07.png' },
] as const

// 与 runes.set_type 对齐。key 顺序为展示顺序。
export const RUNE_SET_TIERS: Record<string, number[]> = {
    '体力': [2, 4, 6],
    '机会': [2, 4, 6],
    '坚固': [2, 4, 6],
    '伤害': [2, 4, 6],
    '生存': [2, 4, 6],
    '强烈': [4],
    '快速': [4],
    '吸血': [2],
    '冥想': [4],
    '反击': [2],
    '决心': [4],
    '仇恨': [2],
}

// 搭配选择的可选套组展示顺序
export const RUNE_SET_REQ_ORDER = Object.keys(RUNE_SET_TIERS) as string[]

/**
 * 把后端返回的 rune_set_reqs 统一解析为 [{ setType, count }]
 * 新映射结果：{"生存":6,"体力":2}
 * 旧映射结果支持：[{"setType":"生存","count":2}]
 * 解析失败或无值返回 []
 */
export function parseRuneSetReqs(v: unknown): { setType: string; count: number }[] {
    if (!v) return []
    let parsed: any = v
    if (typeof v === 'string') {
        try { parsed = JSON.parse(v) } catch { return [] }
    }
    if (!parsed || typeof parsed !== 'object') return []
    if (Array.isArray(parsed)) {
        // 旧映射结果
        return parsed
            .filter((item: any) =>
                item && typeof item.setType === 'string' && typeof item.count === 'number'
            )
            .map((item: any) => ({ setType: item.setType, count: item.count }))
    }
    return Object.entries(parsed)
        .filter((entry): entry is [string, number] => typeof entry[1] === 'number')
        .map(([setType, count]) => ({ setType, count }))
}

// 符文品质对应的背景渐变（取自 colored 星星：保留原星明暗渐变 + 品质色相/饱和度）
export const RUNE_QUALITY_COLORS: Record<string, string> = {
    '灰色': 'linear-gradient(135deg, #8E8E8E 0%, #6A6A6A 45%, #343434 100%)',
    '蓝色': 'linear-gradient(180deg, #5FA8E0 0%, #12298F 100%)',
    '紫色': 'radial-gradient(circle farthest-side at 100% 100%, #3A0C48 0%, #460E56 52%, #55106A 99.6%, rgba(85,16,106,0) 100%), linear-gradient(180deg, #6A1780 0%, #5E1374 55%, #541068 100%)',
    '橙色': 'linear-gradient(135deg, #F2A03E 0%, #CB6E1F 45%, #8E4512 100%)',
}

export const STAT_TYPES = [
    { label: '攻击速度%', value: '攻击速度%' },
    { label: '攻击力%',   value: '攻击力%' },
    { label: '攻击力+',   value: '攻击力+' },
    { label: '防御力%',   value: '防御力%' },
    { label: '防御力+',   value: '防御力+' },
    { label: '生命力%',   value: '生命力%' },
    { label: '生命力+',   value: '生命力+' },
    { label: '致命率%',   value: '致命率%' },
    { label: '致命伤害%', value: '致命伤害%' },
    { label: '命中%',     value: '命中%' },
    { label: '回避%',     value: '回避%' },
    { label: '效果命中%', value: '效果命中%' },
    { label: '效果抵抗%', value: '效果抵抗%' },
] as const


// 属性总和展示顺序：固定值在前，百分比按 攻击→生存→异常 排，便于同行比对数值
export const STAT_DISPLAY_ORDER = [
    '攻击力+', '防御力+', '生命力+',
    '攻击力%', '致命率%', '致命伤害%', '命中%', '攻击速度%',
    '生命力%', '防御力%', '效果命中%', '效果抵抗%', '回避%',
] as const


export const SET_EFFECTS: Record<string, { count: number; desc: string | null }> = {
    '体力':   { count: 2, desc: '生命力+20%' },
    '机会':   { count: 2, desc: '致命率+20%' },
    '坚固':   { count: 2, desc: '防御力+20%' },
    '强烈':   { count: 4, desc: '攻击力+40%' },
    '伤害':   { count: 2, desc: '致命伤害+20%' },
    '快速':   { count: 4, desc: '攻击速度+40%' },
    '吸血':   { count: 2, desc: '恢复相当于自身造成的伤害量10%的生命力' },
    '冥想':   { count: 4, desc: '每12秒获得1个魔力' },
    '反击':   { count: 2, desc: '如果受到伤害立刻反射给攻击的对象\n反射时，有1秒的冷却时间\n伤害量比照自身的防御力与最大生命力' },
    '生存':   { count: 2, desc: '生命力下降到30%以下时，可发动5秒的无敌效果\n组合效果重复时无敌时间叠加' },
    '决心':   { count: 4, desc: '有20%的概率代替自身后方的我军接收伤害' },
    '仇恨':   { count: 2, desc: '受到伤害时，有20%的概率减少拖放技能冷却时间1秒\n组合效果重复时发动概率增加' },
    '活动':   { count: 1, desc: '没有套组效果' },
}

export function getSetEffect(setType: string) {
    return SET_EFFECTS[setType] ?? { count: 2, desc: '' }
}