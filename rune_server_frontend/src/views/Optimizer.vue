<!--
  符文分配器（Optimizer）

  页面流程：
    ① 顶部筛选栏 + 龙头像网格 → 选择目标龙（悬停显示龙名，点击选中）
    ② 词条权重滑块：默认取 getInit 返回的初始权重（AI 分析 / 类别基础权重）
    ③ 点击「应用」→ POST /optimize → 生成「推荐符文」「研磨搭配」两行
    ④ 研磨搭配行下方给出逐槽研磨建议明细
    ⑤ 场景输入（AI）预留：后端场景接口开发中，按钮暂禁用
    ⑥「应用到龙」→ saveDragonEquipment 把推荐搭配写入当前龙
-->
<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DRAGON_CATEGORIES, DRAGON_ELEMENTS, STAT_TYPES, STAT_DISPLAY_ORDER, getSetEffect } from '@/utils/constants'
import { getDragonEquipment, getDragons, saveDragonEquipment } from '@/api/dragon'
import {getInit, optimize, optimizeScene} from '@/api/optimize'
import OptimizerRuneSlot from '@/components/OptimizerRuneSlot.vue'
import RuneDetailDrawer from '@/components/rune/RuneDetailDrawer.vue'
import RuneIconSelect from '@/components/rune/RuneIconSelect.vue'

const route = useRoute()

// ================= 龙筛选 + 选择 =================
const searchName = ref('')
const filterElement = ref('')
const filterCategory = ref('')
const elementFilterOptions = DRAGON_ELEMENTS.map((e) => ({ label: e.label, value: e.value, icon: e.image }))
const categoryFilterOptions = DRAGON_CATEGORIES.map((c) => ({ label: c.label, value: c.value, icon: c.image }))
let timer: ReturnType<typeof setTimeout> | null = null
const loadingDragon = ref(false)
const dragons = ref<any[]>([])
// 全部龙（不受筛选影响），用于把「已装备在别的龙身上」的符文映射回占用龙的头像
const allDragons = ref<any[]>([])
const selectedDragonId = ref<number | null>(null)

const selectedDragon = computed(() =>
    dragons.value.find(d => d.dragonId === selectedDragonId.value) ?? null
)

// 筛选变化 → 200ms 防抖重查
watch([searchName, filterElement, filterCategory], () => {
  if (timer) clearTimeout(timer)
  timer = setTimeout(fetchDragons, 200)
})

async function fetchDragons() {
  loadingDragon.value = true
  try {
    const res = await getDragons({
      dragonName: searchName.value || undefined,
      element: filterElement.value || undefined,
      category: filterCategory.value || undefined,
      // 头像网格一次拉取，暂不分页；数据量超出 200 时再考虑分页
      pageNum: 1,
      pageSize: 200,
    })
    dragons.value = res.data?.records ?? []
  } catch {
    // 拦截器已提示
  } finally {
    loadingDragon.value = false
  }
}

async function fetchAllDragons() {
  try {
    const res = await getDragons({ pageNum: 1, pageSize: 200 })
    allDragons.value = res.data?.records ?? []
  } catch {
    // 拦截器已提示
  }
}

// ================= 词条权重 =================
// 数值型套组加成（与后端 RuneConstants.setBonusScore 一致），仅用于计算「当前穿搭」总分的展示
const NUMERICAL_SETS = ['体力', '机会', '坚固', '强烈', '伤害', '快速']
const SET_BONUS: Record<string, { stat: string; value: number }> = {
  体力: { stat: '生命力%', value: 20 },
  机会: { stat: '致命率%', value: 20 },
  坚固: { stat: '防御力%', value: 20 },
  强烈: { stat: '攻击力%', value: 40 },
  伤害: { stat: '致命伤害%', value: 20 },
  快速: { stat: '攻击速度%', value: 40 },
}
const SET_THRESHOLD: Record<string, number> = { 体力: 2, 机会: 2, 坚固: 2, 强烈: 4, 伤害: 2, 快速: 4 }

// 滑块权重上限
const WEIGHT_MAX = 10
const initWeights = ref<Record<string, number>>({})  // getInit 返回的初始权重（「重置」用）
const weights = ref<Record<string, number>>({})       // 滑块当前值
const rationale = ref('')

