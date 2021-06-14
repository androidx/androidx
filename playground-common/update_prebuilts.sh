#!/bin/bash
# updates the prebuilt versions in playground
PLAYGROUND_REL_PATH=$(dirname $0)
BUILDID_ANDROIDX=`curl -s https://androidx.dev/snapshots/builds | sed -nr 's|.*snapshots/builds/([0-9]*).*|\1|gp' | head -n 1`
BUILDID_METALAVA=`curl -s https://androidx.dev/metalava/builds | sed -nr 's|.*metalava/builds/([0-9]*).*|\1|gp' | head -n 1`
BUILDID_DOKKA=`curl -s https://androidx.dev/dokka/builds | sed -nr 's|.*dokka/builds/([0-9]*).*|\1|gp' | head -n 1`
echo $BUILDID_ANDROIDX
echo $BUILDID_METALAVA
echo $BUILDID_DOKKA
sed -i "s/androidx.playground.snapshotBuildId=[0-9]*/androidx.playground.snapshotBuildId=$BUILDID_ANDROIDX/g" $PLAYGROUND_REL_PATH/playground.properties
sed -i "s/androidx.playground.metalavaBuildId=[0-9]*/androidx.playground.metalavaBuildId=$BUILDID_METALAVA/g" $PLAYGROUND_REL_PATH/playground.properties
sed -i "s/androidx.playground.dokkaBuildId=[0-9]*/androidx.playground.dokkaBuildId=$BUILDID_DOKKA/g" $PLAYGROUND_REL_PATH/playground.properties
