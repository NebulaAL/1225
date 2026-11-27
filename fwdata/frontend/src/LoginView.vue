<script setup>
import { ref } from 'vue'
const props = defineProps({ login: { type: Function, required: true } })
const username = ref('')
const password = ref('')
const visible = ref(false)
const busy = ref(false)
const error = ref('')

async function submit() {
  busy.value = true
  error.value = ''
  try {
    await props.login({ username: username.value.trim(), password: password.value })
  } catch (e) {
    error.value = e.status === 401 ? '账号或密码错误，请重新输入' : e.message
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <main class="login-layout">
    <div class="login-art" aria-hidden="true">
      <div class="art-grid"></div>
      <div class="file-stack"><i></i><i></i><i></i><i><b></b><b></b><b></b></i></div>
    </div>
    <section class="login-panel" aria-labelledby="login-heading">
      <div class="login-inner">
        <div class="brand"><span class="brand-mark" aria-hidden="true"></span>FW Data</div>
        <h1 id="login-heading">登录管理后台</h1>
        <p class="muted login-note">使用你的工作区账号登录。</p>
        <form @submit.prevent="submit">
          <label>登录账号<input v-model="username" name="username" autocomplete="username" required maxlength="40" placeholder="输入账号"></label>
          <label>密码<span class="input-action">
            <input v-model="password" name="password" :type="visible ? 'text' : 'password'" autocomplete="current-password" required maxlength="128" placeholder="输入密码">
            <button type="button" :aria-pressed="visible" @click="visible = !visible">{{ visible ? '隐藏' : '显示' }}</button>
          </span></label>
          <p v-if="error" role="alert" class="error">{{ error }}</p>
          <button class="btn primary login-submit" :disabled="busy">{{ busy ? '正在登录…' : '登录' }}<span aria-hidden="true">→</span></button>
        </form>
      </div>
    </section>
  </main>
</template>