// 用 initWeights 铺满全部 13 个词条（缺失补 0，超过上限截断到 WEIGHT_MAX）
function applyInitWeights() {
  const next: Record<string, number> = {}
  for (const s of STAT_TYPES) next[s.value] = Math.min(WEIGHT_MAX, initWeights.value[s.value] ?? 0)
  weights.value = next
}
function resetWeights() {
  applyInitWeights()
  ElMessage.success('已重置为初始权重')
}

async function loadInit() {
  if (selectedDragonId.value == null) return
  try {
    const res = await getInit(selectedDragonId.value)
    const data = res.data
    initWeights.value = data?.weights ?? {}
    rationale.value = data?.rationale ?? ''
    applyInitWeights()
  } catch {
    // 拦截器已提示
  }
}

// ================= 三行符文 =================
const equipment = ref<any[]>([])   // 当前穿搭
const recSlots = ref<any[]>([])    // 推荐符文 slotResults
const recTotal = ref<number | null>(null)
const grindSlots = ref<any[]>([])  // 研磨搭配 grindSlots
const grindTotal = ref<number | null>(null)
const optimizing = ref(false)

// 按槽位号(1~6)建立索引：equipment 元素即符文本身；rec/grind 元素为 {slotNumber, rune, score, ...}
function slotMap(arr: any[]): Record<number, any> {
  const map: Record<number, any> = {}
  for (const it of arr) if (it && it.slotNumber != null) map[it.slotNumber] = it
  return map
}
const equipmentMap = computed(() => slotMap(equipment.value))
const recMap = computed(() => slotMap(recSlots.value))
const grindMap = computed(() => slotMap(grindSlots.value))

// 单个符文按当前权重的词条分（主属性 + 副属性）
function runeScore(r: any, w: Record<string, number>): number {
  if (!r) return 0
  let s = (w[r.mainStatType] ?? 0) * (Number(r.mainStatValue) || 0)
  for (const sub of r.subStats ?? []) s += (w[sub.statType] ?? 0) * (Number(sub.statValue) || 0)
  return s
}
// 「当前穿搭」总分：词条分 + 数值套组加成（与后端口径一致）
const currentTotal = computed(() => {
  let s = 0
  const counts: Record<string, number> = {}
  for (const r of equipment.value) {
    s += runeScore(r, weights.value)
    counts[r.setType] = (counts[r.setType] ?? 0) + 1
  }
  for (const set of NUMERICAL_SETS) {
    const info = SET_BONUS[set]
    const threshold = SET_THRESHOLD[set]
    if (!info || !threshold) continue
    const stacks = Math.floor((counts[set] ?? 0) / threshold)
    if (stacks > 0) s += (weights.value[info.stat] ?? 0) * info.value * stacks
  }
  return s
})

// 研磨后总分相对推荐总分的提升量
const grindDelta = computed(() =>
    grindTotal.value != null && recTotal.value != null ? grindTotal.value - recTotal.value : null
)

// 逐槽研磨建议明细（有 message 的才展示）
const grindAdvices = computed(() =>
    grindSlots.value
        .filter(s => s?.grind?.message)
        .map(s => ({ slot: s.slotNumber, message: s.grind.message }))
)

