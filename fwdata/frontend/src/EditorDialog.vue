<script setup>
import { onMounted, ref } from 'vue'
const props = defineProps({ title: { type: String, required: true }, busy: Boolean })
const emit = defineEmits(['close'])
const dialog = ref(null)
onMounted(() => dialog.value.showModal())
function cancel() {
  if (!props.busy) emit('close')
}
</script>

<template>
  <dialog ref="dialog" class="editor" aria-labelledby="dialog-title" @cancel.prevent="cancel">
    <header><h2 id="dialog-title">{{ title }}</h2><button class="close" aria-label="关闭" :disabled="busy" @click="cancel">×</button></header>
    <slot></slot>
  </dialog>
</template>
