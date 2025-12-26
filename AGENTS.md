# Repository Guidelines

## Project Overview
This command-line tool interacts with Memcached using Quarkus and Picocli. The main entry point is `io.github.yuokada.memcached.adapter.in.cli.EntryCommand`, which wires top-level options and subcommands for `generate`, `dump`, `keys`, `stats`, and `flush`. The standard Maven layout applies: sources under `src/main/java`, resources in `src/main/resources`, and tests in `src/test/java`. Releases are handled through the `maven-release-plugin`.

## Project Structure & Module Organization
Application code lives under `src/main/java/io/github/yuokada/memcached` and is split into `domain` (aggregates/enums), `application/port|usecase` (ports and use cases), `adapter/in/cli` (Picocli commands), `adapter/out/{memcached,faker}` (external service adapters), and `bootstrap` (DI wiring). Store configuration and assets in `src/main/resources`, keep auxiliary assets such as Compose files under `src/test/resources/compose.yaml` or `misc/`, and ensure build artifacts stay inside `target/`.

## Build, Test, and Development Commands
- `./mvnw clean package` — builds the executable JAR via the Quarkus plugin, producing `target/memcached-tool.jar`.
- `./mvnw test` — runs the Quarkus JUnit 5 suite, matching the CI sanity checks.
- `./mvnw quarkus:dev` — launches Picocli commands in hot-reload development mode.
- `java -jar target/memcached-tool.jar stats --host localhost` — sample runtime check; follow existing Picocli subcommand naming when adding commands.

## Coding Style & Naming Conventions
Assume Java 17, 4-space indentation, and UTF-8. Package names follow `io.github.yuokada.<layer>`: CLI classes in `adapter.in.cli.*Command`, use cases in `application.usecase.*UseCase`, ports in `application.port.*Port`, and implementations in `adapter.out.*`. Provide both long (`--option`) and short (`-o`) Picocli flags, and name classes/methods with verb-first semantics that reflect their responsibility. When adding dependencies, update the central management sections in `pom.xml` to avoid duplicate version declarations.

## Testing Guidelines
Write unit tests with Quarkus JUnit 5 (`@QuarkusTest`) under `src/test/java`, using the `*Test` suffix. If Docker is available, start Memcached with `docker compose -f src/test/resources/compose.yaml up -d` and run integration tests via `./mvnw test -DskipITs=false`. Coverage targets are not enforced, but every new command should have at least one success and one failure-path test.

## Commit & Pull Request Guidelines
Follow the existing Git history: short, imperative commit summaries (for example `Add stats dump command`) and append PR numbers when needed. In PR descriptions, list the intent, notable changes, and executed commands (e.g., ``./mvnw test``); include terminal snippets when they clarify CLI behavior. For release-related work, document the required `~/.m2/settings.xml` context, and provide a migration checklist whenever changes are breaking.

### Available Commands

The following commands are available:

*   `generate`: Generate items on memcached.
*   `dump`: Dumps keys and values from memcached.
*   `keys`: Lists keys from memcached.
*   `stats`: Perform stats command.
*   `flush`: Flush items on memcached.
*   `settings`: Perform stats settings command.
*   `sizes`: Perform stats sizes command.

You can get more information about each command by running the command with the `--help` option. For example:

```bash
java -jar target/memcached-tool.jar generate --help
```

## Development Conventions

The project uses the standard Maven project structure. The source code is located in the `src/main/java` directory and the tests are located in the `src/test/java` directory.

The project uses the `maven-release-plugin` for releasing new versions of the application.