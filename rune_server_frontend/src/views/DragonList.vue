<script setup lang="ts">
import {Search, Plus} from "@element-plus/icons-vue";
import {DRAGON_CATEGORIES, DRAGON_ELEMENTS, getCategoryImage, getElementImage} from "@/utils/constants.ts";
import {onMounted, ref, watch} from "vue";
import {useRouter} from 'vue-router'
import {deleteDragon, getDragons, getDragonEquipment} from "@/api/dragon.ts";
import {ElMessage} from "element-plus";
import DeleteConfirmDialog from "@/components/DeleteConfirmDialog.vue";

const router = useRouter()
// 计时器
let timer: ReturnType<typeof setTimeout> | null = null
// 转圈动画
const loading = ref(false)

const dragons = ref([])
const searchName = ref('')
const filterElement = ref('')
const filterCategory = ref('')
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)

const categoryLabel = (category: string) => DRAGON_CATEGORIES.find(c => c.value === category)?.label || category
const elementLabel = (element: string) => DRAGON_ELEMENTS.find(e => e.value === element)?.label || element

/**
 * 通过watch函数实现对搜索内容的监听
 * 只有 element 和 category 发生变化时才触发 loading 加载动画
 */
watch([searchName, filterElement, filterCategory], ([newName, newElem, newCat], [oldName, oldElem, oldCat]) => {
  if (newElem !== oldElem || newCat !== oldCat) {
    loading.value = true
  }
  currentPage.value = 1
  if (timer) clearTimeout(timer)
  timer = setTimeout(searchDragons, 200)
})

async function searchDragons() {
  try{
    const res = await getDragons({
      dragonName: searchName.value || undefined,
      element: filterElement.value || undefined,
      category: filterCategory.value || undefined,
      pageNum: currentPage.value,
      pageSize: pageSize
    })
    if (!res.data) return
    const pageData = res.data
    dragons.value = pageData.records
    total.value = pageData.total
  } catch {

  } finally {
    loading.value = false
  }
}
function goDetail(row: any) {
  router.push(`/dragons/${row.dragonId}`)
}

// 删除弹窗
const showDeleteDialog = ref(false)
const delLoading = ref(false)
const pendingDragon = ref<{ dragonId: number; dragonName: string } | null>(null)
const lockedCount = ref(0)
async function remove(dragonId: number, dragonName: string) {
  pendingDragon.value = { dragonId, dragonName }
  lockedCount.value = 0
  showDeleteDialog.value = true
  try {
    const res = await getDragonEquipment(dragonId)
    // 切龙 → 丢弃过期信息
    if (pendingDragon.value?.dragonId !== dragonId) return
    lockedCount.value = ((res.data ?? []) as any[]).filter(r => r.locked).length
  } catch {
    // 取不到就退回通用提示，不显示数量
  }
}

async function onConfirmDelete() {
  if (!pendingDragon.value) return
  delLoading.value = true
  try {
    await deleteDragon(pendingDragon.value.dragonId)
    ElMessage.success('删除成功')
    if (dragons.value.length === 1 && currentPage.value > 1) {
      currentPage.value--
    }
    showDeleteDialog.value = false
    await searchDragons()
  } catch {

  } finally {
    delLoading.value = false
  }
}

onMounted(() => {
  loading.value = true
  searchDragons()
})
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2>群龙汇聚</h2>
        <el-button type="primary" @click="router.push('/dragons/new')">
          <el-icon><Plus/></el-icon>添加龙
        </el-button>
    </div>

    <div style="display:flex;gap:12px;margin-bottom:16px">
      <el-input
          v-model="searchName"
          placeholder="搜索龙的名称"
          clearable
          style="width:260px"
          :prefix-icon="Search"
      />
      <el-select v-model="filterElement" placeholder="属性筛选" clearable style="width:150px">
        <template #prefix>
          <img
              v-if="filterElement"
              :src="DRAGON_ELEMENTS.find(e => e.value === filterElement)?.image"
              style="width:20px;height:20px;vertical-align:middle"
          />
        </template>
        <el-option v-for="e in DRAGON_ELEMENTS" :key="e.value" :label="e.label" :value="e.value">
          <img :src="e.image" style="width:22px;height:22px;vertical-align:middle;margin-right:6px"/>
          <span>{{ e.label }}</span>
        </el-option>
      </el-select>
      <el-select v-model="filterCategory" placeholder="类别筛选" clearable style="width:150px">
        <template #prefix>
          <img
              v-if="filterCategory"
              :src="DRAGON_CATEGORIES.find(c => c.value === filterCategory)?.image"
              style="width:20px;height:20px;vertical-align:middle"
          />
        </template>
        <el-option v-for="c in DRAGON_CATEGORIES" :key="c.value" :label="c.label" :value="c.value">
          <img :src="c.image" style="width:22px;height:22px;vertical-align:middle;margin-right:6px"/>
          <span>{{ c.label }}</span>
        </el-option>
      </el-select>
      <span style="color:#909399;line-height:32px">共 {{ total }} 条</span>
    </div>

    <el-table :data="dragons" v-loading="loading" stripe style="width:100%" empty-text="没有找到匹配的龙"  @row-click="goDetail">
      <el-table-column label="头像" width="100" align="center">
        <template #default="{row}">
          <img v-if="row.dragonImage" :src="row.dragonImage" style="width:90px;height:90px;object-fit:cover;border-radius:4px" />
          <div v-else style="width:90px; height:90px; display:inline-flex; align-items:center; justify-content:center; background:#f5f7fa; border-radius:4px; color:#c0c4cc; font-size:20px">?</div>
        </template>
      </el-table-column>
      <el-table-column prop="dragonName" label="名称" min-width="100" max-width="350" align="center"/>
      <el-table-column label="属性" min-width="30" max-width="150" align="center">
        <template #default="{row}">
          <img :src="getElementImage(row.element)" style="width:30px;height:30px;vertical-align:middle" />
          {{ elementLabel(row.element) }}
        </template>
      </el-table-column>
      <el-table-column label="类别" min-width="30" max-width="150" align="center">
        <template #default="{row}">
          <img :src="getCategoryImage(row.category)" style="width:30px;height:30px;vertical-align:middle" />
          {{ categoryLabel(row.category) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right" align="center">
        <template #default="{row}">
          <el-button size="small" type="primary" plain @click.stop="router.push(`/dragons/${row.dragonId}/edit`)">修改</el-button>
          <el-button size="small" type="danger" @click.stop="remove(row.dragonId, row.dragonName)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="display:flex;justify-content:flex-end;margin-top:16px">
      <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          background
          @current-change="searchDragons"
      />
    </div>
    <DeleteConfirmDialog
        v-model="showDeleteDialog"
        title="确定删除该角色吗"
        message="此操作不可撤销"
        :description="lockedCount > 0 ? `将自动解锁 ${lockedCount} 件锁定符文` : ''"
        :loading="delLoading"
        @confirm="onConfirmDelete"
    />
  </div>
</template>

<style scoped>
/* ===== 表头字体变黑 ===== */
:deep(.el-table__header th) {
  background-color: #fff;
  color: #303133;
  text-align: center;
}
:deep(.el-table__row) {
  cursor: pointer;
}
</style>