# Transitdata-hfp-parser [![Test and create Docker image](https://github.com/HSLdevcom/transitdata-hfp-parser/actions/workflows/test-and-build.yml/badge.svg)](https://github.com/HSLdevcom/transitdata-hfp-parser/actions/workflows/test-and-build.yml)

## Description

Application for parsing HFP messages or APC messages from MQTT raw messages. Messages are read
from one Pulsar topic and the output is written to another Pulsar topic.

## Building

### Dependencies

This project depends on [transitdata-common](https://github.com/HSLdevcom/transitdata-common) project.

### Locally

- ```mvn compile```  
- ```mvn package```  

### Docker image

- Run [this script](build-image.sh) to build the Docker image

## Running

### Dependencies

* Pulsar

### Environment variables

* `MESSAGE_TYPE`: the type of messages to parse, either `hfp` for HFP messages or `apc` for APC messages
