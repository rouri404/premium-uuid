# premium-uuid

[![GitHub Release](https://img.shields.io/github/v/release/rouri404/premium-uuid?style=plastic&label=Release)](https://github.com/rouri404/premium-uuid/releases)
[![Paper Version](https://img.shields.io/badge/Paper-1.21+-232323?style=plastic)](https://papermc.io/downloads/paper)
[![Mojang API](https://img.shields.io/website?url=https%3A%2F%2Fapi.mojang.com&style=plastic&label=Mojang%20API)](https://api.mojang.com/)
[![License](https://img.shields.io/github/license/rouri404/premium-uuid?style=plastic)](LICENSE) *[Ler em Português](README.pt.md)*


A lightweight Paper plugin that resolves premium (Mojang) UUIDs on `online-mode=false` servers by checking player nicknames against the Mojang API.
## Quick Start

```bash
# 1. Download the latest release
curl -LO https://github.com/rouri404/premium-uuid/releases/latest/download/PremiumUUID-1.0.0.jar

# 2. Move it to your server's plugins folder
mv PremiumUUID-*.jar /path/to/server/plugins/

# 3. Restart the server
```

That's it — the plugin works out of the box with sensible defaults. No extra configuration needed.

## Why?

On offline-mode servers, every player receives an offline UUID, which breaks account continuity for players who own Minecraft. PremiumUUID detects whether a nickname belongs to an existing premium account and, if so, assigns the real Mojang UUID — preserving inventories, stats, and permissions across sessions.

> [!CAUTION]
> **This plugin does NOT verify account ownership.** It only checks whether a nickname exists as a premium account on Mojang — it does not authenticate the player. This means a player using a pirated launcher can log in with any premium nickname and will receive that account's UUID, gaining full access to its inventory, stats, and permissions. **Use this plugin only on private, trusted servers where you know and trust every player.**

## Features

- **Automatic UUID resolution** via `AsyncPlayerPreLoginEvent` (lowest priority)
- **Persistent disk cache** (`uuid-cache.yml`) with configurable TTL
- **Graceful fallback** — API failures never block login; stale cache or offline UUID is used instead
- **Admin commands** — reload config, lookup cache entries, clear cache
- **Zero overhead** when disabled via config

## Configuration

`plugins/PremiumUUID/config.yml`:

```yaml
premium-uuid-enabled: true

mojang-api:
  timeout-ms: 3000

cache:
  file: "uuid-cache.yml"
  ttl-minutes: 1440       # 24 hours

fallback:
  log-warning: true

logging:
  level: "INFO"            # INFO | DEBUG
```

| Key | Description | Default |
|-----|-------------|---------|
| `premium-uuid-enabled` | Master toggle for the plugin logic | `true` |
| `mojang-api.timeout-ms` | HTTP request timeout in milliseconds | `3000` |
| `cache.file` | Cache filename inside the plugin data folder | `uuid-cache.yml` |
| `cache.ttl-minutes` | Time before a cache entry is revalidated | `1440` |
| `fallback.log-warning` | Log a warning when falling back to offline UUID | `true` |
| `logging.level` | `INFO` for decisions only, `DEBUG` for API call details | `INFO` |

## Commands

Base command: `/premiumuuid` (alias: `/puuid`)
Permission: `premiumuuid.admin` (default: op)

| Command | Description |
|---------|-------------|
| `/premiumuuid reload` | Reloads `config.yml` without restarting (useful to hot-swap `DEBUG` mode, timeouts, or disable the plugin) |
| `/premiumuuid lookup <player>` | Shows the cached state for a player |
| `/premiumuuid clearcache [player]` | Clears the entire cache, or a single entry |

## How It Works

```mermaid
flowchart TD
    A["Player joins server"] --> B{"premium-uuid-enabled?"}
    B -- "false" --> C["Do nothing\n(default offline UUID)"]
    B -- "true" --> D{"Valid cache\nentry?"}
    D -- "yes" --> E{"Cached as\npremium?"}
    E -- "yes" --> F["Set real Mojang UUID\nvia PlayerProfile"]
    E -- "no" --> G["Keep offline UUID"]
    D -- "no" --> H["Call Mojang API\nGET /users/profiles/minecraft/name"]
    H --> I{"API response"}
    I -- "HTTP 200" --> J["Premium ✓\nSet real UUID + cache"]
    I -- "HTTP 204 / 404" --> K["Not premium\nKeep offline UUID + cache"]
    I -- "Error / Timeout / 429" --> L{"Stale cache\navailable?"}
    L -- "yes" --> M["Use stale cache\n(even if expired)"]
    L -- "no" --> N["Fallback to\noffline UUID + warn"]

    style A fill:#4a9eff,color:#fff
    style F fill:#2ea043,color:#fff
    style J fill:#2ea043,color:#fff
    style G fill:#6e7681,color:#fff
    style K fill:#6e7681,color:#fff
    style N fill:#d29922,color:#fff
    style M fill:#d29922,color:#fff
    style C fill:#6e7681,color:#fff
```

## Building from Source

```bash
# Requires Java 21+
./gradlew build

# Output: build/libs/PremiumUUID-1.0.0.jar
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE).
