# Comandos

Comando base `/premiumuuid`, alias `/puuid`.

| Permissão | Padrão |
|-----------|--------|
| `premiumuuid.admin` | op |

Quem não tem a permissão recebe o "Unknown or incomplete command" do vanilla e não vê o comando no tab-complete.

| Comando | Descrição |
|---------|-----------|
| `/premiumuuid reload` | Recarrega o `config.yml` sem reiniciar. Avisa se o `cache.file` mudou. |
| `/premiumuuid status <nick>` | Mostra o UUID em cache, se é premium, a última consulta, a idade, se expirou e o override de um jogador. |
| `/premiumuuid clearcache [nick]` | Limpa uma entrada do cache, ou o cache inteiro quando nenhum nick é informado. |
| `/premiumuuid enable <nick>` | Força a checagem premium **ligada** para esse nick, mesmo com `premium-uuid-enabled: false`. |
| `/premiumuuid disable <nick>` | Força a checagem premium **desligada**: o jogador sempre entra com o UUID offline, sem consulta ao cache nem à API. |
| `/premiumuuid reset <nick>` | Remove o override, devolvendo o nick à configuração global (`unset`). |

O tab-complete sugere os subcomandos, os nicks em cache para `status` e `clearcache`, e os nicks com override para `enable`, `disable` e `reset`.

## Exemplo

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
