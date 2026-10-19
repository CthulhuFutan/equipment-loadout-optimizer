<!--
  符文状态切换按钮 —— 菱形 + 两态锁图
  * 已锁定 → /lock/lock.png           整把锁
  * 未锁定 → /lock/lock_body.png      锁体
             /lock/lock_shackle.png   锁环
-->
<script setup lang="ts">
withDefaults(
  defineProps<{
    locked?: boolean
    /* 禁用（空态幽灵占位用） */
    disabled?: boolean
    /* 提示 */
    title?: string
  }>(),
  { locked: false, disabled: false, title: '' },
)

const emit = defineEmits<{ (e: 'toggle'): void }>()
</script>

<template>
  <button
    class="rlk"
    type="button"
    :disabled="disabled"
    :title="title || (locked ? '已锁定 —— 点击解锁' : '未锁定 —— 点击上锁')"
    @click.stop="emit('toggle')"
  >
    <span class="rlk-art">
      <img v-if="locked" class="rlk-locked" src="/lock/lock.png" alt="" />
      <template v-else>
        <img class="rlk-body" src="/lock/lock_body.png" alt="" />
        <img class="rlk-shackle" src="/lock/lock_shackle.png" alt="" />
      </template>
    </span>
  </button>
</template>

<style scoped>
.rlk {
  --u: 0.6485px;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  padding: 0;
  cursor: pointer;
  box-sizing: border-box;
  background: #2a1b10;
  border: 2px solid #b08d57;
  border-radius: 2px;
  transform: rotate(45deg);
}
.rlk:hover:not(:disabled) {
  border-color: #d8b87a;
}
.rlk:disabled {
  cursor: default;
}

.rlk-art {
  position: relative;
  display: block;
  flex-shrink: 0;
  width: calc(var(--u) * 27);
  height: calc(var(--u) * 34);
  transform: rotate(-45deg);
}
.rlk-art > img {
  position: absolute;
  display: block;
}
.rlk-locked {
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
}

.rlk-body {
  z-index: 2;
  left: 0;
  bottom: 0;
  width: 100%;
  height: calc(var(--u) * 22);
}
.rlk-shackle {
  z-index: 1;
  width: calc(var(--u) * 21);
  height: auto;
  left: calc(var(--u) * (3 + 11));
  bottom: calc(var(--u) * (20 + 0));
}
</style>
