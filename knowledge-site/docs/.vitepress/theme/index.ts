import DefaultTheme from 'vitepress/theme'
import { h } from 'vue'
import DeepSeekChat from './DeepSeekChat.vue'
import FloatingChat from './FloatingChat.vue'
import './style.css'

export default {
  ...DefaultTheme,
  Layout() {
    return h(DefaultTheme.Layout, null, {
      'layout-bottom': () => h(FloatingChat)
    })
  },
  enhanceApp({ app }) {
    app.component('DeepSeekChat', DeepSeekChat)
  }
}
