<!-- 符文详情抽屉 -->
<script setup lang="ts">
import { computed } from 'vue'
import { RUNE_QUALITY_COLORS, getSetEffect } from '@/utils/constants.ts'
import RuneLockIcon from '@/components/rune/RuneLockIcon.vue'

const props = withDefaults(defineProps<{
  modelValue: boolean
  rune: any
  // 当前分
  score?: number | null
  // 研磨后潜在分
  potentialScore?: number | null
  // 研磨建议
  grind?: any
  title?: string
}>(), {
  score: null,
  potentialScore: null,
  grind: null,
  title: '符文详情',
})

const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void }>()

const visible = computed({
  get: () => props.modelValue,
  set: (v: boolean) => emit('update:modelValue', v),
})

const extraStat = computed(() =>
    props.rune?.subStats?.find((s: any) => s.sortOrder === 0) ?? null
)
const normalSubStats = computed(() =>
    (props.rune?.subStats ?? []).filter((s: any) => s.sortOrder !== 0)
)
function statLabel(statType: string): string {
  return statType.replace(/[%+]$/, '')
}
function formatStatValue(statType: string, statValue: any): string {
  const suffix = statType.endsWith('%') ? '%' : ''
  return `+${statValue}${suffix}`
}
// 已研磨词条
function isGroundStat(sub: any): boolean {
  const groundId = props.rune?.grindSortOrder
  if (groundId == null) return false
  return sub.sortOrder === groundId
}
function starSrc(starRating: number): string {
  return `/runes/star/rune_star_0${starRating}.png`
}
function slotBackground(rune: any): string {
  if (!rune) return '#2a1b10'
  return RUNE_QUALITY_COLORS[rune.quality] ?? 'linear-gradient(180deg, #969696 0%, #303030 100%)'
}
</script>

<template>
  <el-drawer v-model="visible" :title="title" direction="rtl" size="360px">
    <template v-if="rune">
      <div style="text-align:center;margin-bottom:20px">
        <div class="drawer-rune-box" :style="{ background: slotBackground(rune) }">
          <img v-if="rune.imageUrl" :src="rune.imageUrl" class="drawer-rune-img"
               :class="{ 'is-slot-1': rune.slotNumber === 1 }" />
          <img v-else :src="`/runes/slot/rune_slot_010${rune.slotNumber}.png`" class="drawer-rune-img"
               :class="{ 'is-slot-1': rune.slotNumber === 1 }" />
          <img :src="starSrc(rune.starRating)" class="drawer-rune-star"
               :class="{ 'is-7star': rune.starRating === 7 }" />
          <!-- 等级数字 -->
          <span v-if="rune.levels != null" class="drawer-rune-lv">+{{ rune.levels }}</span>
          <!-- 锁定状态图标 -->
          <RuneLockIcon :locked="!!rune.locked" base="var(--dr-box)" />
        </div>
        <div style="font-size:18px;font-weight:bold;margin-top:8px">
          {{ rune.runeName || rune.setType }}{{ rune.slotNumber }}号符文
        </div>
        <div style="color:#909399;font-size:13px">
          <span v-if="rune.levels != null">等级 +{{ rune.levels }}</span>
        </div>
      </div>

      <div v-if="score != null || potentialScore != null" class="score-box">
        <div v-if="score != null" class="score-line">
          <span>当前分</span><strong>{{ score }}</strong>
        </div>
        <div v-if="potentialScore != null" class="score-line">
          <span>研磨后潜在分</span><strong>{{ potentialScore }}</strong>
        </div>
      </div>

      <div style="margin-bottom:20px">
        <div style="margin-bottom:12px">
          <div class="stat-title">主属性</div>
          <div class="stat-line">
            <span style="display:flex;align-items:center;gap:6px">
              <img src="/runes/info/prefix_option_0101.png" style="width:18px;height:18px" />
              {{ statLabel(rune.mainStatType) }}
            </span>
            <strong>{{ formatStatValue(rune.mainStatType, rune.mainStatValue) }}</strong>
          </div>
        </div>

        <div v-if="extraStat" style="margin-bottom:12px">
          <div class="stat-title">额外属性</div>
          <div class="stat-line">
            <span style="display:flex;align-items:center;gap:6px">
              <img src="/runes/info/prefix_option_0102.png" style="width:18px;height:18px" />
              {{ statLabel(extraStat.statType) }}
            </span>
            <strong>{{ formatStatValue(extraStat.statType, extraStat.statValue) }}</strong>
          </div>
        </div>

        <div>
          <div class="stat-title">副属性</div>
          <div v-if="normalSubStats.length === 0" class="no-sub">暂无副属性</div>
          <div v-for="sub in normalSubStats" :key="sub.sortOrder"
               class="stat-line" :class="{ ground: isGroundStat(sub) }">
            <span style="display:flex;align-items:center;gap:6px">
              <img v-if="isGroundStat(sub)" src="/runes/info/grind_rune_icon.png" style="width:18px;height:18px" />
              <img v-else src="/runes/info/prefix_option_0103.png" style="width:18px;height:18px" />
              {{ statLabel(sub.statType) }}
            </span>
            <strong>{{ formatStatValue(sub.statType, sub.statValue) }}</strong>
          </div>
        </div>
      </div>

      <div v-if="grind" class="grind-box" :class="{ disabled: !grind.grindable }">
        <div class="grind-head">
          <img src="/runes/info/grind_rune_icon.png" style="width:16px;height:16px" />
          <strong>研磨建议</strong>
        </div>
        <div class="grind-msg">{{ grind.message }}</div>
        <div v-if="grind.targetStat" class="grind-row"><span>目标词条</span><strong>{{ grind.targetStat }}</strong></div>
        <div v-if="grind.minValue != null" class="grind-row"><span>下限值</span><strong>≥ {{ grind.minValue }}</strong></div>
        <div v-if="grind.alreadyGround" class="grind-row"><span>当前研磨词条</span><strong>{{ grind.groundStatType }}（{{ grind.groundStatValue }}）</strong></div>
      </div>

      <div class="set-box">
        <div style="font-weight:bold;margin-bottom:4px">
          {{ rune.setType }} {{ getSetEffect(rune.setType).count }}套组
        </div>
        <div style="color:#606266;font-size:13px;white-space:pre-line">
          {{ getSetEffect(rune.setType).desc }}
        </div>
      </div>

      <!-- 底部动作（可选插槽：页面自放按钮，如 DragonDetail「去搭配」；未传则不渲染） -->
      <slot name="footer" />
    </template>
  </el-drawer>
