# Configuração

Arquivo: `plugins/PremiumUUID/config.yml`. Ele é criado com estes valores padrão na primeira inicialização:

```yaml
premium-uuid-enabled: true

mojang-api:
  timeout-ms: 3000

cache:
  file: "uuid-cache.yml"
  ttl-minutes: 1440       # 24 horas

fallback:
  log-warning: true

logging:
  level: "INFO"           # INFO | DEBUG
```

| Chave | Descrição | Padrão |
|-------|-----------|--------|
| `premium-uuid-enabled` | Toggle principal da resolução de UUID premium. O [tratamento de ban e whitelist](/pt/guide/bans-and-whitelist) continua ativo. | `true` |
| `mojang-api.timeout-ms` | Tempo máximo de uma consulta completa à Mojang, em milissegundos. | `3000` |
| `cache.file` | Nome do arquivo de cache dentro da pasta do plugin. Exige reinício. | `uuid-cache.yml` |
| `cache.ttl-minutes` | Tempo até uma entrada do cache ser consultada de novo. `0` ou menos significa que nunca expira. | `1440` |
| `fallback.log-warning` | Registra um aviso no log ao cair no UUID offline. | `true` |
| `logging.level` | `INFO` só para decisões, `DEBUG` para detalhes das chamadas à API. | `INFO` |

## Recarregando

O `/premiumuuid reload` aplica todas as configurações na hora, menos o `cache.file`, que só é lido na inicialização. O comando avisa quando o `cache.file` mudou.
