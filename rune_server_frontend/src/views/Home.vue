<!-- 首页：功能模块跳转入口 -->
<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Star, Collection, MagicStick, Connection } from '@element-plus/icons-vue'

const router = useRouter()

const modules = [
  { title: '龙管理', desc: '录入与维护龙的信息', icon: Star, path: '/dragons' },
  { title: '符文仓库', desc: '管理所有符文与词条', icon: Collection, path: '/runes' },
  { title: '符文分配器', desc: '按权重自动搭配 + 研磨建议', icon: MagicStick, path: '/optimizer' },
  { title: '装备调整', desc: '对龙装备的符文进行自定义处理', icon: Connection, path: '/equipped' },
]
</script>

<template>
  <div class="home-page">
    <h2>龙符文管理系统</h2>
    <p class="home-desc">选择功能模块开始</p>
    <div class="module-grid">
      <div v-for="m in modules" :key="m.path" class="module-card" @click="router.push(m.path)">
        <el-icon :size="36" color="#409EFF"><component :is="m.icon" /></el-icon>
        <div class="module-title">{{ m.title }}</div>
        <div class="module-desc">{{ m.desc }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home-page {
  text-align: center;
  padding-top: 48px;
}
.home-page h2 {
  color: #303133;
  margin-bottom: 8px;
}
.home-desc {
  color: #909399;
  margin-bottom: 40px;
}
.module-grid {
  display: flex;
  justify-content: center;
  gap: 24px;
  flex-wrap: wrap;
}
.module-card {
  width: 180px;
  padding: 28px 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  cursor: pointer;
  transition: box-shadow .15s, transform .15s;
}
.module-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, .08);
  transform: translateY(-2px);
}
.module-title {
  font-size: 17px;
  font-weight: bold;
  color: #303133;
  margin-top: 12px;
}
.module-desc {
  font-size: 13px;
  color: #909399;
  margin-top: 6px;
}
</style>
