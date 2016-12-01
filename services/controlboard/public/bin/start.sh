#!/bin/bash
java -jar /controlboard/libs/bytescheme-controlboard-public-0.0.1-SNAPSHOT.jar --spring.config.location=/controlboard/conf/application.properties  > /controlboard/logs/console.log 2>&1