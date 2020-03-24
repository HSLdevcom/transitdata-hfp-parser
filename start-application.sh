#!/bin/bash

if [[ "${DEBUG_ENABLED}" = true ]]; then
  java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar /usr/app/transitdata-hfp-parser.jar
else
  java -jar /usr/app/transitdata-hfp-parser.jar
fi
