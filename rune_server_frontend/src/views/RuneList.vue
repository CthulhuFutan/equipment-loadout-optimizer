<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRunes, deleteRune } from '@/api/rune'
import { RUNE_SET_TYPES, STAT_TYPES, RUNE_QUALITY_COLORS, getSetEffect } from '@/utils/constants'

const router = useRouter()

const activeSlot = ref<number | null>(null)
const filterSetType = ref('')
const filterMainStatType = ref('')
const filterEquipped = ref(true)

const allRunes = ref<any[]>([])
const selectedRune = ref<any>(null)
const loading = ref(false)

/** 统计每个槽位的符文数量，对传过来的数据 filter 出对应信息后 .length 统计 */
const slotCounts = computed(() => {
  const counts: Record<number, number> = {}
  for (let i = 1; i <= 6; i++) {
    counts[i] = allRunes.value.filter(r => r.slotNumber === i).length
  }
  return counts
})

/** 加载符文*/
async function fetchAllRunes() {
  loading.value = true
  try {
    const res = await getRunes()
    allRunes.value = (res as any).data ?? []
  } catch {

  } finally {
    loading.value = false
  }
}
// 过滤，返回结果为 true 的内容
// 例： [1, 2, 3, 4].filter(n => n > 2) 返回[3, 4]
const filteredRunes = computed(() => {
  return allRunes.value.filter(r => {
    if (activeSlot.value !== null && r.slotNumber !== activeSlot.value) return false
    if (filterSetType.value && r.setType !== filterSetType.value) return false
    if (filterMainStatType.value && r.mainStatType !== filterMainStatType.value) return false
    // 「查看装备中的符文」勾选（默认）= 仓库同时显示已装备与未装备符文；
    // 取消勾选 = 只显示未装备符文（过滤掉已装备）
    if (!filterEquipped.value && r.equipped) return false
    return true
  })
})

/** 点击槽位选择器：切换筛选 */
function clickSlot(n: number) {
  activeSlot.value = activeSlot.value === n ? null : n
}

/** 点击符文图标：显示详情 */
function clickRune(rune: any) {
  selectedRune.value = rune
}


/** 额外属性：sortOrder === 0 */
const extraStat = computed(() =>
    selectedRune.value?.subStats?.find((s: any) => s.sortOrder === 0) ?? null
)

/** 普通副属性：sortOrder !== 0 */
const normalSubStats = computed(() =>
    (selectedRune.value?.subStats ?? []).filter((s: any) => s.sortOrder !== 0)
)

/** 去掉词条类型末尾的 % 或 + */
function statLabel(statType: string): string {
  return statType.replace(/[%+]$/, '')
}

/** 格式化数值：始终 + 开头，原词条以 % 结尾则追加 % */
function formatStatValue(statType: string, statValue: any): string {
  const suffix = statType.endsWith('%') ? '%' : ''
  return `+${statValue}${suffix}`
}

/** 是否为被研磨的词条 */
function isGroundStat(sub: any): boolean {
  return selectedRune.value?.grindSortOrder != null
      && sub.sortOrder === selectedRune.value.grindSortOrder
}

/** 星级图路径（star/ 原生星图） */
function starSrc(starRating: number): string {
  return `/runes/star/rune_star_0${starRating}.png`
}

/** 槽位背景：按品质返回底色渐变（与 RuneSlot / DragonDetail 同源） */
function slotBackground(rune: any): string {
  return RUNE_QUALITY_COLORS[rune.quality] ?? 'linear-gradient(180deg, #969696 0%, #303030 100%)'
}

