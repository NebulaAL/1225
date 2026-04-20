<template>
  <div class="ds-chat">
    <!-- 顶栏 -->
    <div class="ds-topbar">
      <div class="ds-model-badge" @click="showSettings = true">
        <span class="ds-model-dot"></span>
        {{ currentPreset.name }}
        <svg class="ds-chevron" viewBox="0 0 16 16"><path d="M4 6l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>
      </div>
      <button class="ds-icon-btn" @click="clearMessages" title="清空对话">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14H6L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4h6v2"/>
        </svg>
      </button>
    </div>

    <!-- 消息列表 -->
    <div class="ds-messages" ref="messagesEl">
      <div v-if="messages.length === 0" class="ds-empty">
        <div class="ds-empty-icon">✨</div>
        <div>有什么想问的？</div>
      </div>
      <div v-for="(msg, index) in messages" :key="index" :class="['ds-msg-row', msg.role]">
        <div :class="['ds-avatar', msg.role]">
          <svg v-if="msg.role === 'assistant'" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2z" fill="rgba(255,255,255,0.2)"/>
            <path d="M9 9h1.5v1.5H9V9zm4.5 0H15v1.5h-1.5V9zM7.5 14.5c.83 1.5 2.42 2.5 4.5 2.5s3.67-1 4.5-2.5H7.5z" fill="white"/>
            <circle cx="9.75" cy="9.75" r="1" fill="white"/>
            <circle cx="14.25" cy="9.75" r="1" fill="white"/>
            <path d="M7.5 14.5S9 17 12 17s4.5-2.5 4.5-2.5H7.5z" fill="white"/>
          </svg>
          <span v-else>You</span>
        </div>
        <div v-if="msg.content" :class="['ds-bubble', msg.role]">
          <div v-if="msg.type === 'image'" class="ds-img-grid">
            <img v-for="(url, i) in msg.images" :key="i" :src="url" class="ds-img" />
          </div>
          <div v-else v-html="renderMarkdown(msg.content)"></div>
        </div>
        <div v-if="msg.role === 'assistant' && index === messages.length - 1 && isStreaming && !msg.content" class="ds-typing">
          <span></span><span></span><span></span>
        </div>
      </div>
    </div>

    <!-- 错误提示 -->
    <div v-if="errorMsg" class="ds-error-bar">{{ errorMsg }}</div>

    <!-- 输入区域 -->
    <div class="ds-input-wrap">
      <textarea
        v-model="userInput"
        class="ds-textarea"
        placeholder="有什么想问的？（Enter 发送，Shift+Enter 换行）"
        @keydown="handleKeydown"
        @input="autoResize"
        ref="textareaEl"
        :disabled="isStreaming"
      ></textarea>
      <div class="ds-input-actions">
        <span class="ds-input-hint">Shift+Enter 换行</span>
        <div style="display:flex;gap:6px;align-items:center">
          <button v-if="isStreaming" class="ds-send-btn ds-stop-btn" @click="stopMessage">
            <svg viewBox="0 0 24 24"><rect x="6" y="6" width="12" height="12" rx="2"/></svg>
          </button>
          <button v-else class="ds-send-btn" @click="sendMessage()" :disabled="!userInput.trim()">
            <svg viewBox="0 0 24 24"><path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/></svg>
          </button>
        </div>
      </div>
    </div>

    <!-- 设置弹窗遮罩 -->
    <Teleport to="body">
      <div v-if="showSettings" class="ds-overlay" @click.self="showSettings = false">
        <div class="ds-settings-modal">
          <div class="ds-modal-header">
            <span class="ds-modal-title">模型设置</span>
            <button class="ds-icon-btn" @click="showSettings = false">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>

          <div class="ds-modal-body">
            <!-- 模型选择 -->
            <div class="ds-field">
              <label class="ds-field-label">模型</label>
              <div class="ds-preset-list">
                <div
                  v-for="p in PRESETS"
                  :key="p.id"
                  :class="['ds-preset-item', { active: selectedPresetId === p.id }]"
                  @click="selectPreset(p.id)"
                >
                  <span class="ds-preset-name">{{ p.name }}</span>
                  <svg v-if="selectedPresetId === p.id" class="ds-check" viewBox="0 0 16 16"><path d="M3 8l4 4 6-6" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>
                </div>
              </div>
            </div>

            <!-- 自定义字段 -->
            <template v-if="selectedPresetId === 'custom'">
              <div class="ds-field">
                <label class="ds-field-label">API URL</label>
                <input v-model="customUrl" class="ds-field-input" placeholder="https://..." />
              </div>
              <div class="ds-field">
                <label class="ds-field-label">Model Name</label>
                <input v-model="customModel" class="ds-field-input" placeholder="model-name" />
              </div>
            </template>

            <!-- API Key -->
            <div class="ds-field">
              <label class="ds-field-label">
                API Key
                <span v-if="savedKey" class="ds-saved-tag">✓ 已保存</span>
              </label>
              <div class="ds-key-row">
                <input
                  v-model="apiKeyInput"
                  type="password"
                  class="ds-field-input"
                  placeholder="sk-..."
                />
                <button class="ds-btn-clear" @click="clearKey" v-if="savedKey">清除</button>
              </div>
            </div>
          </div>

          <div class="ds-modal-footer">
            <button class="ds-btn-cancel" @click="showSettings = false">取消</button>
            <button class="ds-btn-save" @click="saveAndClose">保存</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'

