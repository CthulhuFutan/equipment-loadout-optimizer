<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  modelValue: boolean
  title?: string
  message?: string
  description?: string
  confirmText?: string
  cancelText?: string
  loading?: boolean
  width?: string | number
  dangerColor?: string
  iconColor?: string
}>(), {
  title: '确认删除吗',
  message: '删除后将无法恢复',
  description: '',
  confirmText: '删除',
  cancelText: '取消',
  loading: false,
  width: 420,
  dangerColor: '#e83735',
  iconColor: '#F2B247',
})

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'confirm'): void
  (e: 'cancel'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (v: boolean) => emit('update:modelValue', v),
})

/** 颜色明暗调节：pct 正数变亮、负数变暗 */
function shade(hex: string, pct: number): string {
  const n = parseInt(hex.replace('#', ''), 16)
  const t = (c: number) => Math.max(0, Math.min(255, Math.round(c * (1 + pct))))
  return `rgb(${t((n >> 16) & 255)}, ${t((n >> 8) & 255)}, ${t(n & 255)})`
}

/** 删除按钮内联样式 */
const confirmBtnStyle = computed<Record<string, string>>(() => ({
  backgroundColor: props.dangerColor,
  borderColor: props.dangerColor,
  color: '#ffffff',
  transition: 'none',
  '--el-button-bg-color': props.dangerColor,
  '--el-button-border-color': props.dangerColor,
  '--el-button-hover-bg-color': shade(props.dangerColor, 0.08),
  '--el-button-hover-border-color': shade(props.dangerColor, 0.08),
  '--el-button-active-bg-color': shade(props.dangerColor, -0.08),
  '--el-button-active-border-color': shade(props.dangerColor, -0.08),
  '--el-button-text-color': '#ffffff',
  '--el-button-hover-text-color': '#ffffff',
  '--el-button-active-text-color': '#ffffff',
}))

function handleCancel() {
  emit('update:modelValue', false)
  emit('cancel')
}

function handleConfirm() {
  emit('confirm')
}
</script>

<template>
  <el-dialog
      v-model="visible"
      class="delete-confirm-dialog"
      :width="width"
      append-to-body
      align-center
      :show-close="true"
      :close-on-click-modal="true"
      :close-on-press-escape="false"
  >
  <div class="dc-body">
    <!-- 棕黄圆形 + 白色感叹号 -->
    <div class="dc-icon">
      <svg width="56" height="56" viewBox="0 0 48 48">
        <circle cx="24" cy="24" r="22" :fill="iconColor" />
        <rect x="22.5" y="12" width="3" height="15" rx="1.5" fill="#ffffff" />
        <circle cx="24" cy="32.5" r="2.8" fill="#ffffff" />
      </svg>
    </div>

    <div class="dc-title">{{ title }}</div>
    <div class="dc-message">{{ message }}</div>
    <div v-if="description" class="dc-description">{{ description }}</div>

    <slot />

    <div class="dc-footer">
      <el-button class="dc-cancel" @click="handleCancel">{{ cancelText }}</el-button>
      <el-button class="dc-confirm" :style="confirmBtnStyle" :loading="loading" @click="handleConfirm">
        {{ confirmText }}
      </el-button>
    </div>
  </div>
  </el-dialog>
</template>

<style scoped>
/* 弹窗卡片：白底 + 灰边框 + 圆角 */
.delete-confirm-dialog :deep(.el-dialog) {
  border: 1px solid #8e8e8e;
  border-radius: 8px;
  padding: 0;
  box-shadow: none;
}

.delete-confirm-dialog :deep(.el-dialog__header) {
  padding: 0;
}
/* 关闭按钮位置微调 */
.delete-confirm-dialog :deep(.el-dialog__headerbtn) {
  top: 12px;
  right: 12px;
}
.delete-confirm-dialog :deep(.el-dialog__body) {
  padding: 30px 28px 22px;
}

.dc-body { text-align: center; }
.dc-icon { display: flex; justify-content: center; margin-bottom: 12px; }

.dc-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}
.dc-message {
  font-size: 14px;
  color: #909399;
  line-height: 1.6;
}
.dc-description {
  margin-top: 6px;
  font-size: 13px;
  color: #c0c4cc;
  line-height: 1.6;
}

.dc-footer {
  margin-top: 35px;
  display: flex;
  justify-content: center;
  gap: 12px;
}

.dc-cancel,
.dc-confirm {
  min-width: 110px;
  height: 35px;
  padding: 0 16px;
}
</style>