<!--
  共享符文槽组件：单槽显示
   - 空槽：深色底 + 金色角标 + 槽位占位图
   - 装备：按品质渐变底色 + 符文图 + 星级（星级溢出槽位下方）
   - 悬停 tooltip 显示符文名；点击 emit('click') 交给父级（打开详情抽屉）
  尺寸通过 size 切换：md=65px（默认）/ lg=84px
-->
<script setup lang="ts">
import { computed } from 'vue'
import { RUNE_QUALITY_COLORS } from '@/utils/constants'

const props = withDefaults(defineProps<{
  /** 槽位上的符文，null/undefined 表示空槽 */
  rune: any
  /** 槽位号 1~6（空槽占位图路径依赖它） */
  slot: number
  /** 尺寸：md=65px 槽位（默认）；lg=84px 大槽位 */
  size?: 'md' | 'lg'
  /** 左下角角标：该符文已装备在别的龙身上，展示占用龙头像与名字 */
  owner?: { name: string; avatar: string | null } | null
}>(), {
  size: 'md',
  owner: null,
})

const emit = defineEmits<{ (e: 'click'): void }>()

// 不同尺寸对应的槽位边长（符文图 / 星星比例见下方 .slot-img / .slot-star，随 --slot 缩放）
const slotVars = computed<Record<string, string>>(() =>
  props.size === 'lg'
    ? { '--slot': '84px' }
    : { '--slot': '65px' }
)

function slotBackground(rune: any): string {
  if (!rune) return '#2a1b10'
  return RUNE_QUALITY_COLORS[rune.quality] ?? 'linear-gradient(180deg, #969696 0%, #303030 100%)'
}
function slotImage(rune: any, slot: number): string {
  return rune?.imageUrl || `/runes/slot/rune_slot_010${slot}.png`
}
function starSrc(starRating: number): string {
  return `/runes/star/rune_star_0${starRating}.png`
}

const tooltipText = computed(() => {
  if (!props.rune) return `${props.slot}号槽位（空）`
  const name = `${props.rune.runeName || props.rune.setType}${props.slot}号符文`
  let text = props.rune.levels != null ? `${name} · +${props.rune.levels}` : name
  if (props.owner) text += `\n已装备在「${props.owner.name}」身上`
  return text
})
</script>

<template>
  <el-tooltip placement="top" :show-after="200">
    <template #content>
      <div>{{ tooltipText }}</div>
    </template>
    <div
      class="rune-slot"
      :class="{ 'is-empty': !rune, 'is-lg': size === 'lg', 'is-slot-1': slot === 1 }"
      :style="{
        ...slotVars,
        background: slotBackground(rune),
      }"
      @click="emit('click')"
    >
      <span class="corner tl" />
      <span class="corner tr" />
      <span class="corner bl" />
      <span class="corner br" />
      <img :src="slotImage(rune, slot)" class="slot-img" />
      <img v-if="rune" :src="starSrc(rune.starRating)" class="slot-star"
           :class="{ 'is-7star': rune.starRating === 7 }" />
      <!-- 占龙头像角标（该符文已装备在别的龙身上） -->
      <span v-if="owner" class="owner-badge" :title="`已装备在「${owner.name}」身上`">
        <img v-if="owner.avatar" :src="owner.avatar" />
        <span v-else class="owner-ph">{{ owner.name.slice(0, 1) }}</span>
      </span>
    </div>
  </el-tooltip>
</template>

<style scoped>
.rune-slot {
  position: relative;
  width: var(--slot);
  height: var(--slot);
  border-radius: 0; /* 尖角方形符文框（对齐游戏）；owner-badge 角标仍保留圆角 */
  border: 2px solid #C0C4CC; /* 边框与 RuneList 符文格一致（--rune-border 2px） */
  cursor: pointer;
  box-sizing: border-box;
  transition: transform .1s;
}
.rune-slot:hover { transform: scale(1.06); }
.rune-slot.is-empty { cursor: default; }
.rune-slot.is-empty:hover { transform: none; }

/* 符文图：对齐 DragonDetail 槽内样式——img 居中 + top 43%（图案中心偏上），
   img = 槽宽 × 0.925（图案在源图内约占 50%，自然显示为槽宽一半略小） */
.slot-img {
  position: absolute;
  left: 50%;
  top: 43%;
  transform: translate(-50%, -50%);
  width: calc(var(--slot) * 0.925);
  height: calc(var(--slot) * 0.925);
  object-fit: contain;
  pointer-events: none;
}
.rune-slot.is-empty .slot-img {
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: calc(var(--slot) * 0.925);
  height: calc(var(--slot) * 0.925);
}
/* 1号三角形符文：原图图案在画布内偏高约7%（43% + 7% = 50%），下移对齐 */
.rune-slot.is-slot-1:not(.is-empty) .slot-img {
  top: 50%;
}

/* 星级：对齐 DragonDetail——bottom 锚定框底；height=7星、max-width=1~6星。
   比例：bottom ×0.0143、height ×0.2857、max-width ×0.9571，再统一 ×0.925 与符文图同缩放 */
.slot-star {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: calc(var(--slot) * 0.0143);
  height: calc(var(--slot) * 0.2857 * 0.925);
  max-width: calc(var(--slot) * 0.9571 * 0.925);
  object-fit: contain;
  pointer-events: none;
}
/* 7星星星：竖块比6星高 → 单独抬升（×0.0286） */
.rune-slot:not(.is-empty) .slot-star.is-7star {
  bottom: calc(var(--slot) * 0.0286);
}

/* 占龙头像角标：左下角圆角方形小头像 */
.owner-badge {
  position: absolute;
  left: 1px;
  bottom: 1px;
  width: 18px;
  height: 18px;
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid #fff;
  box-shadow: 0 0 3px rgba(0, 0, 0, .35);
  background: #f5f7fa;
  pointer-events: none;
}
.owner-badge img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.owner-ph {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 10px;
  color: #fff;
  background: #409eff;
}

/* 角三角形：默认隐藏，空槽时显示 */
.rune-slot .corner {
  position: absolute;
  width: 6px;
  height: 6px;
  background: #c9a05a;
  display: none;
}
.rune-slot.is-empty .corner { display: block; }
.corner.tl { top: 0; left: 0; clip-path: polygon(0 0, 100% 0, 0 100%); }
.corner.tr { top: 0; right: 0; clip-path: polygon(0 0, 100% 0, 100% 100%); }
.corner.bl { bottom: 0; left: 0; clip-path: polygon(0 0, 0 100%, 100% 100%); }
.corner.br { bottom: 0; right: 0; clip-path: polygon(100% 0, 100% 100%, 0 100%); }
</style>
