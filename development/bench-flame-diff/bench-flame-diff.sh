#!/usr/bin/env bash

./gradlew --quiet installDist && ./app/build/install/bench-flame-diff/bin/bench-flame-diff "$@"
