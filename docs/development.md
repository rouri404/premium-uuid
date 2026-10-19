# Building & contributing

## Building from source

```bash
# Requires Java 21+
./gradlew jar

# Output: build/libs/PremiumUUID-1.3.1.jar
```

## Running the tests

```bash
./gradlew test
```

The end-to-end suite in `e2e/` runs in GitHub Actions: it builds the jar, starts Paper in Docker for each supported version and drives it with [Mineflayer](https://github.com/PrismarineJS/mineflayer) bots.

## Editing this documentation

These pages are Markdown files in the [`docs/`](https://github.com/rouri404/premium-uuid/tree/main/docs) folder, built with [VitePress](https://vitepress.dev). English pages live at the top level and Portuguese pages under `docs/pt/`.

```bash
cd docs
npm install
npm run docs:dev
```

Changes merged into `main` are published automatically.

## Contributing

Bug reports and pull requests are welcome; see [CONTRIBUTING.md](https://github.com/rouri404/premium-uuid/blob/main/CONTRIBUTING.md). PremiumUUID is free software under the [GNU GPL v3](https://github.com/rouri404/premium-uuid/blob/main/LICENSE).
