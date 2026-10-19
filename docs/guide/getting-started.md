# Getting started

On an offline-mode server, every player gets an **offline UUID** derived from their nickname. That UUID does not match the player's real Minecraft account, so anything tied to the account (inventories, statistics, permissions, data from other plugins) is split from what they have on online-mode servers.

PremiumUUID checks each nickname against the Mojang API at login. When the nickname belongs to an existing premium account, the plugin assigns that account's real Mojang UUID; otherwise the player keeps the offline UUID.

## Important notice

::: danger This plugin does not verify account ownership
It only checks whether a nickname exists as a premium account on Mojang's servers; it performs no authentication of the connecting player. Anyone can join with any premium nickname and receive that account's UUID, along with full access to its inventory, statistics and permissions.

**Use it only on private, trusted servers where you know every player.** It is not a substitute for proper authentication.
:::

## Requirements

- A [supported Paper version](/reference/compatibility) (1.21.1 – 26.3)
- Java 21 or newer
- `online-mode=false` in `server.properties`

## Installation

1. Download the latest jar from [Modrinth](https://modrinth.com/plugin/premiumuuid) or [GitHub Releases](https://github.com/rouri404/premium-uuid/releases/latest).
2. Drop it into your server's `plugins/` folder.
3. Restart the server. This creates `config.yml`, `uuid-cache.yml` and `overrides.yml` in `plugins/PremiumUUID/`.

```bash
curl -LO https://github.com/rouri404/premium-uuid/releases/latest/download/PremiumUUID-1.3.1.jar
mv PremiumUUID-*.jar /path/to/server/plugins/
```

That's it: the plugin is enabled by default and starts resolving premium UUIDs on the next login. No extra configuration is needed.

## Next steps

- [How it works](/guide/how-it-works): what happens at each login.
- [Configuration](/reference/configuration): timeouts, cache and logging.
- [Commands](/reference/commands): inspect players, clear the cache and set overrides.
