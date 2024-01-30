#!/bin/bash
set -e

if [ -z "$1" ]; then
echo "Specify the previous and the current merged versions. For example: ./mergeAOSP.sh 1.5.1 1.6.0-alpha08"
exit 1
fi

if [ -z "$2" ]; then
echo "Specify the previous and the current merged versions. For example: ./mergeAOSP.sh 1.5.1 1.6.0-alpha08"
exit 1
fi

PREVIOUS_VERSION=$1
CURRENT_VERSION=$2

# create a branch with reverted commits that exists only in the previous version. This can happen often, as release branches can contain cherry-picks from unmerged CL's

CURRENT_COMMIT=$(git rev-parse --abbrev-ref HEAD)
MERGE_BASE=$(git merge-base jetpack-compose/$PREVIOUS_VERSION jetpack-compose/$CURRENT_VERSION)
MERGE_BRANCH=sync-androidx/revert/revert-${PREVIOUS_VERSION}_merge-${CURRENT_VERSION}
git checkout $MERGE_BASE -b $MERGE_BRANCH
PREVIOUS_MERGE_RESULT=$(git merge jetpack-compose/$PREVIOUS_VERSION --no-ff)
if [ "$PREVIOUS_MERGE_RESULT" != "Already up to date." ]; then
git revert HEAD -m 1 --no-edit
fi
git merge jetpack-compose/$CURRENT_VERSION
git checkout $CURRENT_COMMIT

git merge $MERGE_BRANCH --no-commit --no-ff || true
git branch -D $MERGE_BRANCH

git checkout --no-overlay HEAD --ours -- './buildSrc' || true
git checkout --no-overlay HEAD --ours -- './compose/*/build.gradle' || true
git checkout --no-overlay HEAD --ours -- './.github' || true
git checkout --no-overlay HEAD --ours -- './.idea' || true
git checkout --no-overlay HEAD --ours -- './.run' || true
git checkout --no-overlay HEAD --ours -- 'CONTRIBUTING.md' || true
git checkout --no-overlay HEAD --ours -- 'README.md' || true
git checkout --no-overlay HEAD --ours -- 'gradlew' || true
git checkout --no-overlay HEAD --ours -- 'gradlew.bat' || true
git checkout --no-overlay HEAD --ours -- 'gradle.properties' || true
git checkout --no-overlay HEAD --ours -- './gradle/*' || true
git checkout --no-overlay HEAD --ours -- 'build.gradle' || true
git checkout --no-overlay HEAD --ours -- 'settings.gradle' || true
git checkout --no-overlay HEAD --ours -- './collection/*/jvmMain/*' || true
git checkout --no-overlay HEAD --ours -- './collection/*/jvmTest/*' || true
git checkout --no-overlay HEAD --ours -- './compose/*/skikoMain/*' || true
git checkout --no-overlay HEAD --ours -- './compose/*/desktopMain/*' || true
git checkout --no-overlay HEAD --ours -- './compose/*/skikoTest/*' || true
git checkout --no-overlay HEAD --ours -- './compose/*/desktopTest/*' || true
git checkout --no-overlay HEAD --ours -- './compose/desktop/*' || true
# some files are small, and git can thin that some other file was renamed, and we can have conflicts with AOSP
git checkout --no-overlay HEAD --ours -- './compose/mpp/demo/*' || true
git checkout --no-overlay HEAD --theirs -- './busytown/*' || true
git checkout --no-overlay HEAD --theirs -- './buildSrc-tests/*' || true
git checkout --no-overlay HEAD --theirs -- './*/api/*.txt' || true
git checkout --no-overlay HEAD --theirs -- './*/api/*.ignore' || true
git checkout --no-overlay HEAD --theirs -- './compose/material/material/icons/generator/api/**.txt' || true

#material3 only for now, there is a lot of issues with it right now, we'll fix them soon, and we shouldn't reset it after that
git checkout --no-overlay HEAD --ours -- './compose/material3/*' || true