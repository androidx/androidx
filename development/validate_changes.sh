#!/bin/bash
#
# Copyright 2025 The Android Open Source Project
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
set -euo pipefail

usage() {
  cat << EOF
Usage: $0 [baseCommit] [tasksToRun] [--runOnDependentProjects]

Optional arguments:
  baseCommit                Commit hash to use for the :listAffectedProjects task.
                            If not provided, the task will run with the last merge commit as the baseCommit.
  tasksToRun                Comma-separated list of Gradle tasks to run for each affected project.
                            If not provided, defaults to 'buildOnServer'.
  --runOnDependentProjects  If specified, the tasks will also run on dependent projects.
                            Enabling this flag will increase the processing time.

Examples:
  1. Run with a base commit (e.g. HEAD~1) and a specified list of tasks:
     ./development/validate_changes.sh HEAD~1 bOS,allHostTests

  2. Run with a base commit, a specified list of tasks, and enable running on dependent projects:
     ./development/validate_changes.sh HEAD~1 bOS,allHostTests --runOnDependentProjects

  3. Run with default base commit (last merge commit), specified tasks, and enabling dependent projects:
     ./development/validate_changes.sh "" bOS,allHostTests --runOnDependentProjects

Options:
  --help                  Display this help message and exit.
EOF
}

if [[ "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

# ANSI color codes for error messages
RED='\033[0;31m'
NC='\033[0m'

# Parse arguments
RUN_ON_DEPENDENT_PROJECTS="false"
ARGS=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    --runOnDependentProjects)
      RUN_ON_DEPENDENT_PROJECTS="true"
      shift
      ;;
    --help)
      usage
      exit 0
      ;;
    *)
      ARGS+=("$1")
      shift
      ;;
  esac
done

BASE_COMMIT="${ARGS[0]:-}"
TASKS_TO_RUN="${ARGS[1]:-bOS}"

if [[ "$RUN_ON_DEPENDENT_PROJECTS" != "true" ]]; then
  echo -e "${RED}Dependent projects will NOT be captured. Running with --runOnDependentProjects will\
 run tasks on dependent projects, but it may take more time.${NC}"
fi

REPO_ROOT=$(git rev-parse --show-toplevel)
cd "$REPO_ROOT" || { echo "Error: Unable to change directory to repository root"; exit 1; }

OUTPUT_FILE="$REPO_ROOT/../../out/androidx/build/changedProjects.txt"

gradle_cmd=(./gradlew :listAffectedProjects)
if [[ -n "$BASE_COMMIT" ]]; then
  gradle_cmd+=("--baseCommit=$BASE_COMMIT")
fi
gradle_cmd+=("--tasksToRun=$TASKS_TO_RUN")
if [[ "$RUN_ON_DEPENDENT_PROJECTS" == "true" ]]; then
  gradle_cmd+=("--runOnDependentProjects=true")
fi

echo "Running: ${gradle_cmd[*]}"
"${gradle_cmd[@]}"

if [[ ! -f "$OUTPUT_FILE" ]]; then
  echo -e "${RED}Output file '$OUTPUT_FILE' not found. Exiting.${NC}"
  exit 1
fi

TASKS=$(<"$OUTPUT_FILE")

if [[ -z "$TASKS" ]]; then
  echo -e "${RED}No tasks found in '$OUTPUT_FILE'. Nothing to execute.${NC}"
  exit 0
fi

read -r -a tasks_array <<< "$TASKS"

echo "Running: ./gradlew "${tasks_array[@]}""
PROJECT_PREFIX=: ANDROIDX_PROJECTS=ALL ./gradlew --strict "${tasks_array[@]}"
