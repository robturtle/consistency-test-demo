#!/usr/bin/env bash
java -Xms1G -Xmx1G -jar `dirname "$0"`/kvserver.jar "$@"
