<!-- 数值控制组件自定义 -->
<script setup lang="ts">
import { clampNum } from '@/utils/RuneFormRules.ts'

const props = withDefaults(defineProps<{
  modelValue: number
  min?: number
  max: number
  percent?: boolean
  disabled?: boolean
  showUnit?: boolean
}>(), {
  min: 1,
  percent: false,
  disabled: false,
  showUnit: true,
})

const emit = defineEmits<{ (e: 'update:modelValue', v: number): void }>()

function step(delta: number) {
  if (props.disabled) return
  emit('update:modelValue', clampNum(props.modelValue + delta, props.min, props.max))
}

function onInput(e: Event) {
  const raw = (e.target as HTMLInputElement).value
  const n = Number(raw)
  emit('update:modelValue', Number.isFinite(n) ? n : 0)
}

function onBlur(e: Event) {
  const el = e.target as HTMLInputElement
  const v = clampNum(el.value, props.min, props.max)
  el.value = String(v)
  emit('update:modelValue', v)
}
</script>

<template>
  <div class="rvs" :class="{ 'is-disabled': disabled }">
    <button type="button" class="vbtn" :disabled="disabled || modelValue <= min" @click="step(-1)">−</button>
    <span class="val-in">
      <input
        type="number"
        :value="modelValue"
        :min="min"
        :max="max"
        step="1"
        :disabled="disabled"
        @input="onInput"
        @blur="onBlur"
      />
    </span>
    <button type="button" class="vbtn" :disabled="disabled || modelValue >= max" @click="step(1)">+</button>
    <span v-if="showUnit" class="val-unit" :style="{ visibility: percent ? 'visible' : 'hidden' }">%</span>
  </div>
</template>

<style scoped>
.rvs {
  display: flex;
  align-items: center;
  width: 100%;
  min-width: 0;
}
.val-in {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  align-items: center;
  height: 32px;
  padding: 0 4px;
  border-top: 1px solid #dcdfe6;
  border-bottom: 1px solid #dcdfe6;
  background: #fff;
  box-sizing: border-box;
}
.val-in:focus-within {
  border-color: #409eff;
}
.val-in input {
  flex: 1 1 auto;
  width: 100%;
  min-width: 0;
  height: 30px;
  padding: 0 4px;
  border: 0;
  outline: none;
  background: transparent;
  text-align: center;
  font-size: 13px;
  font-variant-numeric: tabular-nums;
  color: #303133;
}
.val-in input::-webkit-outer-spin-button,
.val-in input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
.val-in input[type='number'] {
  -moz-appearance: textfield;
  appearance: textfield;
}
.vbtn {
  width: 30px;
  height: 32px;
  flex: 0 0 auto;
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #606266;
  cursor: pointer;
  font-size: 15px;
  line-height: 1;
  box-sizing: border-box;
}
.vbtn:first-child {
  border-radius: 4px 0 0 4px;
}
.vbtn:last-of-type {
  border-radius: 0 4px 4px 0;
}
.vbtn:hover:not(:disabled) {
  color: #409eff;
  border-color: #c6e2ff;
  background: #ecf5ff;
}
.vbtn:disabled {
  color: #c0c4cc;
  background: #f5f7fa;
  cursor: not-allowed;
}
.val-unit {
  font-size: 12px;
  color: #909399;
  margin-left: 6px;
  flex: 0 0 auto;
}
.rvs.is-disabled .val-in {
  background: #f5f7fa;
}
.rvs.is-disabled .val-in input {
  color: #a8abb2;
}
</style>