const PRESETS = [
  { id: 'minimax-m27', name: 'MiniMax M2.7', url: 'https://api.minimaxi.com/v1/chat/completions', model: 'MiniMax-M2.7', imageModel: 'image-01' },
  { id: 'minimax-m27hs', name: 'MiniMax M2.7 (快速)', url: 'https://api.minimaxi.com/v1/chat/completions', model: 'MiniMax-M2.7-highspeed', imageModel: 'image-01' },
  { id: 'minimax-m25', name: 'MiniMax M2.5', url: 'https://api.minimaxi.com/v1/chat/completions', model: 'MiniMax-M2.5', imageModel: 'image-01' },
  { id: 'deepseek', name: 'DeepSeek Chat', url: 'https://api.deepseek.com/v1/chat/completions', model: 'deepseek-chat' },
  { id: 'deepseek-r1', name: 'DeepSeek R1', url: 'https://api.deepseek.com/v1/chat/completions', model: 'deepseek-reasoner' },
  { id: 'custom', name: '自定义', url: '', model: '' },
]

const selectedPresetId = ref(PRESETS[0].id)
const customUrl = ref('')
const customModel = ref('')
const apiKeyInput = ref('')
const savedKey = ref('')
const showSettings = ref(false)
const userInput = ref('')
const messages = ref((() => {
  try {
    const saved = sessionStorage.getItem('chat_messages')
    return saved ? JSON.parse(saved) : []
  } catch { return [] }
})())
const isStreaming = ref(false)
const abortController = ref(null)
const errorMsg = ref('')
const messagesEl = ref(null)
const textareaEl = ref(null)

const currentPreset = computed(() => PRESETS.find(p => p.id === selectedPresetId.value) || PRESETS[0])

watch(messages, (val) => {
  try { sessionStorage.setItem('chat_messages', JSON.stringify(val)) } catch {}
}, { deep: true })

function storageKey(presetId) {
  if (presetId === 'custom') return 'chat_api_key_custom'
  const preset = PRESETS.find(p => p.id === presetId)
  if (!preset?.url) return `chat_api_key_${presetId}`
  try { return `chat_api_key_${new URL(preset.url).hostname}` } catch { return `chat_api_key_${presetId}` }
}

onMounted(() => {
  document.body.classList.add('ds-no-scroll')
  const savedPreset = localStorage.getItem('chat_selected_preset')
  if (savedPreset && PRESETS.find(p => p.id === savedPreset)) selectedPresetId.value = savedPreset
  loadKeyForPreset(selectedPresetId.value)
  const cu = localStorage.getItem('chat_custom_url')
  const cm = localStorage.getItem('chat_custom_model')
  if (cu) customUrl.value = cu
  if (cm) customModel.value = cm
})

onUnmounted(() => {
  document.body.classList.remove('ds-no-scroll')
})

function loadKeyForPreset(presetId) {
  const stored = localStorage.getItem(storageKey(presetId))
  savedKey.value = stored || ''
  apiKeyInput.value = stored || ''
}

function selectPreset(id) {
  selectedPresetId.value = id
  loadKeyForPreset(id)
}

function clearKey() {
  localStorage.removeItem(storageKey(selectedPresetId.value))
  savedKey.value = ''
  apiKeyInput.value = ''
}

function sanitizeKey(raw) {
  // HTTP headers only allow ISO-8859-1 printable chars; strip anything else
  return raw.trim().replace(/[^\x20-\x7E]/g, '')
}

function saveAndClose() {
  const key = sanitizeKey(apiKeyInput.value)
  if (key) {
    localStorage.setItem(storageKey(selectedPresetId.value), key)
    savedKey.value = key
  }
  localStorage.setItem('chat_selected_preset', selectedPresetId.value)
  if (selectedPresetId.value === 'custom') {
    localStorage.setItem('chat_custom_url', customUrl.value.trim())
    localStorage.setItem('chat_custom_model', customModel.value.trim())
  }
  showSettings.value = false
}

