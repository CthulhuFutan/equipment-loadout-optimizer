<script setup lang="ts">
import {ref, computed, onMounted} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ArrowLeft, Loading} from '@element-plus/icons-vue'
import {
  DRAGON_CATEGORIES, DRAGON_ELEMENTS,
  getCategoryImage, getElementImage,
  RUNE_QUALITY_COLORS
} from '@/utils/constants.ts'
import {getDragon, getDragonEquipment} from "@/api/dragon.ts";
import {ElMessage, ElMessageBox} from "element-plus";
import RuneDetailDrawer from '@/components/RuneDetailDrawer.vue'

const route = useRoute()
const router = useRouter()

const dragonId = Number(route.params.id)
const idValid = Number.isInteger(dragonId) && dragonId > 0
const dragon = ref<any>(null)

// 符文
const equipment = ref<any[]>([])
const selectedRune = ref<any>(null)
const drawerVisible = ref(false)

// 获取单条龙数据
async function searchDetail() {
  try {
    if (!idValid) {
      router.replace('/dragons')
      ElMessage.error('访问错误，请重试')
      return
    }
    const res = await getDragon(dragonId)
    if (!res.data) {
      ElMessage.warning('角色不存在或已被删除')
      router.replace('/dragons')
      return
    }
    dragon.value = res.data
  } catch {
    router.push('/dragons')
  }
}

// 技能列表
const skills = computed(() => {
  if (!dragon.value) return []
  const list: { icon: string | null; name: string; desc: string }[] = []

  if (dragon.value.leaderSkillName || dragon.value.leaderSkillDesc) {
    list.push({
      icon: dragon.value.leaderSkillIcon || null,
      name: dragon.value.leaderSkillName || '队长技能',
      desc: dragon.value.leaderSkillDesc || ''
    })
  }
  if (dragon.value.activeSkillName || dragon.value.activeSkillDesc) {
    list.push({
      icon: dragon.value.activeSkillIcon || null,
      name: dragon.value.activeSkillName || '主动技能',
      desc: dragon.value.activeSkillDesc || ''
    })
  }
  if (dragon.value.passiveSkill1Name || dragon.value.passiveSkill1Desc) {
    list.push({
      icon: dragon.value.passiveSkill1Icon || null,
      name: dragon.value.passiveSkill1Name || '被动技能①',
      desc: dragon.value.passiveSkill1Desc || ''
    })
  }
  if (dragon.value.passiveSkill2Name || dragon.value.passiveSkill2Desc) {
    list.push({
      icon: dragon.value.passiveSkill2Icon || null,
      name: dragon.value.passiveSkill2Name || '被动技能②',
      desc: dragon.value.passiveSkill2Desc || ''
    })
  }
  if (dragon.value.passiveSkill3Name || dragon.value.passiveSkill3Desc) {
    list.push({
      icon: dragon.value.passiveSkill3Icon || null,
      name: dragon.value.passiveSkill3Name || '被动技能③',
      desc: dragon.value.passiveSkill3Desc || ''
    })
  }
  return list
})

// 符文装备
// slot -> 符文 映射（slotNumber 1~6）
const slotRunes = computed(() => {
  const map: Record<number, any> = {}
  for (const r of equipment.value) {
    if (r.slotNumber != null) map[r.slotNumber] = r
  }
  return map
})

function slotBackground(rune: any): string {
  if (!rune) return '#2a1b10'
  return RUNE_QUALITY_COLORS[rune.quality] ?? 'linear-gradient(180deg, #969696 0%, #303030 100%)'
}

function slotImage(rune: any, slot: number): string {
  return rune?.imageUrl || `/runes/slot/rune_slot_010${slot}.png`
}

// drawer符文详情
function openDrawer(rune: any) {
  selectedRune.value = rune
  drawerVisible.value = true
}

async function goOptimize() {
  await ElMessageBox.confirm(
    '将跳转到符文分配器进行符文调整，是否继续',
    '提示',
    { confirmButtonText: '是', cancelButtonText: '否', type: 'warning' }
  )
  router.push({ path: '/optimizer', query: { dragonId } })
}

function starSrc(starRating: number): string {
  return `/runes/star/rune_star_0${starRating}.png`
}

async function fetchEquipment() {
  if (!idValid) return
  try {
    const res = await getDragonEquipment(dragonId)
    if (res.data) equipment.value = res.data
  } catch {
    // 装备加载失败不阻塞龙详情展示
  }
}

onMounted(() => {
  searchDetail()
  fetchEquipment()
})
</script>

