<!--
  共享符文槽组件：单槽显示
   - 空槽：深色底 + 金色角标 + 槽位占位图
   - 装备：按品质渐变底色 + 符文图 + 星级
-->
<script setup lang="ts">
import { computed } from 'vue'
import RuneVisual from '@/components/rune/RuneVisual.vue'

const props = withDefaults(defineProps<{
  /** 槽位上的符文，null/undefined 表示空槽 */
  rune: any
  /** 槽位号 1~6（空槽占位图路径依赖它） */
  slot: number
  /** 尺寸：md=65px 槽位（默认）；lg=84px 大槽位 */
  size?: 'md' | 'lg'
  /** 右上角角标：该符文已装备在别的龙身上，展示占用龙头像与名字 */
  owner?: { name: string; avatar: string | null } | null
}>(), {
  size: 'md',
  owner: null,
})

const emit = defineEmits<{ (e: 'click'): void }>()


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
    <div class="rune-slot" :class="{ 'is-empty': !rune }" @click="emit('click')">
      <RuneVisual :rune="rune" :slot="slot" :size="size === 'lg' ? 84 : 65" corners :owner="owner" />
    </div>
  </el-tooltip>
</template>

<style scoped>
.rune-slot {
  position: relative;
  cursor: pointer;
  transition: transform .1s;
}
.rune-slot:hover { transform: scale(1.06); }
.rune-slot.is-empty { cursor: default; }
.rune-slot.is-empty:hover { transform: none; }

</style>
