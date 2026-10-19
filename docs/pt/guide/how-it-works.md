# Como funciona

O plugin decide o UUID de cada jogador no `AsyncPlayerPreLoginEvent` (prioridade mínima), antes de o jogador ser criado. Em cada login, ele segue estes passos e para no primeiro que decide o UUID:

1. **Existe override para este nick?**
   `disable` → UUID offline, sem consulta ao cache nem à API. `enable` → segue para o passo 3, mesmo com o plugin desativado globalmente. Veja [Overrides](/pt/guide/overrides).
2. **`premium-uuid-enabled` está ligado?**
   Só é verificado quando não há override. Se estiver desligado → UUID offline.
3. **Há uma entrada válida no cache?**
   Se o nick foi consultado dentro de `cache.ttl-minutes`, usa a resposta em cache: UUID premium ou UUID offline.
4. **Consulta a API da Mojang.**
   `GET https://api.mojang.com/users/profiles/minecraft/<nick>`
   - HTTP 200 → UUID premium, salvo no cache.
   - HTTP 204 / 404 → UUID offline, salvo no cache.
   - Nicks que não são nomes válidos da Mojang (1 a 16 caracteres entre `A-Z`, `a-z`, `0-9` e `_`) nem chegam a ser consultados e são tratados como não premium.
5. **A API falhou?** (timeout, erro de rede, HTTP 429, resposta malformada)
   Usa a entrada expirada do cache, se houver; senão → UUID offline e um aviso no log.

::: tip O login sempre acontece
Nenhuma falha da API bloqueia o login nem o atrasa além de `mojang-api.timeout-ms`.
:::

## Backoff da API da Mojang

Para não sobrecarregar a API quando ela está com problemas:

| Situação | Comportamento |
|----------|---------------|
| Toda consulta | Limitada por `mojang-api.timeout-ms` do início ao fim, incluindo a leitura da resposta. |
| HTTP 429 (rate limit) | Nenhum nick é consultado por 60 segundos, ou pelo tempo do cabeçalho `Retry-After`. |
| Qualquer outra falha | O mesmo nick só é consultado de novo após 30 segundos, para que reconexões em sequência não batam na API. |

Durante o backoff, os logins usam o cache ou o UUID offline, exatamente como no passo 5.

## Onde os dados ficam

| Arquivo | Conteúdo |
|---------|----------|
| `plugins/PremiumUUID/config.yml` | Configurações do plugin. Veja [Configuração](/pt/reference/configuration). |
| `plugins/PremiumUUID/uuid-cache.yml` | Resultado das consultas por nick: UUID, se é premium e quando foi consultado. Salvo a cada 5 minutos (só quando mudou) e ao desligar o servidor. |
| `plugins/PremiumUUID/overrides.yml` | Overrides por nick. Veja [Overrides](/pt/guide/overrides). |
