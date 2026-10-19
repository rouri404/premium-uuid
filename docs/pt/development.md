# Compilar e contribuir

## Compilando do código-fonte

```bash
# Requer Java 21+
./gradlew jar

# Saída: build/libs/PremiumUUID-1.3.1.jar
```

## Rodando os testes

```bash
./gradlew test
```

A suíte end-to-end em `e2e/` roda no GitHub Actions: ela gera o jar, sobe o Paper no Docker para cada versão suportada e o controla com bots do [Mineflayer](https://github.com/PrismarineJS/mineflayer).

## Editando esta documentação

Estas páginas são arquivos Markdown na pasta [`docs/`](https://github.com/rouri404/premium-uuid/tree/main/docs), gerados com o [VitePress](https://vitepress.dev). As páginas em inglês ficam na raiz e as em português em `docs/pt/`.

```bash
cd docs
npm install
npm run docs:dev
```

O que for mergeado na `main` é publicado automaticamente.

## Contribuindo

Relatos de bug e pull requests são bem-vindos; veja o [CONTRIBUTING.md](https://github.com/rouri404/premium-uuid/blob/main/CONTRIBUTING.md). O PremiumUUID é software livre sob a [GNU GPL v3](https://github.com/rouri404/premium-uuid/blob/main/LICENSE).
