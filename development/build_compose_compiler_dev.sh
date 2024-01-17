#
# Copyright 2023 The Android Open Source Project
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

COMPOSE_CUSTOM_VERSION="$1"
COMPOSE_MAVEN_LOCAL="$2"

function usage() {
  echo "usage: $0 <compose-version> <maven-local-output>"
  echo
  echo "Compiles compose compiler and publishes it to the specified local maven folder."
  echo
  exit 1
}

if [ -z "$COMPOSE_CUSTOM_VERSION" ]; then
  usage;
fi

if [ -z "$COMPOSE_MAVEN_LOCAL" ]; then
  usage;
fi

./gradlew \
  -Dmaven.repo.local="$COMPOSE_MAVEN_LOCAL" \
  -Pandroidx.versionExtraCheckEnabled=false \
  :compose:compiler:compiler-hosted:publishToMavenLocal \
  :compose:compiler:compiler:publishToMavenLocal \
  --stacktrace

find "$COMPOSE_MAVEN_LOCAL" | grep "$COMPOSE_CUSTOM_VERSION"

