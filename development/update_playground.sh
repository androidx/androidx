#!/bin/bash

MODE=${1:-s}

function fn_update_snapshot {
  BUILDID_ANDROIDX=`curl -s https://androidx.dev/snapshots/builds | sed -nr 's|.*snapshots/builds/([0-9]*).*|\1|gp' | head -n 1`
  echo "Updating snapshot id: $BUILDID_ANDROIDX"
  sed -i "s/androidx.playground.snapshotBuildId=[0-9]\+/androidx.playground.snapshotBuildId=$BUILDID_ANDROIDX/g" playground-common/playground.properties
}

function fn_update_metalava {
  BUILDID_METALAVA=`curl -s https://androidx.dev/metalava/builds | sed -nr 's|.*metalava/builds/([0-9]*).*|\1|gp' | head -n 1`
  echo "Updating metalava id: $BUILDID_METALAVA"
  sed -i "s/androidx.playground.metalavaBuildId=[0-9]\+/androidx.playground.metalavaBuildId=$BUILDID_METALAVA/g" playground-common/playground.properties
}

function fn_update_dokka {
  BUILDID_DOKKA=`curl -s https://androidx.dev/dokka/builds | sed -nr 's|.*dokka/builds/([0-9]*).*|\1|gp' | head -n 1`
  echo "Updating dokka id: $BUILDID_DOKKA"
  sed -i "s/androidx.playground.dokkaBuildId=[0-9]\+/androidx.playground.dokkaBuildId=$BUILDID_DOKKA/g" playground-common/playground.properties
}

if [ "$MODE" == "s" ]; then
  fn_update_snapshot
elif [ "$MODE" == "a" ]; then
  fn_update_snapshot
  fn_update_metalava
  fn_update_dokka
fi
