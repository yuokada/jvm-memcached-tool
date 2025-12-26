# jvm-memcached-tool

## Project Overview

This is a command-line tool for interacting with a Memcached server. It is written in Java using the Quarkus framework and Picocli for command-line argument parsing. The tool provides subcommands for various Memcached operations.

The main entry point of the application is the `io.github.yuokada.memcached.adapter.in.cli.EntryCommand` class. This class defines the main command and its subcommands.

## Building and Running

### Prerequisites

*   JDK 17 or later
*   Maven

### Building the project

To build the project, run the following command:

```bash
./mvnw clean package
```

This will create an executable JAR file in the `target` directory named `memcached-tool.jar`.

### Running the application

To run the application, use the following command:

```bash
java -jar target/memcached-tool.jar
```

You can also run the application in development mode using the following command:

```bash
./mvnw quarkus:dev
```

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
