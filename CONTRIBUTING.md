# Contributing to PremiumUUID

First off, thanks for taking the time to contribute!

## How to Contribute

### Reporting Bugs

- Open an [issue](https://github.com/rouri404/premium-uuid/issues/new) with a clear title and description.
- Include your Paper version, Java version, and relevant `config.yml` settings.
- Attach any relevant log output (use `logging.level: DEBUG` for detailed logs).

### Suggesting Features

- Check [existing issues](https://github.com/rouri404/premium-uuid/issues) first to avoid duplicates.
- Open an issue describing the feature, the problem it solves, and your proposed approach.

### Submitting Changes

1. Fork the repository.
2. Create a feature branch from `main`:
   ```bash
   git checkout -b feature/my-change
   ```
3. Make your changes — keep commits focused and well-described.
4. Ensure the project builds cleanly:
   ```bash
   ./gradlew build
   ```
5. Open a Pull Request against `main`.

### Code Style

- Java 21, no external dependencies beyond the Paper API.
- Follow the existing code structure and naming conventions.
- Keep changes minimal and scoped — one PR per concern.

### Scope

Please keep in mind the project's [out-of-scope items](README.md#why) before proposing changes. In particular, the following are **not** planned:

- Account ownership verification (authentication/encryption)
- Bedrock / Geyser / Floodgate support
- Anti-squatting protections
- Data migration between UUID states

## License

By contributing, you agree that your contributions will be licensed under the [GNU GPL v3.0](LICENSE).
