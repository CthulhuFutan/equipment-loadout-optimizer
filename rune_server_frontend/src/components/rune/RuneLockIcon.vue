<!--
  符文锁 - 状态图标
  任何显示 锁定符文 的位置都挂这一个组件
-->
<script setup lang="ts">
import { ref } from 'vue'

const props = withDefaults(
  defineProps<{
    /* 是否已锁定；false 时不渲染任何东西 */
    locked?: boolean
    /* 悬停提示 */
    title?: string
    base?: string
    /* 仅 RuneForm 预览启用：图片失败时隐藏 */
    hideOnError?: boolean
  }>(),
  { locked: false, title: '已锁定', base: '', hideOnError: false },
)

const failed = ref(false)
function onError() { if (props.hideOnError) failed.value = true }
</script>

<template>
  <img
    v-if="locked && !failed"
    class="rlb"
    src="/lock/lock.png"
    alt=""
    :title="title"
    :style="base ? { '--rlb-base': base } : undefined"
    @error="onError"
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
