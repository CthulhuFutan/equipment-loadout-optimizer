<!--
  装备调整 —— 按龙逐槽调整符文。

  数据来源：
    1. getRunes()：每件符文带 equipped / equippedDragonId / slotNumber → 反推每条龙的配装
    2. getDragons()：龙头像 / 名字 / 属性 / 类别
  保存：saveDragonEquipment(dragonId, runeIds)
  每次 装备/卸下 都把该龙当前各槽的符文 ID 列表整体提交
-->
<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getDragons, saveDragonEquipment } from '@/api/dragon'
import { getRunes } from '@/api/rune'
import { RUNE_SET_TYPES, STAT_TYPES } from '@/utils/constants'
import RuneTile from '@/components/equip/RuneTile.vue'
import RuneComparePanel from '@/components/equip/RuneComparePanel.vue'
import BuildSummaryPanel from '@/components/equip/BuildSummaryPanel.vue'
import DragonPickerDialog from '@/components/equip/DragonPickerDialog.vue'
import {
  aggregate,
  buildLoadoutMap,
  buildOwnerMap,
  equippedIdAt,
  indexRunes,
  loadoutRunes,
  setDelta,
  statDelta,
  statLabel,
  fmtStat,
} from '@/components/equip/runeStats'
import type { Dragon, Loadout, Rune } from '@/components/equip/runeStats'

const SLOTS = [1, 2, 3, 4, 5, 6]

const loading = ref(false)
const saving = ref(false)
const runes = ref<Rune[]>([])
const dragons = ref<Dragon[]>([])

const runeMap = computed(() => indexRunes(runes.value))
// 龙 ID → 配装
const loadouts = computed(() => buildLoadoutMap(runes.value))
const ownerMap = computed(() => buildOwnerMap(dragons.value, loadouts.value))
const dragonMap = computed(() => {
  const m = new Map<number, Dragon>()
  for (const d of dragons.value) m.set(d.dragonId, d)
  return m
})

const curDragonId = ref<number | null>(null)
const curSlot = ref(1)
const candidateId = ref<number | null>(null)

const curLoadout = computed<Loadout>(() =>
  curDragonId.value == null ? {} : (loadouts.value[curDragonId.value] ?? {}),
)
const equippedRune = computed<Rune | null>(() => {
  const id = equippedIdAt(curLoadout.value, curSlot.value)
  return id == null ? null : (runeMap.value.get(id) ?? null)
})

function equippedCount(dragonId: number): number {
  return Object.keys(loadouts.value[dragonId] ?? {}).length
}
function ownerOf(runeId: number): Dragon | null {
  const id = ownerMap.value.get(runeId)
  return id == null ? null : (dragonMap.value.get(id) ?? null)
}
// 如果该符文是否正被装备，显示装备龙的头像
function isStolen(runeId: number): boolean {
  const owner = ownerOf(runeId)
  return !!owner && owner.dragonId !== curDragonId.value
}

// 顶部横条：列出已装备符文的龙
const barDragons = computed(() =>
  dragons.value.filter((d) => equippedCount(d.dragonId) > 0 || d.dragonId === curDragonId.value),
)

const filterSet = ref('')
const filterMain = ref('')
const filterSub = ref('')
// 默认可以查看已装备符文
const showEquipped = ref(true)

const setOptions = RUNE_SET_TYPES.map((s) => s.value)
const statOptions = STAT_TYPES.map((s) => s.value)

const filtering = computed(
  () => (filterSet.value || filterMain.value || filterSub.value || !showEquipped.value),
)

function runeMatches(r: Rune): boolean {
  if (filterSet.value && r.setType !== filterSet.value) return false
  if (filterMain.value && r.mainStatType !== filterMain.value) return false
  if (filterSub.value && !(r.subStats ?? []).some((s) => s.statType === filterSub.value)) return false
  if (!showEquipped.value && r.equipped) return false
  return true
}

/**
 * 候选池：当前槽位 + 通过筛选
 * 采用后端 ENUM 预定顺序进行展示
 */
const candidatePool = computed(() =>
  runes.value.filter((r) => r.slotNumber === curSlot.value && runeMatches(r)),
)
const candidateRune = computed<Rune | null>(() =>
  candidateId.value == null ? null : (runeMap.value.get(candidateId.value) ?? null),
)
const sameAsEquipped = computed(
  () => !!candidateRune.value && candidateRune.value.runeId === equippedRune.value?.runeId,
)

