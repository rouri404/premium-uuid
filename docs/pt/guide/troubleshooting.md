# Solução de problemas

## Um jogador premium entrou com UUID offline

Procure por `Mojang API check failed` no console. Se a API deu timeout ou rate limit e ainda não havia entrada no cache, o plugin usa o UUID offline para não bloquear o login.

1. Rode `/premiumuuid clearcache <nick>`.
2. Peça para o jogador reconectar quando a API voltar a responder.

Aumentar `mojang-api.timeout-ms` ajuda em conexões lentas.

## Um jogador perdeu o inventário depois que instalei o plugin

Os dados dele estão salvos sob o UUID offline que ele tinha antes. Agora que ele tem um UUID premium, o servidor procura outro arquivo de jogador. Migre os dados do jogador para o novo UUID, ou mantenha-o no UUID offline com `/premiumuuid disable <nick>`.

## Mudei o `cache.file` e nada aconteceu

O arquivo de cache só é lido na inicialização. O `/premiumuuid reload` avisa sobre isso; reinicie o servidor para usar o novo arquivo.

## Um jogador banido continua entrando

O PremiumUUID aplica os bans em todas as identidades conhecidas, mas só os bans da lista de perfis do vanilla. Confira se o ban existe com `/banlist`. Bans de outros plugins são tratados por esses plugins.

## Como vejo o que o plugin está fazendo?

Coloque `logging.level: DEBUG` no `config.yml` e rode `/premiumuuid reload`. O console passa a mostrar cada chamada à API, decisão de cache e checagem de ban/whitelist.

## Relatando um bug

[Abra uma issue](https://github.com/rouri404/premium-uuid/issues/new) com:

- as versões do Paper e do Java;
- o seu `config.yml`;
- a saída do console com `logging.level: DEBUG`.
