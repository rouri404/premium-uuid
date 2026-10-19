import { defineConfig, type DefaultTheme } from 'vitepress'

const REPO = 'https://github.com/rouri404/premium-uuid'
const MODRINTH = 'https://modrinth.com/plugin/premiumuuid'
const VERSION = 'v1.3.1'

function sidebar(prefix: string, t: Record<string, string>): DefaultTheme.SidebarItem[] {
  return [
    {
      text: t.guide,
      items: [
        { text: t.gettingStarted, link: `${prefix}/guide/getting-started` },
        { text: t.howItWorks, link: `${prefix}/guide/how-it-works` },
        { text: t.bans, link: `${prefix}/guide/bans-and-whitelist` },
        { text: t.overrides, link: `${prefix}/guide/overrides` },
        { text: t.troubleshooting, link: `${prefix}/guide/troubleshooting` }
      ]
    },
    {
      text: t.reference,
      items: [
        { text: t.configuration, link: `${prefix}/reference/configuration` },
        { text: t.commands, link: `${prefix}/reference/commands` },
        { text: t.compatibility, link: `${prefix}/reference/compatibility` }
      ]
    },
    {
      text: t.development,
      items: [{ text: t.building, link: `${prefix}/development` }]
    }
  ]
}

function nav(prefix: string, t: Record<string, string>): DefaultTheme.NavItem[] {
  return [
    { text: t.guide, link: `${prefix}/guide/getting-started`, activeMatch: `${prefix}/guide/` },
    { text: t.reference, link: `${prefix}/reference/configuration`, activeMatch: `${prefix}/reference/` },
    {
      text: VERSION,
      items: [
        { text: t.releases, link: `${REPO}/releases` },
        { text: 'Modrinth', link: MODRINTH }
      ]
    }
  ]
}

const en = {
  guide: 'Guide', gettingStarted: 'Getting started', howItWorks: 'How it works', bans: 'Bans & whitelist',
  overrides: 'Overrides', troubleshooting: 'Troubleshooting', reference: 'Reference', configuration: 'Configuration',
  commands: 'Commands', compatibility: 'Compatibility', development: 'Development', building: 'Building & contributing',
  releases: 'Releases & changelog'
}

const pt = {
  guide: 'Guia', gettingStarted: 'Primeiros passos', howItWorks: 'Como funciona', bans: 'Bans e whitelist',
  overrides: 'Overrides', troubleshooting: 'Solução de problemas', reference: 'Referência', configuration: 'Configuração',
  commands: 'Comandos', compatibility: 'Compatibilidade', development: 'Desenvolvimento', building: 'Compilar e contribuir',
  releases: 'Releases e changelog'
}

export default defineConfig({
  title: 'PremiumUUID',
  // Served from the custom domain root: https://premiumuuid.cotoo.dev/
  base: '/',
  cleanUrls: true,
  lastUpdated: true,
  head: [
    ['link', { rel: 'icon', href: '/favicon.png', type: 'image/png' }],
    ['meta', { name: 'theme-color', content: '#007b18' }],
    ['meta', { property: 'og:image', content: 'https://premiumuuid.cotoo.dev/logo.png' }]
  ],

  locales: {
    root: {
      label: 'English',
      lang: 'en',
      description: 'Resolves premium (Mojang) UUIDs on online-mode=false Paper servers.',
      themeConfig: {
        nav: nav('', en),
        sidebar: sidebar('', en),
        editLink: { pattern: `${REPO}/edit/main/docs/:path`, text: 'Edit this page on GitHub' },
        footer: { message: 'Released under the GNU GPL v3.', copyright: 'Copyright © 2026 Gabriel Couto' }
      }
    },
    pt: {
      label: 'Português',
      lang: 'pt-BR',
      link: '/pt/',
      description: 'Resolve UUIDs premium (Mojang) em servidores Paper com online-mode=false.',
      themeConfig: {
        nav: nav('/pt', pt),
        sidebar: sidebar('/pt', pt),
        editLink: { pattern: `${REPO}/edit/main/docs/:path`, text: 'Editar esta página no GitHub' },
        footer: { message: 'Distribuído sob a GNU GPL v3.', copyright: 'Copyright © 2026 Gabriel Couto' },
        outline: { label: 'Nesta página' },
        docFooter: { prev: 'Anterior', next: 'Próxima' },
        lastUpdated: { text: 'Atualizado em' },
        returnToTopLabel: 'Voltar ao topo',
        sidebarMenuLabel: 'Menu',
        darkModeSwitchLabel: 'Tema',
        langMenuLabel: 'Idioma'
      }
    }
  },

  themeConfig: {
    logo: { src: '/emerald.png', alt: 'PremiumUUID' },
    socialLinks: [{ icon: 'github', link: REPO }],
    search: {
      provider: 'local',
      options: {
        locales: {
          pt: {
            translations: {
              button: { buttonText: 'Buscar', buttonAriaLabel: 'Buscar' },
              modal: {
                noResultsText: 'Nenhum resultado para',
                resetButtonTitle: 'Limpar busca',
                footer: { selectText: 'selecionar', navigateText: 'navegar', closeText: 'fechar' }
              }
            }
          }
        }
      }
    }
  }
})
