---
layout: home

hero:
  name: PremiumUUID
  text: Real Mojang UUIDs on offline-mode servers
  tagline: Premium players keep their inventories, stats and permissions on online-mode=false Paper servers.
  actions:
    - theme: brand
      text: Get started
      link: /guide/getting-started
    - theme: alt
      text: Download on Modrinth
      link: https://modrinth.com/plugin/premiumuuid
    - theme: alt
      text: GitHub
      link: https://github.com/rouri404/premium-uuid

features:
  - title: Automatic resolution
    details: Each nickname is checked against the Mojang API at login. Premium accounts get their real UUID; everyone else keeps the offline one.
  - title: Persistent cache
    details: Results are stored in uuid-cache.yml with a configurable TTL, so Mojang is not queried on every join.
  - title: Per-nickname overrides
    details: Force the premium check on or off for a single nickname, regardless of the global setting.
  - title: Never blocks a login
    details: If Mojang is slow, down or rate-limiting, a stale cache entry or the offline UUID is used instead.
  - title: Identity-aware bans
    details: A ban on any known identity of a nickname is enforced, so players cannot evade bans by switching UUIDs.
  - title: Paper 1.21.1 – 26.3
    details: Tested end to end on seven Paper versions on every commit.
---

::: danger Does not verify account ownership
PremiumUUID only checks whether a nickname exists as a premium account. Anyone can join with any premium nickname and get that account's UUID. **Use it only on private, trusted servers.** [Read more](/guide/getting-started#important-notice)
:::
