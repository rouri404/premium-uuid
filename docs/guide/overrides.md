# Overrides

An override forces the premium check on or off for one nickname, regardless of the global `premium-uuid-enabled` setting.

| Override | Effect |
|----------|--------|
| `enable` (`true`) | Always runs the premium check, even with `premium-uuid-enabled: false`. |
| `disable` (`false`) | Never runs it: the player always joins with the offline UUID, with no cache lookup or API call. |
| `unset` | No override; the global setting applies. |

## Managing overrides

```text
/premiumuuid enable <nick>
/premiumuuid disable <nick>
/premiumuuid reset <nick>
/premiumuuid status <nick>
```

`status` shows the current override as `active`, `inactive` or `unset`. See [Commands](/reference/commands).

## The overrides file

Overrides live in `plugins/PremiumUUID/overrides.yml` and survive restarts. You can also edit the file while the server is stopped:

```yaml
overrides:
  steve: true    # always runs the premium check
  alex: false    # always uses the offline UUID
```

- Nicknames are stored in lowercase.
- Commands are idempotent: enabling an already enabled nickname just confirms it.
- The player does not need to have joined before, so you can configure them in advance.

## Precedence at login

1. An override always wins.
2. `premium-uuid-enabled` only applies to nicknames without an override.