</template>

<style scoped>
.drawer-rune-box {
  --dr-box: 100px;
  position: relative;
  width: var(--dr-box);
  height: var(--dr-box);
  margin: 0 auto;
  border: 2px solid #C0C4CC;
  border-radius: 0;
}

.drawer-rune-img {
  position: absolute;
  left: 50%;
  top: 43%;
  transform: translate(-50%, -50%);
  width: calc(var(--dr-box) * 0.925);
  height: calc(var(--dr-box) * 0.925);
  object-fit: contain;
}
/* 1号三角形符文：原图图案偏高约7% → 下移对齐 */
.drawer-rune-img.is-slot-1 {
  top: 50%;
}

.drawer-rune-star {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  /* 统一到 Equipped（EquipRuneCell .rt-star）基准 */
  bottom: calc(var(--dr-box) * 0.02);
  height: calc(var(--dr-box) * 0.26);
  max-width: calc(var(--dr-box) * 0.88);
  object-fit: contain;
}

/* 7星符文标识：单独抬升 */
.drawer-rune-star.is-7star {
  bottom: calc(var(--dr-box) * 0.04);
}
/* 等级数字 +N */
.drawer-rune-lv {
  --star-h: calc(var(--dr-box) * 0.26);
  position: absolute;
  left: calc(var(--star-h) * 0.3297);
  bottom: calc(var(--star-h) * 0.808);
  z-index: 2;
  font-size: calc(var(--star-h) * 0.769);
  line-height: calc(var(--star-h) * 0.923);
  font-weight: 500;
  color: #fff;
  pointer-events: none;
  --lv-o: calc(var(--star-h) * 0.055);
  --lv-s: calc(var(--star-h) * 0.115);
  text-shadow:
    calc(-1 * var(--lv-o)) calc(-1 * var(--lv-o)) 0 rgba(0, 0, 0, 0.9),
    var(--lv-o) calc(-1 * var(--lv-o)) 0 rgba(0, 0, 0, 0.9),
    calc(-1 * var(--lv-o)) var(--lv-o) 0 rgba(0, 0, 0, 0.9),
    var(--lv-o) var(--lv-o) 0 rgba(0, 0, 0, 0.9),
    0 0 var(--lv-s) rgba(0, 0, 0, 0.45);
}
.score-box {
  border: 1px solid #e6a23c;
  background: #fdf6ec;
  border-radius: 4px;
  padding: 8px 12px;
  margin-bottom: 16px;
}
.score-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: #606266;
}
.stat-title {
  font-weight: bold;
  margin-bottom: 4px;
  font-size: 13px;
  color: #303133;
}
.stat-line {
  padding: 6px 12px;
  margin-bottom: 1px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}
.stat-line.ground { background: #fef0f0; }
.no-sub { color: #c0c4cc; font-size: 13px; padding: 4px 0; }
.grind-box {
  border: 1px solid #b3d8ff;
  background: #ecf5ff;
  border-radius: 4px;
  padding: 10px 12px;
  margin-bottom: 16px;
}
.grind-box.disabled { border-color: #dcdfe6; background: #f4f4f5; }
.grind-head {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 6px;
}
.grind-msg { font-size: 13px; color: #303133; line-height: 1.6; margin-bottom: 6px; }
.grind-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #606266;
  padding: 2px 0;
}
.set-box {
  text-align: center;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}
</style>
