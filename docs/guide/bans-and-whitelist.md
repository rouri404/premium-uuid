# Bans & whitelist

A player can end up with different UUIDs over time: the offline UUID before the plugin was installed or while an override is `disable`, and the premium UUID afterwards. Vanilla bans and the whitelist are tied to a single UUID, so PremiumUUID checks every **known identity** of a nickname:

- the **offline UUID** of the nickname;
- the **premium UUID** stored in the cache, if the nickname is premium;
- the **nickname** itself.

## What the plugin does

| Situation | Result |
|-----------|--------|
| Any known identity is banned | The login is rejected with the ban reason, even if the player now has a different UUID. Players cannot evade a ban by switching between offline and premium UUIDs. |
| Kicked as banned by something else | Nothing. IP bans (`/ban-ip`) and bans from other plugins are always kept. |
| Kicked by the whitelist, but another identity is whitelisted or op | The login is allowed. |
| Any other kick (e.g. server full) | Nothing. The original kick and message are kept. |

## Pardons

When `/pardon <nick>` or `/unban <nick>` is used (including namespaced forms such as `/minecraft:pardon`), the plugin also pardons the other known identities of that nickname, so the player can join no matter which UUID they get next.

This only happens when the sender is allowed to unban:

- the sender has the `minecraft.command.pardon` permission (console and RCON always do), **or**
- the command itself lifted at least one of the bans, which covers `/unban` commands from other plugins.

A player without permission who types `/pardon` gets the vanilla error and nothing is pardoned.

::: info Stays active when the plugin is disabled
`premium-uuid-enabled: false` only stops the UUID resolution. The ban, whitelist and pardon handling keeps working, so players who received a premium UUID earlier cannot use their offline identity to evade a ban.
:::
