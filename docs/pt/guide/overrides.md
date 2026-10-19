# Overrides

Um override força a checagem premium ligada ou desligada para um nick, independente da configuração global `premium-uuid-enabled`.

| Override | Efeito |
|----------|--------|
| `enable` (`true`) | Sempre faz a checagem premium, mesmo com `premium-uuid-enabled: false`. |
| `disable` (`false`) | Nunca faz: o jogador sempre entra com o UUID offline, sem consulta ao cache nem à API. |
| `unset` | Sem override; vale a configuração global. |

## Gerenciando overrides

```text
/premiumuuid enable <nick>
/premiumuuid disable <nick>
/premiumuuid reset <nick>
/premiumuuid status <nick>
```

O `status` mostra o override atual como `active`, `inactive` ou `unset`. Veja [Comandos](/pt/reference/commands).

## O arquivo de overrides

Os overrides ficam em `plugins/PremiumUUID/overrides.yml` e sobrevivem a reinícios. Você também pode editar o arquivo com o servidor desligado:

```yaml
overrides:
  steve: true    # sempre faz a checagem premium
  alex: false    # sempre usa o UUID offline
```

- Os nicks são gravados em minúsculas.
- Os comandos são idempotentes: habilitar um nick já habilitado só confirma o estado.
- O jogador não precisa ter entrado antes, então dá para configurar com antecedência.

## Precedência no login

1. O override sempre vence.
2. `premium-uuid-enabled` só vale para nicks sem override.
