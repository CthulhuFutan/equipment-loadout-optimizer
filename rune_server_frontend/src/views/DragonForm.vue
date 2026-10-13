<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { DRAGON_CATEGORIES, DRAGON_ELEMENTS, LEADER_SKILLS, RUNE_SET_TIERS, RUNE_SET_REQ_ORDER, parseRuneSetReqs } from '@/utils/constants.ts'
import { createDragon, getDragon, updateDragon } from "@/api/dragon.ts";
import { imageUpload } from "@/api/upload.ts";

const route = useRoute()
const router = useRouter()

// 有 id：编辑；没有 id：新建
const dragonId = route.params.id ? Number(route.params.id) : null
const isEdit = !!dragonId

const DEFAULT_LEADER_INDEX = LEADER_SKILLS.findIndex(s => s.icon === '/leader_skill/skill_empty.png')
interface DragonFormData {
  dragonName: string
  dragonImage: string
  element: string
  category: string
  leaderSkillIndex: number
  leaderSkillName: string
  leaderSkillIcon: string
  leaderSkillDesc: string | null
  activeSkillName: string | null
  activeSkillIcon: string | null
  activeSkillDesc: string | null
  manaCost: number
  passiveSkill1Name: string | null
  passiveSkill1Icon: string | null
  passiveSkill1Desc: string | null
  passiveSkill2Name: string | null
  passiveSkill2Icon: string | null
  passiveSkill2Desc: string | null
  passiveSkill3Name: string | null
  passiveSkill3Icon: string | null
  passiveSkill3Desc: string | null
  treasureName: string | null
  treasureIcon: string | null
  treasureDesc: string | null
  runeSetReqs: string | null
}


const form = ref<DragonFormData>({
  dragonName: '',
  dragonImage: '',
  element: '',
  category: '',
  leaderSkillIndex: DEFAULT_LEADER_INDEX,
  leaderSkillName: '无队长技能',
  leaderSkillIcon: '/leader_skill/skill_empty.png',
  leaderSkillDesc: null,
  activeSkillName: null,
  activeSkillIcon: null,
  activeSkillDesc: null,
  manaCost: 1,
  passiveSkill1Name: null,
  passiveSkill1Icon: null,
  passiveSkill1Desc: null,
  passiveSkill2Name: null,
  passiveSkill2Icon: null,
  passiveSkill2Desc: null,
  passiveSkill3Name: null,
  passiveSkill3Icon: null,
  passiveSkill3Desc: null,
  treasureName: null,
  treasureIcon: null,
  treasureDesc: null,
  runeSetReqs: null
})

// 每套当前勾选的件数；0 = 不勾选
const setReqs = reactive<Record<string, number>>(
  Object.fromEntries(RUNE_SET_REQ_ORDER.map(t => [t, 0])) as Record<string, number>
)
// 已选总件数，条件 ≤ 6
const setReqsTotal = computed(() =>
  RUNE_SET_REQ_ORDER.reduce((sum, t) => sum + (setReqs[t] || 0), 0)
)
const MAX_PIECE = 6

function canSelectCount(setType: string, count: number): boolean {
  if (count === 0) return true
  return setReqsTotal.value - (setReqs[setType] || 0) + count <= MAX_PIECE
}

// 某套全部可选档位（含「无」0）；兜底为空档位
function tierOptions(setType: string): number[] {
  return [0, ...(RUNE_SET_TIERS[setType] ?? [])]
}

// 把勾选结果序列化为后端 rune_set_reqs 存储的 JSON 映射字符串；
// 例： {"生存":6,"体力":2}
function buildRuneSetReqs(): string | null {
  const obj: Record<string, number> = {}
  for (const t of RUNE_SET_REQ_ORDER) {
    const c = setReqs[t] || 0
    if (c > 0) obj[t] = c
  }
  return Object.keys(obj).length ? JSON.stringify(obj) : null
}

const fileInputs = reactive<Record<string, HTMLInputElement | null>>({
  dragon: null,
  active: null,
  passive1: null,
  passive2: null,
  passive3: null,
  treasure: null,
})