// ================= 属性总和 + 套组效果 =================
// 汇总一个构建（6 槽符文）：按词条类型求和（主属性 + 额外 + 副属性），并统计各套组件数
// 属性总和的固定展示顺序索引（constants.STAT_DISPLAY_ORDER）
const STAT_ORDER_INDEX = new Map<string, number>(STAT_DISPLAY_ORDER.map((t, i) => [t, i] as [string, number]))
function aggregateBuild(runes: any[]): { stats: { label: string; base: number; total: number; setBonus: number; isPercent: boolean }[]; sets: any[] } {
  const statSum: Record<string, number> = {}
  const setCount: Record<string, number> = {}
  for (const r of runes) {
    if (!r) continue
    const add = (type: string | undefined, val: any) => {
      if (!type) return
      statSum[type] = (statSum[type] ?? 0) + (Number(val) || 0)
    }
    add(r.mainStatType, r.mainStatValue)
    for (const sub of r.subStats ?? []) add(sub.statType, sub.statValue)
    setCount[r.setType] = (setCount[r.setType] ?? 0) + 1
  }

  // 套装加成：数值型套组按 ⌊件数/阈值⌋ 叠加，加到对应词条上（如 4 件强烈 = 攻击力% +40%）
  const setBonusSum: Record<string, number> = {}
  for (const [setType, count] of Object.entries(setCount)) {
    const info = SET_BONUS[setType]
    const threshold = SET_THRESHOLD[setType]
    if (!info || !threshold) continue
    const stacks = Math.floor(count / threshold)
    if (stacks > 0) setBonusSum[info.stat] = (setBonusSum[info.stat] ?? 0) + stacks * info.value
  }

  // 词条 = 符文自带 + 套装加成 的并集（如基础 0% 但 2 件机会 = 致命率 +20% 也会出现）
  const statKeys = new Set([...Object.keys(statSum), ...Object.keys(setBonusSum)])
  const stats = [...statKeys]
      .map((type) => {
        const base = statSum[type] ?? 0
        const setBonus = setBonusSum[type] ?? 0
        return { label: type, base, total: base + setBonus, setBonus, isPercent: type.endsWith('%') }
      })
      .filter(s => s.total !== 0)
      // 按 STAT_DISPLAY_ORDER 固定顺序展示（固定值→攻击→生存→异常），便于同行比对
      .sort((a, b) => (STAT_ORDER_INDEX.get(a.label) ?? 999) - (STAT_ORDER_INDEX.get(b.label) ?? 999))

  // 套组：数值型（体力/机会/坚固/强烈/伤害/快速）可多层叠加（⌊件数/阈值⌋），
  // 其余套组达到 SET_EFFECTS.count 即激活一层
  const sets = Object.entries(setCount)
      .map(([setType, count]) => {
        const effect = getSetEffect(setType)
        const threshold = SET_THRESHOLD[setType]
        const isNumerical = threshold != null
        const stacks = isNumerical ? Math.floor(count / threshold) : 0
        const active = isNumerical ? stacks > 0 : count >= effect.count
        return { setType, count, stacks, active, isNumerical, effect }
      })
      .sort((a, b) => b.count - a.count)

  return { stats, sets }
}

const equipmentAgg = computed(() => aggregateBuild(equipment.value))
const recAgg = computed(() => aggregateBuild(recSlots.value.map(s => s?.rune)))
const grindAgg = computed(() => aggregateBuild(grindSlots.value.map(s => s?.rune)))

function formatStatAgg(s: { label: string; base: number; total: number; setBonus: number; isPercent: boolean }): string {
  // 含套装加成：攻击力% +80%（40% + 套装效果 40%）
  if (s.setBonus > 0) {
    return `${s.label} +${s.total}%（${s.base}% + 套装效果 ${s.setBonus}%）`
  }
  return s.isPercent ? `${s.label} +${s.total}%` : `${s.label} ${s.total}`
}
function setChipText(set: any): string {
  if (!set.active) return `${set.setType} ${set.count}/${set.effect.count}`
  if (set.isNumerical) return `${set.setType} ${set.count}件 ×${set.stacks}层`
  return `${set.setType} ${set.count}件`
}

// ================= 优化请求 =================
// 是否把其他龙已装备的符文也纳入候选池（关闭=仅闲置符文 + 当前龙已装备）
const includeOthersEquipped = ref(false)

// 推荐符文若已装备在别的龙身上，返回 {name, avatar} 供右上角头像角标展示
function runeOwner(rune: any): { name: string; avatar: string | null } | null {
  if (!rune?.equippedDragonId || rune.equippedDragonId === selectedDragonId.value) return null
  const d = allDragons.value.find(d => d.dragonId === rune.equippedDragonId)
  if (!d) return { name: `龙ID ${rune.equippedDragonId}`, avatar: null }
  return { name: d.dragonName, avatar: d.dragonImage || null }
}

async function runOptimize() {
  if (selectedDragonId.value == null) return
  optimizing.value = true
  try {
    const res = await optimize({
      dragonId: selectedDragonId.value,
      weights: { ...weights.value },
      includeOthersEquipped: includeOthersEquipped.value,
    })
    const data = res.data
    recSlots.value = data?.slotResults ?? []
    recTotal.value = data?.totalScore ?? null
    grindSlots.value = data?.grindSlots ?? []
    grindTotal.value = data?.grindTotalScore ?? null
  } catch {
    // 拦截器已提示
  } finally {
    optimizing.value = false
  }
}