// 槽位卡数字，显示当前符文数量，会因为筛选而变化
const slotCounts = computed(() => {
  const total: Record<number, number> = {}
  const matched: Record<number, number> = {}
  for (const r of runes.value) {
    total[r.slotNumber] = (total[r.slotNumber] ?? 0) + 1
    if (runeMatches(r)) matched[r.slotNumber] = (matched[r.slotNumber] ?? 0) + 1
  }
  return SLOTS.map((n) => (filtering.value ? (matched[n] ?? 0) : (total[n] ?? 0)))
})

// 筛选条件变化，展示的符文默认选中第一个或返回空
function ensureCandidate() {
  const pool = candidatePool.value
  if (!pool.length) {
    candidateId.value = null
    return
  }
  if (candidateId.value == null || !pool.some((r) => r.runeId === candidateId.value)) {
    candidateId.value = pool[0]?.runeId ?? null
  }
}

const equippedRunes = computed(() => loadoutRunes(curLoadout.value, runeMap.value))
const totalSum = computed(() => aggregate(equippedRunes.value))
const maxExtra = computed(() =>
  Math.max(1, ...runes.value.map((r) => (r.subStats ?? []).filter((s) => s.sortOrder === 0).length)),
)
const maxSub = computed(() =>
  Math.max(1, ...runes.value.map((r) => (r.subStats ?? []).filter((s) => s.sortOrder !== 0).length)),
)

// 辅助显示装备后符文数值和套组的变化
const afterRunes = computed<Rune[]>(() => {
  const cand = candidateRune.value
  if (!cand) return equippedRunes.value
  const rest = equippedRunes.value.filter((r) => r.slotNumber !== curSlot.value)
  return [...rest, cand].sort((a, b) => a.slotNumber - b.slotNumber)
})

const deltaRows = computed(() => statDelta(equippedRunes.value, afterRunes.value))
const deltaSets = computed(() => setDelta(equippedRunes.value, afterRunes.value))
const deltaHint = computed(() =>
  candidateRune.value
    ? `${candidateRune.value.runeName || candidateRune.value.setType}${candidateRune.value.slotNumber}号符文 → ${curSlot.value} 号槽`
    : '',
)
// 属性变化的备选展示内容：无候选 / 候选就是当前装备
const deltaPlaceholder = computed<{ title: string; text: string } | null>(() => {
  if (!candidateRune.value) return { title: '尚未选择符文', text: '点左侧网格里的符文作为候选' }
  if (sameAsEquipped.value) return { title: '已是当前装备', text: '该符文已装在本槽，换一件才能看到变化' }
  return null
})

async function reloadRunes() {
  const res = await getRunes()
  runes.value = (res.data ?? []) as Rune[]
}