/**
 * 用于临时存储当前用户的图片选择状态信息
 * 如果用户在当前页面选择了插入图片，则会临时保存图片路径
 * 如果没有插入，则会判断用户是否之前在 form / 数据库 中存储了图片
 * 如果存储了，则会直接调用并显示该图片
 * 否则，执行 v-else 函数内容，显示 “点击上传”
 * 该操作用于防止直接存储图片但用户没保存导致的原图片丢失
 */
const previews = reactive<Record<string, string>>({
  dragon: '',
  active: '',
  passive1: '',
  passive2: '',
  passive3: '',
  treasure: '',
})

// 临时存储用户选择的图片
const pendingFiles: Record<string, File | null> = {}

const pathMap: Record<string, string> = {
  dragon: 'dragonImage',
  active: 'activeSkillIcon',
  passive1: 'passiveSkill1Icon',
  passive2: 'passiveSkill2Icon',
  passive3: 'passiveSkill3Icon',
  treasure: 'treasureIcon',
}

function triggerUpload(key: string) {
  fileInputs[key]?.click()
}

async function onFileChange(e: Event, key: string) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  previews[key] = URL.createObjectURL(file)
  pendingFiles[key] = file
}

function onLeaderSelect(index: number) {
  const skill = LEADER_SKILLS[index]
  if (skill) {
    form.value.leaderSkillName = skill.name
    form.value.leaderSkillIcon = skill.icon
    form.value.leaderSkillDesc = skill.desc
  } else {
    form.value.leaderSkillName = ''
    form.value.leaderSkillIcon = ''
    form.value.leaderSkillDesc = ''
  }
}

const skillRows = computed(() => [
  {
    label: '主动技能', previewKey: 'active',
    nameKey: 'activeSkillName', descKey: 'activeSkillDesc',
    icon: form.value.activeSkillIcon,
    name: form.value.activeSkillName,
    desc: form.value.activeSkillDesc,
    isActive: true
  },
  {
    label: '被动技能①', previewKey: 'passive1',
    nameKey: 'passiveSkill1Name', descKey: 'passiveSkill1Desc',
    icon: form.value.passiveSkill1Icon,
    name: form.value.passiveSkill1Name,
    desc: form.value.passiveSkill1Desc,
    isActive: false
  },
  {
    label: '被动技能②', previewKey: 'passive2',
    nameKey: 'passiveSkill2Name', descKey: 'passiveSkill2Desc',
    icon: form.value.passiveSkill2Icon,
    name: form.value.passiveSkill2Name,
    desc: form.value.passiveSkill2Desc,
    isActive: false
  },
  {
    label: '被动技能③', previewKey: 'passive3',
    nameKey: 'passiveSkill3Name', descKey: 'passiveSkill3Desc',
    icon: form.value.passiveSkill3Icon,
    name: form.value.passiveSkill3Name,
    desc: form.value.passiveSkill3Desc,
    isActive: false
  }
])

function updateForm(key: string, value: string) {
  ;(form.value as any)[key] = value
}

// submit添加loading动画
const submitting = ref(false)
async function submit() {
  if (submitting.value) return
  // 先校验再上传：防止校验失败时已上传的图片成为孤儿文件
  if (!form.value.dragonName) {
    ElMessage.warning('请输入龙名称')
    return
  }
  submitting.value = true
  try {
    form.value.runeSetReqs = buildRuneSetReqs()
    for (const [key, file] of Object.entries(pendingFiles)) {
      if (file) {
        const type = key === 'dragon' ? 'dragon' : key === 'treasure' ? 'treasure' : 'skill'
        const res = await imageUpload(file, type)
        ;(form.value as any)[pathMap[key]!] = res.data
      }
    }
    const res = isEdit ?  await updateDragon(dragonId!, form.value) : await createDragon(form.value)
    ElMessage.success(isEdit ? '修改成功' : '新增成功')
    router.push('/dragons')
  } catch {

  } finally {
    submitting.value = false
  }
}