// ================= 选择龙 / 装备 =================
function clearResults() {
  equipment.value = []
  recSlots.value = []
  recTotal.value = null
  grindSlots.value = []
  grindTotal.value = null
}

async function selectDragon(id: number) {
  if (id === selectedDragonId.value) return
  selectedDragonId.value = id
  clearResults()
  await fetchEquipment()
  await loadInit()
  // 选中龙后先按初始权重跑一次，之后调整滑块需手动点「应用」
  await runOptimize()
}

async function fetchEquipment() {
  if (selectedDragonId.value == null) return
  try {
    const res = await getDragonEquipment(selectedDragonId.value)
    equipment.value = res.data ?? []
  } catch {
    // 拦截器已提示
  }
}

// ================= 应用到龙 =================
async function applyToDragon() {
  const ids = recSlots.value.filter(s => s?.rune?.runeId != null).map(s => s.rune.runeId)
  if (!ids.length) {
    ElMessage.warning('推荐符文为空，请先点击「应用」生成推荐搭配')
    return
  }
  await ElMessageBox.confirm(
    `将推荐符文应用到「${selectedDragon.value?.dragonName ?? ''}」，是否继续？`,
    '应用到龙',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  )
  await saveDragonEquipment(selectedDragonId.value!, ids)
  ElMessage.success('应用成功')
  await fetchEquipment()
}

// 结合场景进行分析，如果没有场景内容则进行基础分析
const sceneText = ref('')
const sceneAnalyzing = ref(false)
async function handleSceneAnalysis() {
  if (selectedDragonId.value == null) return
  sceneAnalyzing.value = true
  try {
    const res = await optimizeScene({
      dragonId: selectedDragonId.value,
      weights: { ...weights.value },
      scene: sceneText.value,
    })
    const data = res.data
    if (data?.weights) {
      // 用 AI 结果作为新的初始权重（「重置」回到 AI 权重），并立即按它刷新两行
      initWeights.value = data.weights
      rationale.value = data.rationale ?? ''
      applyInitWeights()
      await runOptimize()
      // 成功后清空场景输入，防止再次点击重复发送上一段
      sceneText.value = ''
    }
  } catch {
    // 拦截器已提示
  } finally {
    sceneAnalyzing.value = false
  }
}

// ================= 详情抽屉 =================
const drawerVisible = ref(false)
const drawerRune = ref<any>(null)
const drawerScore = ref<number | null>(null)
const drawerPotential = ref<number | null>(null)
const drawerGrind = ref<any>(null)

function openDrawer(rune: any, score: number | null = null, potential: number | null = null, grind: any = null) {
  if (!rune) return
  drawerRune.value = rune
  drawerScore.value = score
  drawerPotential.value = potential
  drawerGrind.value = grind
  drawerVisible.value = true
}

onMounted(async () => {
  await Promise.all([fetchDragons(), fetchAllDragons()])
  // 从龙详情页「进入符文分配器」跳转时携带 dragonId，自动选中
  const qId = Number(route.query.dragonId)
  if (qId) await selectDragon(qId)
})
</script>

