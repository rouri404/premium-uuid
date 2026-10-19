# Primeiros passos

Em um servidor offline, todo jogador recebe um **UUID offline** derivado do nick. Esse UUID não corresponde à conta real de Minecraft do jogador, então tudo o que é ligado à conta (inventário, estatísticas, permissões, dados de outros plugins) fica separado do que ele tem em servidores online.

O PremiumUUID consulta cada nick na API da Mojang no login. Quando o nick pertence a uma conta premium existente, o plugin atribui o UUID real dessa conta; caso contrário, o jogador mantém o UUID offline.

## Aviso importante

::: danger Este plugin não verifica a posse da conta
Ele só confere se um nick existe como conta premium nos servidores da Mojang; não autentica o jogador que está conectando. Qualquer pessoa pode entrar com qualquer nick premium e receber o UUID dessa conta, com acesso total ao inventário, às estatísticas e às permissões dela.

**Use apenas em servidores privados e confiáveis, onde você conhece todos os jogadores.** Ele não substitui uma autenticação de verdade.
:::

## Requisitos

- Uma [versão suportada do Paper](/pt/reference/compatibility) (1.21.1 – 26.3)
- Java 21 ou mais recente
- `online-mode=false` no `server.properties`

## Instalação

1. Baixe o jar mais recente no [Modrinth](https://modrinth.com/plugin/premiumuuid) ou nas [Releases do GitHub](https://github.com/rouri404/premium-uuid/releases/latest).
2. Coloque-o na pasta `plugins/` do servidor.
3. Reinicie o servidor. Isso cria `config.yml`, `uuid-cache.yml` e `overrides.yml` em `plugins/PremiumUUID/`.

```bash
curl -LO https://github.com/rouri404/premium-uuid/releases/latest/download/PremiumUUID-1.3.1.jar
mv PremiumUUID-*.jar /caminho/do/servidor/plugins/
```

Pronto: o plugin vem habilitado por padrão e começa a resolver UUIDs premium no próximo login. Nenhuma configuração extra é necessária.

## Próximos passos

- [Como funciona](/pt/guide/how-it-works): o que acontece em cada login.
- [Configuração](/pt/reference/configuration): timeouts, cache e logs.
- [Comandos](/pt/reference/commands): inspecionar jogadores, limpar o cache e definir overrides.
