# Compatibility

| Platform | Status |
|----------|--------|
| Paper 1.21.1 – 26.3 | Supported. |
| Paper forks (e.g. Purpur) | Should work, since they run Paper plugins. Not tested in CI. |
| Folia | Not supported: the plugin uses the Bukkit scheduler. |
| Spigot / Bukkit | Not supported: the plugin uses Paper-only APIs. |

Requires **Java 21** or newer.

## Tested versions

The end-to-end suite starts a real Paper server with the plugin and connects bots to check logins, bans, pardons, the whitelist, commands and overrides. It runs on every commit against:

`1.21.1` · `1.21.4` · `1.21.7` · `1.21.11` · `26.1.2` · `26.2` · `26.3`

## Client versions

The client version does not matter to the plugin. Which clients can join is decided by your server version, plus tools such as ViaVersion.
