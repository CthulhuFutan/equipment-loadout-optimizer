<!--
  选择角色面板 —— 快速切到要调整装备的龙。
  筛选与搜索的口径对齐项目 DragonList / RuneList：
    1. 属性 / 类别 多选（带图标）与「已装备」状态筛选 叠加生效，重置一键清空；
    2. 名称搜索大小写不敏感
    3. 只进行筛选显示便于装备符文，不对龙的信息进行修改
-->
<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { DRAGON_CATEGORIES, DRAGON_ELEMENTS } from '@/utils/constants'
import type { Dragon } from './runeStats'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    dragons: Dragon[]
    // 龙装备的符文数
    equippedCount: (dragonId: number) => number
    // 当前正在调整的龙
    currentDragonId?: number | null
  }>(),
  { currentDragonId: null },
)

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'select', dragonId: number): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (v: boolean) => emit('update:modelValue', v),
})

const filterElements = ref<string[]>([])
const filterCategories = ref<string[]>([])
const onlyEquipped = ref(false)
const keyword = ref('')

// 面板打开即重置
watch(visible, (v) => {
  if (v) {
    filterElements.value = []
    filterCategories.value = []
    onlyEquipped.value = false
    keyword.value = ''
  }
})

function toggle(list: string[], value: string): string[] {
  return list.includes(value) ? list.filter((v) => v !== value) : [...list, value]
}

const equippedTotal = computed(
  () => props.dragons.filter((d) => props.equippedCount(d.dragonId) > 0).length,
)

// 命中强度：3 整串前缀 / 2 某段前缀 / 1 任意包含 / -1 不匹配
function nameScore(name: string, q: string): number {
  const n = (name || '').toLowerCase()
  const s = q.trim().toLowerCase()
  if (!s) return 0
  if (n.startsWith(s)) return 3
  const segs = n.split(/[-·/\s]+/).filter(Boolean)
  if (segs.some((seg) => seg.startsWith(s))) return 2
  return n.includes(s) ? 1 : -1
}

const list = computed(() => {
  const q = keyword.value
  const matched = props.dragons.filter((d) => {
    if (filterElements.value.length && !filterElements.value.includes(d.element)) return false
    if (filterCategories.value.length && !filterCategories.value.includes(d.category)) return false
    if (onlyEquipped.value && props.equippedCount(d.dragonId) === 0) return false
    return nameScore(d.dragonName, q) >= 0
  })
  if (!q.trim()) return matched
  return matched
    .map((d, i) => ({ d, score: nameScore(d.dragonName, q), i }))
    .sort((a, b) => b.score - a.score || a.i - b.i)
    .map((x) => x.d)
})

function reset() {
  filterElements.value = []
  filterCategories.value = []
  onlyEquipped.value = false
  keyword.value = ''
}

function pick(d: Dragon) {
  emit('select', d.dragonId)
  visible.value = false
}

function elementImg(element: string) {
  return DRAGON_ELEMENTS.find((e) => e.value === element)?.image
}
</script>

<template>
  <el-dialog v-model="visible" width="1200px" top="5vh" append-to-body class="dragon-picker">
    <template #header>
      <span style="font-size: 15px; font-weight: 700">选择角色</span>
    </template>

    <div class="pf-filters">
      <!-- 第一行：属性 / 类别 / 状态 -->
      <div class="pf-row">
        <span class="pf-label">属性</span>
        <span
          v-for="e in DRAGON_ELEMENTS"
          :key="e.value"
          class="pf-chip"
          :class="{ on: filterElements.includes(e.value) }"
          @click="filterElements = toggle(filterElements, e.value)"
        >
          <img :src="e.image" alt="" />{{ e.label }}
        </span>

        <span class="pf-label" style="width: auto; margin-left: 8px">类别</span>
        <span
          v-for="c in DRAGON_CATEGORIES"
          :key="c.value"
          class="pf-chip"
          :class="{ on: filterCategories.includes(c.value) }"
          @click="filterCategories = toggle(filterCategories, c.value)"
        >
          <img :src="c.image" alt="" />{{ c.label }}
        </span>

        <span class="pf-label" style="width: auto; margin-left: 8px">状态</span>
        <span
          class="pf-chip"
          :class="{ on: onlyEquipped, off: equippedTotal === 0 }"
          @click="equippedTotal && (onlyEquipped = !onlyEquipped)"
        >
          已装备
        </span>
      </div>

      <!-- 第二行：搜索 / 重置 -->
      <div class="pf-row">
        <input v-model="keyword" class="pf-search" placeholder="请输入名称" />
        <span class="pf-btn" @click="keyword = keyword.trim()">搜索</span>
        <span class="pf-btn ghost" @click="reset">重置</span>
      </div>
    </div>

    <div class="picker-grid">
      <div
        v-for="d in list"
        :key="d.dragonId"
        class="pk-cell"
        :class="{ active: d.dragonId === currentDragonId }"
        :title="d.dragonName"
        @click="pick(d)"
      >
        <div class="pk-pic">
          <img v-if="d.dragonImage" class="pk-img" :src="d.dragonImage" alt="" />
          <div v-else class="pk-ph">?</div>
          <img v-if="elementImg(d.element)" class="pk-el" :src="elementImg(d.element)" alt="" />
          <span class="pk-eq" :class="{ none: !equippedCount(d.dragonId) }">
            {{ equippedCount(d.dragonId) ? `已装备 ${equippedCount(d.dragonId)}` : '空' }}
          </span>
        </div>
        <div class="pk-name">{{ d.dragonName }}</div>
      </div>
      <div v-if="!list.length" class="picker-empty">没有符合条件的龙</div>
    </div>
  </el-dialog>