async function load() {
  loading.value = true
  try {
    const [r, d] = await Promise.all([getRunes(), getDragons({ pageNum: 1, pageSize: 500 })])
    runes.value = (r.data ?? []) as Rune[]
    dragons.value = ((d.data?.records ?? []) as Dragon[]) || []
    if (curDragonId.value == null) {
      // 默认选第一条已装备符文的龙；都没有就选第一条
      const lo = buildLoadoutMap(runes.value)
      const initial =
        dragons.value.find((x) => Object.keys(lo[x.dragonId] ?? {}).length > 0) ?? dragons.value[0]
      curDragonId.value = initial?.dragonId ?? null
    }
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

/**
 * 提交配装：后端整装替换，每次都传该龙完整的符文 ID 列表。
 * 失败时重新拉取服务端状态（等效回滚），避免界面与库里不一致。
 */
async function submit(next: Loadout) {
  const dragonId = curDragonId.value
  if (dragonId == null || saving.value) return false
  saving.value = true
  try {
    const ids = Object.values(next).filter((v): v is number => v != null)
    await saveDragonEquipment(dragonId, ids)
    await reloadRunes()
    return true
  } catch {
    await reloadRunes()
    return false
  } finally {
    saving.value = false
  }
}

async function equip() {
  const cand = candidateRune.value
  if (!cand) return
  const slot = curSlot.value
  const ok = await submit({ ...curLoadout.value, [slot]: cand.runeId })
  if (ok) {
    ElMessage.success(`已装备：${cand.runeName || cand.setType}${cand.slotNumber}号符文 → ${slot} 号槽`)
  }
}

async function unequip() {
  const r = equippedRune.value
  if (!r) return
  const next: Loadout = { ...curLoadout.value }
  delete next[curSlot.value]
  const ok = await submit(next)
  ensureCandidate()
  if (ok) {
    ElMessage.success(`已卸下：${r.runeName || r.setType}${r.slotNumber}号符文`)
  }
}

const pickerVisible = ref(false)

function selectDragon(id: number) {
  curDragonId.value = id
  curSlot.value = 1
  candidateId.value = null
  ensureCandidate()
}
function selectSlot(slot: number) {
  curSlot.value = slot
  candidateId.value = null // 换槽等于换了一批候选，需要重挑
  ensureCandidate()
}
function onFilterChange() {
  candidateId.value = null
  ensureCandidate()
}
function quickEquip(runeId: number) {
  candidateId.value = runeId
  equip()
}

watch(candidatePool, () => ensureCandidate())

onMounted(load)
</script>

<template>
  <div v-loading="loading || saving" class="equip-page">
    <div class="page-head">
      <h2>装备调整</h2>
    </div>

    <!-- ===== 顶部：角色选择条 ===== -->
    <div class="dragon-bar">
      <button
        class="btn-pick"
        title="选择角色（打开龙图鉴）"
        aria-label="选择角色"
        @click="pickerVisible = true"
      >
        <span class="caret">▼</span>
      </button>
      <div class="dragon-scroll">
        <div
          v-for="d in barDragons"
          :key="d.dragonId"
          class="dragon-chip"
          :class="{ active: d.dragonId === curDragonId }"
          :title="`${d.dragonName} · 已装备 ${equippedCount(d.dragonId)}/6`"
          @click="selectDragon(d.dragonId)"
        >
          <img v-if="d.dragonImage" :src="d.dragonImage" alt="" />
          <div>
            <div class="dc-name">{{ d.dragonName }}</div>
            <div class="dc-sub">已装备 {{ equippedCount(d.dragonId) }}/6</div>
          </div>
        </div>
        <div v-if="!barDragons.length" class="db-empty">暂无已装备符文的龙</div>
      </div>
    </div>

    <div class="layout">
      <!-- ===== 槽位行 → 筛选行 → 候选符文网格 ===== -->
      <div class="col-left panel">
        <div class="slot-row">
          <div
            v-for="(n, i) in SLOTS"
            :key="n"
            class="slot-card"
            :class="{ active: n === curSlot }"
            :title="filtering ? `${n} 号槽位 · 匹配 ${slotCounts[i] ?? 0} 件` : `${n} 号槽位 · 持有 ${slotCounts[i] ?? 0} 件`"
            @click="selectSlot(n)"
          >
            <RuneTile
              :rune="runeMap.get(equippedIdAt(curLoadout, n) ?? -1) ?? null"
              :slot="n"
            />
            <span class="sc-count" :class="{ zero: filtering && !slotCounts[i] }">
              {{ slotCounts[i] ?? 0 }}
            </span>
          </div>
        </div>

        <div class="filter-row">
          <label>套组</label>
          <select v-model="filterSet" @change="onFilterChange">
            <option value="">全部</option>
            <option v-for="s in setOptions" :key="s" :value="s">{{ s }}</option>
          </select>

          <label>主属性</label>
          <select v-model="filterMain" @change="onFilterChange">
            <option value="">全部</option>
            <option v-for="s in statOptions" :key="s" :value="s">{{ s }}</option>
          </select>

          <label>副属性</label>
          <select v-model="filterSub" @change="onFilterChange">
            <option value="">全部</option>
            <option v-for="s in statOptions" :key="s" :value="s">{{ s }}</option>
          </select>

          <label
            class="fr-check"
            title="勾选：显示全部符文（已装备的会标出占用龙头像）／取消：只看未装备的符文"
          >
            <input v-model="showEquipped" type="checkbox" @change="onFilterChange" /> 已装备
          </label>
          <span class="right">共 {{ candidatePool.length }} 件</span>
        </div>

        <div class="rune-grid">
          <div
            v-for="r in candidatePool"
            :key="r.runeId"
            class="rune-cell"
            :title="`${r.setType} ${r.slotNumber}号符文${isStolen(r.runeId) ? `（已装备在${ownerOf(r.runeId)?.dragonName}）` : ''}｜单击选中，双击直接装备`"
            @click="candidateId = r.runeId"
            @dblclick="quickEquip(r.runeId)"
          >
            <RuneTile
              :rune="r"
              :slot="r.slotNumber"
              :highlight="
                r.runeId === equippedIdAt(curLoadout, curSlot)
                  ? 'equipping'
                  : r.runeId === candidateId
                    ? 'candidate'
                    : null
              "
              :owner-image="ownerOf(r.runeId)?.dragonImage ?? null"
              :owner-name="ownerOf(r.runeId)?.dragonName ?? null"
              :owner-stealable="isStolen(r.runeId)"
            />
            <div class="rc-main">{{ statLabel(r.mainStatType) }} {{ fmtStat(r.mainStatType, r.mainStatValue) }}</div>
          </div>
          <div v-if="!candidatePool.length" class="grid-empty">该槽位没有符合条件的符文</div>
        </div>
      </div>

      <!-- ===== 当前装备 / 候选符文 对比 ===== -->
      <div class="col-cand panel">
        <div class="panel-head"><h3>{{ curSlot }} 号槽位</h3></div>
        <RuneComparePanel
          :equipped-rune="equippedRune"
          :candidate-rune="candidateRune"
          :same-as-equipped="sameAsEquipped"
          :equipped-owner-name="equippedRune ? ownerOf(equippedRune.runeId)?.dragonName ?? null : null"
          :equipped-owner-image="equippedRune ? ownerOf(equippedRune.runeId)?.dragonImage ?? null : null"
          :candidate-owner-name="candidateRune ? ownerOf(candidateRune.runeId)?.dragonName ?? null : null"
          :candidate-owner-image="candidateRune ? ownerOf(candidateRune.runeId)?.dragonImage ?? null : null"
          :candidate-owner-stealable="candidateRune ? isStolen(candidateRune.runeId) : false"
          :max-extra="maxExtra"
          :max-sub="maxSub"
          @equip="equip"
          @unequip="unequip"
        />
      </div>

      <!-- ===== 属性变化 → 属性总和 ===== -->
      <BuildSummaryPanel
        :delta-rows="deltaRows"
        :delta-sets="deltaSets"
        :hint="deltaHint"
        :placeholder-title="deltaPlaceholder?.title ?? null"
        :placeholder-text="deltaPlaceholder?.text ?? null"
        :stats="totalSum.stats"
        :sets="totalSum.sets"
      />
    </div>

    <DragonPickerDialog
      v-model="pickerVisible"
      :dragons="dragons"
      :equipped-count="equippedCount"
      :current-dragon-id="curDragonId"
      @select="selectDragon"
    />
  </div>
</template>

<style scoped>
.equip-page {
  --rune-cell: 70px;
  --left-col-w: 482px;
  --grid-h: 589px;
}
.page-head {
  margin-bottom: 12px;
}
.page-head h2 {
  font-size: 20px;
  font-weight: 700;
  margin: 0;
}

/* ============ 顶部角色横条 ============ */
.dragon-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fff;
  padding: 10px 12px;
  margin-bottom: 14px;
}
.btn-pick {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: 1px solid #409eff;
  background: #ecf5ff;
  color: #409eff;
  border-radius: 4px;
  cursor: pointer;
  font-size: 11px;
  line-height: 1;
}
.btn-pick .caret {
  margin-bottom: 2px;
}
.dragon-scroll {
  display: flex;
  align-items: stretch;
  gap: 6px;
  overflow-x: auto;
  padding-bottom: 2px;
  min-width: 0;
}
.dragon-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 3px 10px 3px 3px;
  cursor: pointer;
  background: #fff;
  transition: 0.12s;
}
.dragon-chip:hover {
  border-color: #a0cfff;
}
.dragon-chip.active {
  border-color: #409eff;
  background: #ecf5ff;
}
.dragon-chip img {
  width: 34px;
  height: 34px;
  border-radius: 3px;
  object-fit: cover;
  display: block;
}
.dc-name {
  font-size: 12.5px;
  color: #303133;
  max-width: 190px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.dc-sub {
  font-size: 11px;
  color: #909399;
}
.db-empty {
  font-size: 12.5px;
  color: #c0c4cc;
  padding: 14px 4px;
}

/* ============ 三列布局 ============ */
.layout {
  display: flex;
  gap: 12px;
  align-items: stretch;
}
.panel {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fff;
}
.panel-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding: 10px 14px;
  border-bottom: 1px solid #ebeef5;
}
.panel-head h3 {
  font-size: 14px;
  font-weight: 700;
  margin: 0;
}
.col-left {
  flex: 0 0 var(--left-col-w);
  width: var(--left-col-w);
  display: flex;
  flex-direction: column;
}
.col-cand {
  flex: 1 1 auto;
  min-width: 520px;
  display: flex;
  flex-direction: column;
}

