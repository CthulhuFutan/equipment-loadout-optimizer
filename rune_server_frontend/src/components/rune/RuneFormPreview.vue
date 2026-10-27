<!-- 新建/编辑符文当前效果信息预览 -->
<script setup lang="ts">
import { computed } from 'vue'
import RuneVisual from '@/components/rune/RuneVisual.vue'
import type { Rune } from '@/components/runeStats'
import {
  fmtValue, imagePath, isActivity, previewImgScale, previewImgTop, qualityTagType,
  setEffect, statLabel, type RuneFormState,
} from '@/utils/RuneFormRules.ts'

const props = defineProps<{ state: RuneFormState }>()

const displayName = computed(() => {
  const s = props.state
  if (isActivity(s.setType)) return s.runeName.trim() || `活动${s.slotNumber}号符文`
  return `${s.setType}${s.slotNumber}号符文`
})

const runeModel = computed<Rune>(() => {
  const s = props.state
  const subs = [] as { statType: string; statValue: number; sortOrder: number }[]
  if (s.extra) subs.push({ statType: s.extra.statType, statValue: s.extra.value, sortOrder: 0 })
  s.subs.forEach((x, i) => subs.push({ statType: x.statType, statValue: x.value, sortOrder: i + 1 }))
  return {
    runeId: 0,
    runeName: displayName.value,
    setType: s.setType,
    slotNumber: s.slotNumber,
    quality: s.quality,
    starRating: s.starRating,
    levels: s.levels,
    imageUrl: imagePath(s.slotNumber, s.setType, s.starRating, s.actImage),
    mainStatType: s.mainType,
    mainStatValue: s.mainValue,
    subStats: subs,
    grindSortOrder: s.grind >= 0 ? s.grind + 1 : null,
    locked: s.locked,
  }
})

const imgScale = computed(() => previewImgScale(props.state.setType, props.state.actImage))
const imgTop = computed(() => previewImgTop(props.state.setType, props.state.actImage))
const eff = computed(() => setEffect(props.state.setType))

const subEntries = computed(() =>
  props.state.subs.map((s, i) => ({ stat: s, grounded: i === props.state.grind })).filter((x) => String(x.stat.value) !== '')
)

function hideBroken(e: Event) {
  const el = e.target as HTMLImageElement | null
  if (el) el.style.visibility = 'hidden'
}
</script>

<template>
  <div class="pv">
    <div class="pv-top">
      <RuneVisual :rune="runeModel" :slot="state.slotNumber" :size="120" :img-scale="imgScale" :img-top="imgTop" :fallback="true" />
      <div class="pv-info">
        <div class="pv-name">
          {{ displayName }}
          <el-tag size="small" :type="qualityTagType(state.quality)" :class="['qtag', { 'qtag-purple': state.quality === '紫色' }]">
            {{ state.quality }}
          </el-tag>
        </div>
        <dl class="kv">
          <dt>槽位</dt><dd>{{ state.slotNumber }} 号槽</dd>
          <dt>套组</dt><dd>{{ state.setType }}</dd>
          <dt>星等</dt><dd>{{ state.starRating }} 星</dd>
          <dt>等级</dt><dd>{{ state.levels }} / 15</dd>
        </dl>
      </div>
    </div>

    <div class="detail">
      <div class="rl-grp">
        <b>主属性</b>
        <div class="rl-row">
          <span class="rl-k">
            <img src="/runes/info/prefix_option_0101.png" alt="" @error="hideBroken" />
            {{ statLabel(state.mainType) }}
          </span>
          <strong>{{ fmtValue(state.mainType, state.mainValue) }}</strong>
        </div>
      </div>

      <div v-if="state.extra" class="rl-grp">
        <b>额外属性</b>
        <div class="rl-row">
          <span class="rl-k">
            <img src="/runes/info/prefix_option_0102.png" alt="" @error="hideBroken" />
            {{ statLabel(state.extra.statType) }}
          </span>
          <strong>{{ fmtValue(state.extra.statType, state.extra.value) }}</strong>
        </div>
      </div>

      <div class="rl-grp">
        <b>副属性</b>
        <div v-if="!subEntries.length" class="rl-empty">
          <img src="/runes/info/prefix_option_0103.png" alt="" @error="hideBroken" />——
        </div>
        <div v-for="(e, i) in subEntries" :key="i" class="rl-row">
          <span class="rl-k">
            <img :src="e.grounded ? '/runes/info/grind_rune_icon.png' : '/runes/info/prefix_option_0103.png'" alt="" @error="hideBroken" />
            {{ statLabel(e.stat.statType) }}
          </span>
          <strong>{{ fmtValue(e.stat.statType, e.stat.value) }}</strong>
        </div>
      </div>
    </div>

    <div class="setfx">
      <div class="t">套组效果</div>
      <div class="hd">
        <b>{{ isActivity(state.setType) ? '活动' : `${state.setType} ${eff.count}套组` }}</b>
      </div>
      <div class="txt">{{ isActivity(state.setType) ? '没有套组效果' : eff.desc }}</div>
    </div>
  </div>
</template>

<style scoped>
.pv {
  display: flex;
  flex-direction: column;
}
.pv-top {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}
.pv-info {
  flex: 1;
  min-width: 0;
  padding-top: 0;
}
.pv-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 600;
  line-height: 24px;
  color: #303133;
}
.qtag {
  flex: 0 0 auto;
  font-weight: 600;
}
.pv-name .qtag.qtag-purple {
  color: #7a3f96;
  border-color: #d8bfe5;
  background-color: #f6eefa;
  --el-tag-text-color: #7a3f96;
  --el-tag-border-color: #d8bfe5;
  --el-tag-bg-color: #f6eefa;
}
.kv {
  display: grid;
  grid-template-columns: max-content 1fr;
  gap: 4px 20px;
  margin: 7px 0 12px;
  font-size: 12px;
  line-height: 19.2px;
}
.kv dt {
  color: #909399;
}
.kv dd {
  margin: 0;
  color: #606266;
}
.detail {
  margin-top: 32px;
  padding-top: 12px;
  border-top: 1px dashed #ebeef5;
}
.rl-grp {
  margin-bottom: 12px;
}
.rl-grp > b {
  display: block;
  margin-bottom: 4px;
  font-size: 13px;
  font-weight: 700;
  color: #303133;
}
.rl-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 1px;
  padding: 6px 12px;
  font-size: 14px;
  color: #606266;
}
.rl-k {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}
.rl-k img {
  width: 18px;
  height: 18px;
  object-fit: contain;
  flex: 0 0 auto;
}
.rl-row strong {
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
}
.rl-empty {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  color: #c0c4cc;
  font-size: 14px;
}
.rl-empty img {
  width: 18px;
  height: 18px;
  object-fit: contain;
}
.setfx {
  margin-top: 48px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
  text-align: center;
}
.setfx .t {
  text-align: left;
  font-size: 13px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 8px;
}
.setfx .hd {
  margin-bottom: 4px;
}
.setfx .hd b {
  font-size: 14px;
  font-weight: 700;
  color: #303133;
}
.setfx .txt {
  color: #606266;
  font-size: 13px;
  white-space: pre-line;
}
</style>
