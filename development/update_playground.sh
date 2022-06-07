#!/bin/bash
MODE=${1:-s}

# Needed for compat between GNU sed (Linux default) and BSD sed (OSX default).
#   - GNU sed requires no space between value for -i
#   - BSD sed requires that a value be passed for -i.
function fn_sed_inplace {
  sed -i.bak "$@" && rm "${@: -1}.bak"
}

function fn_update_snapshot {
  BUILDID_ANDROIDX=`curl -s https://androidx.dev/snapshots/builds | sed -nr 's|.*snapshots/builds/([0-9]*).*|\1|gp' | head -n 1`
  echo "Updating snapshot id: $BUILDID_ANDROIDX"
  fn_sed_inplace "s/androidx.playground.snapshotBuildId=.*/androidx.playground.snapshotBuildId=$BUILDID_ANDROIDX/g" playground-common/playground.properties
}

function fn_update_metalava {
  BUILDID_METALAVA=`curl -s https://androidx.dev/metalava/builds | sed -nr 's|.*metalava/builds/([0-9]*).*|\1|gp' | head -n 1`
  echo "Updating metalava id: $BUILDID_METALAVA"
  fn_sed_inplace "s/androidx.playground.metalavaBuildId=.*/androidx.playground.metalavaBuildId=$BUILDID_METALAVA/g" playground-common/playground.properties
}

function fn_update_dokka {
  BUILDID_DOKKA=`curl -s https://androidx.dev/dokka/builds | sed -nr 's|.*dokka/builds/([0-9]*).*|\1|gp' | head -n 1`
  echo "Updating dokka id: $BUILDID_DOKKA"
  fn_sed_inplace "s/androidx.playground.dokkaBuildId=.*/androidx.playground.dokkaBuildId=$BUILDID_DOKKA/g" playground-common/playground.properties
}

if [ "$MODE" == "s" ]; then
  fn_update_snapshot
elif [ "$MODE" == "a" ]; then
  fn_update_snapshot
  fn_update_metalava
  fn_update_dokka
fi
