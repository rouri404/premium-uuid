# Troubleshooting

## A premium player joined with an offline UUID

Look for `Mojang API check failed` in the console. If the API timed out or rate-limited the server and there was no cache entry yet, the plugin falls back to the offline UUID so the login is not blocked.

1. Run `/premiumuuid clearcache <nick>`.
2. Have the player reconnect once the API responds.

Raising `mojang-api.timeout-ms` can help on slow connections.

## A player lost their inventory after installing the plugin

Their data is stored under the offline UUID they had before. Now that they have a premium UUID, the server looks for a different player file. Either migrate the player data to the new UUID, or keep them on the offline UUID with `/premiumuuid disable <nick>`.

## I changed `cache.file` and nothing happened

The cache file is only read at startup. `/premiumuuid reload` warns about this; restart the server to use the new file.

## A banned player can still join

PremiumUUID enforces bans on every known identity, but only for bans in the vanilla profile ban list. Check that the ban exists with `/banlist`. Bans from other plugins are handled by those plugins.

## How do I see what the plugin is doing?

Set `logging.level: DEBUG` in `config.yml` and run `/premiumuuid reload`. The console then shows every API call, cache decision and ban/whitelist check.

## Reporting a bug

[Open an issue](https://github.com/rouri404/premium-uuid/issues/new) with:

- your Paper and Java versions;
- your `config.yml`;
- the console output with `logging.level: DEBUG`.
