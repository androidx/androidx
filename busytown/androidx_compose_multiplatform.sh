#!/bin/bash
set -e

cd "$(dirname $0)"

# androidx.sh disables the cache unless explicitly enabled
export USE_ANDROIDX_REMOTE_BUILD_CACHE=gcp
export ANDROIDX_PROJECTS=COMPOSE


# b/235340662 don't verify dependency versions because we cannot pin to multiplatform deps
impl/build.sh buildOnServer createAllArchives checkExternalLicenses listTaskOutputs \
      -Pandroidx.compose.multiplatformEnabled=true \
      -Pandroidx.enableComposeCompilerMetrics=true \
      -Pandroidx.enableComposeCompilerReports=true \
      -Pandroidx.constraints=true \
      --no-daemon \
      --profile \
      compileDebugAndroidTestSources \
      compileDebugSources \
      desktopTestClasses \
      -x verifyDependencyVersions \
      -Pandroidx.enableAffectedModuleDetection=false "$@"