/* ---- 槽位行：6 列，与下方网格逐列对齐 ---- */
.slot-row {
  display: grid;
  grid-template-columns: repeat(6, var(--rune-cell));
  gap: 4px;
  padding: 12px 14px;
  align-content: start;
  border-bottom: 1px solid #ebeef5;
}
.slot-card {
  width: var(--rune-cell);
  cursor: pointer;
  border-radius: 3px;
  outline: 2px solid transparent;
  outline-offset: 1px;
  transition: 0.12s;
}
.slot-card:hover {
  outline-color: #a0cfff;
}
.slot-card.active {
  outline-color: #409eff;
}
.sc-count {
  display: block;
  text-align: center;
  font-size: 10.5px;
  line-height: 14px;
  color: #909399;
  margin-top: 1px;
  pointer-events: none;
}
.slot-card.active .sc-count {
  color: #409eff;
  font-weight: 700;
}
.sc-count.zero,
.slot-card.active .sc-count.zero {
  color: #dcdfe6;
}
.slot-card.active .sc-count.zero {
  font-weight: 400;
}

/* ---- 筛选行 ---- */
.filter-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 8px;
  padding: 8px 14px;
  border-bottom: 1px solid #ebeef5;
  background: #fafbfc;
}
.filter-row label {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}
.filter-row select {
  /* 符文主属性宽度写死，避免属性内容长度不一导致的抖动 */
  width: 118px;
  font-family: inherit;
  font-size: 12.5px;
  color: #303133;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 4px 8px;
  outline: none;
}
.filter-row select:focus {
  border-color: #409eff;
}
.filter-row .fr-check {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #606266;
  cursor: pointer;
  user-select: none;
  flex-shrink: 0;
}
.filter-row .fr-check input {
  width: 14px;
  height: 14px;
  margin: 0;
  accent-color: #409eff;
  cursor: pointer;
}
.filter-row .right {
  font-size: 12px;
  color: #909399;
  margin-left: auto;
  white-space: nowrap;
}

