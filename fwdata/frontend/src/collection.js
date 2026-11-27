import { computed, onMounted, onUnmounted, ref } from 'vue'
import { get } from './api.js'

export function useCollection(path, filters = () => ({})) {
  const search = ref('')
  const query = ref('')
  const page = ref(1)
  const pageSize = ref(20)
  const total = ref(0)
  const items = ref([])
  const loading = ref(true)
  const error = ref('')
  const pages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))
  let revision = 0

  async function load(number = page.value) {
    const current = ++revision
    loading.value = true
    error.value = ''
    try {
      const params = new URLSearchParams({ page: String(number), ...filters() })
      if (query.value) params.set('q', query.value)
      const data = await get(path + '?' + params)
      if (current !== revision) return
      pageSize.value = data.pageSize
      total.value = data.total
      if (number > pages.value) return await load(pages.value)
      items.value = data.items
      page.value = data.page
    } catch (e) {
      if (current === revision) error.value = e.message
    } finally {
      if (current === revision) loading.value = false
    }
  }

  function find() {
    query.value = search.value.trim()
    return load(1)
  }

  onMounted(() => load())
  onUnmounted(() => { revision++ })
  return { search, query, page, total, items, loading, error, pages, load, find }
}

const dateFormat = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false,
})
export const formatTime = value => dateFormat.format(new Date(value))
export const roleName = role => role === 'ADMIN' ? '管理员' : '普通用户'
export const kindName = kind => ({ SINGLE: '单选', MULTIPLE: '多选', TEXT: '文本' })[kind]