async function fetchDetail() {
  if (!dragonId) return
  try {
    const res = await getDragon(dragonId)
    const d = res.data
    if (!d) {
      ElMessage.warning('角色不存在或已被删除')
      router.replace('/dragons')
      return
    }
    const keys = [
      'dragonName', 'dragonImage', 'element', 'category',
      'leaderSkillName', 'leaderSkillIcon', 'leaderSkillDesc',
      'activeSkillName', 'activeSkillIcon', 'activeSkillDesc', 'manaCost',
      'passiveSkill1Name', 'passiveSkill1Icon', 'passiveSkill1Desc',
      'passiveSkill2Name', 'passiveSkill2Icon', 'passiveSkill2Desc',
      'passiveSkill3Name', 'passiveSkill3Icon', 'passiveSkill3Desc',
      'treasureName', 'treasureIcon', 'treasureDesc',
    ]
    for (const k of keys) {
      ;(form.value as any)[k] = d[k]
    }
    for (const t of RUNE_SET_REQ_ORDER) setReqs[t] = 0
    for (const r of parseRuneSetReqs(d.runeSetReqs)) {
      if (RUNE_SET_TIERS[r.setType]?.includes(r.count)) setReqs[r.setType] = r.count
    }
    const idx = LEADER_SKILLS.findIndex(s => s.icon === d.leaderSkillIcon)
    form.value.leaderSkillIndex = idx >= 0 ? idx : DEFAULT_LEADER_INDEX
  } catch {

  }
}
onMounted(fetchDetail)
</script>

