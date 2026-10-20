<!--
  右列：属性变化 → 属性总和（基础数值 + 套组效果）。
    · 属性变化面板 flex:0 1 350px —— 变化条数 0~12 行固定占据一定区域，避免下方属性总和块位置反复变动
-->
<script setup lang="ts">
import { computed } from 'vue'
import type { DeltaRow, SetDeltaRow, SetLine, StatLine } from '../runeStats.ts'
import { fmtNum } from '../runeStats.ts'

const props = withDefaults(
  defineProps<{
    // 逐条属性的变化
    deltaRows?: DeltaRow[]
    // 套组件数的变化
    deltaSets?: SetDeltaRow[]
    // “谁 → 哪号槽” 的提示文字 */
    hint?: string
    // 有值时属性变化块显示为占位 / 无候选 / 已是当前装备
    placeholderTitle?: string | null
    placeholderText?: string | null
    // 属性总和
    stats?: StatLine[]
    // 套组效果
    sets?: SetLine[]
  }>(),
  {
    deltaRows: () => [],
    deltaSets: () => [],
    hint: '',
    placeholderTitle: null,
    placeholderText: null,
    stats: () => [],
    sets: () => [],
  },
)

const hasDelta = computed(
  () => props.deltaRows.length > 0 || props.deltaSets.length > 0,
)
const isPlaceholder = computed(() => props.placeholderTitle != null)
</script>

<template>
  <div class="col-sum">
    <!-- ===== 属性变化 ===== -->
    <div class="panel delta-panel">
      <div class="panel-head">
        <h3>属性变化</h3>
        <span class="hint">{{ hint }}</span>
      </div>
      <div class="panel-body delta-body">
        <div v-if="isPlaceholder" class="delta ph">
          <div class="dt">{{ placeholderTitle }}</div>
          <div class="dr"><span>{{ placeholderText }}</span></div>
        </div>
        <div v-else-if="hasDelta" class="delta">
          <div class="dt">装上去之后</div>
          <div v-for="r in deltaRows" :key="r.label" class="dr">
            <span>{{ r.label }}</span>
            <span :class="r.diff > 0 ? 'up' : 'down'">
              {{ fmtNum(r.from) }} → {{ fmtNum(r.to) }}（{{ r.diff > 0 ? '+' : '' }}{{ fmtNum(r.diff) }}）
            </span>
          </div>
          <div v-for="s in deltaSets" :key="`set-${s.setType}`" class="dr">
            <span>{{ s.setType }}</span>
            <span :class="s.to > s.from ? 'up' : 'down'">
              {{ s.from }} → {{ s.to }} 件
              <template v-if="s.mark === 1">激活↑</template>
              <template v-else-if="s.mark === -1">失效↓</template>
            </span>
          </div>
        </div>
        <div v-else class="delta ph">
          <div class="dt">装上去之后</div>
          <div class="dr"><span>换一件符文才能看到变化</span></div>
        </div>
      </div>
    </div>

    <!-- ===== 属性总和（基础数值 + 套组效果） ===== -->
    <div class="panel sum-panel">
      <div class="panel-head">
        <h3>属性总和 <span class="h3-sub">（基础数值 + 套组效果）</span></h3>
      </div>
      <div class="sum-scroll">
        <div class="sum-block">
          <div class="sum-stats">
            <div v-if="!stats.length" class="sum-empty">该槽位尚未装备符文</div>
            <div v-for="s in stats" :key="s.label" class="sum-item">
              <span>{{ s.label }}</span>
              <span class="v">
                +{{ fmtNum(s.total) }}{{ s.isPercent ? '%' : '' }}
                <em v-if="s.setBonus">（{{ fmtNum(s.base) }}{{ s.isPercent ? '%' : '' }} + 套装效果 {{ fmtNum(s.setBonus) }}{{ s.isPercent ? '%' : '' }}）</em>
              </span>
            </div>
          </div>
        </div>
        <div class="sum-block set-block">
          <div class="sb-title">套组效果</div>
          <div class="chips">
            <span v-if="!sets.length" class="sum-empty">—</span>
            <span
              v-for="s in sets"
              :key="s.setType"
              class="chip"
              :class="{ on: s.active, stack: s.stacks > 1 }"
              :title="`${s.setType}：${s.count} 件（${s.threshold} 件生效）\n${s.desc ?? ''}`"
            >
              {{ s.setType }} {{ s.count }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.col-sum {
  flex: 0 0 324px;
  width: 324px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.panel {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fff;
  display: flex;
  flex-direction: column;
}
/* 属性变化 */
.delta-panel {
  flex: 0 1 350px;
  min-height: 0;
}
.sum-panel {
  flex: 0 0 auto;
  min-height: 420px;
  margin-top: auto;
}
.panel-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding: 10px 14px;
  border-bottom: 1px solid #ebeef5;
}
.panel-head h3 {
  font-size: 14px;
  font-weight: 700;
  margin: 0;
}
.panel-head .h3-sub {
  font-size: 12px;
  font-weight: 400;
  color: #909399;
}
.panel-head .hint {
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.panel-body {
  padding: 12px 14px;
}
.delta-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

/* ---- 属性变化块 ---- */
.delta {
  border: 1px solid #f5dab1;
  background: #fdf6ec;
  border-radius: 4px;
  padding: 7px 10px;
}
.delta .dt {
  font-size: 11.5px;
  font-weight: 700;
  color: #b88230;
  margin-bottom: 3px;
}
.delta .dr {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 12px;
  color: #606266;
  padding: 1px 0;
}
.delta .up {
  color: #67c23a;
  font-weight: 700;
}
.delta .down {
  color: #f56c6c;
  font-weight: 700;
}
.delta.ph {
  border-style: dashed;
  border-color: #dcdfe6;
  background: #fafbfc;
  min-height: 84px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 3px;
}
.delta.ph .dt,
.delta.ph .dr {
  color: #c0c4cc;
}

/* ---- 属性总和 ---- */
.sum-scroll {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.sum-block {
  padding: 8px 14px;
}
.set-block {
  margin-top: auto;
  min-height: 95px;
  border-top: 1px solid #ebeef5;
}
.sum-block .sb-title {
  font-size: 12.5px;
  font-weight: 700;
  color: #606266;
  margin-bottom: 6px;
}
.sum-stats {
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.sum-item {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  font-size: 12.5px;
  line-height: 1.5;
  color: #606266;
}
.sum-item .v {
  color: #303133;
  font-weight: 700;
}
.sum-item .v em {
  font-style: normal;
  color: #909399;
  font-weight: 400;
  font-size: 11.5px;
}
.sum-empty {
  color: #c0c4cc;
  font-size: 12.5px;
}
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 6px;
}
.chip {
  display: inline-flex;
  align-items: center;
  font-size: 11.5px;
  line-height: 19px;
  padding: 0 8px;
  border-radius: 3px;
  border: 1px solid #dcdfe6;
  color: #909399;
  background: #fff;
  cursor: help;
}
.chip.on {
  border-color: #e6a23c;
  color: #b88230;
  background: #fdf6ec;
}
.chip.stack {
  border-color: #e6a23c;
  background: #fbe9d3;
  color: #a05d00;
  font-weight: 700;
}
</style>
