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
# This file defines the JDK, the Android SDK, and output and cache
# directory variables that are used throughout the build.
#
# Consumed by sourcing this file (gradlew sources it so every Gradle daemon
# and managed IDE process inherits this environment).
# -----------------------------------------------------------------------------

# Extracts the session workspace name when running inside a CoG workspace or a
# local Git worktree. Returns an empty string for standard repo checkouts.
function androidx_extract_workspace_name() {
  local support_root="$1"
  case "$support_root" in
    /google/cog/cloud/*/*)
      local rel="${support_root#/google/cog/cloud/*/}"
      echo "${rel%%/*}"
      ;;
    */.worktrees/*)
      local rel="${support_root#*/.worktrees/}"
      echo "${rel%%/*}"
      ;;
    */.system_generated/worktrees/*)
      local rel="${support_root#*/.system_generated/worktrees/}"
      echo "${rel%%/*}"
      ;;
    *)
      echo ""
      ;;
  esac
}

function androidx_apply_build_environment() {
  local support_root="${support_root:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)}"

  local ws_name=""
  if [ -z "$OUT_DIR" ]; then
    ws_name="$(androidx_extract_workspace_name "$support_root")"

    if [[ -n "$ws_name" ]]; then
      # Isolated session workspace (CoG / Git Worktree)
      export OUT_DIR="$HOME/androidxout/$ws_name"
    else
      # Standard repository checkout (Repo / Busytown)
      local checkout_root
      checkout_root="$(cd "$support_root/../.." && pwd -P)"
      export OUT_DIR="$checkout_root/out"
    fi
  fi

  mkdir -p "$OUT_DIR"
  OUT_DIR="$(cd "$OUT_DIR" && pwd -P)"

  if [[ -n "$ws_name" ]]; then
    # Isolated session workspace (CoG / Git Worktree)
    export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/androidxout/.gradle}"
    export KONAN_DATA_DIR="${KONAN_DATA_DIR:-$HOME/androidxout/.konan}"
  else
    # Explicit OUT_DIR or standard repository checkout
    export GRADLE_USER_HOME="$OUT_DIR/.gradle"
    export KONAN_DATA_DIR="$OUT_DIR/.konan"
  fi

  export TMPDIR="$OUT_DIR/tmp"
  mkdir -p "$TMPDIR"

  # Unset ANDROID_BUILD_TOP so Lint does not assume platform build context
  unset ANDROID_BUILD_TOP

  # Pick OS platform and architecture
  local plat="linux"
  [[ "$(uname)" == Darwin* ]] && plat="darwin"

  local platform_suffix="x86"
  [[ "$(arch)" == arm64* ]] && platform_suffix="arm64"

  # Toolchain and SDK environment variables
  export ANDROID_HOME="$support_root/../../prebuilts/fullsdk-$plat"
  export ANDROIDX_JDK21="$support_root/../../prebuilts/jdk/jdk21/$plat-$platform_suffix"
  export JAVA_HOME="$ANDROIDX_JDK21"
  export STUDIO_GRADLE_JDK="$JAVA_HOME"

  # Validate toolchain presence
  if [[ ! -d "$JAVA_HOME" ]]; then
    cat >&2 <<EOF
Failed to find: $JAVA_HOME

Typically, this means either:
1. You are using the standalone AndroidX checkout, e.g. GitHub, which only supports
   building a subset of projects. See CONTRIBUTING.md for details.
2. You are using the repo checkout, but the last repo sync failed. Use repo status
   to check for projects which are partially-synced, e.g. showing ***NO BRANCH***.
EOF
    exit 1
  fi

  # Generate/update local.properties to preserve Gradle configuration cache
  "$support_root/development/write_sdk_path.sh"

  # Google Auth SDK resolves ADC relative to System.getProperty("user.home"). Because gradlew
  # overrides user.home to $GRADLE_USER_HOME, DefaultCredentialsProvider fails to find ADC
  # Fallback to the user's ~/.config ADC file if GOOGLE_APPLICATION_CREDENTIALS is not set.
  if [ -z "$GOOGLE_APPLICATION_CREDENTIALS" ] && [ -f "$HOME/.config/gcloud/application_default_credentials.json" ]; then
    export GOOGLE_APPLICATION_CREDENTIALS="$HOME/.config/gcloud/application_default_credentials.json"
  fi

  export ANDROIDX_PROJECT_CACHE_DIR="$OUT_DIR/gradle-project-cache"
}

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
  echo "$0 must be sourced, not executed, to apply the AndroidX build environment contract" >&2
  exit 1
fi

androidx_apply_build_environment
