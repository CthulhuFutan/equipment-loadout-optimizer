<script setup lang="ts">
import icon from "@/assets/icon.png";
import {ref} from "vue";
import {useRouter} from "vue-router";
import {Collection, Connection, HomeFilled, MagicStick, Star} from "@element-plus/icons-vue";

const router = useRouter()
const username = ref(localStorage.getItem('username') || '')

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('userId')
  localStorage.removeItem('username')
  router.push('/login')
}
</script>

<template>
  <el-container style="min-height: 100vh">
    <el-aside width="200px" style="background: #304156">
      <div class="logo-area">
        <img :src="icon" alt="logo" class="logo-img" />
        <span class="logo-text">龙村M</span>
      </div>
      <el-menu
          :default-active="router.currentRoute.value.path"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
          router
      >
        <el-menu-item index="/">
          <el-icon><HomeFilled/></el-icon>
          <span>首页</span>
        </el-menu-item>
        <el-menu-item index="/dragons">
          <el-icon><Star/></el-icon>
          <span>龙管理</span>
        </el-menu-item>
        <el-menu-item index="/runes">
          <el-icon><Collection/></el-icon>
          <span>符文仓库</span>
        </el-menu-item>
        <el-menu-item index="/optimizer">
          <el-icon><MagicStick/></el-icon>
          <span>符文分配器</span>
        </el-menu-item>
        <el-menu-item index="/equipped">
          <el-icon><Connection/></el-icon>
          <span>已装备龙</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="background:#fff;border-bottom:1px solid #dcdfe6;display:flex;justify-content:flex-end;align-items:center;padding:0 20px">
        <span style="margin-right:16px;color:#666">{{ username }}</span>
        <el-button @click="logout" type="danger" size="small" plain>退出</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.logo-area {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #4a5064;
}
.logo-img {
  width: 36px;
  height: 36px;
  margin-right: 10px;
}
.logo-text {
  color: #fff;
  font-size: 16px;
  font-weight: bold;
}
</style>