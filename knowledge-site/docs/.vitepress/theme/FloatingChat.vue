<template>
  <a v-if="!isAiChatPage" class="ds-fab" href="/1225/ai-chat/" title="AI 对话">
    <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"
        stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
    </svg>
    <span class="ds-fab-label">AI 对话</span>
  </a>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vitepress'

const route = useRoute()
const isAiChatPage = computed(() => route.path.includes('/ai-chat'))
</script>

<style scoped>
.ds-fab {
  position: fixed;
  bottom: 32px;
  right: 28px;
  z-index: 999;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 16px 12px 14px;
  background: linear-gradient(135deg, #7c5cfc, #5b9cf6);
  border-radius: 50px;
  text-decoration: none;
  box-shadow: 0 4px 16px rgba(124, 92, 252, 0.4);
  transition: transform 0.2s, box-shadow 0.2s;
}

.ds-fab:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(124, 92, 252, 0.5);
  text-decoration: none;
}

.ds-fab:active {
  transform: translateY(0);
}

.ds-fab svg {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.ds-fab-label {
  font-size: 14px;
  font-weight: 500;
  color: white;
  white-space: nowrap;
}
</style>
