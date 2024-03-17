# jvm-memcached-tool

This is a practice project to learn how to use Quarkus, the Supersonic Subatomic Java Framework, with Picocli.

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
  generate, gen  Generate items on memcached!
  dump
  keys
  stats          Perform stats command
  flush          Flush items on memcached
```

## Development

TBD