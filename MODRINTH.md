# PremiumUUID

A lightweight Paper plugin that resolves premium (Mojang) UUIDs on `online-mode=false` servers, matching player nicknames against the Mojang API.

On offline-mode servers, every player is assigned an offline UUID, which breaks account continuity for players who own a legitimate copy of Minecraft — inventories, statistics, and permissions all remain tied to a UUID that doesn't correspond to their real account. PremiumUUID detects when a nickname belongs to an existing premium account and, when it does, assigns that account's real Mojang UUID at login, preserving continuity across sessions.

## IMPORTANT NOTICE

**This plugin does not verify account ownership.** It only checks whether a nickname exists as a premium account on Mojang's servers, it performs no authentication of the connecting player. As a result, any unauthenticated player can join with any premium nickname and will be granted that account's UUID, along with full access to its inventory, statistics, and permissions.

**This plugin is intended for private, trusted servers where every player is known.** It is not a substitute for proper authentication and should not be used on public or semi-public servers.

## Quick Start

1. Download and drop the jar into your server's `plugins/` folder.
2. Make sure the server runs in offline mode (`online-mode=false` in `server.properties`), this plugin only makes sense on offline-mode servers.
3. Restart the server. This generates `config.yml`, `uuid-cache.yml`, and `overrides.yml` inside `plugins/PremiumUUID/`.

## Features

- **Automatic UUID resolution** via `AsyncPlayerPreLoginEvent`, registered at lowest priority
- **Persistent disk cache** (`uuid-cache.yml`) with a configurable TTL
- **Per-nickname overrides** (`overrides.yml`) to force the premium check on or off for a specific nickname, independent of the global setting
- **Graceful degradation** — API failures never block a login; a stale cache entry or the offline UUID is used as a fallback. Lookups are bounded by `timeout-ms`, with a back-off after rate limits and failed lookups
- **Administrative commands** for reloading configuration, inspecting player status, clearing the cache, and managing overrides
- **Identity-aware bans and whitelist** — bans on any known identity of a nickname are enforced, whitelist kicks are lifted when another identity is whitelisted or op, and an authorized `/pardon` or `/unban` clears every identity. Bans from other sources, such as IP bans or other plugins, are never lifted

Setting `premium-uuid-enabled: false` stops premium UUID resolution only; the ban, whitelist and pardon handling stays active so earlier premium identities cannot be used to evade bans.

That's it — the plugin is enabled by default and starts resolving premium UUIDs on the next login.

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
  level: "INFO"           # INFO | DEBUG
```

| Key | Description | Default |
|-----|-------------|---------|
| `premium-uuid-enabled` | Master toggle for premium UUID resolution (ban/whitelist handling stays active) | `true` |
| `mojang-api.timeout-ms` | Maximum time for a whole Mojang lookup, in milliseconds | `3000` |
| `cache.file` | Cache filename within the plugin's data folder (requires a restart) | `uuid-cache.yml` |
| `cache.ttl-minutes` | Time before a cache entry is revalidated | `1440` |
| `fallback.log-warning` | Log a warning when falling back to the offline UUID | `true` |
| `logging.level` | `INFO` for high-level decisions, `DEBUG` for API call details | `INFO` |

## Commands

Base command: `/premiumuuid` (alias: `/puuid`)
Permission: `premiumuuid.admin` (default: op)

| Command | Description |
|---------|-------------|
| `/premiumuuid reload` | Reloads `config.yml` without a server restart |
| `/premiumuuid status <nick>` | Displays the cached state and individual override for a player |
| `/premiumuuid clearcache [nick]` | Clears the entire UUID cache, or a single entry |
| `/premiumuuid enable <nick>` | Forces the premium check **on** for that nickname, even when `premium-uuid-enabled` is `false` |
| `/premiumuuid disable <nick>` | Forces the premium check **off** for that nickname, even when `premium-uuid-enabled` is `true` — the player always logs in with an offline UUID, with no cache lookup or API call |
| `/premiumuuid reset <nick>` | Removes any override for that nickname, resetting it to default (`unset`) |

### Per-nickname overrides

Overrides are stored in `plugins/PremiumUUID/overrides.yml` and persist across restarts:

```yaml
overrides:
  steve: true    # always runs the premium check
  alex: false    # always uses the offline UUID
```

Nicknames are normalized to lowercase before being stored. Commands are idempotent, and a player does not need to have joined the server before for an override to be pre-configured on their behalf.

**Precedence at login:**

1. An individual override in `overrides.yml` takes priority over everything else.
2. The global `premium-uuid-enabled` setting applies only when no override is present.
