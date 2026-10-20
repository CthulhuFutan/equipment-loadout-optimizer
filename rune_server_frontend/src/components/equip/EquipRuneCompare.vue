<!--
  中列：当前装备 + 候选符文 符文数值对比卡。
    1. 词条行补齐到全库最大值：主属性 + 额外属性 + 副属性 × 4
    2. 符文套组效果始终保持位于同一位置方便比较查看
-->
<script setup lang="ts">
import { computed } from 'vue'
import { getSetEffect } from '@/utils/constants'
import type { Rune } from '../runeStats.ts'
import { fmtStat, normalSubStatsOf, extraStatOf, statLabel } from '../runeStats.ts'
import RuneVisual from '../rune/RuneVisual.vue'
import RuneLockChange from '../rune/RuneLockChange.vue'

const props = withDefaults(
  defineProps<{
    // 该槽当前装备的符文 / null
    equippedRune?: Rune | null
    // 选中的候选符文 / null
    candidateRune?: Rune | null
    // 候选符文是否为当前装备 → 按钮变化
    sameAsEquipped?: boolean
    // 候选符文被别的龙占用时的信息
    candidateOwnerName?: string | null
    candidateOwnerImage?: string | null
    candidateOwnerStealable?: boolean
    // 当前装备的符文被哪条龙装备（正常情况下就是当前调整的龙）
    equippedOwnerName?: string | null
    equippedOwnerImage?: string | null
    // 全库最大词条行数计算 → 用于填充
    maxExtra?: number
    maxSub?: number
  }>(),
  {
    equippedRune: null,
    candidateRune: null,
    sameAsEquipped: false,
    candidateOwnerName: null,
    candidateOwnerImage: null,
    candidateOwnerStealable: false,
    equippedOwnerName: null,
    equippedOwnerImage: null,
    maxExtra: 1,
    maxSub: 4,
  },
)

const emit = defineEmits<{
  (e: 'equip'): void
  (e: 'unequip'): void
  // 点锁按钮：把要切换的符文 id 传输
  (e: 'toggle-lock', runeId: number): void
}>()

const MAX_CELL = 70

// 卡片描述。两张卡结构一致
interface Side {
  key: string
  title: string
  pillClass: string
  pillText: string
  rune: Rune | null
  ownerName: string | null
  ownerImage: string | null
  // 属于别的龙 → 提示语里说明"装备后会自动从它身上拆下"
  ownerStealable: boolean
  emptyLock: string
  emptyName: string
  emptySub: string
  act: 'equip' | 'unequip'
  btnText: string
  btnDisabled: boolean
  btnClass: string
}

const sides = computed<Side[]>(() => [
  {
    key: 'eq',
    title: '当前装备',
    pillClass: props.equippedRune ? 'now' : 'none',
    pillText: props.equippedRune ? '装备中' : '未装备',
    rune: props.equippedRune,
    // 当前装备的符文就是这条龙的，所以它不是「别人的」
    ownerName: props.equippedOwnerName,
    ownerImage: props.equippedOwnerImage,
    ownerStealable: false,
    emptyLock: '未装备',
    emptyName: '该槽位当前为空',
    emptySub: '挑选一个符文装备',
    act: 'unequip',
    btnText: '卸下',
    btnDisabled: !props.equippedRune,
    btnClass: 'danger',
  },
  {
    key: 'cand',
    title: '候选符文',
    pillClass: 'cand',
    pillText: props.candidateRune ? '候选' : '未选择',
    rune: props.candidateRune,
    ownerName: props.candidateOwnerName,
    ownerImage: props.candidateOwnerImage,
    ownerStealable: props.candidateOwnerStealable,
    emptyLock: '未选择',
    emptyName: '尚未选择符文',
    emptySub: '挑选一个符文装备',
    act: 'equip',
    btnText: props.sameAsEquipped ? '已是当前装备' : '装 备',
    btnDisabled: !props.candidateRune || props.sameAsEquipped,
    btnClass: 'primary',
  },
])

const detailPx = computed(() => `${MAX_CELL * 1.5}px`)

// 额外属性补齐：没有时显示「—」占位
function padExtra(n: number): number[] {
  return Array.from({ length: Math.max(0, props.maxExtra - n) })
}
// 副属性补齐：用不可见幽灵行占位
function padSub(n: number): number[] {
  return Array.from({ length: Math.max(0, props.maxSub - n) })
}
function onAct(side: Side) {
  if (side.btnDisabled) return
  if (side.act === 'equip') emit('equip')
  else emit('unequip')
}
</script>

