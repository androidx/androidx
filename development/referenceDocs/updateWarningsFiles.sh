#!/bin/bash
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

# Updates the warnings.ignore files for docs-public and docs-tip-of-tree.

# Usage: `updateWarningsFiles.sh [--build-id ID] [--gc]`

# If `--build-id` is specified, docs logs are downloaded from the androidx_incremental target.
# If `--build-id` is not specified, local copies of the docs logs are used (`./gradlew docs` must
# have been run first).

# By default, new warnings will be added to the ignore files. If `--gc` is specified, old warnings
# will be removed as well.

# Loop through all arguments provided to the script
BUILD_ID=""
GC_FLAG=""
while [[ "$#" -gt 0 ]]; do
  case $1 in
    --build-id)
      if [[ -n "$2" && ! "$2" =~ ^- ]]; then
        BUILD_ID="$2"
        shift 2 # Shift past the flag and the value
      else
        echo "Error: Argument for --build-id is missing"
        exit 1
      fi
      ;;
    --gc)
      GC_FLAG="--gc"
      shift # Shift past the flag
      ;;
    *)
      echo "Error: Unknown option: $1"
      echo "Usage: $0 [--build-id ID] [--gc]"
      exit 1
      ;;
  esac
done

# Common file paths
SCRIPT_DIR="$(cd $(dirname $0) && pwd)"
SUPPORT_ROOT="$(cd $SCRIPT_DIR/../.. && pwd)"
CHECKOUT_ROOT="$(cd $SUPPORT_ROOT/../.. && pwd)"
BUILD_LOG_SIMPLIFIER=$SUPPORT_ROOT/development/build_log_simplifier/build_log_simplifier.py
DOCS_PUBLIC_EXEMPTION_FILE=$SUPPORT_ROOT/docs-public/warnings.ignore
DOCS_TOT_EXEMPTION_FILE=$SUPPORT_ROOT/docs-tip-of-tree/warnings.ignore

# If `--build-id` was specified, download the logs files.
if [[ "$BUILD_ID" != "" ]]; then
  source gbash.sh || { echo "--build-id only works on gLinux and requires running gcert"; exit 1; }

  # Set up a directory to download the files to
  TMP_DIR=$(mktemp -d)
  cd $TMP_DIR

  DOCS_PUBLIC_LOG_FILE=docs-public.log
  DOCS_TOT_LOG_FILE=docs-tip-of-tree.log

  # Download the logs
  echo "Downloading logs from build ID $BUILD_ID"
  FETCH_ARTIFACT=/google/data/ro/projects/android/fetch_artifact
  $FETCH_ARTIFACT --bid $BUILD_ID --target androidx_incremental $DOCS_PUBLIC_LOG_FILE
  $FETCH_ARTIFACT --bid $BUILD_ID --target androidx_incremental $DOCS_TOT_LOG_FILE

  # Verify that at least one log file exists
  if [ ! -f $DOCS_PUBLIC_LOG_FILE ]; then
    echo "Failed to download docs-public.log"
  fi

  if [ ! -f $DOCS_TOT_LOG_FILE ]; then
    echo "Failed to download docs-tip-of-tree.log"
    if [ ! -f $DOCS_PUBLIC_LOG_FILE ]; then
      echo "Exiting because neither log file was downloaded"
      exit 1
    fi
  fi
else
  # No `--build-id` specified, use the local copies
  DOCS_PUBLIC_LOG_FILE=$CHECKOUT_ROOT/out/dist/docs-public.log
  DOCS_TOT_LOG_FILE=$CHECKOUT_ROOT/out/dist/docs-tip-of-tree.log
fi

# Run docs-tip-of-tree update
# Happens before the docs-public update so that new warnings common to both will only be added here
if [ -f $DOCS_TOT_LOG_FILE ]; then
  $BUILD_LOG_SIMPLIFIER $DOCS_TOT_LOG_FILE --update $GC_FLAG \
      --override_exemptions_path $DOCS_TOT_EXEMPTION_FILE
else
  echo "Skipping update for docs-tip-of-tree because the log does not exist"
fi

# Run docs-public update
# The `--override_exemptions_flake_path` is used so that shared warnings are not added to both files
if [ -f $DOCS_PUBLIC_LOG_FILE ]; then
  $BUILD_LOG_SIMPLIFIER $DOCS_PUBLIC_LOG_FILE --update $GC_FLAG \
      --override_exemptions_path $DOCS_PUBLIC_EXEMPTION_FILE \
      --override_exemptions_flake_path $DOCS_TOT_EXEMPTION_FILE
else
  echo "Skipping update for docs-public because the log does not exist"
fi
