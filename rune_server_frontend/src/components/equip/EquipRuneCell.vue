<!-- 符文格 -->
<script setup lang="ts">
import { computed } from 'vue'
import type { Rune } from '../runeStats.ts'
import RuneVisual from '../rune/RuneVisual.vue'

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
    // 是否属别的龙 → 影响提示语"会被自动拆下"
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

// 尺寸比例，用于传递给组件
const sizeVar = 'var(--rune-cell, 70px)'
</script>

<template>
  <div
    class="rune-tile"
    :class="{
      empty: !isEquipped,
      'is-equipping': highlight === 'equipping',
      'is-candidate': highlight === 'candidate',
    }"
  >
    <RuneVisual
      :rune="rune"
      :slot="slot"
      :size="sizeVar"
      :show-level="showLevel"
      :owner="ownerName ? { name: ownerName, avatar: ownerImage } : null"
      :owner-stealable="ownerStealable"
    />
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
.rune-tile.is-equipping :deep(.rv-frame) {
  outline: 3px solid #67c23a;
  outline-offset: -3px;
}
.rune-tile.is-candidate :deep(.rv-frame) {
  outline: 3px solid #409eff;
  outline-offset: -3px;
}
</style>
