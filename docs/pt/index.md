---
layout: home

hero:
  name: PremiumUUID
  text: UUIDs reais da Mojang em servidores offline
  tagline: Jogadores premium mantêm inventário, estatísticas e permissões em servidores Paper com online-mode=false.
  image:
    src: /emerald.png
    alt: PremiumUUID
  actions:
    - theme: brand
      text: Primeiros passos
      link: /pt/guide/getting-started
    - theme: alt
      text: Baixar no Modrinth
      link: https://modrinth.com/plugin/premiumuuid
    - theme: alt
      text: GitHub
      link: https://github.com/rouri404/premium-uuid

features:
  - title: Resolução automática
    details: Cada nick é consultado na API da Mojang no login. Contas premium recebem o UUID real; os demais mantêm o UUID offline.
  - title: Cache persistente
    details: Os resultados ficam em uuid-cache.yml com TTL configurável, para não consultar a Mojang a cada entrada.
  - title: Overrides por nick
    details: Força a checagem premium ligada ou desligada para um nick específico, independente da configuração global.
  - title: Nunca bloqueia o login
    details: Se a Mojang estiver lenta, fora do ar ou limitando requisições, usa um cache expirado ou o UUID offline.
  - title: Bans por identidade
    details: Um ban em qualquer identidade conhecida do nick é aplicado, então ninguém burla um ban trocando de UUID.
  - title: Paper 1.21.1 – 26.3
    details: Testado de ponta a ponta em sete versões do Paper a cada commit.
---

::: danger Não verifica a posse da conta
O PremiumUUID só confere se um nick existe como conta premium. Qualquer pessoa pode entrar com qualquer nick premium e receber o UUID dessa conta. **Use apenas em servidores privados e confiáveis.** [Saiba mais](/pt/guide/getting-started#aviso-importante)
:::