/** 删除当前选中的符文 */
async function removeRune() {
  if (!selectedRune.value) return
  try {
    await ElMessageBox.confirm(
      `确定删除该符文吗？`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteRune(selectedRune.value.runeId)
    ElMessage.success('符文已删除')
    selectedRune.value = null
    await fetchAllRunes()
  } catch {
    // 取消或失败
  }
}

onMounted(() => {
  fetchAllRunes()
})
</script>

<template>
  <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
    <h2>符文仓库</h2>
  </div>
  <div style="display:flex;height:calc(100vh - 168px);gap:0">
    <div style="flex:1;display:flex;flex-direction:column;min-width:0;align-items:center">
      <!-- 符文格：品质渐变 + 银白细框 + 可调间隙 -->
      <div style="width:100%;max-width:692px">
        <!-- 槽位选择器：通过控制亮度来用于表示当前选择的槽位 -->
        <div style="display:flex;justify-content:center;gap:16px;padding:12px 8px;border-bottom:1px solid #dcdfe6">
          <div v-for="n in 6" :key="n"
               style="cursor:pointer;text-align:center;user-select:none"
               :style="{ opacity: activeSlot === null || activeSlot === n ? 1 : 0.35 }"
               @click="clickSlot(n)">
            <img :src="`/runes/slot/rune_slot_010${n}.png`" style="width:52px;height:52px;display:block"/>
            <div style="font-size:12px;color:#606266;margin-top:2px">
              {{ slotCounts[n] || 0 }}
            </div>
          </div>
        </div>

        <!-- 筛选行 + 符文图标 -->
        <div style="border:1px solid #dcdfe6;border-top:none;box-sizing:border-box">
          <div style="display:flex;align-items:center;gap:12px;padding:8px 12px;border-bottom:1px solid #dcdfe6;flex-shrink:0">
            <el-select v-model="filterSetType" placeholder="符文类型" clearable style="width:140px">
              <el-option v-for="t in RUNE_SET_TYPES" :key="t.value" :label="t.label" :value="t.value">
                <img :src="t.image" style="width:25px;height:25px;vertical-align:middle;margin-right:6px" />
                <span>{{ t.label }}</span>
              </el-option>
            </el-select>
            <el-select v-model="filterMainStatType" placeholder="主属性" clearable style="width:140px">
              <el-option v-for="s in STAT_TYPES" :key="s.value" :label="s.label" :value="s.value" />
            </el-select>
            <el-checkbox v-model="filterEquipped">查看装备中的符文</el-checkbox>
            <div style="flex:1" />
              <el-button type="primary" @click="router.push('/runes/new')">
                <el-icon><Plus/></el-icon>添加符文
              </el-button>
            </div>

            <!-- 符文图标（方形网格：品质背景 + 符文 + 星星，黑色分隔线） -->
            <div v-loading="loading" class="rune-grid">
            <div v-for="rune in filteredRunes" :key="rune.runeId"
                 class="rune-item"
                 :class="{ selected: selectedRune?.runeId === rune.runeId }"
                 :style="{ background: slotBackground(rune) }"
                 @click="clickRune(rune)">
              <img v-if="rune.imageUrl" :src="rune.imageUrl" class="rune-item-img"
                   :class="{ 'is-slot-1': rune.slotNumber === 1 }" />
              <div v-else class="rune-item-ph">?</div>
              <img :src="starSrc(rune.starRating)" class="rune-item-star"
                   :class="{ 'is-7star': rune.starRating === 7 }" />
            </div>
            <div v-if="filteredRunes.length === 0 && !loading" class="rune-empty">没有匹配的符文</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧固定详情面板  -->
    <div style="width:320px;border-left:1px solid #ebeef5;padding:16px;flex-shrink:0;overflow-y:auto">
      <!-- 未选中 -->
      <div v-if="!selectedRune" style="color:#c0c4cc;text-align:center;padding-top:100px;font-size:14px">
        点击符文查看详情
      </div>

      <template v-else>
        <!-- 符文外观：品质渐变背景框（框住符文图 + 星星，样式对齐符文格） -->
        <div style="text-align:center;margin-bottom:20px">
          <div class="detail-rune-box" :style="{ background: slotBackground(selectedRune) }">
            <img v-if="selectedRune.imageUrl" :src="selectedRune.imageUrl" class="detail-rune-img"
                 :class="{ 'is-slot-1': selectedRune.slotNumber === 1 }" />
            <img v-else :src="`/runes/slot/rune_slot_010${selectedRune.slotNumber}.png`" class="detail-rune-img"
                 :class="{ 'is-slot-1': selectedRune.slotNumber === 1 }" />
            <img :src="starSrc(selectedRune.starRating)" class="detail-rune-star"
                 :class="{ 'is-7star': selectedRune.starRating === 7 }" />
          </div>
          <div style="font-size:18px;font-weight:bold;margin-top:8px">
            {{ selectedRune.runeName || selectedRune.setType }}{{ selectedRune.slotNumber }}号符文
          </div>
          <div style="color:#909399;font-size:13px">
            <span v-if="selectedRune.levels != null">等级 +{{ selectedRune.levels }}</span>
          </div>
        </div>

        <!-- 词条信息 -->
        <div style="margin-bottom:20px">
          <!-- 主属性 -->
          <div style="margin-bottom:12px">
            <div style="font-weight:bold;margin-bottom:4px;font-size:13px;color:#303133">主属性</div>
            <div style="padding:6px 12px;display:flex;justify-content:space-between;align-items:center;font-size:14px">
              <span style="display:flex;align-items:center;gap:6px">
                <img src="/runes/info/prefix_option_0101.png" style="width:18px;height:18px" />
                {{ statLabel(selectedRune.mainStatType) }}
              </span>
              <strong>{{ formatStatValue(selectedRune.mainStatType, selectedRune.mainStatValue) }}</strong>
            </div>
          </div>

          <!-- 额外属性 -->
          <div v-if="extraStat" style="margin-bottom:12px">
            <div style="font-weight:bold;margin-bottom:4px;font-size:13px;color:#303133">额外属性</div>
            <div style="padding:6px 12px;display:flex;justify-content:space-between;align-items:center;font-size:14px">
              <span style="display:flex;align-items:center;gap:6px">
                <img src="/runes/info/prefix_option_0102.png" style="width:18px;height:18px" />
                {{ statLabel(extraStat.statType) }}
              </span>
              <strong>{{ formatStatValue(extraStat.statType, extraStat.statValue) }}</strong>
            </div>
          </div>

          <!-- 副属性 -->
          <div>
            <div style="font-weight:bold;margin-bottom:4px;font-size:13px;color:#303133">副属性</div>
            <div v-if="normalSubStats.length === 0"
                 style="color:#c0c4cc;font-size:13px;padding:4px 0">暂无副属性</div>
            <div v-for="sub in normalSubStats" :key="sub.sortOrder"
                 style="padding:6px 12px;margin-bottom:1px;display:flex;justify-content:space-between;align-items:center;font-size:14px"
                 :style="isGroundStat(sub) ? { background: '#fef0f0' } : {}">
              <span style="display:flex;align-items:center;gap:6px">
                <img v-if="isGroundStat(sub)" src="/runes/info/grind_rune_icon.png" style="width:18px;height:18px" />
                <img v-else src="/runes/info/prefix_option_0103.png" style="width:18px;height:18px" />
                {{ statLabel(sub.statType) }}
              </span>
              <strong>{{ formatStatValue(sub.statType, sub.statValue) }}</strong>
            </div>
          </div>
        </div>

        <!-- 套组效果 -->
        <div style="text-align:center;padding-top:16px;border-top:1px solid #ebeef5">
          <div style="font-weight:bold;margin-bottom:4px">
            {{ selectedRune.setType }}
            {{ getSetEffect(selectedRune.setType).count }}套组
          </div>
          <div style="color:#606266;font-size:13px;white-space:pre-line">
            {{ getSetEffect(selectedRune.setType).desc }}
          </div>
        </div>

        <!-- 删除按钮 -->
        <div style="margin-top:20px;text-align:center">
          <el-button type="danger" :icon="Delete" @click="removeRune">
            删除
          </el-button>
        </div>
      </template>
    </div>

  </div>
</template>

<style scoped>
/* 按游戏截图还原符文格：品质渐变（橙/紫/蓝，值在 constants.ts）+ 银白细框 + 可调间隙。
   四个变量可直接改：--rune-cell 符文格尺寸、--rune-gap 间隙、--rune-border-width 边框粗细、
   --rune-border 边框颜色 */
.rune-grid {
  --rune-cell: 70px;           /* 符文格最小宽度（改这里调符文大小，高度随宽度等比） */
  --rune-gap: calc(var(--rune-cell) * 0.0286); /* 符文间距：2px/70 ≈ 0.0286（随格缩放） */
  --rune-border-width: 2px;    /* 边框粗细：1px 太细、2px 清晰、3px 厚重 */
  --rune-border: #B08D57;      /* 边框颜色：白底推荐 #C0C4CC（浅）/ #909399（深）；
                                  游戏近白 #E8ECEF 只在深色背景上可见；棕色可用 #B08D57 */
  flex: 1;
  min-height: 78px;
  overflow-y: auto;
  overflow-x: hidden;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(var(--rune-cell), 1fr));
  gap: var(--rune-gap);
  align-items: start;
  box-sizing: border-box;
}
/* 方格：方形 + 银白细框；浏览器变窄时列数自动减少、格体不缩小。
   每行格数 ≈ ⌊(容器宽 + 间隙) ÷ (格宽 + 间隙)⌋ */