<template>
  <div v-if="dragon" class="detail-page">

    <!-- ===== 顶部信息区 ===== -->
    <div class="dragon-header">
      <img
        v-if="dragon.dragonImage"
        :src="dragon.dragonImage"
        class="dragon-portrait"
      />
      <div v-else class="dragon-portrait placeholder">暂无</div>

      <div class="dragon-info">
        <h1 class="dragon-name">{{ dragon.dragonName }}</h1>
        <div class="dragon-tags">
          <span class="tag category-tag">
            <img :src="getCategoryImage(dragon.category)" class="tag-icon" />
            {{ DRAGON_CATEGORIES.find(c => c.value === dragon.category)?.label }}
          </span>
          <span class="tag element-tag">
            <img :src="getElementImage(dragon.element)" class="tag-icon" />
            {{ DRAGON_ELEMENTS.find(e => e.value === dragon.element)?.label }}
          </span>
        </div>
      </div>

      <!-- 符文装备栏 -->
      <div class="rune-equipment">
        <div class="slot-grid">
          <div v-for="slot in 6" :key="slot"
               class="rune-slot"
               :class="{ 'is-empty': !slotRunes[slot], 'is-slot-1': slot === 1 }"
               :style="{ background: slotBackground(slotRunes[slot]) }"
               :title="slotRunes[slot] ? `${slotRunes[slot].runeName || slotRunes[slot].setType}${slot}号符文` : `${slot}号槽位（空）`"
               @click="slotRunes[slot] && openDrawer(slotRunes[slot])">
            <span class="corner tl"></span>
            <span class="corner tr"></span>
            <span class="corner bl"></span>
            <span class="corner br"></span>
            <img :src="slotImage(slotRunes[slot], slot)" class="slot-img" />
            <img v-if="slotRunes[slot]" :src="starSrc(slotRunes[slot].starRating)" class="slot-star"
                 :class="{ 'is-7star': slotRunes[slot].starRating === 7 }" />
          </div>
        </div>
      </div>

      <el-button class="back-btn" type="primary" @click="router.push('/dragons')">
        <el-icon><ArrowLeft/></el-icon>返回
      </el-button>
    </div>

    <!-- ===== 技能描述区 ===== -->
    <h2 class="section-title">技能描述</h2>

    <el-table
      :data="skills"
      stripe
      border
      style="width:100%"
      empty-text="暂无技能数据"
    >
      <el-table-column label="图标" width="120" align="center">
        <template #default="{ row }">
          <img v-if="row.icon" :src="row.icon" class="skill-icon" />
          <span v-else style="color:#c0c4cc">暂无</span>
        </template>
      </el-table-column>

      <el-table-column label="名称" width="120">
        <template #default="{ row }">
          <span class="skill-name">{{ row.name }}</span>
        </template>
      </el-table-column>

      <el-table-column label="描述" min-width="300">
        <template #default="{ row }">
          <span class="skill-desc">{{ row.desc }}</span>
        </template>
      </el-table-column>
    </el-table>

    <!-- ===== 宝物 ===== -->
    <template v-if="dragon.treasureName || dragon.treasureDesc">
      <h2 class="section-title" style="margin-top:20px">宝物</h2>
      <el-table
        :data="[{ icon: dragon.treasureIcon, name: dragon.treasureName, desc: dragon.treasureDesc }]"
        stripe
        border
        style="width:100%"
        empty-text=""
      >
        <el-table-column label="图标" width="120" align="center">
          <template #default="{ row }">
            <img v-if="row.icon" :src="row.icon" class="skill-icon" />
            <span v-else style="color:#c0c4cc">暂无</span>
          </template>
        </el-table-column>
        <el-table-column label="名称" width="120">
          <template #default="{ row }">
            <span class="skill-name">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column label="描述" min-width="300">
          <template #default="{ row }">
            <span class="skill-desc">{{ row.desc }}</span>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </div>

  <!-- 加载中 -->
  <div v-else class="loading-state">
    <el-icon class="is-loading" :size="32"><Loading/></el-icon>
    <p>加载中...</p>
  </div>

  <!-- 符文详情 -->
  <RuneDetailDrawer v-model="drawerVisible" :rune="selectedRune">
    <template #footer>
      <div style="margin-top:20px;text-align:center">
        <el-button type="primary" @click="goOptimize">去搭配</el-button>
      </div>
    </template>
  </RuneDetailDrawer>
</template>

