<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { get, write } from './api.js'
import LoginView from './LoginView.vue'
import Workspace from './Workspace.vue'
import PublicForm from './PublicForm.vue'

const account = ref(null)
const loading = ref(true)
const failure = ref('')
const formKey = ref(null)
function route() {
  const previous = formKey.value
  formKey.value = location.hash.startsWith('#/f/') ? location.hash.slice(4) : null
  if (previous !== null && formKey.value === null) restore()
}

async function restore() {
  loading.value = true
  failure.value = ''
  try {
    account.value = await get('/auth/me')
  } catch (error) {
    if (error.status !== 401) failure.value = error.message
  } finally {
    loading.value = false
  }
}

function expire() {
  account.value = null
}

async function login(credentials) {
  await write('/auth/login', 'POST', new URLSearchParams(credentials))
  account.value = await get('/auth/me')
}

async function logout() {
  await write('/auth/logout', 'POST')
  expire()
}

onMounted(() => {
  window.addEventListener('session-expired', expire)
  window.addEventListener('hashchange', route)
  route()
  if (formKey.value === null) restore()
})
onUnmounted(() => {
  window.removeEventListener('session-expired', expire)
  window.removeEventListener('hashchange', route)
})
</script>

<template>
  <PublicForm v-if="formKey !== null" :key="formKey" :form-key="formKey" />
  <main v-else-if="loading || failure" class="startup">
    <strong class="brand"><span class="brand-mark" aria-hidden="true"></span>FW Data</strong>
    <p role="status">{{ failure || '正在连接工作区…' }}</p>
    <button v-if="failure" class="btn primary" @click="restore">重新连接</button>
  </main>
  <Workspace v-else-if="account" :account="account" :logout="logout" />
  <LoginView v-else :login="login" />
</template>
