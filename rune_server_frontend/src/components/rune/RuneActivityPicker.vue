<!-- 活动符文选择器 -->
<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ACT_IMAGES } from '@/utils/RuneFormRules.ts'

const props = defineProps<{ modelValue: string }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: string): void }>()

const root = ref<HTMLElement | null>(null)
const open = ref(false)

function toggle() {
  open.value = !open.value
}

function pick(f: string) {
  emit('update:modelValue', f)
  open.value = false
}

function onDocPointerDown(e: PointerEvent) {
  if (!root.value) return
  if (!root.value.contains(e.target as Node)) open.value = false
}

function hideBroken(e: Event) {
  const el = e.target as HTMLImageElement | null
  if (el) el.style.visibility = 'hidden'
}

onMounted(() => document.addEventListener('pointerdown', onDocPointerDown))
onBeforeUnmount(() => document.removeEventListener('pointerdown', onDocPointerDown))
</script>

<template>
  <div ref="root" class="rap">
    <span class="thumb"><img :key="props.modelValue" :src="`/runes/special/${props.modelValue}`" alt="" @error="hideBroken" /></span>
    <button type="button" class="btn" @click="toggle">选择图片</button>
    <div v-if="open" class="pop">
      <button
        v-for="f in ACT_IMAGES"
        :key="f"
        type="button"
        class="opt"
        :class="{ on: modelValue === f }"
        @click="pick(f)"
      >
        <img :key="f" :src="`/runes/special/${f}`" alt="" @error="hideBroken" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.rap {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.thumb {
  width: 38px;
  height: 38px;
  flex: 0 0 auto;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.thumb img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.btn {
  height: 32px;
  padding: 0 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #fff;
  color: #606266;
  font-size: 13px;
  cursor: pointer;
}
.btn:hover {
  color: #409eff;
  border-color: #c6e2ff;
  background: #ecf5ff;
}
.pop {
  position: absolute;
  left: 0;
  top: 44px;
  z-index: 40;
  width: 268px;
  padding: 8px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, .12);
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 6px;
}
.opt {
  width: 100%;
  aspect-ratio: 1;
  padding: 2px;
  border: 1px solid transparent;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.opt img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.opt:hover {
  border-color: #a0cfff;
  background: #ecf5ff;
}
.opt.on {
  border-color: #409eff;
}
</style>
