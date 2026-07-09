#!/usr/bin/env bash
#
# Copyright 2026 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# -----------------------------------------------------------------------------
# The AndroidX build environment contract.
#
# This file is defines the JDK, the Android SDK, and output and cache
# directories variables that are used throughout the build.
#
# The definitions are consumed by sourcing this file (gradlew does, so every
# Gradle daemon and every process launched from it - including the managed
# IDEs started inherits the environment)
#
# -----------------------------------------------------------------------------

function androidx_apply_build_environment() {
  local support_root
  support_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)"

  if [ -n "$OUT_DIR" ] ; then
      mkdir -p "$OUT_DIR"
      OUT_DIR="$(cd $OUT_DIR && pwd -P)"
      export TMPDIR="$OUT_DIR/tmp"
  elif [[ $support_root == /google/cog/* ]] ; then
      export OUT_DIR="$HOME/androidxout"
  else
      local checkout_root
      checkout_root="$(cd $support_root/../.. && pwd -P)"
      export OUT_DIR="$checkout_root/out"
  fi
  export GRADLE_USER_HOME="$OUT_DIR/.gradle"
  export KONAN_DATA_DIR="$OUT_DIR/.konan"

  # unset ANDROID_BUILD_TOP so that Lint doesn't think we're building the platform itself
  unset ANDROID_BUILD_TOP

  # Pick the correct fullsdk for this OS.
  local plat="linux"
  case "$(uname)" in
    Darwin* )
      plat="darwin"
      ;;
  esac
  local platform_suffix="x86"
  case "$(arch)" in
    arm64* )
      platform_suffix="arm64"
  esac

  # Tests for lint checks default to using sdk defined by this variable. This removes a lot of
  # setup from each lint module.
  export ANDROID_HOME="$support_root/../../prebuilts/fullsdk-$plat"
  # override JAVA_HOME, because CI machines have it and it points to very old JDK
  export ANDROIDX_JDK21="$support_root/../../prebuilts/jdk/jdk21/$plat-$platform_suffix"
  export JAVA_HOME=$ANDROIDX_JDK21
  export STUDIO_GRADLE_JDK=$JAVA_HOME

  # Warn developers if they try to build top level project without the full checkout
  [ ! -d "$JAVA_HOME" ] && echo "Failed to find: $JAVA_HOME

Typically, this means either:
1. You are using the standalone AndroidX checkout, e.g. GitHub, which only supports
   building a subset of projects. See CONTRIBUTING.md for details.
2. You are using the repo checkout, but the last repo sync failed. Use repo status
   to check for projects which are partially-synced, e.g. showing ***NO BRANCH***." && exit -1

  # Creates/overwrites local.properties with sdk.dir and cmake.dir to avoid invalidating configuration cache
  $support_root/development/write_sdk_path.sh

  ANDROIDX_PROJECT_CACHE_DIR="$OUT_DIR/gradle-project-cache"
}

if [[ "${BASH_SOURCE[0]}" == "$0" ]] ; then
  # Executing this script would apply the contract to a throwaway shell.
  echo "$0 must be sourced, not executed, to apply the AndroidX build environment contract" >&2
  exit 1
fi

androidx_apply_build_environment