/* 候选符文网格：固定高度 + 内部滚动 */
.rune-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, var(--rune-cell)));
  justify-content: start;
  align-content: start;
  gap: 7px 4px;
  padding: 12px 14px;
  height: var(--grid-h);
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-gutter: stable;
}
.rune-grid::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}
.rune-grid::-webkit-scrollbar-thumb {
  background: #c0c4cc;
  border-radius: 4px;
}
.rune-grid::-webkit-scrollbar-track {
  background: #f7f8fa;
}
.rune-cell {
  cursor: pointer;
  width: 100%;
  min-width: 0;
}
.rune-cell:hover :deep(.rt-icon) {
  outline: 2px solid #a0cfff;
  outline-offset: -2px;
}
.rc-main {
  text-align: center;
  font-size: 10px;
  line-height: 15px;
  color: #606266;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 2px;
  white-space: nowrap;
  overflow: hidden;
}
.grid-empty {
  grid-column: 1 / -1;
  text-align: center;
  color: #c0c4cc;
  padding: 36px 0;
  font-size: 13px;
}

@media (min-width: 1641px) {
  .filter-row {
    min-height: 82px;
  }
}
@media (max-width: 1640px) {
  .layout {
    flex-wrap: wrap;
  }
  .col-left {
    flex: 1 1 100%;
    width: auto;
    min-width: 0;
  }
  /* 左列占满整行后，网格用 auto-fill 把右侧空着的宽度也排上符文 */
  .rune-grid {
    grid-template-columns: repeat(auto-fill, var(--rune-cell));
  }
  .col-cand {
    flex: 1 1 480px;
    width: auto;
    min-width: 0;
  }
}
@media (max-width: 1140px) {
  .col-cand {
    flex: 1 1 100%;
  }
}
/* 尽量确保两张符文属性对比卡处于同一横线 */
@media (max-width: 800px) {
  :deep(.cmp-area) {
    flex-direction: column;
  }
  :deep(.cmp-card) {
    border-right: none;
    border-bottom: 1px dashed #ebeef5;
  }
  :deep(.cmp-card:last-child) {
    border-bottom: none;
  }
}
</style>
