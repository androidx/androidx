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

git checkout --no-overlay HEAD --ours -- './buildSrc'
git checkout --no-overlay HEAD --ours -- './compose/*/build.gradle'
git checkout --no-overlay HEAD --ours -- './.github'
git checkout --no-overlay HEAD --ours -- './.idea'
git checkout --no-overlay HEAD --ours -- './.run'
git checkout --no-overlay HEAD --ours -- 'CONTRIBUTING.md'
git checkout --no-overlay HEAD --ours -- 'README.md'
git checkout --no-overlay HEAD --ours -- 'gradlew'
git checkout --no-overlay HEAD --ours -- 'gradlew.bat'
git checkout --no-overlay HEAD --ours -- 'gradle.properties'
git checkout --no-overlay HEAD --ours -- './gradle/*'
git checkout --no-overlay HEAD --ours -- 'build.gradle'
git checkout --no-overlay HEAD --ours -- 'settings.gradle'
git checkout --no-overlay HEAD --ours -- './collection/*/skikoMain/*'
git checkout --no-overlay HEAD --ours -- './collection/*/desktopMain/*'
git checkout --no-overlay HEAD --ours -- './collection/*/jvmMain/*'
git checkout --no-overlay HEAD --ours -- './collection/*/skikoTest/*'
git checkout --no-overlay HEAD --ours -- './collection/*/desktopTest/*'
git checkout --no-overlay HEAD --ours -- './collection/*/jvmTest/*'
git checkout --no-overlay HEAD --ours -- './compose/*/skikoMain/*'
git checkout --no-overlay HEAD --ours -- './compose/*/desktopMain/*'
git checkout --no-overlay HEAD --ours -- './compose/*/skikoTest/*'
git checkout --no-overlay HEAD --ours -- './compose/*/desktopTest/*'
git checkout --no-overlay HEAD --ours -- './compose/desktop/*'
git checkout --no-overlay HEAD --theirs -- './*/api/*.txt'
git checkout --no-overlay HEAD --theirs -- './*/api/*current.ignore'

#material3 only for now, there is a lot of issues with it right now, we'll fix them soon, and we shouldn't reset it after that
git checkout --no-overlay HEAD --ours -- './compose/material3/*'