<template>
  <div class="cmp-area">
    <div v-for="s in sides" :key="s.key" class="cmp-card">
      <div class="cmp-title">
        {{ s.title }}
        <span class="pill" :class="s.pillClass">{{ s.pillText }}</span>
      </div>

      <!-- ===== 有符文：完整信息 ===== -->
      <div v-if="s.rune" class="cmp-body">
        <div class="cmp-rune">
          <RuneVisual
            :rune="s.rune"
            :slot="s.rune.slotNumber"
            :size="detailPx"
            :owner="s.ownerName ? { name: s.ownerName, avatar: s.ownerImage } : null"
            :owner-stealable="s.ownerStealable"
          />
        </div>

        <div class="cmp-info">
          <div class="cmp-name">{{ s.rune.runeName || s.rune.setType }}{{ s.rune.slotNumber }}号符文</div>
          <!-- 原副标题，已被清除，但保留，用于占位 -->
          <div class="cmp-sub"></div>

          <div class="stat-group-title">主属性</div>
          <div class="stat-line">
            <span class="k"><img src="/runes/info/prefix_option_0101.png" alt="" />{{ statLabel(s.rune.mainStatType) }}</span>
            <span class="v"><strong>{{ fmtStat(s.rune.mainStatType, s.rune.mainStatValue) }}</strong></span>
          </div>

          <div class="stat-group-title">额外属性</div>
          <div v-if="extraStatOf(s.rune)" class="stat-line">
            <span class="k"><img src="/runes/info/prefix_option_0102.png" alt="" />{{ statLabel(extraStatOf(s.rune)!.statType) }}</span>
            <span class="v"><strong>{{ fmtStat(extraStatOf(s.rune)!.statType, extraStatOf(s.rune)!.statValue) }}</strong></span>
          </div>
          <div v-for="(_, i) in padExtra(extraStatOf(s.rune) ? 1 : 0)" :key="`pe${i}`" class="stat-line empty">
            <span class="k">—</span><span class="v"></span>
          </div>

          <div class="stat-group-title">副属性</div>
          <div
            v-for="sub in normalSubStatsOf(s.rune)"
            :key="`s${sub.sortOrder}`"
            class="stat-line"
            :class="{ ground: s.rune.grindSortOrder === sub.sortOrder }"
          >
            <span class="k">
              <img
                :src="s.rune.grindSortOrder === sub.sortOrder ? '/runes/info/grind_rune_icon.png' : '/runes/info/prefix_option_0103.png'"
                alt=""
              />{{ statLabel(sub.statType) }}
            </span>
            <span class="v"><strong>{{ fmtStat(sub.statType, sub.statValue) }}</strong></span>
          </div>
          <div v-for="(_, i) in padSub(normalSubStatsOf(s.rune).length)" :key="`ps${i}`" class="stat-line ghost">
            <span class="k">—</span><span class="v"></span>
          </div>

          <!-- 符文锁定按钮 -->
          <div class="lock-row">
            <RuneLockChange :locked="!!s.rune.locked" @toggle="emit('toggle-lock', s.rune.runeId)" />
          </div>
          <div class="set-line">
            <div class="sn">{{ s.rune.setType }} {{ getSetEffect(s.rune.setType).count }}套组</div>
            <div class="sd">{{ getSetEffect(s.rune.setType).desc || '—' }}</div>
          </div>
        </div>
      </div>

      <!-- ===== 空态：与填充卡同格式，幽灵块撑高、分隔线与套组块照常启用 ===== -->
      <div v-else class="cmp-body">
        <div class="cmp-rune empty"><div class="lock">{{ s.emptyLock }}</div></div>
        <div class="cmp-info">
          <div class="cmp-name" style="color: #c0c4cc">{{ s.emptyName }}</div>
          <div class="cmp-sub">{{ s.emptySub }}</div>
          <div class="ghost-stats">
            <div class="stat-group-title">主属性</div>
            <div class="stat-line"><span class="k">—</span><span class="v"></span></div>
            <div class="stat-group-title">额外属性</div>
            <div v-for="i in maxExtra" :key="`ge${i}`" class="stat-line"><span class="k">—</span><span class="v"></span></div>
            <div class="stat-group-title">副属性</div>
            <div v-for="i in maxSub" :key="`gs${i}`" class="stat-line"><span class="k">—</span><span class="v"></span></div>
          </div>
          <!-- 幽灵锁按钮行：与填充卡同高，保证左右两卡的套组效果停在同一高度 -->
          <div class="lock-row"><RuneLockChange disabled style="visibility: hidden" /></div>
          <div class="set-line"><div class="sn">无套组</div><div class="sd"></div></div>
        </div>
      </div>

      <div class="cmp-actions">
        <button class="cbtn" :class="s.btnClass" :disabled="s.btnDisabled" @click="onAct(s)">
          {{ s.btnText }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cmp-area {
  display: flex;
  align-items: stretch;
  flex: 1;
  min-height: 0;
}
.cmp-card {
  flex: 1 1 0;
  min-width: 0;
  display: flex;
  flex-direction: column;
  border-right: 1px dashed #ebeef5;
}
.cmp-card:last-child {
  border-right: none;
}
.cmp-title {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 14px 0;
  font-size: 12.5px;
  font-weight: 700;
  color: #606266;
}
.pill {
  font-size: 11px;
  font-weight: 400;
  border-radius: 9px;
  padding: 0 7px;
  line-height: 16px;
}
.pill.now {
  background: #f0f9eb;
  color: #529b2e;
  border: 1px solid #d5edc4;
}
.pill.cand {
  background: #ecf5ff;
  color: #337ecc;
  border: 1px solid #c6e2ff;
}
.pill.none {
  background: #f4f4f5;
  color: #909399;
  border: 1px solid #e9e9eb;
}

.cmp-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  min-height: 272px;
  flex: 1;
}
.cmp-info {
  width: 100%;
  flex: 1;
  min-width: 0;
}

