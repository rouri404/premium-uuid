# Compatibilidade

| Plataforma | Status |
|------------|--------|
| Paper 1.21.1 – 26.3 | Suportado. |
| Forks do Paper (ex.: Purpur) | Deve funcionar, pois rodam plugins do Paper. Não testado no CI. |
| Folia | Não suportado: o plugin usa o scheduler do Bukkit. |
| Spigot / Bukkit | Não suportado: o plugin usa APIs exclusivas do Paper. |

Requer **Java 21** ou mais recente.

## Versões testadas

A suíte end-to-end sobe um servidor Paper de verdade com o plugin e conecta bots para testar logins, bans, pardons, whitelist, comandos e overrides. Ela roda a cada commit em:

`1.21.1` · `1.21.4` · `1.21.7` · `1.21.11` · `26.1.2` · `26.2` · `26.3`

## Versões do cliente

A versão do cliente não importa para o plugin. Quais clientes conseguem entrar é decidido pela versão do servidor e por ferramentas como o ViaVersion.
