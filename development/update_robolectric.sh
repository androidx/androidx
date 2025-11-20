#!/bin/bash
set -e

ARTIFACTS_TO_DOWNLOAD="org.robolectric:robolectric:4.16-beta-1,"

# Latest versions from https://mvnrepository.com/artifact/org.robolectric/android-all-instrumented
declare -a arr=("16-robolectric-13921718-i7" "15-robolectric-12714715-i7" "14-robolectric-10818077-i7" "13-robolectric-9030017-i7" "12.1-robolectric-8229987-i7" "12-robolectric-7732740-i7" "11-robolectric-6757853-i7" "10-robolectric-5803371-i7" "9-robolectric-4913185-2-i7" "8.1.0-robolectric-4611349-i7" "8.0.0_r4-robolectric-r1-i7" "7.1.0_r7-robolectric-r1-i7" "7.0.0_r1-robolectric-r1-i7" "6.0.1_r3-robolectric-r1-i7")

for i in "${arr[@]}"
do
    ARTIFACTS_TO_DOWNLOAD+="org.robolectric:android-all-instrumented:$i,"
done

INSTRUMENTED_DIR="../../prebuilts/androidx/external/org/robolectric/android-all-instrumented"
rm -fr "$INSTRUMENTED_DIR"

./development/importMaven/importMaven.sh "$ARTIFACTS_TO_DOWNLOAD"

# androidx wants to avoid robolectric fetching Android images from the network,
# so we set `robolectric.dependency.dir` to
# `androidx/external/org/robolectric/android-all-instrumented`. Sadly, that does
# not work out of the box, so we add symlinks so robolectric can find all of these
# system images.

for i in "${arr[@]}"
do
    ln -s -f "$i/android-all-instrumented-$i.jar" \
        "$INSTRUMENTED_DIR/android-all-instrumented-$i.jar"
done
