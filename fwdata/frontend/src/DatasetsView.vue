<script setup>
import { onMounted, ref } from 'vue'
import { get, write } from './api.js'
import { kindName } from './collection.js'
import NewDatasetDialog from './NewDatasetDialog.vue'
const datasets = ref([])
const selected = ref('')
const detail = ref(null)
const creating = ref(false)
const loading = ref(true)
const saving = ref(false)
const source = ref('')
const error = ref('')
const notice = ref('')
let revision = 0
const link = key => new URL('#/f/' + key, location.href).href
async function select(id) {
  const current = ++revision
  selected.value = id
  detail.value = null
  error.value = notice.value = source.value = ''
  loading.value = true
  try {
    const result = await get('/manage/datasets/' + id)
    if (current === revision) detail.value = result
  } catch (e) { if (current === revision) error.value = e.message }
  finally { if (current === revision) loading.value = false }
}
async function load(id) {
  loading.value = true
  error.value = ''
  try {
    datasets.value = await get('/datasets')
    if (datasets.value.length) await select(id || datasets.value[0].id)
  } catch (e) { error.value = e.message }
  finally { loading.value = false }
}
async function created(id) {
  creating.value = false
  await load(id)
}
async function addSource() {
  const id = selected.value
  error.value = notice.value = ''
  saving.value = true
  try {
    const result = await write('/manage/datasets/' + id + '/sources', 'POST', { name: source.value.trim() })
    if (id === selected.value) {
      detail.value.sources.push(result)
      source.value = ''
      notice.value = '已生成「' + result.name + '」的填报链接'
    }
  } catch (e) { if (id === selected.value) error.value = e.message }
  finally { saving.value = false }
}
async function copy(key) {
  try {
    await navigator.clipboard.writeText(link(key))
    notice.value = '已复制填报链接'
  } catch {
    const field = document.getElementById('link-' + key)
    field.focus(); field.select()
    notice.value = '链接已选中，可直接复制'
  }
}
onMounted(() => load())
</script>

<template>
  <div class="page-head"><div><h1>数据集</h1><p class="muted">配置采集题目，为每个来源生成独立的填报链接。</p></div>
    <button class="btn primary" @click="creating = true"><span aria-hidden="true">＋</span>新建数据集</button></div>
  <p v-if="error" class="error" role="alert">{{ error }} <button v-if="!detail" class="text-button" @click="load(selected)">重试</button></p>
  <div v-if="datasets.length" class="dataset-picker"><label>当前数据集<select :value="selected" @change="select(Number($event.target.value))"><option v-for="d in datasets" :key="d.id" :value="d.id">{{ d.name }}</option></select></label></div>
  <div v-if="loading" class="list-state" role="status">正在读取…</div>
  <section v-else-if="detail" class="dataset-detail">
    <p v-if="detail.dataset.description" class="muted multiline">{{ detail.dataset.description }}</p>
    <h2>收集来源</h2>
    <form class="source-form" @submit.prevent="addSource"><label class="sr-only" for="source-name">新来源名称</label><input id="source-name" v-model.trim="source" maxlength="80" required placeholder="新来源名称，例如线下门店"><button class="btn" :disabled="saving">{{ saving ? '生成中…' : '生成链接' }}</button></form>
    <p v-if="notice" role="status" class="notice">{{ notice }}</p>
    <div v-for="s in detail.sources" :key="s.id" class="source-item">
      <label :for="'link-' + s.publicKey">{{ s.name }}</label>
      <input :id="'link-' + s.publicKey" :value="link(s.publicKey)" readonly @focus="$event.target.select()">
      <div class="source-actions"><button class="text-button" @click="copy(s.publicKey)">复制</button><a :href="link(s.publicKey)" target="_blank" rel="noopener">打开填报页 ↗</a></div>
    </div>
    <h2>采集题目</h2>
    <ol class="field-list"><li v-for="f in detail.fields" :key="f.id"><strong>{{ f.label }}</strong><small>{{ kindName(f.kind) }} · {{ f.required ? '必填' : '选填' }}</small><p v-if="f.options.length">{{ f.options.map(o => o.name).join(' / ') }}</p></li></ol>
  </section>
  <div v-else-if="!error" class="list-state"><span class="empty-folder" aria-hidden="true"></span><p>还没有数据集</p><small>新建数据集后即可配置题目并获取填报链接。</small></div>
  <NewDatasetDialog v-if="creating" @close="creating = false" @created="created"/>
</template>
