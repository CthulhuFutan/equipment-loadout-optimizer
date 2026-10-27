<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Delete, Edit } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRunes, deleteRune, setRuneLock } from '@/api/rune'
import { RUNE_SET_TYPES, STAT_TYPES, getSetEffect } from '@/utils/constants'
import RuneLockChange from '@/components/rune/RuneLockChange.vue'
import RuneVisual from '@/components/rune/RuneVisual.vue'
import RuneIconSelect from '@/components/rune/RuneIconSelect.vue'

const router = useRouter()

const activeSlot = ref<number | null>(null)
const filterSetType = ref('')
const filterMainStatType = ref('')
const filterEquipped = ref(true)

const setFilterOptions = RUNE_SET_TYPES.map((t) => ({
  label: t.label,
  value: t.value,
  icon: t.image,
}))

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

// 符文锁定按钮：上锁 ↔ 解锁
async function onToggleLock() {
  const r = selectedRune.value
  if (!r) return
  const status = !r.locked
  try {
    await setRuneLock([r.runeId], status)
    await fetchAllRunes()
    // 刷新后重新指向同一件符文，避免面板回到空态
    selectedRune.value = allRunes.value.find((x: any) => x.runeId === r.runeId) ?? null
    ElMessage.success(status ? '已锁定' : '已解锁')
  } catch {

  }
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
            <RuneIconSelect
              v-model="filterSetType"
              :options="setFilterOptions"
              placeholder="符文类型"
              clearable
              :width="140"
              :height="32"
              :selected-icon-size="25"
              :option-icon-size="25"
              :selected-gap="5"
              :option-gap="5"
              :icon-offset-y="2"
            />
            <el-select v-model="filterMainStatType" placeholder="主属性" clearable style="width:140px">
              <el-option v-for="s in STAT_TYPES" :key="s.value" :label="s.label" :value="s.value" />
            </el-select>
            <el-checkbox v-model="filterEquipped">查看装备中的符文</el-checkbox>
            <div style="flex:1" />
              <el-button type="primary" @click="router.push('/runes/new')">
                <el-icon><Plus/></el-icon>添加符文
              </el-button>
            </div>

            <!-- 符文格网格，每格由 RuneVisual fluid 渲染：品质底 + 图 + 星条 + 等级 + 锁 -->
            <div v-loading="loading" class="rune-grid">
            <div v-for="rune in filteredRunes" :key="rune.runeId"
                 class="rune-item"
                 :class="{ selected: selectedRune?.runeId === rune.runeId }"
                 @click="clickRune(rune)">
              <RuneVisual fluid :rune="rune" :slot="rune.slotNumber" />
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
          <RuneVisual :rune="selectedRune" :slot="selectedRune.slotNumber" :size="110" />
          <div style="font-size:18px;font-weight:bold;margin-top:8px">
            {{ selectedRune.runeName || selectedRune.setType }}{{ selectedRune.slotNumber }}号符文
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

        <!-- 符文锁定按钮：符文详情 -->
        <div class="rlk-row">
          <RuneLockChange :locked="!!selectedRune.locked" @toggle="onToggleLock" />
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

        <!-- 编辑 / 删除 -->
        <div style="margin-top:20px;display:flex;justify-content:center;gap:8px">
          <el-button :icon="Edit" @click="router.push({ name: 'RuneEdit', params: { id: selectedRune.runeId } })">
            编辑
          </el-button>
          <el-button type="danger" :icon="Delete" @click="removeRune">
            删除
          </el-button>
        </div>
      </template>
    </div>

  </div>
</template>

<style scoped>
.rlk-row {
  display: flex;
  justify-content: flex-end;
  padding: 11px 0 31px;
}

.rune-grid {
  --rune-cell: 70px;              /* 符文格最小宽度 */
  --rune-gap: calc(var(--rune-cell) * 0.0286); /* 符文间距 */

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

.rune-item {
  position: relative;
  box-sizing: border-box;
  cursor: pointer;
}
.rune-item.selected {
  z-index: 1;
}
/* 选中描边处于上层，避免被覆盖 */
.rune-item.selected :deep(.rv-frame) {
  outline: 2px solid #409eff;
  outline-offset: -1px;
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