/* 详情区符文框 */
.cmp-rune {
  --detail: v-bind(detailPx);
  position: relative;
  width: var(--detail);
  height: var(--detail);
  flex-shrink: 0;
  box-sizing: border-box;
}
.cmp-rune.empty {
  border: 2px dashed #dcdfe6;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
}
.cmp-rune .lock {
  color: #c0c4cc;
  font-size: 13px;
  letter-spacing: 2px;
}

/* 名称 / 副标题 */
.cmp-name {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 2px;
  text-align: center;
}
.cmp-sub {
  font-size: 13px;
  color: #909399;
  min-height: 20px;
  margin-bottom: 8px;
  text-align: center;
}

/* ---- 词条 ---- */
.stat-group-title {
  font-size: 11.5px;
  font-weight: 700;
  color: #909399;
  margin: 7px 0 2px;
}
.stat-line {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 12.5px;
  padding: 1px 0;
  color: #606266;
}
.stat-line .k {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
}
.stat-line .k img {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}
.stat-line.ground {
  background: #fef0f0;
  color: #c45656;
  border-radius: 3px;
}
.stat-line.empty {
  color: #c0c4cc;
}
.stat-line.ghost {
  visibility: hidden;
}
/* 空态卡的幽灵词条块：结构与填充卡一致，只占高度，内容不显示 */
.ghost-stats {
  visibility: hidden;
}

/* 锁按钮行 */
.lock-row {
  display: flex;
  justify-content: flex-end;
  padding: 42px 7px 11px;
}
/* 隔线 + 套组块 */
.set-line {
  margin-top: 32px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
  text-align: center;
}
.set-line .sn {
  font-weight: 700;
  font-size: 13px;
  color: #303133;
  margin-bottom: 4px;
}
.set-line .sd {
  color: #606266;
  font-size: 13px;
  white-space: pre-line;
  line-height: 1.6;
}

/* ---- 操作按钮 ---- */
.cmp-actions {
  display: flex;
  gap: 8px;
  justify-content: center;
  padding: 0 14px 22px;
}
.cbtn {
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #606266;
  border-radius: 4px;
  padding: 5px 14px;
  font-size: 12.5px;
  cursor: pointer;
  transition: 0.12s;
  font-family: inherit;
}
.cbtn:hover:not(:disabled) {
  border-color: #a0cfff;
  color: #409eff;
  background: #f7fbff;
}
.cbtn.primary {
  background: #409eff;
  border-color: #409eff;
  color: #fff;
}
.cbtn.primary:hover:not(:disabled) {
  background: #66b1ff;
  border-color: #66b1ff;
  color: #fff;
}
.cbtn.danger {
  color: #f56c6c;
  border-color: #fbc4c4;
}
.cbtn.danger:hover:not(:disabled) {
  background: #fef0f0;
  border-color: #f56c6c;
  color: #f56c6c;
}
.cbtn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
</style>
