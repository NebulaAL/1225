<script setup>
import { onMounted, ref } from 'vue'
import { get } from './api.js'
import { formatTime, kindName, useCollection } from './collection.js'
import ListFrame from './ListFrame.vue'
import EditorDialog from './EditorDialog.vue'
defineProps({ admin: Boolean })
const datasets = ref([])
const sources = ref([])
const datasetId = ref('')
const sourceId = ref('')
const metaError = ref('')
const active = ref(null)
const detail = ref(null)
const detailError = ref('')
const list = useCollection('/submissions', () => ({
  ...(datasetId.value && { datasetId: datasetId.value }),
  ...(sourceId.value && { sourceId: sourceId.value }),
}))
async function loadFilters() {
  metaError.value = ''
  try { datasets.value = await get('/datasets') }
  catch (e) { metaError.value = e.message }
}
async function loadSources() {
  const selected = datasetId.value
  if (!selected) return
  try {
    const result = await get('/datasets/' + selected + '/sources')
    if (selected === datasetId.value) sources.value = result
  } catch (e) { if (selected === datasetId.value) metaError.value = e.message }
}
function selectDataset() {
  sources.value = []
  sourceId.value = ''
  metaError.value = ''
  loadSources()
  list.load(1)
}
function refresh() {
  loadFilters()
  loadSources()
  list.load()
}
async function open(item) {
  active.value = item
  detail.value = null
  detailError.value = ''
  try {
    const result = await get('/submissions/' + item.id)
    if (active.value?.id === item.id) detail.value = result
  } catch (e) { if (active.value?.id === item.id) detailError.value = e.message }
}
onMounted(loadFilters)
</script>

<template>
  <div class="page-head"><div><h1>提交记录</h1><p class="muted">汇总各来源的前台填报，按数据集和来源查阅答案。</p></div>
    <button class="btn" @click="refresh">刷新记录</button></div>
  <p v-if="metaError" class="error" role="alert">{{ metaError }}</p>
  <ListFrame :list="list" empty="当前范围还没有提交记录">
    <template #filters><div class="filters">
      <label>数据集<select v-model="datasetId" aria-label="数据集" @change="selectDataset"><option value="">全部数据集</option><option v-for="d in datasets" :key="d.id" :value="d.id">{{ d.name }}</option></select></label>
      <label>收集来源<select v-model="sourceId" aria-label="收集来源" :disabled="!datasetId" @change="list.load(1)"><option value="">全部来源</option><option v-for="s in sources" :key="s.id" :value="s.id">{{ s.name }}</option></select></label>
    </div></template>
    <table><thead><tr><th>提交编号</th><th>数据集</th><th>收集来源</th><th>提交时间</th><th>填写内容</th></tr></thead>
      <tbody><tr v-for="item in list.items.value" :key="item.id"><td class="record-id">#{{ item.id }}</td><td class="strong">{{ item.datasetName }}</td><td>{{ item.sourceName }}</td><td class="date">{{ formatTime(item.submittedAt) }}</td>
        <td><button class="text-button" @click="open(item)">查看答案</button></td></tr></tbody>
    </table>
  </ListFrame>
  <p v-if="admin && !datasets.length && !list.loading.value && !metaError" class="first-step"><a href="#datasets">创建第一个数据集</a> 后，即可分享填报链接。</p>
  <EditorDialog v-if="active" :title="'提交记录 #' + active.id" @close="active = null">
    <p class="muted">{{ active.datasetName }} · {{ active.sourceName }} · {{ formatTime(active.submittedAt) }}</p>
    <p v-if="detailError" class="error" role="alert">{{ detailError }} <button class="text-button" @click="open(active)">重试</button></p>
    <dl v-else-if="detail" class="answer-list"><template v-for="(a, index) in detail.answers" :key="index"><dt>{{ a.label }} <small>{{ kindName(a.kind) }}</small></dt>
      <dd class="multiline">{{ a.selected.length ? a.selected.join('、') : a.text || '未填写' }}</dd></template></dl>
    <p v-else role="status">正在读取答案…</p>
  </EditorDialog>
</template>
