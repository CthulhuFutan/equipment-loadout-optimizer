<script setup lang="ts">

import {ref} from "vue";
import {useRouter} from 'vue-router'
import {ElMessage} from "element-plus";
import {login, register} from "@/api/auth.ts";

const router = useRouter()
const isLogin = ref(true);
const loginForm = ref({
  username: '',
  password: ''
});
const registerForm = ref({
  username: '',
  password: '',
  repeatPassword: ''
});

// 登录
async function submitLogin() {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  const res = await login(loginForm.value);
  // res：{code: 200, message: 'success', data: {userId, token, username}}
  if (res.code === 200) {
    ElMessage.success('登录成功！');
    localStorage.setItem('token', res.data.token);
    localStorage.setItem('username', res.data.username);
    localStorage.setItem('userId', res.data.userId);
    router.push('/')
    return
  }
  // res：{code: 401, message: '用户名或密码错误'}
  else if (res.code === 401) {
    ElMessage.error(res.message)
    return
  }
}
// 注册
async function submitRegister() {
  const checks = [
    { value: registerForm.value.username,       msg: '请输入用户名' },
    { value: registerForm.value.password,       msg: '请输入密码' },
    { value: registerForm.value.repeatPassword, msg: '请再次输入密码' }
  ]
  for(const check of checks) {
    if (!check.value) {
      ElMessage.warning(check.msg)
      return
    }
  }
  if (registerForm.value.password !== registerForm.value.repeatPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  const res = await register(registerForm.value);
  // 200
  if (res.code === 200) {
    ElMessage.success('注册成功！')
    isLogin.value = true
    return
  }
  // 400
  if (res.code === 400) {
    ElMessage.error(res.message)
    return
  }
  // 402
  else if (res.code === 402) {
    ElMessage.error(res.message)
    return
  }
}

// 切换
const toggleMode = () => {
  isLogin.value = !isLogin.value
}
</script>

<template>
  <div class="login-page">
    <div class="container" :class="{ 'sign-up-mode': !isLogin }">
      <!-- 左侧：表单区域 -->
      <div class="form-panel">
        <el-form v-show="isLogin" class="form-box" :model="loginForm">
          <h2 class="form-title">登录</h2>
          <el-input id="loginUsername" v-model="loginForm.username" placeholder="用户名" prefix-icon="User" size="large" class="form-input"/>
          <el-input id="loginPassword" v-model="loginForm.password" type="password" placeholder="密码" prefix-icon="Lock" size="large" class="form-input" show-password/>
          <el-button type="primary" size="large" class="submit-btn" @click="submitLogin">
            登 录
          </el-button>
        </el-form>

        <el-form v-show="!isLogin" class="form-box" :model="registerForm">
          <h2 class="form-title">注册</h2>
          <el-input id="registerUsername" v-model="registerForm.username" placeholder="用户名" prefix-icon="User" size="large" class="form-input"/>
          <el-input id="registerPassword" v-model="registerForm.password" type="password" placeholder="密码" prefix-icon="Lock" size="large" class="form-input" show-password/>
          <el-input id="registerRepeatPassword" v-model="registerForm.repeatPassword" type="password" placeholder="确认密码" prefix-icon="Lock" size="large" class="form-input" show-password/>
          <el-button type="primary" size="large" class="submit-btn" @click="submitRegister">
            注 册
          </el-button>
        </el-form>
      </div>

      <!-- 切换面板 -->
      <div class="toggle-panel">
        <!-- 登录模式：提示去注册 -->
        <div v-show="isLogin" class="toggle-content">
          <h2>你好！</h2>
          <p>还没有账号？点击下方按钮快速注册</p>
          <el-button class="toggle-btn" @click="toggleMode">去注册</el-button>
        </div>

        <!-- 注册模式：提示去登录 -->
        <div v-show="!isLogin" class="toggle-content">
          <h2>欢迎回来！</h2>
          <p>已有账号？点击下方按钮立即登录</p>
          <el-button class="toggle-btn" @click="toggleMode">去登录</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea, #764ba2);
}

.container {
  width: 900px;
  height: 520px;
  display: flex;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  overflow: hidden;
  position: relative;
}
/*
  form-panel:   translateX  =  toggle-panel.width / form-panel.width × 100%
  toggle-panel: translateX  =  form-panel.width / toggle-panel.width × 100%
*/
/* 登录 → 注册时，表单滑到右边 */
.container.sign-up-mode .form-panel {
  transform: translateX(42.86%);
}
.form-panel {
  width: 70%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.6s ease-in-out;
}
.form-box {
  width: 320px;
  text-align: center;
}

.form-title {
  margin-bottom: 30px;
  color: #333;
  font-size: 28px;
}

.form-input {
  margin-bottom: 16px;
}

.submit-btn {
  width: 100%;
  margin-top: 10px;
  height: 44px;
  font-size: 16px;
  border-radius: 25px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
}
/* 登录 → 注册时，表单滑到左边 */
.container.sign-up-mode .toggle-panel {
  transform: translateX(-233.33%);
}
.toggle-panel {
  width: 30%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  text-align: center;
  transition: transform 0.6s ease-in-out;
}
.toggle-content {
  padding: 40px;
}

.toggle-content h2 {
  font-size: 26px;
  margin-bottom: 16px;
}

.toggle-content p {
  font-size: 14px;
  margin-bottom: 30px;
  opacity: 0.9;
  line-height: 1.6;
}

.toggle-btn {
  background: transparent;
  border: 2px solid #fff;
  color: #fff;
  border-radius: 25px;
  padding: 10px 40px;
  font-size: 16px;
}

.toggle-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}

</style>