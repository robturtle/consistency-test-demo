# Distributed System Project

## Prerequisites
- Java 8
- `thrift-compiler`. Please install it with a proper installer. For instance, `brew install thrift` on OS X, or `sudo apt-get install thrift-compiler` on Debian/Ubuntu.

You can use the following command to check their existence:

```shell
▶ java -version
java version "1.8.0_91"
Java(TM) SE Runtime Environment (build 1.8.0_91-b14)
Java HotSpot(TM) 64-Bit Server VM (build 25.91-b14, mixed mode)

▶ thrift -version
Thrift version 0.9.3
```

This project is built by Gradle, other requiring libraries/executables will be auto downloaded while building. The `gradle` itself can also be bootstrapped from the `gradlew` script, which means we do not need to install `gradle` manually.

## Build the project
> *You can safely skip this section if you do not want to build it in IntelliJ*:  
  
> NOTE for later OS X, `brew` install things on `/usr/local`,
which is not in the default GUI application PATH variable. To fix this, refer to
[this article](http://depressiverobot.com/2016/02/05/intellij-path.html) to set a proper PATH variable
 so that the thrift compiler can be found inside IntelliJ.

Building from command line is straight forward:

```shell
git clone https://github.com/robturtle/distributed-system-project.git
cd distributed-system-project
./gradlew build # on *inx, or
./gradlew.bat build # on Windows
```

*NOTE*: If you intend to build the project with your own `gradle` executable, please make sure it's compatible with the `./gradlew -version`:

```shell
./gradlew -version
------------------------------------------------------------
Gradle 2.13
------------------------------------------------------------

Build time:   2016-04-25 04:10:10 UTC
Build number: none
Revision:     3b427b1481e46232107303c90be7b05079b05b1c

Groovy:       2.4.4
Ant:          Apache Ant(TM) version 1.9.6 compiled on June 29 2015
JVM:          1.8.0_91 (Oracle Corporation 25.91-b14)
OS:           Mac OS X 10.11.6 x86_64
```

After the build process, the all-in-one executable jar files and its shortcut startup scripts are reside in `publish` folder.

```shell
cd publish
./server.sh # start the key-value store server
./client.sh # checkout the client syntax
```

## Compress into zip

```shell
gradle publish
```

The zip file will reside in `build/distributions/`, consists all files in `publish` folder.
