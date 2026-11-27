<script setup>
const props = defineProps({
  list: { type: Object, required: true },
  placeholder: { type: String, default: '' },
  empty: { type: String, required: true },
})
const { search, query, page, total, items, loading, error, pages, load, find } = props.list
</script>

<template>
  <section class="list-frame" aria-label="列表">
    <div class="list-tools">
      <slot name="filters"></slot>
      <form v-if="placeholder" class="search" role="search" @submit.prevent="find">
        <span class="magnifier" aria-hidden="true"></span>
        <input v-model="search" type="search" maxlength="120" :aria-label="placeholder" :placeholder="placeholder" @search="find">
        <button type="submit">搜索</button>
      </form>
      <span class="count">共 <b>{{ total }}</b> 条</span>
    </div>
    <div v-if="error" role="alert" class="list-state"><p>{{ error }}</p><button class="btn" @click="load()">重试</button></div>
    <div v-else-if="loading" class="list-state" role="status">正在读取…</div>
    <div v-else-if="!items.length" class="list-state">
      <span class="empty-folder" aria-hidden="true"></span>
      <p>{{ query ? '没有找到匹配的结果' : empty }}</p>
      <button v-if="query" class="text-button" @click="search = ''; find()">清除搜索</button>
    </div>
    <div v-else class="table-scroll"><slot></slot></div>
    <nav v-if="pages > 1" class="pagination" aria-label="列表分页">
      <button class="btn" :disabled="loading || page === 1" @click="load(page - 1)">上一页</button>
      <span>{{ page }} / {{ pages }}</span>
      <button class="btn" :disabled="loading || page === pages" @click="load(page + 1)">下一页</button>
    </nav>
  </section>
</template>
