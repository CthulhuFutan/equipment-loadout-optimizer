<!-- 70×70 符文格 -->
<script setup lang="ts">
import { computed } from 'vue'
import { RUNE_QUALITY_COLORS } from '@/utils/constants'
import type { Rune } from './runeStats'

const props = withDefaults(
  defineProps<{
    // 该槽已装备的符文；为空 → 显示槽位形状图
    rune?: Rune | null
    // 槽位号 1~6
    slot: number
    // 是否显示左下角等级角标
    showLevel?: boolean
    // 高亮描边：装备中（绿）/ 候选（蓝）
    highlight?: 'equipping' | 'candidate' | null
    // 该符文处于装备状态时，右上角挂装备它的龙头像（本龙装备也会挂）
    ownerImage?: string | null
    ownerName?: string | null
    // 是否属于「别的龙」—— 仅影响提示语里要不要提"会被自动拆下"
    ownerStealable?: boolean
  }>(),
  {
    rune: null,
    showLevel: true,
    highlight: null,
    ownerImage: null,
    ownerName: null,
    ownerStealable: false,
  },
)

const isEquipped = computed(() => !!props.rune)
// 已装备：符文图；空槽：该槽位的空槽形状图
const imgSrc = computed(
  () => props.rune?.imageUrl || `/runes/slot/rune_slot_010${props.slot}.png`,
)
const bg = computed(() => (props.rune && RUNE_QUALITY_COLORS[props.rune.quality]) || '#2a1b10')
const starSrc = computed(() =>
  props.rune ? `/runes/star/rune_star_0${props.rune.starRating}.png` : '',
)
</script>

<template>
  <div
    class="rune-tile"
    :class="[
      `slot-${slot}`,
      {
        empty: !isEquipped,
        'is-equipping': highlight === 'equipping',
        'is-candidate': highlight === 'candidate',
      },
    ]"
  >
    <div class="rt-icon" :style="{ background: bg }">
      <img class="rt-img" :src="imgSrc" alt="" />
      <img
        v-if="isEquipped"
        class="rt-star"
        :class="{ is7: rune?.starRating === 7 }"
        :src="starSrc"
        alt=""
      />
      <!-- 占用龙头像 -->
      <span
        v-if="ownerName"
        class="rt-owner"
        :title="`已装备在「${ownerName}」身上${ownerStealable ? '，装备后将自动从它身上拆下' : ''}`"
      >
        <img v-if="ownerImage" :src="ownerImage" alt="" />
        <span v-else class="rt-owner-ph">{{ ownerName.slice(0, 1) }}</span>
      </span>
      <!-- 等级角标：位于符文图片之后 → 之上 -->
      <span v-if="isEquipped && showLevel && rune?.levels != null" class="rt-lv">
        +{{ rune.levels }}
      </span>
    </div>
    <slot />
  </div>
</template>

<style scoped>
.rune-tile {
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: 100%;
  min-width: 0;
}
.rt-icon {
  position: relative;
  width: 100%;
  height: auto;
  aspect-ratio: 1 / 1;
  border: 2px solid #c0c4cc;
  overflow: hidden;
  box-sizing: border-box;
}
.rune-tile.is-equipping .rt-icon {
  outline: 3px solid #67c23a;
  outline-offset: -3px;
}
.rune-tile.is-candidate .rt-icon {
  outline: 3px solid #409eff;
  outline-offset: -3px;
}

/* ---- 符文图 ---- */
.rt-img {
  position: absolute;
  left: 50%;
  top: 43%;
  transform: translate(-50%, -50%);
  width: calc(var(--rune-cell, 70px) * 0.925);
  height: calc(var(--rune-cell, 70px) * 0.925);
  object-fit: contain;
  pointer-events: none;
}
.rune-tile.empty .rt-img,
.rune-tile.slot-1 .rt-img {
  top: 50%;
}

/* ---- 星标 ---- */
.rt-star {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: calc(var(--rune-cell, 70px) * 0.02);
  height: calc(var(--rune-cell, 70px) * 0.26);
  max-width: calc(var(--rune-cell, 70px) * 0.88);
  object-fit: contain;
  pointer-events: none;
}
.rt-star.is7 {
  bottom: calc(var(--rune-cell, 70px) * 0.04);
}

/* ---- 等级角标：左下角、紧贴星标之上。6星 与 7星 存在一定差距，需要微调 ---- */
.rt-lv {
  position: absolute;
  left: calc(var(--rune-cell, 70px) * 0.05);
  bottom: calc(var(--rune-cell, 70px) * 0.193);
  z-index: 2;
  font-size: calc(var(--rune-cell, 70px) * 0.2);
  line-height: calc(var(--rune-cell, 70px) * 0.24);
  font-weight: 500;
  color: #fff;
  pointer-events: none;
  --lv-o: calc(var(--rune-cell, 70px) * 0.0143);
  --lv-s: calc(var(--rune-cell, 70px) * 0.03);
  text-shadow:
    calc(-1 * var(--lv-o)) calc(-1 * var(--lv-o)) 0 rgba(0, 0, 0, 0.9),
    var(--lv-o) calc(-1 * var(--lv-o)) 0 rgba(0, 0, 0, 0.9),
    calc(-1 * var(--lv-o)) var(--lv-o) 0 rgba(0, 0, 0, 0.9),
    var(--lv-o) var(--lv-o) 0 rgba(0, 0, 0, 0.9),
    0 0 var(--lv-s) rgba(0, 0, 0, 0.45);
}

/* ---- 符文装备对应龙头像：尖角矩形、紧贴右上边缘 ---- */
.rt-owner {
  position: absolute;
  right: 0;
  top: 0;
  width: calc(var(--rune-cell, 70px) * 0.26);
  height: calc(var(--rune-cell, 70px) * 0.26);
  overflow: hidden;
  border-left: 1px solid #fff;
  border-bottom: 1px solid #fff;
  box-shadow: 0 0 2px rgba(0, 0, 0, 0.3);
  pointer-events: none;
}
.rt-owner img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
/* 龙没有立绘时的兜底：显示名字首字（与 RuneSlot.vue 的 owner-ph 同做法） */
.rt-owner-ph {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: #909399;
  color: #fff;
  font-size: calc(var(--rune-cell, 70px) * 0.12);
  line-height: 1;
}
</style>
