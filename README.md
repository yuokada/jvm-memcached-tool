# jvm-memcached-tool
[![Maven Central](https://img.shields.io/maven-central/v/io.github.yuokada/jvm-memcached-tool.svg)](https://central.sonatype.com/artifact/io.github.yuokada/jvm-memcached-tool)
[![Maven Central Last Update](https://img.shields.io/maven-central/last-update/io.github.yuokada/jvm-memcached-tool)](https://central.sonatype.com/artifact/io.github.yuokada/jvm-memcached-tool)
[![javadoc](https://javadoc.io/badge2/io.github.yuokada/jvm-memcached-tool/javadoc.svg)](https://javadoc.io/doc/io.github.yuokada/jvm-memcached-tool)

<!--
![Maven Central Version](https://img.shields.io/maven-central/v/io.github.yuokada/jvm-memcached-tool?link=https%3A%2F%2Fcentral.sonatype.com%2Fartifact%2Fio.github.yuokada%2Fjvm-memcached-tool)
-->
This is a practice project to learn how to use Quarkus, the Supersonic Subatomic Java Framework, with Picocli.

- [Command Mode with Picocli \- Quarkus](https://quarkus.io/guides/picocli)
  - [Command Mode Applications \- Quarkus](https://quarkus.io/guides/command-mode-reference)
- [picocli \- a mighty tiny command line interface](https://picocli.info/)

original: [scripts/memcached-tool](https://github.com/memcached/memcached/blob/1b3b8555734f9b7b8d979924c7f8d6cf82194ba8/scripts/memcached-tool)

## Prerequisite

- JDK 17

## Build

```shell
$ ./mvnw clean package

# the below jar is an executable jar. Please rename it if necessary.
$ ls target/memcached-tool.jar
target/memcached-tool.jar
```

## Run

```shell
$ java -jar target/memcached-tool.jar
Usage: memcached-tool [-hvV] [--host=<configEndpoint>] [-p=<clusterPort>]
                      [COMMAND]
Simple tool to handle memcached
  -h, --help
      --host=<configEndpoint>
                             Cluster hostname.
                               Default: localhost
  -p, --port=<clusterPort>   Cluster port number.
                               Default: 11211
  -v, --verbose              Enable verbose mode.
  -V, --version              print version information and exit
Commands:
  display        Display slab statistics
  generate, gen  Generate items on memcached!
  dump
  keys
  stats          Perform stats command
  sizes          Display item size histogram
  flush          Flush items on memcached
```

## Development

### 📦 Releasing jvm-memcached-tool to Maven Central

This document describes how to release this library to [Maven Central](https://central.sonatype.com/) using `maven-release-plugin` and `central-publishing-maven-plugin`.

---

#### ✅ Prerequisites

- You must have a Sonatype Central Portal account.
- Your project must be registered and approved via OSSRH (JIRA).
- Your `~/.m2/settings.xml` must contain the following credentials:

```xml
<servers>
  <server>
    <id>central</id>
    <username>your-token-username</username>
    <password>your-token-password</password>
  </server>
</servers>
```

```shell
$ mvn -Prelease release:prepare
$ mvn -Prelease release:perform
```

#### (If necessary) When the release commands are failed

```shell
mvn release:clean
git reset --hard
git clean -fd
git tag -d <TAG: 0.2.1>
```