function clearMessages() {
  messages.value = []
  errorMsg.value = ''
  try { sessionStorage.removeItem('chat_messages') } catch {}
}

function stopMessage() {
  if (abortController.value) abortController.value.abort()
}

function getApiConfig() {
  if (selectedPresetId.value === 'custom') return { url: customUrl.value.trim(), model: customModel.value.trim() }
  return { url: currentPreset.value.url, model: currentPreset.value.model }
}

function renderMarkdown(text) {
  if (!text) return ''
  return text
    .replace(/<think>[\s\S]*?<\/think>/g, '')  // 完整的 think 块
    .replace(/<think>[\s\S]*/g, '')              // 未闭合的 think 块
    .trim()
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/_([^_]+)_/g, '<em>$1</em>')
    .replace(/^\n+/, '')
    .replace(/\n/g, '<br>')
}

async function scrollToBottom() {
  await nextTick()
  if (messagesEl.value) messagesEl.value.scrollTop = messagesEl.value.scrollHeight
}

function autoResize() {
  const el = textareaEl.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 160) + 'px'
}

function handleKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendMessage(false) }
}

async function sendMessage(isImage = false) {
  const key = sanitizeKey(savedKey.value || localStorage.getItem(storageKey(selectedPresetId.value)) || '')
  if (!key) { showSettings.value = true; return }

  const content = userInput.value.trim()
  if (!content || isStreaming.value) return

  const { url, model } = getApiConfig()
  if (!url || !model) { errorMsg.value = '请在设置中填写自定义 API URL 和 Model'; return }

  console.log('[chat] key length:', key.length, '| first4:', key.slice(0, 4), '| last4:', key.slice(-4))
  console.log('[chat] url:', url, '| model:', model)

  errorMsg.value = ''
  userInput.value = ''
  if (textareaEl.value) textareaEl.value.style.height = 'auto'

  messages.value.push({ role: 'user', content })
  await scrollToBottom()
  messages.value.push({ role: 'assistant', content: '' })
  isStreaming.value = true
  abortController.value = new AbortController()
  await scrollToBottom()

  const preset = currentPreset.value
  const isImageMode = isImage && !!preset.imageModel
  try {
    if (isImageMode) {
      const response = await fetch('https://api.minimaxi.com/v1/image_generation', {
        method: 'POST',
        signal: abortController.value.signal,
        headers: { 'Authorization': `Bearer ${key}`, 'Content-Type': 'application/json' },
        body: JSON.stringify({ model: preset.imageModel, prompt: content, n: 1, response_format: 'url' })
      })
      const j = await response.json()
      if (!response.ok) throw new Error(j?.base_resp?.status_msg || `HTTP ${response.status}`)
      const urls = j?.data?.image_urls || []
      if (!urls.length) throw new Error('未返回图片')
      const last = messages.value[messages.value.length - 1]
      last.content = '图片生成完成'
      last.type = 'image'
      last.images = urls
    } else {
    const response = await fetch(url, {
      method: 'POST',
      signal: abortController.value.signal,
      headers: { 'Authorization': `Bearer ${key}`, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        model,
        messages: messages.value.slice(0, -1).map(m => ({ role: m.role, content: m.content })),
        stream: true
      })
    })

    if (!response.ok) {
      let errText = `HTTP ${response.status}`
      try {
        const j = await response.json()
        errText = j?.error?.message || j?.base_resp?.status_msg || JSON.stringify(j) || errText
      } catch {}
      throw new Error(errText)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop()
      for (const line of lines) {
        const trimmed = line.trim()
        if (!trimmed.startsWith('data: ')) continue
        const data = trimmed.slice(6)
        if (data === '[DONE]') break
        try {
          const delta = JSON.parse(data)?.choices?.[0]?.delta?.content
          if (delta) { messages.value[messages.value.length - 1].content += delta; await scrollToBottom() }
        } catch {}
      }
    }
    } // end else (chat mode)
  } catch (err) {
    if (err.name === 'AbortError') {
      const last = messages.value[messages.value.length - 1]
      if (last?.role === 'assistant') {
        last.content = (last.content || '') + '\n\n_（已中止）_'
      }
    } else {
      messages.value[messages.value.length - 1].content = `[错误] ${err.message}`
      errorMsg.value = err.message
    }
  } finally {
    isStreaming.value = false
    abortController.value = null
    await scrollToBottom()
  }
}
</script>