<template>
  <div class="optimizer-page">
    <!-- ===== 页面标题 ===== -->
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2>龙与符的共鸣</h2>
    </div>

    <!-- ===== 筛选栏 ===== -->
    <div style="display:flex;gap:12px;margin-bottom:16px">
      <el-input v-model="searchName" placeholder="搜索龙的名称" clearable style="width:260px" />
      <RuneIconSelect
        v-model="filterElement"
        :options="elementFilterOptions"
        placeholder="属性筛选"
        clearable
        :width="150"
        :height="32"
        :selected-icon-size="20"
        :option-icon-size="20"
        :selected-gap="6"
        :option-gap="6"
      />
      <RuneIconSelect
        v-model="filterCategory"
        :options="categoryFilterOptions"
        placeholder="类别筛选"
        clearable
        :width="150"
        :height="32"
        :selected-icon-size="20"
        :option-icon-size="20"
        :selected-gap="6"
        :option-gap="6"
      />
      <span style="color:#909399;line-height:32px">共 {{ dragons.length }} 条</span>
    </div>

    <!-- ===== 龙选择网格 ===== -->
    <h3 class="section-title">选择要优化的龙</h3>
    <div class="dragon-grid" v-loading="loadingDragon">
      <el-tooltip v-for="d in dragons" :key="d.dragonId" placement="top">
        <template #content>{{ d.dragonName }}</template>
        <div class="dragon-cell" :class="{ active: d.dragonId === selectedDragonId }"
             @click="selectDragon(d.dragonId)">
          <img v-if="d.dragonImage" :src="d.dragonImage" />
          <div v-else class="dragon-ph">?</div>
        </div>
      </el-tooltip>
      <div v-if="!dragons.length && !loadingDragon" class="dragon-empty">没有找到匹配的龙</div>
    </div>

    <!-- ===== 选中龙后的优化区 ===== -->
    <template v-if="selectedDragonId != null">
      <!-- 权重面板 -->
      <div class="weight-panel">
        <div class="panel-head">
          <h3 class="panel-title">词条权重</h3>
          <span v-if="rationale" class="panel-rationale" :title="rationale">{{ rationale }}</span>
        </div>
        <div class="weight-grid">
          <div v-for="s in STAT_TYPES" :key="s.value" class="weight-row">
            <span class="weight-label">{{ s.label }}</span>
            <el-slider v-model="weights[s.value]" :max="WEIGHT_MAX" :step="1" class="weight-slider" />
            <span class="weight-val">{{ weights[s.value] }}</span>
          </div>
        </div>
        <div class="weight-actions">
          <el-checkbox v-model="includeOthersEquipped" @change="runOptimize">从其他龙已装备符文中挑选</el-checkbox>
          <div style="flex:1"></div>
          <el-button @click="resetWeights">重置权重</el-button>
          <el-button type="primary" :loading="optimizing" @click="runOptimize">应用</el-button>
        </div>
      </div>

      <!-- 当前穿搭 -->
      <div class="rune-row">
        <div class="row-header">
          <span class="row-title">当前穿搭</span>
          <span v-if="equipment.length" class="row-total">
            {{ selectedDragon?.dragonName }} 当前总分 <b>{{ currentTotal.toFixed(1) }}</b>
          </span>
        </div>
        <div class="slot-row">
          <OptimizerRuneSlot v-for="n in 6" :key="n" :rune="equipmentMap[n]" :slot="n"
                    @click="openDrawer(equipmentMap[n])" />
        </div>

        <!-- 属性总和 + 套组效果 -->
        <div v-if="equipmentAgg.stats.length || equipmentAgg.sets.length" class="build-summary">
          <div class="sum-row sum-col">
            <span class="sum-label">属性总和</span>
            <div v-if="equipmentAgg.stats.length" class="sum-stats">
              <div v-for="s in equipmentAgg.stats" :key="s.label" class="sum-stat">{{ formatStatAgg(s) }}</div>
            </div>
            <span v-else class="sum-empty">—</span>
          </div>
          <div class="sum-row">
            <span class="sum-label">套组效果</span>
            <el-tooltip v-for="set in equipmentAgg.sets" :key="set.setType" placement="top" :show-after="150">
              <template #content>
                <div style="white-space:pre-line">{{ set.effect.desc || '无套组效果' }}</div>
              </template>
              <span class="set-chip" :class="{ on: set.active, stack: set.stacks > 1 }">{{ setChipText(set) }}</span>
            </el-tooltip>
          </div>
        </div>
      </div>

      <!-- 推荐符文 -->
      <div class="rune-row">
        <div class="row-header">
          <span class="row-title">推荐符文</span>
          <span v-if="recTotal != null" class="row-total">
            推荐总分 <b>{{ recTotal.toFixed(1) }}</b>
            <span v-if="grindDelta != null && grindDelta !== 0" style="color:#67c23a">
              （研磨后可再 +{{ grindDelta.toFixed(1) }}）
            </span>
          </span>
        </div>
        <div class="slot-row">
          <OptimizerRuneSlot v-for="n in 6" :key="n" :rune="recMap[n]?.rune" :slot="n"
                    :owner="runeOwner(recMap[n]?.rune)"
                    @click="openDrawer(recMap[n]?.rune, recMap[n]?.score)" />
        </div>

        <!-- 属性总和 + 套组效果 -->
        <div v-if="recAgg.stats.length || recAgg.sets.length" class="build-summary">
          <div class="sum-row sum-col">
            <span class="sum-label">属性总和</span>
            <div v-if="recAgg.stats.length" class="sum-stats">
              <div v-for="s in recAgg.stats" :key="s.label" class="sum-stat">{{ formatStatAgg(s) }}</div>
            </div>
            <span v-else class="sum-empty">—</span>
          </div>
          <div class="sum-row">
            <span class="sum-label">套组效果</span>
            <el-tooltip v-for="set in recAgg.sets" :key="set.setType" placement="top" :show-after="150">
              <template #content>
                <div style="white-space:pre-line">{{ set.effect.desc || '无套组效果' }}</div>
              </template>
              <span class="set-chip" :class="{ on: set.active, stack: set.stacks > 1 }">{{ setChipText(set) }}</span>
            </el-tooltip>
          </div>
        </div>
      </div>

      <!-- 研磨搭配 -->
      <div class="rune-row">
        <div class="row-header">
          <span class="row-title">研磨搭配</span>
          <span v-if="grindTotal != null" class="row-total">
            研磨后总分 <b>{{ grindTotal.toFixed(1) }}</b>
          </span>
        </div>
        <div class="slot-row">
          <OptimizerRuneSlot v-for="n in 6" :key="n" :rune="grindMap[n]?.rune" :slot="n"
                    :owner="runeOwner(grindMap[n]?.rune)"
                    @click="openDrawer(grindMap[n]?.rune, grindMap[n]?.score, grindMap[n]?.potentialScore, grindMap[n]?.grind)" />
        </div>

        <!-- 属性总和 + 套组效果 -->
        <div v-if="grindAgg.stats.length || grindAgg.sets.length" class="build-summary">
          <div class="sum-row sum-col">
            <span class="sum-label">属性总和</span>
            <div v-if="grindAgg.stats.length" class="sum-stats">
              <div v-for="s in grindAgg.stats" :key="s.label" class="sum-stat">{{ formatStatAgg(s) }}</div>
            </div>
            <span v-else class="sum-empty">—</span>
          </div>
          <div class="sum-row">
            <span class="sum-label">套组效果</span>
            <el-tooltip v-for="set in grindAgg.sets" :key="set.setType" placement="top" :show-after="150">
              <template #content>
                <div style="white-space:pre-line">{{ set.effect.desc || '无套组效果' }}</div>
              </template>
              <span class="set-chip" :class="{ on: set.active, stack: set.stacks > 1 }">{{ setChipText(set) }}</span>
            </el-tooltip>
          </div>
        </div>
      </div>

      <!-- 研磨建议明细 -->
      <div v-if="grindAdvices.length" class="advice-panel">
        <div class="advice-title">研磨建议</div>
        <div v-for="a in grindAdvices" :key="a.slot" class="advice-line">
          <span class="advice-slot">{{ a.slot }}号</span>
          <span>{{ a.message }}</span>
        </div>
      </div>

      <!-- 场景输入（AI，后端开发中） -->
      <div class="scene-panel">
        <div class="panel-head">
          <h3 class="panel-title">场景需求</h3>
          <span class="panel-rationale">AI 将结合词条权重与场景需求生成推荐权重</span>
        </div>
        <el-input v-model="sceneText" type="textarea" :rows="2"
                  placeholder="例：需要高命中，优先致命三件套；主动技能刷新依赖自身…" />
        <el-button type="primary" :loading="sceneAnalyzing" @click="handleSceneAnalysis"
                   style="margin-top:8px">AI 分析</el-button>
      </div>

      <!-- 应用到龙 -->
      <div class="apply-bar">
        <el-button type="success" size="large" @click="applyToDragon">应用到龙</el-button>
      </div>
    </template>

    <!-- 详情抽屉 -->
    <RuneDetailDrawer
      v-model="drawerVisible"
      :rune="drawerRune"
      :score="drawerScore"
      :potential-score="drawerPotential"
      :grind="drawerGrind"
    />
  </div>
