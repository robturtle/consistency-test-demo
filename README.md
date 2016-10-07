# Distributed System Project

## Prerequisites
```shell
brew install thrift
brew install gradle
```

NOTE for later OS X, `brew` install things on `/usr/local`, which is not in the default GUI application PATH variable. 
To fix this, refer to [this article](http://depressiverobot.com/2016/02/05/intellij-path.html) to set a proper PATH variable
 so that the thrift compiler can be found inside IntelliJ.
 
## Build the project
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

## Compress

```shell
gradle publish
```

The zip file will reside in `build/distributions/`, consists all files in `publish` folder.
