<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { roleName } from './collection.js'
import AccountsView from './AccountsView.vue'
import RecordsView from './RecordsView.vue'
import DatasetsView from './DatasetsView.vue'

const props = defineProps({
  account: { type: Object, required: true },
  logout: { type: Function, required: true },
})
const isAdmin = props.account.role === 'ADMIN'
const readTab = () => isAdmin && ['#accounts', '#datasets'].includes(location.hash) ? location.hash.slice(1) : 'records'
const tab = ref(readTab())
const error = ref('')
const loggingOut = ref(false)
function navigate() { tab.value = readTab() }
onMounted(() => window.addEventListener('hashchange', navigate))
onUnmounted(() => window.removeEventListener('hashchange', navigate))
async function leave() {
  loggingOut.value = true
  try {
    await props.logout()
    location.hash = 'records'
  } catch (e) { error.value = e.message }
  finally { loggingOut.value = false }
}
</script>

<template>
  <div class="workspace">
    <aside class="sidebar">
      <div class="brand"><span class="brand-mark" aria-hidden="true"></span>FW Data</div>
      <nav aria-label="工作区导航">
        <a href="#records" :aria-current="tab === 'records' ? 'page' : undefined"><span class="nav-record" aria-hidden="true"></span>提交记录</a>
        <a v-if="isAdmin" href="#datasets" :aria-current="tab === 'datasets' ? 'page' : undefined"><span class="nav-folder" aria-hidden="true"></span>数据集</a>
        <a v-if="isAdmin" href="#accounts" :aria-current="tab === 'accounts' ? 'page' : undefined"><span class="nav-account" aria-hidden="true"></span>账号管理</a>
      </nav>
      <div class="identity">
        <div class="avatar" aria-hidden="true">{{ Array.from(account.displayName)[0] }}</div>
        <div class="who"><strong>{{ account.displayName }}</strong><small>{{ roleName(account.role) }}</small></div>
        <button class="signout" :disabled="loggingOut" @click="leave">退出</button>
      </div>
    </aside>
    <main class="work-main">
      <p v-if="error" class="error" role="alert">{{ error }}</p>
      <AccountsView v-if="tab === 'accounts'"/><DatasetsView v-else-if="tab === 'datasets'"/><RecordsView v-else :admin="isAdmin"/>
    </main>
  </div>
</template>
