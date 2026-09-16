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
          { text: 'Client vs Server', link: '/develop/client-vs-server' },
          { text: 'Creating Scripts', link: '/develop/creating-scripts' },
          {
            text: 'Client Scripting',
            link: '/develop/client/',
            items: [
              { text: 'Events', link: '/develop/client/events' },
              { text: 'Keybinds', link: '/develop/client/keybinds' },
              { text: 'Persistent Data', link: '/develop/common/persistent-data' },
            ]
          },
          {
            text: 'Server Scripting',
            link: '/develop/server/',
            items: [
              { text: 'Events', link: '/develop/server/events' },
              { text: 'Commands', link: '/develop/server/commands' },
              { text: 'Persistent Data', link: '/develop/common/persistent-data' },
            ]
          }
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
      { icon: 'github', link: 'https://github.com/senseiwells/Kursive' },
      { icon: 'discord', link: 'https://discord.gg/7R9SfktZxH' }
    ]
  }
})