<template>
  <div class="form-page">

    <!-- ===== 顶部 ===== -->
    <div class="dragon-header">
      <div class="portrait-upload" @click="triggerUpload('dragon')">
        <img v-if="previews.dragon || form.dragonImage" :src="previews.dragon || form.dragonImage" class="dragon-portrait" />
        <div v-else class="dragon-portrait placeholder">点击上传</div>
        <input :ref="el => fileInputs.dragon = el as HTMLInputElement" type="file" accept="image/*" hidden @change="onFileChange($event, 'dragon')" />
      </div>

      <div class="dragon-info">
        <el-input v-model="form.dragonName" placeholder="输入龙名称" class="name-input" />
        <div class="selects-row">
          <el-select v-model="form.element" placeholder="属性" style="width:160px" class="size-match">
            <template #prefix>
              <img v-if="form.element" :src="DRAGON_ELEMENTS.find(e => e.value === form.element)?.image" style="width:20px;height:20px;vertical-align:middle" />
            </template>
            <el-option v-for="e in DRAGON_ELEMENTS" :key="e.value" :label="e.label" :value="e.value">
              <img :src="e.image" style="width:22px;height:22px;vertical-align:middle;margin-right:6px" />
              <span>{{ e.label }}</span>
            </el-option>
          </el-select>
          <el-select v-model="form.category" placeholder="类别" style="width:160px" class="size-match">
            <template #prefix>
              <img v-if="form.category" :src="DRAGON_CATEGORIES.find(c => c.value === form.category)?.image" style="width:20px;height:20px;vertical-align:middle" />
            </template>
            <el-option v-for="c in DRAGON_CATEGORIES" :key="c.value" :label="c.label" :value="c.value">
              <img :src="c.image" style="width:22px;height:22px;vertical-align:middle;margin-right:6px" />
              <span>{{ c.label }}</span>
            </el-option>
          </el-select>
        </div>
      </div>

      <el-button class="back-btn" type="primary" @click="router.push('/dragons')">
        <el-icon><ArrowLeft/></el-icon>返回
      </el-button>
    </div>

    <!-- ===== 队长技能 ===== -->
    <h2 class="section-title">队长技能</h2>
    <div class="leader-row">
      <el-select v-model="form.leaderSkillIndex" placeholder="选择队长技能" class="leader-select" @change="onLeaderSelect">
        <template #prefix>
          <img v-if="form.leaderSkillIcon" :src="form.leaderSkillIcon" style="width:26px;height:26px;vertical-align:middle" />
        </template>
        <el-option v-for="(s, index) in LEADER_SKILLS" :key="index" :label="s.name" :value="index">
          <img :src="s.icon" style="width:28px;height:28px;vertical-align:middle;margin-right:8px" />
          <span style="font-size:15px">{{ s.name }}</span>
        </el-option>
      </el-select>
      <span v-if="form.leaderSkillDesc" class="leader-desc-inline">{{ form.leaderSkillDesc }}</span>
    </div>

    <!-- ===== 技能配置表格 ===== -->
    <h2 class="section-title">技能配置</h2>
    <el-table :data="skillRows" border class="skill-table" style="width:100%">
      <el-table-column label="图标" width="140" align="center">
        <template #default="{ row }">
          <div class="table-icon-upload" @click="triggerUpload(row.previewKey)">
            <img v-if="previews[row.previewKey] || row.icon" :src="previews[row.previewKey] || row.icon" class="table-icon-img" />
            <div v-else class="table-icon-img placeholder">点击上传</div>
            <input :ref="el => fileInputs[row.previewKey] = el as HTMLInputElement" type="file" accept="image/*" hidden @change="onFileChange($event, row.previewKey)" />
          </div>
        </template>
      </el-table-column>

      <el-table-column label="名称" width="220">
        <template #default="{ row }">
          <div class="cell-input-wrap">
            <el-input
              :model-value="row.name"
              @update:model-value="updateForm(row.nameKey, $event)"
              :placeholder="row.label"
              class="borderless-input"
            />
          </div>
          <div v-if="row.isActive" class="mana-row">
            耗蓝<el-input-number v-model="form.manaCost" :min="1" :max="3" size="small" class="mana-num" />
          </div>
        </template>
      </el-table-column>

      <el-table-column label="描述">
        <template #default="{ row }">
          <div class="cell-input-wrap">
            <el-input
              :model-value="row.desc"
              @update:model-value="updateForm(row.descKey, $event)"
              type="textarea"
              :rows="5"
              :placeholder="`${row.label}描述`"
              class="desc-textarea"
            />
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- ===== 宝物 ===== -->
    <h2 class="section-title">宝物</h2>
    <el-table :data="[{ label: '宝物' }]" border class="skill-table" style="width:100%">
      <el-table-column label="图标" width="140" align="center">
        <template #default>
          <div class="table-icon-upload" @click="triggerUpload('treasure')">
            <img v-if="previews.treasure || form.treasureIcon" :src="previews.treasure || form.treasureIcon" class="table-icon-img" />
            <div v-else class="table-icon-img placeholder">点击上传</div>
            <input :ref="el => fileInputs.treasure = el as HTMLInputElement" type="file" accept="image/*" hidden @change="onFileChange($event, 'treasure')" />
          </div>
        </template>
      </el-table-column>
      <el-table-column label="名称" width="220">
        <template #default>
          <div class="cell-input-wrap">
            <el-input v-model="form.treasureName" placeholder="宝物名称" class="borderless-input" />
          </div>
        </template>
      </el-table-column>
      <el-table-column label="描述" class-name="desc-cell">
        <template #default>
          <div class="cell-input-wrap">
            <el-input v-model="form.treasureDesc" type="textarea" :rows="5" placeholder="宝物描述" class="desc-textarea" />
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- ===== 搭配选择 ===== -->
    <h2 class="section-title">搭配选择</h2>
    <p class="setreq-hint">请设定你对符文组合的需求</p>
    <div class="setreq-grid">
      <div class="setreq-item" v-for="setType in RUNE_SET_REQ_ORDER" :key="setType">
        <div class="setreq-name">{{ setType }}</div>
        <el-radio-group :model-value="setReqs[setType]" @update:model-value="(v: any) => setReqs[setType] = Number(v)">
          <el-radio-button
            v-for="n in tierOptions(setType)"
            :key="n"
            :value="n"
            :disabled="!canSelectCount(setType, n)"
          >{{ n === 0 ? '无' : `×${n}` }}</el-radio-button>
        </el-radio-group>
      </div>
    </div>
    <div class="setreq-total" :class="{ 'is-full': setReqsTotal === MAX_PIECE }">已选 {{ setReqsTotal }} / {{ MAX_PIECE }} 件</div>

    <!-- ===== 提交 ===== -->
    <div style="text-align:center;margin-top:24px">
      <el-button type="primary" size="large" :loading="submitting" @click="submit">{{ isEdit ? '保存修改' : '添加龙' }}</el-button>
      <el-button size="large" @click="router.push('/dragons')">取消</el-button>
    </div>
  </div>
