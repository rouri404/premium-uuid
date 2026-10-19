# Configuration

File: `plugins/PremiumUUID/config.yml`. It is created with these defaults on the first start:

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
| `premium-uuid-enabled` | Master toggle for premium UUID resolution. [Ban and whitelist handling](/guide/bans-and-whitelist) stays active. | `true` |
| `mojang-api.timeout-ms` | Maximum time for a whole Mojang lookup, in milliseconds. | `3000` |
| `cache.file` | Cache filename inside the plugin folder. Requires a restart. | `uuid-cache.yml` |
| `cache.ttl-minutes` | Time before a cache entry is checked again. `0` or less means entries never expire. | `1440` |
| `fallback.log-warning` | Log a warning when falling back to the offline UUID. | `true` |
| `logging.level` | `INFO` for decisions only, `DEBUG` for API call details. | `INFO` |

## Reloading

`/premiumuuid reload` applies every setting immediately, except `cache.file`, which is only read at startup. The command warns you when `cache.file` changed.