.rune-item {
  position: relative;
  aspect-ratio: 1 / 1;
  box-sizing: border-box;
  border: var(--rune-border-width) solid var(--rune-border);
  cursor: pointer;
}
.rune-item.selected {
  outline: 2px solid #409eff;
  outline-offset: -1px;
  z-index: 1;
}
/* 符文图：居中（top 为垂直位置，可调；1号槽下面单独补偿）。
   宽高 = 格宽（比例 1），改 --rune-cell 时图片随格缩放 */
.rune-item-img {
  --img-top: 43%;          /* 中心图垂直位置：改这里统一调所有槽位 */
  position: absolute;
  left: 50%;
  top: var(--img-top);
  transform: translate(-50%, -50%);
  width: calc(var(--rune-cell));
  height: calc(var(--rune-cell));
  object-fit: contain;
  pointer-events: none;
}
/* 1号三角形符文：原图图案在画布内偏高约7%框高（可见区垂直中心≈0.42~0.44，其余槽位0.50），
   → 单独下移对齐。若调整上面 --img-top，请同步把这里改成 (--img-top + 7%) */
.rune-item-img.is-slot-1 {
  --img-top: 50%;
}
/* 无图占位 */
.rune-item-ph {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 16px;
}
/* 星级：以格宽为基准的比例值。bottom 锚定格底 → 格体大小变化时星星位置稳定（不漂移）；
   height=7 星大小、max-width=1~6 星大小（星条宽高比 5.64，宽度先撞 max-width）。
   比例：bottom 1px→×0.0143、height 20px→×0.2857、max-width 67px→×0.9571（相对 70px 格） */
