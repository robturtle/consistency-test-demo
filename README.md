# Distributed System Project

## Prerequisites
```shell
// For other Linux distro, please install them with proper installer
brew install thrift
brew install gradle
```
## Build the project
> *You can safely skip this section if you do not want to build it in IntelliJ*: 
NOTE for later OS X, `brew` install things on `/usr/local`,
which is not in the default GUI application PATH variable. To fix this, refer to
[this article](http://depressiverobot.com/2016/02/05/intellij-path.html) to set a proper PATH variable
 so that the thrift compiler can be found inside IntelliJ.

Building from command line is straight forward:

```shell
git clone https://github.com/robturtle/distributed-system-project.git
cd distributed-system-project
gradle build
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
