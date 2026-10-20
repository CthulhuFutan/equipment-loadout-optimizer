<!--
  符文外观原子组件
  · 外框 + 品质底 + 符文图（空槽 → 槽位占位图）+ 星等 + 等级数字 + 锁图标 + 可选头像角标 + 可选空槽四角
  · 不负责：tooltip、hover 放大、点击、选中/高亮描边、对比卡与抽屉的布局
-->
<script setup lang="ts">
import { computed } from 'vue'
import { RUNE_QUALITY_COLORS } from '@/utils/constants.ts'
import type { Rune } from '../runeStats.ts'
import RuneLockIcon from './RuneLockIcon.vue'

const props = withDefaults(
  defineProps<{
    rune?: Rune | null
    slot: number
    size?: number | string
    /* 宽度跟随父容器，内部改用 cqw 跟踪真实格宽 */
    fluid?: boolean
    showStar?: boolean
    showLevel?: boolean
    showLock?: boolean
    /* 空槽时显示四角三角 */
    corners?: boolean
    owner?: { name: string; avatar?: string | null } | null
    ownerStealable?: boolean
    /* 符文图相对外框的比例 */
    imgScale?: number
    /* 空槽占位图相对外框的比例 */
    emptyScale?: number
  }>(),
  {
    rune: null,
    size: 70,
    fluid: false,
    showStar: true,
    showLevel: true,
    showLock: true,
    corners: false,
    owner: null,
    ownerStealable: false,
    imgScale: 0.925,
    emptyScale: 0.925,
  },
)

const isEmpty = computed(() => !props.rune)

const rv = computed(() => (typeof props.size === 'number' ? `${props.size}px` : props.size))

const imgSrc = computed(
  () => props.rune?.imageUrl || `/runes/slot/rune_slot_010${props.slot}.png`,
)
const starSrc = computed(() =>
  props.rune ? `/runes/star/rune_star_0${props.rune.starRating}.png` : '',
)
const bg = computed(
  () => (props.rune && RUNE_QUALITY_COLORS[props.rune.quality]) || '#2a1b10',
)

const ownerTitle = computed(() => {
  if (!props.owner) return ''
  const base = `已装备在「${props.owner.name}」身上`
  return props.ownerStealable ? `${base}，装备后将自动从它身上拆下` : base
})
</script>

<template>
  <div class="rv-host" :class="{ 'is-fluid': fluid }" :style="{ '--rv': rv }">
    <div
      class="rv-frame"
      :class="{ 'is-empty': isEmpty, 'is-slot-1': slot === 1 }"
      :style="{
        background: bg,
        '--rv-img-scale': isEmpty ? emptyScale : imgScale,
      }"
    >
      <!-- 符文图 / 空槽占位图 -->
      <img class="rv-img" :src="imgSrc" alt="" />

      <!-- 星条 -->
      <img
        v-if="showStar && rune"
        class="rv-star"
        :class="{ 'is-7star': rune.starRating === 7 }"
        :src="starSrc"
        alt=""
      />

      <!-- 等级数字（+N） -->
      <span v-if="showLevel && rune && rune.levels != null" class="rv-lv">+{{ rune.levels }}</span>

      <!-- 锁状态图标 -->
      <RuneLockIcon v-if="showLock" :locked="!!rune?.locked" />

      <!-- 空槽四角 -->
      <template v-if="corners && isEmpty">
        <span class="rv-corner tl" />
        <span class="rv-corner tr" />
        <span class="rv-corner bl" />
        <span class="rv-corner br" />
      </template>

      <!-- 龙头像角标 -->
      <span v-if="owner" class="rv-owner" :title="ownerTitle">
        <img v-if="owner.avatar" :src="owner.avatar" alt="" />
        <span v-else class="rv-owner-ph">{{ owner.name.slice(0, 1) }}</span>
      </span>
    </div>
  </div>
</template>

<style scoped>
.rv-host {
  display: inline-block;
  vertical-align: top;
}
.rv-host.is-fluid {
  display: block;
  width: 100%;
  container-type: inline-size;
}

/* ===== 外框 ===== */
.rv-frame {
  --rv-bw: 2px;
  --rv-bc: #c0c4cc;
  --rv-img-scale: 0.925;
  --rlb-base: var(--rv);
  position: relative;
  box-sizing: border-box;
  width: var(--rv);
  height: var(--rv);
  border: var(--rv-bw) solid var(--rv-bc);
  border-radius: 0; /* 尖角方形（对齐游戏） */
  overflow: hidden;
}
.rv-host.is-fluid .rv-frame {
  --rv: var(--rune-cell, 70px);
  --rv: 100cqw;
}

/* ===== 符文图：横居中、按外框比例缩放 ===== */
.rv-img {
  position: absolute;
  left: 50%;
  top: 43%;
  transform: translate(-50%, -50%);
  width: calc(var(--rv) * var(--rv-img-scale));
  height: calc(var(--rv) * var(--rv-img-scale));
  object-fit: contain;
  pointer-events: none;
}
/* 空槽：占位图居中；1 号槽：原图图案偏高约 7%，下移对齐 */
.rv-frame.is-empty .rv-img,
.rv-frame.is-slot-1 .rv-img {
  top: 50%;
}

/* ===== 星条：height 决定 7 星、max-width 决定 1~6 星 ===== */
.rv-star {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: calc(var(--rv) * 0.02);
  height: calc(var(--rv) * 0.26);
  max-width: calc(var(--rv) * 0.88);
  object-fit: contain;
  pointer-events: none;
}
.rv-star.is-7star {
  bottom: calc(var(--rv) * 0.04);
}

/* ===== 等级数字：以星等高为唯一基准 ===== */
.rv-lv {
  --star-h: calc(var(--rv) * 0.26);
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

/* ===== 空槽四角 ===== */
.rv-corner {
  position: absolute;
  width: var(--rv-corner, 6px);
  height: var(--rv-corner, 6px);
  background: #c9a05a;
}
.rv-corner.tl {
  top: 0;
  left: 0;
  clip-path: polygon(0 0, 100% 0, 0 100%);
}
.rv-corner.tr {
  top: 0;
  right: 0;
  clip-path: polygon(0 0, 100% 0, 100% 100%);
}
.rv-corner.bl {
  bottom: 0;
  left: 0;
  clip-path: polygon(0 0, 0 100%, 100% 100%);
}
.rv-corner.br {
  bottom: 0;
  right: 0;
  clip-path: polygon(100% 0, 100% 100%, 0 100%);
}

/* ===== 头像角标 ===== */
.rv-owner {
  position: absolute;
  right: calc(var(--rv) / 105);
  top: calc(var(--rv) / 105);
  width: calc(var(--rv) * 0.3);
  height: calc(var(--rv) * 0.3);
  overflow: hidden;
  border: 1px solid #b08d57;
  border-radius: 2px;
  background: #2a1b10;
  box-shadow: 0 0 2px rgba(0, 0, 0, 0.3);
  pointer-events: none;
}
.rv-owner img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* 无立绘兜底：显示名字首字 */
.rv-owner-ph {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: #fff;
  line-height: 1;
}
.rv-owner .rv-owner-ph {
  background: #909399;
  font-size: calc(var(--rv) * 0.12);
}
</style>