.rune-item-star {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: calc(var(--rune-cell) * 0.0143);
  height: calc(var(--rune-cell) * 0.2857);
  max-width: calc(var(--rune-cell) * 0.9571);
  object-fit: contain;
  pointer-events: none;
}
/* 7星星星：竖块(56×36)比6星长条(124×22)高 → 单独抬升，使7/6星视觉同一高度
   （bottom 2px → ×0.0286） */
.rune-item-star.is-7star {
  bottom: calc(var(--rune-cell) * 0.0286);
}
/* 右侧详情面板上方符文框：参照左侧 runeList 的比例。
   基准 = 自身框尺寸 --detail-cell（110px）。注意：左侧符文图在格内四周有间隙
   （格子被 grid 1fr 拉伸 > 图宽），故右侧图也保留间隙、不填满框。
   边框 --rune-border-width / --rune-border 与符文格共用（固定 2px） */
.detail-rune-box {
  --detail-cell: 110px;         /* 框边长（基准，改这里整体缩放右侧面板） */
  --rune-border-width: 2px;
  --rune-border: #C0C4CC;
  box-sizing: border-box;       /* 与左侧 .rune-item 一致：宽含边框 */
  position: relative;
  width: var(--detail-cell);
  height: var(--detail-cell);
  margin: 0 auto;
  border: var(--rune-border-width) solid var(--rune-border);
  border-radius: 0; /* 尖角方形 */
}
/* 符文图：img 元素尺寸可调。图案实际显示大小 = 框宽 × --img-scale × 图案在源图占比。
   你测量第7个符文图案占格 39/78 = 50%，若想让图案正好占框 50%（四边各 1/4），
   --img-scale 应≈1.0（因图案在 70px 源图内约占 50%，img 需接近框宽）。
   觉得偏大就调小 --img-scale（如 0.7 → 图案约 35%），偏小就调大。
   垂直位置 --img-top 与左侧一致（43%，图案略偏上：上方留白<下方）。 */
.detail-rune-img {
  --img-top: 43%;            /* 中心图垂直位置：与左侧 .rune-item-img 相同 */
  --img-scale: 0.9;          /* 图占框比例（可调：0.5 小 / 0.8 中 / 1.0 大） */
  position: absolute;
  left: 50%;
  top: var(--img-top);
  transform: translate(-50%, -50%);
  width: calc(var(--detail-cell) * var(--img-scale));
  height: calc(var(--detail-cell) * var(--img-scale));
  object-fit: contain;
}
/* 1号三角形符文：与左侧 .rune-item-img.is-slot-1 一致（下移 7% 对齐） */
.detail-rune-img.is-slot-1 {
  --img-top: 50%;
}
/* 星星：与图片一样用可调系数 --star-scale（默认 1.0 = 与左侧等比例）。
   基准比例（相对 110px 框）：height 20px/70.875≈0.282、max-width 67px/70.875≈0.9454、
   bottom 1px/70.875≈0.0141。调大 --star-scale 星星整体放大，调小缩小。
   height=7星、max-width=1~6星（星条宽高比 5.64，宽度先撞 max-width）。 */
.detail-rune-star {
  --star-scale: 0.855;       /* 星星大小系数（0.95 × 0.9 ≈ 当前大小的 0.9 倍） */
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: calc(var(--detail-cell) * 0.03);   /* 1.55px + 上移 1px ≈ 2.55px */
  height: calc(var(--detail-cell) * 0.282 * var(--star-scale));
  max-width: calc(var(--detail-cell) * 0.9454 * var(--star-scale));
  object-fit: contain;
}
/* 右侧 7 星：与中间一致，单独抬升（2px/70.875 ≈ 0.0282） */
.detail-rune-star.is-7star {
  bottom: calc(var(--detail-cell) * 0.0282);
}

/* 空结果提示：横跨整行 */
.rune-empty {
  grid-column: 1 / -1;
  min-height: 79px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
}
</style>
