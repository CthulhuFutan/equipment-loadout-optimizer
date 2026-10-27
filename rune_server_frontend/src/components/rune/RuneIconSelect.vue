<script setup lang="ts">
import { computed } from 'vue'

export interface RuneIconOption {
  label: string
  value: string | number
  icon?: string
}

const props = withDefaults(defineProps<{
  modelValue: string | number
  options: RuneIconOption[]
  placeholder?: string
  disabled?: boolean
  disabledText?: string
  clearable?: boolean
  width?: string | number
  height?: number
  selectedIconSize?: number
  optionIconSize?: number
  selectedGap?: number
  optionGap?: number
  fontSize?: number
  optionFontSize?: number
  /** 仅 1 号符文展示控件使用，其余为 0 */
  iconOffsetY?: number
}>(), {
  placeholder: '请选择',
  disabled: false,
  disabledText: '',
  clearable: false,
  width: '100%',
  height: 32,
  selectedIconSize: 16,
  optionIconSize: 16,
  selectedGap: 5,
  optionGap: 5,
  fontSize: 14,
  optionFontSize: 14,
  iconOffsetY: 0,
})

const emit = defineEmits<{ (e: 'update:modelValue', v: string | number): void }>()

const selected = computed(() =>
  props.options.find((o) => String(o.value) === String(props.modelValue)),
)

const rootStyle = computed(() => ({
  width: typeof props.width === 'number' ? `${props.width}px` : props.width,
  '--ris-height': `${props.height}px`,
  '--ris-selected-gap': `${props.selectedGap}px`,
  '--ris-font-size': `${props.fontSize}px`,
}))

function onUpdate(v: string | number) {
  emit('update:modelValue', v)
}

function hideBroken(e: Event) {
  const el = e.target as HTMLImageElement | null
  if (el) el.style.visibility = 'hidden'
}
</script>

<template>
  <div class="ris" :style="rootStyle">
    <el-select
      :model-value="props.modelValue"
      :placeholder="props.placeholder"
      :disabled="props.disabled"
      :clearable="props.clearable"
      style="width: 100%"
      @update:model-value="onUpdate"
    >
      <template #prefix>
        <img
          v-if="selected?.icon"
          :key="selected.icon"
          :src="selected.icon"
          alt=""
          :style="{
            width: selectedIconSize + 'px',
            height: selectedIconSize + 'px',
            transform: `translateY(${iconOffsetY}px)`,
          }"
          @error="hideBroken"
        />
      </template>
      <template #label="{ label }">
        <span>{{ disabled && disabledText ? disabledText : label }}</span>
      </template>
      <el-option
        v-for="opt in props.options"
        :key="String(opt.value)"
        :label="opt.label"
        :value="opt.value"
      >
        <span class="ris-opt" :style="{ gap: optionGap + 'px' }">
          <img
            v-if="opt.icon"
            :key="opt.icon"
            :src="opt.icon"
            alt=""
            :style="{ width: optionIconSize + 'px', height: optionIconSize + 'px' }"
            @error="hideBroken"
          />
          <span :style="{ fontSize: optionFontSize + 'px' }">{{ opt.label }}</span>
        </span>
      </el-option>
    </el-select>
  </div>
</template>

<style scoped>
.ris {
  min-width: 0;
}
.ris :deep(.el-select__wrapper) {
  min-height: var(--ris-height);
  height: var(--ris-height);
  gap: var(--ris-selected-gap) !important;
}
.ris :deep(.el-select__selection) {
  gap: 0 !important;
}
.ris :deep(.el-select__prefix) {
  margin-right: 0;
}
.ris :deep(.el-select__selected-item),
.ris :deep(.el-select__placeholder) {
  font-size: var(--ris-font-size);
}
.ris-opt {
  display: flex;
  align-items: center;
  width: 100%;
  white-space: nowrap;
}
.ris-opt img {
  flex: 0 0 auto;
  object-fit: contain;
  display: block;
}
</style>
