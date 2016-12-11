## Prerequisites

- JDK 8
- thrift-compiler

## Installation
```shell
git clone https://github.com/robturtle/distributed-system-project.git
cd distributed-system-project
./gradlew :consistency-check:build
```

## Usage
After the build success, the executable jar file resides in `publish` folder, use

```shell
publish/tester.sh -server localhost:9090
```

to run the test upon the server located at `localhost:9090`.

Use

```shell
publish/tester.sh
```

to see the whole available options.
