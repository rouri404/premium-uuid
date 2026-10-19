# How it works

The plugin decides each player's UUID in `AsyncPlayerPreLoginEvent` (lowest priority), before the player object is created. For every login it goes through these steps and stops at the first one that decides the UUID:

1. **Is there an override for this nickname?**
   `disable` → offline UUID, with no cache lookup or API call. `enable` → continue to step 3, even if the plugin is globally disabled. See [Overrides](/guide/overrides).
2. **Is `premium-uuid-enabled` true?**
   Only checked when there is no override. If it is false → offline UUID.
3. **Is there a valid cache entry?**
   If the nickname was checked within `cache.ttl-minutes`, the cached answer is used: premium UUID or offline UUID.
4. **Ask the Mojang API.**
   `GET https://api.mojang.com/users/profiles/minecraft/<name>`
   - HTTP 200 → premium UUID, saved to the cache.
   - HTTP 204 / 404 → offline UUID, saved to the cache.
   - Nicknames that are not valid Mojang names (1–16 characters of `A-Z`, `a-z`, `0-9`, `_`) skip the request and are treated as not premium.
5. **Did the API fail?** (timeout, network error, HTTP 429, malformed response)
   Use the expired cache entry if there is one; otherwise → offline UUID and a warning in the log.

::: tip The login always goes through
No API failure ever blocks or delays a login beyond `mojang-api.timeout-ms`.
:::

## Backing off from Mojang

To avoid hammering the API when it is struggling:

| Situation | Behaviour |
|-----------|-----------|
| Every lookup | Limited by `mojang-api.timeout-ms` from start to finish, including reading the response. |
| HTTP 429 (rate limited) | No nickname is queried for 60 seconds, or for the time given in the `Retry-After` header. |
| Any other failure | The same nickname is not queried again for 30 seconds, so reconnect spam does not re-hit the API. |

During a back-off, logins use the cache or the offline UUID, exactly as in step 5.

## Where data is stored

| File | Contents |
|------|----------|
| `plugins/PremiumUUID/config.yml` | Plugin settings. See [Configuration](/reference/configuration). |
| `plugins/PremiumUUID/uuid-cache.yml` | Lookup results per nickname: UUID, premium flag and time of the last check. Saved every 5 minutes (only when it changed) and on shutdown. |
| `plugins/PremiumUUID/overrides.yml` | Per-nickname overrides. See [Overrides](/guide/overrides). |
