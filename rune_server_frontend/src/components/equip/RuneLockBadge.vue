<!--
  符文锁 - 状态图标
  任何显示 锁定符文 的位置都挂这一个组件
-->
<script setup lang="ts">
withDefaults(
  defineProps<{
    /* 是否已锁定；false 时不渲染任何东西 */
    locked?: boolean
    /* 悬停提示 */
    title?: string
    base?: string
  }>(),
  { locked: false, title: '已锁定', base: '' },
)
</script>

<template>
  <img
    v-if="locked"
    class="rlb"
    src="/lock/lock.png"
    alt=""
    :title="title"
    :style="base ? { '--rlb-base': base } : undefined"
  />
</template>

<style scoped>
.rlb {
  position: absolute;
  z-index: 2;
  pointer-events: none;
  right: calc(var(--rlb-base, var(--rune-cell, 70px)) * 0.07);
  bottom: calc(var(--rlb-base, var(--rune-cell, 70px)) * 0.24);
  height: calc(var(--rlb-base, var(--rune-cell, 70px)) * 0.2);
  width: auto;
}
</style>
