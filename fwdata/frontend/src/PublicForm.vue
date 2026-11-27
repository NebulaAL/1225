<script setup>
import { onMounted, reactive, ref } from 'vue'
import { get, write } from './api.js'
import { kindName } from './collection.js'
const props = defineProps({ formKey: { type: String, required: true } })
const form = ref(null)
const values = reactive({})
const error = ref('')
const busy = ref(false)
const receipt = ref(null)
const requestKey = Array.from(crypto.getRandomValues(new Uint8Array(16)), n => n.toString(16).padStart(2, '0')).join('')

async function load() {
  error.value = ''
  try {
    if (!/^[0-9a-f]{32}$/.test(props.formKey)) throw new Error('填报链接无效')
    form.value = await get('/public/forms/' + props.formKey)
    for (const field of form.value.fields) values[field.id] = field.kind === 'MULTIPLE' ? [] : ''
  } catch (e) { error.value = e.message }
}
async function submit() {
  error.value = ''
  const answers = []
  for (const f of form.value.fields) {
    const value = values[f.id]
    const text = f.kind === 'TEXT' ? value.trim() : ''
    const optionIds = f.kind === 'MULTIPLE' ? value : f.kind === 'SINGLE' && value ? [value] : []
    if (f.required && !text && !optionIds.length) {
      error.value = '请填写「' + f.label + '」'
      document.getElementById('question-' + f.id)?.focus()
      return
    }
    if (text || optionIds.length) answers.push({ fieldId: f.id, text, optionIds })
  }
  busy.value = true
  try {
    receipt.value = await write('/public/forms/' + props.formKey + '/submissions', 'POST', { requestKey, answers })
  } catch (e) { error.value = e.message }
  finally { busy.value = false }
}
onMounted(load)
</script>

<template>
  <main class="public-shell">
    <div class="brand"><span class="brand-mark" aria-hidden="true"></span>FW Data</div>
    <section v-if="receipt" class="public-paper"><h1>已收到你的填写</h1><p class="muted">提交编号 #{{ receipt.id }}</p></section>
    <section v-else-if="form" class="public-paper">
      <p class="source-caption">收集来源 · {{ form.sourceName }}</p>
      <h1>{{ form.dataset.name }}</h1><p v-if="form.dataset.description" class="multiline muted">{{ form.dataset.description }}</p>
      <form @submit.prevent="submit">
        <fieldset v-for="f in form.fields" :id="'question-' + f.id" :key="f.id" tabindex="-1" class="question" :disabled="busy">
          <legend>{{ f.label }} <small>{{ kindName(f.kind) }} · {{ f.required ? '必填' : '选填' }}</small></legend>
          <textarea v-if="f.kind === 'TEXT'" v-model="values[f.id]" :aria-label="f.label" :required="f.required" maxlength="4000" rows="3"></textarea>
          <label v-for="o in f.options" :key="o.id" class="choice">
            <input v-if="f.kind === 'SINGLE'" v-model="values[f.id]" type="radio" :name="'field-' + f.id" :value="o.id" :required="f.required">
            <input v-else v-model="values[f.id]" type="checkbox" :value="o.id">{{ o.name }}
          </label>
        </fieldset>
        <p v-if="error" role="alert" class="error">{{ error }}</p>
        <button class="btn primary" :disabled="busy">{{ busy ? '提交中…' : '提交填写' }}</button>
      </form>
    </section>
    <section v-else class="public-paper"><p role="status">{{ error || '正在读取表单…' }}</p><button v-if="error" class="btn" @click="load">重新读取</button></section>
  </main>
</template>
