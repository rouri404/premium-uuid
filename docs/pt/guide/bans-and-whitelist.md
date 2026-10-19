# Bans e whitelist

Um jogador pode ter UUIDs diferentes ao longo do tempo: o UUID offline antes de o plugin ser instalado ou enquanto um override está em `disable`, e o UUID premium depois. Os bans e a whitelist do vanilla são ligados a um único UUID, então o PremiumUUID verifica todas as **identidades conhecidas** de um nick:

- o **UUID offline** do nick;
- o **UUID premium** salvo no cache, se o nick for premium;
- o próprio **nick**.

## O que o plugin faz

| Situação | Resultado |
|----------|-----------|
| Alguma identidade conhecida está banida | O login é recusado com o motivo do ban, mesmo que o jogador tenha outro UUID agora. Ninguém burla um ban alternando entre UUID offline e premium. |
| Kick por ban de outra origem | Nada. Bans de IP (`/ban-ip`) e bans de outros plugins são sempre mantidos. |
| Kick da whitelist, mas outra identidade está na whitelist ou é op | O login é liberado. |
| Qualquer outro kick (ex.: servidor cheio) | Nada. O kick e a mensagem originais são mantidos. |

## Pardons

Quando alguém usa `/pardon <nick>` ou `/unban <nick>` (inclusive formas com namespace, como `/minecraft:pardon`), o plugin também perdoa as outras identidades conhecidas desse nick, para que o jogador consiga entrar com qualquer UUID que receber depois.

Isso só acontece quando quem rodou o comando pode desbanir:

- tem a permissão `minecraft.command.pardon` (console e RCON sempre têm), **ou**
- o próprio comando removeu pelo menos um dos bans, o que cobre comandos `/unban` de outros plugins.

Um jogador sem permissão que digitar `/pardon` recebe o erro do vanilla e nada é perdoado.

::: info Continua ativo com o plugin desativado
`premium-uuid-enabled: false` desliga apenas a resolução de UUID. O tratamento de ban, whitelist e pardon continua funcionando, para que jogadores que já receberam um UUID premium não usem a identidade offline para burlar um ban.
:::
