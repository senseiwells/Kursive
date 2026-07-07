import { defineConfig } from 'vitepress'

// https://vitepress.dev/reference/site-config
export default defineConfig({
  title: "Kursive",
  description: "Docs for the Kursive scripting mod",
  head: [
    ['link', { rel: 'icon', type: 'image/png', href: '/logo.png' }]
  ],
  themeConfig: {
    // https://vitepress.dev/reference/default-theme-config

    logo: '/logo.png',

    nav: [
      { text: 'Home', link: '/' },
      { text: 'Develop', link: '/develop/' },
      { text: 'Use', link: '/use/' }
    ],

    sidebar: [
      {
        text: 'Developing Scripts',
        link: '/develop/',
        items: [
          { text: 'Creating Scripts', link: '/develop/creating-scripts' }
        ]
      },
      {
        text: 'Using Kursive',
        link: '/use/',
        items: [
          { text: 'Running Scripts', link: '/use/running-scripts' }
        ]
      }
    ],

    socialLinks: [
      { icon: 'modrinth', link: 'https://modrinth.com/mod/kursive' },
      { icon: 'github', link: 'https://github.com/senseiwells/Kursive' }
    ]
  }
})
