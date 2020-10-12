# Partner Register

## Stack

Language : Kotlin

Framework: Ktor

Database: Elasticsearch

## How to run

We created a run.sh script to execute service

```
# on root folder
./run.sh
```

### Dependencies

To be enable to run the service we need has installed

- docker
- docker-compose

## Comments

In order to run **unit tests** you need previously start elasticsearch container

```
# running just elasticsearch
docker-compose up elasticsearch
```