</template>

<style scoped>
.optimizer-page {
  max-width: 1100px;
  margin: 0 auto;
}

.section-title {
  margin: 0 0 12px;
  font-size: 18px;
  color: #303133;
  padding-left: 8px;
  border-left: 4px solid #409eff;
}

/* ===== 龙选择网格 ===== */
.dragon-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  max-height: 300px;
  overflow-y: auto;
  padding: 8px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  margin-bottom: 20px;
}
.dragon-cell {
  width: 90px;
  height: 90px;
  border-radius: 4px; /* 与 DragonList 头像比例一致 */
  cursor: pointer;
  overflow: hidden;
  border: 2px solid transparent;
  box-sizing: border-box;
  transition: transform .1s, border-color .1s;
}
.dragon-cell:hover { transform: scale(1.06); }
.dragon-cell.active { border-color: #409eff; }
.dragon-cell img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.dragon-ph {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
  font-size: 20px;
}
.dragon-empty {
  width: 100%;
  text-align: center;
  color: #c0c4cc;
  padding: 24px 0;
}

/* ===== 权重面板 ===== */
.weight-panel {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px 16px;
  margin-bottom: 20px;
}
.panel-head {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}
.panel-title {
  margin: 0;
  font-size: 16px;
  color: #303133;
}
.panel-rationale {
  color: #909399;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 60%;
}
.weight-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  column-gap: 24px;
  row-gap: 2px;
}
.weight-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.weight-label {
  width: 88px;
  flex-shrink: 0;
  font-size: 13px;
  color: #606266;
}
.weight-slider {
  flex: 1;
  min-width: 0;
}
.weight-val {
  width: 28px;
  text-align: right;
  font-size: 13px;
  color: #409eff;
  font-weight: bold;
}
.weight-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

