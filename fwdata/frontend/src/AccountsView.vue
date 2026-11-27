<script setup>
import { reactive, ref } from 'vue'
import { write } from './api.js'
import { formatTime, roleName, useCollection } from './collection.js'
import EditorDialog from './EditorDialog.vue'
import ListFrame from './ListFrame.vue'

const list = useCollection('/accounts')
const editing = ref(false)
const removing = ref(null)
const busy = ref(false)
const error = ref('')
const notice = ref('')
const form = reactive({ username: '', displayName: '', role: 'USER', password: '' })
function open() {
  Object.assign(form, { username: '', displayName: '', role: 'USER', password: '' })
  error.value = notice.value = ''
  editing.value = true
}
function close() {
  form.password = ''
  editing.value = false
}
async function remove() {
  busy.value = true
  error.value = ''
  try {
    await write('/accounts/' + removing.value.id, 'DELETE')
    notice.value = '已删除账号 ' + removing.value.username
    removing.value = null
    await list.load()
  } catch (e) { error.value = e.message }
  finally { busy.value = false }
}
async function save() {
  busy.value = true
  error.value = ''
  try {
    await write('/accounts', 'POST', form)
    close()
    notice.value = '已创建账号 ' + form.username
    list.search.value = ''
    await list.find()
  } catch (e) { error.value = e.message }
  finally { busy.value = false }
}
</script>

<template>
  <div class="page-head">
    <div><h1>账号管理</h1><p class="muted">管理工作区的用户与管理员。</p></div>
    <button class="btn primary" @click="open"><span aria-hidden="true">＋</span>新建账号</button>
  </div>
  <p v-if="notice" role="status" class="notice">{{ notice }}</p>
  <ListFrame :list="list" placeholder="搜索账号或姓名" empty="还没有账号">
    <table><thead><tr><th>姓名</th><th>登录账号</th><th>角色</th><th>创建时间</th><th>操作</th></tr></thead>
      <tbody><tr v-for="item in list.items.value" :key="item.id">
        <td class="strong">{{ item.displayName }}</td><td>{{ item.username }}</td>
        <td><span class="role" :class="{ admin: item.role === 'ADMIN' }">{{ roleName(item.role) }}</span></td>
        <td class="date">{{ formatTime(item.createdAt) }}</td>
        <td><button v-if="item.role === 'USER'" class="text-button danger" :aria-label="'删除 ' + item.username" @click="removing = item; error = ''; notice = ''">删除</button><span v-else class="muted">—</span></td>
      </tr></tbody>
    </table>
  </ListFrame>
  <EditorDialog v-if="editing" title="新建账号" :busy="busy" @close="close">
    <form @submit.prevent="save">
      <label>姓名<input v-model.trim="form.displayName" maxlength="64" required autofocus autocomplete="off"></label>
      <label>登录账号<input v-model.trim="form.username" required minlength="3" maxlength="40" pattern="[A-Za-z0-9][A-Za-z0-9_.\-]{2,39}" autocomplete="off"><small>3 至 40 位，以字母或数字开头，可含点、下划线和短横线。</small></label>
      <label>角色<select v-model="form.role"><option value="USER">普通用户</option><option value="ADMIN">管理员</option></select>
        <small>{{ form.role === 'ADMIN' ? '可管理数据集、来源与账号，并查看提交。' : '可以查看全部提交记录与填写的答案。' }}</small></label>
      <label>初始密码<input v-model="form.password" type="password" minlength="8" maxlength="128" required autocomplete="new-password"><small>8 至 128 个字符。</small></label>
      <p v-if="error" class="error" role="alert">{{ error }}</p>
      <footer><button type="button" class="btn" :disabled="busy" @click="close">取消</button><button class="btn primary" :disabled="busy">{{ busy ? '创建中…' : '创建账号' }}</button></footer>
    </form>
  </EditorDialog>
  <EditorDialog v-if="removing" title="删除普通用户" :busy="busy" @close="removing = null">
    <p>删除「{{ removing.displayName }}」（{{ removing.username }}）后，该账号将失去后台访问权限。</p>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <footer><button class="btn" :disabled="busy" @click="removing = null">取消</button><button class="btn destructive" :disabled="busy" @click="remove">{{ busy ? '删除中…' : '确认删除' }}</button></footer>
  </EditorDialog>
</template>
