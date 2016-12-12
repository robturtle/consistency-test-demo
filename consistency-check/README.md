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

After the build succeed, the executable jar file resides in `publish` folder, use

```shell
publish/tester.sh -server localhost:9090
```

to run the test upon the server located at `localhost:9090`.

Use

```shell
publish/tester.sh
```

to see the complete available option list.

## Introduction

This module implements a consistency checker specific targeting on the KVStore server which honor this [Thrift interface contract](../thrift-stub/src/main/thrift/kvstore.thrift). The consistency level this checker testing is atomicity. As return value spec is as follows:

| Return value | Meaning                                  |
| ------------ | ---------------------------------------- |
| 0            | It didn't find any inconsistency within the given timeout |
| 1            | It observed inconsistency.               |
| 2            | Other execution error.                   |

### Testing approaches

The program is equived with a hybrid checking algorithm. Mainly there are 2 checking cores:

- [A DAG-based algorithm](https://www.usenix.org/event/hotdep10/tech/full_papers/Anderson.pdf)
- A single thread checker that repeating check if the client read the most recent write

The DAG algorithm is mainly used to bring a strong confidence before we assert the server to be atomic. But since its inconsistent signals are coming from a detection of cycle in the graph, and the connectioness of a directed graph is hard to be computed online and incrementally, the single thread checker is used to fasten the speed of finding inconsistencies in some obvious buggy servers.

The DAG algorithm consists of 2 parts, where part 1 is sending tons of concurrent random read/write requests to the server and log them as a group of [RPCEntry](src/main/java/dsf16/RPCEntry.java). Part 2 will be treating those entries as vertices and adding corresponding time/data/hybrid edges, and finally, detecting cycles. Its implementation resides at [ConsistencyAnalyst](src/main/java/dsf16/ConsistencyAnalyst.java) class.

Currently the DAG algorithm is implemented in time complexity of O(n^2). On a late 2013 Mackbook Pro, 2.4GHz Intel Core i5 CPU, the running time on a 45k vertices graph is around 20 to 40 seconds.

After part 1 of DAG algorithm, the part 2 and the fast checker is fired concurrently if the fast checker is not disabled. And if fast checker is enabled, it will keep working inspite of the DAG algorithm is returned with no  inconsistency found. It will keep running until the given timeout is reached.

## File structure

```shell
src/main/java
├── dsf16
│   ├── ConsistencyAnalyst.java       # The DAG algorithm
│   ├── KVStoreConsistencyTester.java # The command line interface
│   └── RPCEntry.java
└── graph                             # Graph functions
    ├── CycleDetectedException.java
    ├── Graph.java
    └── Vertex.java

2 directories, 6 files
```

### Configuration

The concurrent request sending phase is bound with both time and number constrains. Either timeout or max rending request number is reached will stop the sending phase. Its value can be changed from command line with **-sendtime** and **-n** respectively.

When testing on a remote server the program is suffered with the network traffic and it's harder to make concurrent requests. When testing on a remote server with average round trip 30ms, the throughput dropped dramatically to let the total sent request number rather small. This will lead a high false negative rate for our algorithm. If that's the case, (e.g. the program reported it only sends few thousands of request within 10 seconds), please consider use **-j** option to specify a larger thread numbers so as to achieve a high concurrency.

Furthermore, if you wish to run the progam in a longer time, use **-timeout** option.