</template>

<style scoped>
.form-page {
  max-width: 70%;
  margin: 0 auto;
}

/* ===== 顶部 ===== */
.dragon-header {
  display: flex;
  align-items: flex-start;
  gap: 24px;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid #ebeef5;
}

.portrait-upload {
  cursor: pointer;
  flex-shrink: 0;
}

.dragon-portrait {
  width: 100px;
  height: 100px;
  border-radius: 8px;
  object-fit: cover;
  border: 2px solid #e4e7ed;
}

.dragon-portrait.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
  font-size: 13px;
}

.dragon-info {
  flex: 1;
}

.name-input {
  padding-top: 5px;
  width: 335px;
  margin-bottom: 15px;
}

/* 名称与选择框统一高度 */
.name-input :deep(.el-input__wrapper),
.size-match :deep(.el-input__wrapper) {
  height: 40px;
}

.selects-row {
  display: flex;
  gap: 16px;
}

.back-btn {
  align-self: flex-start;
  margin-left: auto;
}

/* ===== 宝物style ===== */
.section-title {
  margin: 24px 0 12px;
  font-size: 18px;
  color: #303133;
  padding-left: 8px;
  border-left: 4px solid #409eff;
}

/* ===== 队长技能 ===== */
.leader-row {
  width: 350px;
  margin-bottom: 4px;
}

.leader-select {
  width: 100%;
  max-width: 350px;
}

.leader-select :deep(.el-input__wrapper) {
  height: 37px;
}

.leader-desc-inline {
  color: #606266;
  font-size: 14px;
  line-height: 1.5;
  margin-left: 12px;
  display: block;
  margin-top: 8px;
}

/* ===== 技能表格 ===== */
.skill-table :deep(.el-table__header th) {
  text-align: center;
  color: #000;
  font-weight: 700;
  background-color: #fff;
}

.table-icon-upload {
  cursor: pointer;
  display: inline-block;
}

.table-icon-img {
  width: 100px;
  height: 100px;
  border-radius: 4px;
  object-fit: cover;
  border: 1px dashed #dcdfe6;
}

.table-icon-img.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
  font-size: 12px;
}

/* ===== 表格输入框 ===== */
.skill-table :deep(.el-table__body td) {
  padding: 5px;
}

.cell-input-wrap {
  width: 100%;
}

.borderless-input :deep(.el-input__wrapper) {
  padding-left: 8px;
  padding-right: 8px;
}

/* 描述：固定高度，禁止拉伸，1px上下间距 */
:deep(.desc-cell) {
  padding: 1px 5px !important;
}

.desc-textarea :deep(.el-textarea__inner) {
  resize: none;
  height: 80px;
  padding-left: 10px;
  padding-right: 10px;
}

/* 主动技能：耗蓝在名称下方 */
.mana-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-top: 15px;
  padding-left: 4px;
  font-size: 13px;
  color: #606266;
}

.mana-num {
  width: 100px;
}

.mana-num :deep(.el-input__wrapper) {
  padding-left: 6px;
  padding-right: 6px;
}

/* ===== 搭配选择 ===== */
.setreq-hint {
  color: #909399;
  font-size: 13px;
  line-height: 1.6;
  margin: 2px 0 8px;
}

/* 两套一行 */
.setreq-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 24px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px 16px;
}

.setreq-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.setreq-name {
  width: 40px;
  flex-shrink: 0;
  font-weight: 600;
  color: #303133;
}

@media (max-width: 760px) {
  .setreq-grid {
    grid-template-columns: 1fr;
  }
}

.setreq-total {
  margin-top: 8px;
  font-size: 13px;
  color: #909399;
}
.setreq-total.is-full {
  color: #e6a23c;
  font-weight: 600;
}
</style>
