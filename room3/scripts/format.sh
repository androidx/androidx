#!/bin/bash

# This script runs the ktformat task on all Room projects.

./gradlew \
  :room3:room3-benchmark:ktformat \
  :room3:room3-common:ktformat \
  :room3:room3-compiler:ktformat \
  :room3:room3-compiler-processing:ktformat \
  :room3:room3-compiler-processing-testing:ktformat \
  :room3:room3-external-antlr:ktformat \
  :room3:room3-guava:ktformat \
  :room3:room3-gradle-plugin:ktformat \
  :room3:room3-ktx:ktformat \
  :room3:room3-migration:ktformat \
  :room3:room3-paging:ktformat \
  :room3:room3-paging-guava:ktformat \
  :room3:room3-paging-rxjava2:ktformat \
  :room3:room3-paging-rxjava3:ktformat \
  :room3:room3-runtime:ktformat \
  :room3:room3-rxjava2:ktformat \
  :room3:room3-rxjava3:ktformat \
  :room3:room3-sqlite-wrapper:ktformat \
  :room3:room3-testing:ktformat