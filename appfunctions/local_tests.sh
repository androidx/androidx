#!/bin/bash
# Run this script under frameworks/support/

set -e

./gradlew :appfunctions:appfunctions:connectedAndroidTest
./gradlew :appfunctions:appfunctions:test
./gradlew :appfunctions:appfunctions-service:connectedAndroidTest
./gradlew :appfunctions:appfunctions-service:test
./gradlew :appfunctions:integration-tests:multi-modules-testapp:app:connectedAndroidTest
./gradlew :appfunctions:appfunctions-compiler:test
./gradlew :appfunctions:appfunctions-testing:test