/* ===== 三行符文 ===== */
.rune-row {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 10px 16px 2px;
  margin-bottom: 16px;
}
.row-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.row-title {
  font-size: 15px;
  font-weight: bold;
  color: #303133;
}
.row-total {
  font-size: 13px;
  color: #909399;
}
.row-total b {
  color: #409eff;
  font-size: 15px;
}
.slot-row {
  display: flex;
  gap: 4px;
  padding: 2px 2px 16px; /* 底部留白承接星级溢出 */
}

/* ===== 属性总和 + 套组效果 ===== */
.build-summary {
  border-top: 1px dashed #ebeef5;
  padding: 8px 2px 4px;
  margin-top: 2px;
}
.sum-row {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}
.sum-label {
  flex-shrink: 0;
  width: 56px;
  font-size: 12px;
  color: #909399;
}
.sum-tag {
  font-size: 12px;
  color: #606266;
}
/* 属性总和：纵向列表，一行一种词条，便于审阅 */
.sum-row.sum-col {
  flex-direction: column;
  align-items: flex-start;
  gap: 3px;
}
.sum-stats {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.sum-stat {
  font-size: 12px;
  color: #606266;
  line-height: 1.5;
}
.sum-empty { color: #c0c4cc; font-size: 12px; }
.set-chip {
  display: inline-block;
  font-size: 12px;
  padding: 1px 8px;
  border-radius: 3px;
  border: 1px solid #dcdfe6;
  color: #909399;
  background: #fff;
  cursor: help;
  line-height: 20px;
}
.set-chip.on {
  border-color: #e6a23c;
  color: #b88230;
  background: #fdf6ec;
}
.set-chip.stack {
  border-color: #e6a23c;
  background: #fbe9d3;
  color: #a05d00;
  font-weight: bold;
}

/* ===== 研磨建议明细 ===== */
.advice-panel {
  border: 1px solid #e6a23c;
  background: #fdf6ec;
  border-radius: 6px;
  padding: 10px 14px;
  margin-bottom: 16px;
}
.advice-title {
  font-weight: bold;
  color: #b88230;
  margin-bottom: 6px;
  font-size: 14px;
}
.advice-line {
  display: flex;
  gap: 8px;
  font-size: 13px;
  color: #606266;
  line-height: 1.7;
}
.advice-slot {
  flex-shrink: 0;
  color: #e6a23c;
  font-weight: bold;
}

/* ===== 场景输入 ===== */
.scene-panel {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px 16px;
  margin-bottom: 16px;
}

/* ===== 应用到龙 ===== */
.apply-bar {
  display: flex;
  justify-content: flex-end;
  padding-bottom: 8px;
}
</style>
