<script setup>
import { ref } from 'vue'
import EditorDialog from './EditorDialog.vue'
import { write } from './api.js'
const emit = defineEmits(['created', 'close'])
let sequence = 0
const question = () => ({ key: ++sequence, label: '', kind: 'SINGLE', required: true, optionLines: '' })
const name = ref('')
const description = ref('')
const fields = ref([question()])
const busy = ref(false)
const error = ref('')
async function save() {
  error.value = ''
  const questions = fields.value.map(f => ({
    label: f.label.trim(), kind: f.kind, required: f.required,
    options: f.kind === 'TEXT' ? [] : f.optionLines.split('\n').map(s => s.trim()).filter(Boolean),
  }))
  for (const f of questions) {
    if (f.kind !== 'TEXT' && (f.options.length < 2 || f.options.length > 50 || f.options.some(s => s.length > 120))) {
      error.value = '「' + f.label + '」需要 2 至 50 个选项，每个最多 120 字。'
      return
    }
  }
  busy.value = true
  try {
    const result = await write('/manage/datasets', 'POST', { name: name.value, description: description.value, fields: questions })
    emit('created', result.id)
  } catch (e) { error.value = e.message }
  finally { busy.value = false }
}
</script>

<template>
  <EditorDialog title="新建数据集" :busy="busy" @close="emit('close')">
    <form @submit.prevent="save">
      <label>数据集名称<input v-model.trim="name" required maxlength="120" autofocus></label>
      <label>填报说明 <small>选填</small><textarea v-model.trim="description" maxlength="1000" rows="2"></textarea></label>
      <div v-for="(field, i) in fields" :key="field.key" class="builder-question">
        <div class="section-head"><h3>题目 {{ i + 1 }}</h3><button v-if="fields.length > 1" type="button" class="text-button danger" :aria-label="'移除题目 ' + (i + 1)" @click="fields.splice(i, 1)">移除</button></div>
        <label>题目名称<input v-model.trim="field.label" maxlength="120" required></label>
        <div class="field-row"><label>题型<select v-model="field.kind"><option value="SINGLE">单选</option><option value="MULTIPLE">多选</option><option value="TEXT">文本</option></select></label>
          <label class="choice"><input v-model="field.required" type="checkbox">必填</label></div>
        <label v-if="field.kind !== 'TEXT'">选项<textarea v-model="field.optionLines" required rows="3" placeholder="每行输入一个选项"></textarea></label>
      </div>
      <button v-if="fields.length < 50" type="button" class="btn add-question" @click="fields.push(question())">＋ 添加题目</button>
      <p class="muted hint">创建后即可分发填报链接。题目和选项会保留原样，方便核对历史答案。</p>
      <p v-if="error" class="error" role="alert">{{ error }}</p>
      <footer><button type="button" class="btn" :disabled="busy" @click="emit('close')">取消</button><button class="btn primary" :disabled="busy">{{ busy ? '创建中…' : '创建数据集' }}</button></footer>
    </form>
  </EditorDialog>
</template>
