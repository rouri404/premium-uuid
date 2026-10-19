# Commands

Base command `/premiumuuid`, alias `/puuid`.

| Permission | Default |
|------------|---------|
| `premiumuuid.admin` | op |

Players without the permission get the vanilla "Unknown or incomplete command" and do not see the command in tab-complete.

| Command | Description |
|---------|-------------|
| `/premiumuuid reload` | Reloads `config.yml` without a restart. Warns if `cache.file` changed. |
| `/premiumuuid status <nick>` | Shows the cached UUID, premium state, last check, age, expiry and override for a player. |
| `/premiumuuid clearcache [nick]` | Clears one cache entry, or the whole cache when no nickname is given. |
| `/premiumuuid enable <nick>` | Forces the premium check **on** for that nickname, even with `premium-uuid-enabled: false`. |
| `/premiumuuid disable <nick>` | Forces the premium check **off**: the player always joins with the offline UUID, with no cache lookup or API call. |
| `/premiumuuid reset <nick>` | Removes the override, returning the nickname to the global setting (`unset`). |

Tab-complete suggests the subcommands, cached nicknames for `status` and `clearcache`, and nicknames with overrides for `enable`, `disable` and `reset`.

## Example

```text
> /puuid status Notch
─── Cache: Notch ───
  UUID: 069a79f4-44e9-4726-a5be-fca90e38aaf5
  Premium: true
  Last checked: 2026-10-18 21:42:07 UTC
  Age: 12m ago
  Expired: false
  Override: unset
```
