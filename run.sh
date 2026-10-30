#!/usr/bin/env sh
set -eu
if [ ! -f .env ]; then echo "Missing .env. Copy .env.example to .env and configure it."; exit 1; fi
set -a
. ./.env
set +a
mvn spring-boot:run