</template>

<style scoped>
.pf-filters {
  display: flex;
  flex-direction: column;
  gap: 13px;
  padding: 10px 16px;
  border-bottom: 1px solid #ebeef5;
  background: #fff;
  margin: -10px -20px 0;
}
.pf-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.pf-label {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
  width: 34px;
}
.pf-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12.5px;
  line-height: 22px;
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #606266;
  border-radius: 3px;
  padding: 2px 11px;
  cursor: pointer;
  transition: 0.1s;
  user-select: none;
}
.pf-chip:hover {
  border-color: #a0cfff;
  color: #409eff;
}
.pf-chip.on {
  border-color: #409eff;
  background: #ecf5ff;
  color: #409eff;
  font-weight: 700;
}
.pf-chip.off {
  border-color: #ebeef5;
  color: #c0c4cc;
  cursor: not-allowed;
  background: #fafafa;
}
.pf-chip img {
  width: 16px;
  height: 16px;
  vertical-align: middle;
}
.pf-search {
  flex: 1;
  min-width: 180px;
  font-family: inherit;
  font-size: 13px;
  line-height: 20px;
  color: #303133;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 5px 10px;
  outline: none;
}
.pf-search:focus {
  border-color: #409eff;
}
.pf-btn {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  font-size: 12.5px;
  line-height: 22px;
  font-weight: 600;
  padding: 4px 14px;
  border: 1px solid #409eff;
  border-radius: 4px;
  background: #409eff;
  color: #fff;
  cursor: pointer;
  transition: 0.1s;
  user-select: none;
}
.pf-btn:hover {
  background: #66b1ff;
  border-color: #66b1ff;
}
.pf-btn.ghost {
  background: #fff;
  color: #606266;
  border-color: #dcdfe6;
  font-weight: 400;
}
.pf-btn.ghost:hover {
  border-color: #a0cfff;
  color: #409eff;
  background: #f7fbff;
}

.picker-grid {
  height: 62vh;
  overflow-y: auto;
  padding: 12px 16px;
  display: grid;
  gap: 7px 6px;
  grid-template-columns: repeat(auto-fill, minmax(84px, 1fr));
  align-content: start;
  margin: 0 -20px -10px;
}
.pk-cell {
  display: flex;
  flex-direction: column;
  gap: 1px;
  cursor: pointer;
}
.pk-pic {
  position: relative;
  aspect-ratio: 1 / 1;
  border: 2px solid #dcdfe6;
  overflow: hidden;
  background: #fff;
}
.pk-cell:hover .pk-pic {
  border-color: #a0cfff;
}
.pk-cell.active .pk-pic {
  border-color: #409eff;
  box-shadow: 0 0 0 2px #d9ecff inset;
}
.pk-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
/* ---- 无立绘时的占位 ---- */
.pk-ph {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
  font-size: 20px;
}
.pk-el {
  position: absolute;
  left: 2px;
  top: 2px;
  width: 17px;
  height: 17px;
}
.pk-eq {
  position: absolute;
  right: 2px;
  top: 2px;
  font-size: 10px;
  line-height: 14px;
  padding: 0 4px;
  border-radius: 3px;
  background: rgba(0, 0, 0, 0.62);
  color: #fff;
}
.pk-eq.none {
  background: rgba(0, 0, 0, 0.32);
  color: #dcdfe6;
}
.pk-name {
  font-size: 10.5px;
  line-height: 14px;
  color: #606266;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.picker-empty {
  grid-column: 1 / -1;
  text-align: center;
  color: #c0c4cc;
  padding: 40px 0;
  font-size: 13px;
}
</style>
