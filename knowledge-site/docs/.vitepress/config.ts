import { defineConfig } from 'vitepress'

export default defineConfig({
  base: '/1225/',
  title: 'AI 效率工具站',
  description: '精选 AI 工具 & 编程资源，持续更新',
  lang: 'zh-CN',
  lastUpdated: true,

  themeConfig: {
    nav: [
      { text: '首页', link: '/' },
      { text: 'AI 工具', link: '/ai-tools/claude-guide' },
      { text: '编程资源', link: '/coding/scripts' },
      { text: '效率提升', link: '/productivity/' },
    ],

    sidebar: {
      '/ai-tools/': [
        {
          text: 'AI 工具精选',
          items: [
            { text: 'claude 搭建', link: '/ai-tools/claude-guide' },
          ]
        }
      ],
      '/coding/': [
        {
          text: '编程资源',
          items: [
            { text: '后端项目从 0 到 1 搭建指南', link: '/coding/scripts' },
          ]
        }
      ],
      '/productivity/': [
        {
          text: '效率提升',
          items: [
            { text: '概览', link: '/productivity/' },
            { text: 'Mac 效率工具', link: '/productivity/mac' },
            { text: '自动化工作流', link: '/productivity/automation' },
          ]
        }
      ],
    },

    socialLinks: [
      { icon: 'github', link: 'https://github.com' }
    ],

    footer: {
      message: '持续更新中 | 最后更新于 2026 年 4 月',
      copyright: '© 2026 AI 效率工具站'
    },

    search: {
      provider: 'local'
    },

    outline: {
      label: '目录'
    },

    lastUpdatedText: '最后更新',
    docFooter: {
      prev: '上一篇',
      next: '下一篇'
    }
  }
})