<style scoped>
.detail-page {
  max-width: 75%;
  margin: 0 auto;
}

/* ===== 顶部信息 ===== */
.dragon-header {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 32px;
  padding-bottom: 20px;
  border-bottom: 2px solid #ebeef5;
}

.dragon-portrait {
  width: 100px;
  height: 100px;
  border-radius: 8px;
  object-fit: cover;
  border: 2px solid #e4e7ed;
  flex-shrink: 0;
}

.dragon-portrait.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
  font-size: 14px;
}

.dragon-name {
  margin: 0 0 12px;
  font-size: 28px;
  color: #303133;
}

.dragon-tags {
  display: flex;
  gap: 8px;
}

.tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 14px;
  color: #fff;
}

.category-tag {
  background: #409eff;
}

.element-tag {
  background: #e6a23c;
}

.tag-icon {
  width: 20px;
  height: 20px;
}

/* ===== 返回按钮 ===== */
.back-btn {
  align-self: flex-start;
  margin-left: auto;
  height: 32px;
  font-size: 14px;
}

/* ===== 技能标题 ===== */
.section-title {
  margin: 0 0 16px;
  font-size: 20px;
  color: #303133;
  padding-left: 8px;
  border-left: 4px solid #409eff;
}

/* ===== 技能表格 ===== */
.skill-icon {
  width: 80px;
  height: 80px;
  object-fit: contain;
}

.skill-name {
  color: #303133;
}

.skill-desc {
  color: #606266;
  line-height: 1.6;
}

/* ===== 加载状态 ===== */
.loading-state {
  text-align: center;
  padding: 80px 0;
  color: #909399;
}

:deep(.el-table__cell) {
  padding-top: 15px;
  padding-bottom: 15px;
}

/* ===== 黑色加粗表头 ===== */
:deep(.el-table__header th) {
  background-color: #fff;
  color: #303133;
  text-align: center;
}

/* ===== 符文装备栏 ===== */
.rune-equipment {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex-shrink: 0;
  margin-left: 50px;

  --slot: 65px;
  --gap-x: 3px;
  --gap-y: 3px;
  --corner-size: 7px;
}

.slot-grid {
  display: grid;
  grid-template-columns: repeat(3, var(--slot));
  grid-template-rows: repeat(2, var(--slot));
  column-gap: var(--gap-x);
  row-gap: var(--gap-y);
}
.rune-slot {
  position: relative;
  width: var(--slot);
  height: var(--slot);
  border-radius: 0;
  border: 2px solid #C0C4CC;
  cursor: pointer;
  box-sizing: border-box;
  transition: transform .1s;
}
.rune-slot:hover { transform: scale(1.06); }
.rune-slot.is-empty { cursor: default; }
.rune-slot.is-empty:hover { transform: none; }

.rune-slot:not(.is-empty) .slot-img {
  position: absolute;
  left: 50%;
  top: 43%;
  transform: translate(-50%, -50%);
  width: calc(var(--slot) * 0.925);
  height: calc(var(--slot) * 0.925);
  object-fit: contain;
  pointer-events: none;
}
/* 1号符文：原图在画布内偏高约7%，需要向下移动对齐 */
.rune-slot.is-slot-1:not(.is-empty) .slot-img {
  top: 50%;
}
.rune-slot.is-empty .slot-img {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: calc(var(--slot));
  height: calc(var(--slot));
  object-fit: contain;
  pointer-events: none;
}
.rune-slot:not(.is-empty) .slot-star {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: calc(var(--slot) * 0.0143);
  height: calc(var(--slot) * 0.2857 * 0.925);
  max-width: calc(var(--slot) * 0.9571 * 0.925);
  object-fit: contain;
  pointer-events: none;
}
.rune-slot:not(.is-empty) .slot-star.is-7star {
  bottom: calc(var(--slot) * 0.0286);
}

.rune-slot .corner {
  position: absolute;
  width: var(--corner-size);
  height: var(--corner-size);
  background: #c9a05a;
  display: none;
}
.rune-slot.is-empty .corner { display: block; }
.corner.tl { top: 0; left: 0; clip-path: polygon(0 0, 100% 0, 0 100%); }
.corner.tr { top: 0; right: 0; clip-path: polygon(0 0, 100% 0, 100% 100%); }
.corner.bl { bottom: 0; left: 0; clip-path: polygon(0 0, 0 100%, 100% 100%); }
.corner.br { bottom: 0; right: 0; clip-path: polygon(100% 0, 100% 100%, 0 100%); }

</style>
