#
# Copyright 2022 The Android Open Source Project
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

source gbash.sh || exit

readonly nativeDirs=(
)

# Change directory to this script's location and store the directory
cd "$(dirname $0)"
scriptDirectory=$(pwd)


# Working directories for the refdocs
readonly outDir="$scriptDirectory/out"
readonly doxygenDir="reference-docs-doxygen"

# Remove and recreate the existing out directory to avoid conflicts from previous runs
rm -rf $outDir/$doxygenDir
mkdir -p $outDir/$doxygenDir
cd $outDir

# Create tmp client for building c2devsite
client="$(p4 g4d -f androidx-doxgyen-staging)"
cd "$client"
readonly c2devsiteDir=$client/devsite/tools/doxygen/c/

# Revert all local changes to prevent merge conflicts when syncing.
# This is OK since we always want to start with a fresh CitC client
g4 revert ...
g4 sync

# Build c2devsite target.
blaze build devsite/tools/doxygen/c:c2devsite

for dir in "${nativeDirs[@]}"
do
  printf "Generating doxygen docs for $dir\n"

  # clear previous tmp folder and recreate it
  rm -rf $outDir/$doxygenDir/tmp
  mkdir $outDir/$doxygenDir/tmp
  cd $outDir/$doxygenDir/tmp

  # Copy the project we're generating docs for into tmp dir
  cp -a $scriptDirectory/../../$dir ./

  # Copy default configs for c2devsite
  cp $c2devsiteDir/Doxyfile-local ./Doxyfile-local
  cp $c2devsiteDir/config.cfg ./config.cfg

  # Find header files in include directory, separated by spaces instead of newlines
  headers=$(find . -type f -wholename "**/include/**/*.h" -printf "%p ")

  # Edit Doxyfile to include public header files
  sed -i  "s|^INPUT|INPUT = $headers|" ./Doxyfile-local

  # Edit config
  sed -i  "s|^HTML_PATH=.*|HTML_PATH=/reference/androidx/$dir/|" ./config.cfg
  sed -i  "s|^BOOK_PATH=.*|BOOK_PATH=/reference/androidx/|" ./config.cfg
  sed -i  "s|^PROJECT_PATH=.*|PROJECT_PATH=/reference/|" ./config.cfg
  sed -i  "s|^LANGUAGE=.*|LANGUAGE=cplusplus|" ./config.cfg
  sed -i  "s|^PROJECT_TITLE=.*|PROJECT_TITLE=$dir|" ./config.cfg # placeholder
  sed -i  "s|^PROJECT_DESC=.*|PROJECT_DESC=$dir|" ./config.cfg # placeholder

  # Run c2devsite to generate html files
  $client/blaze-bin/devsite/tools/doxygen/c/c2devsite

  # Update generated TOC to nest contents under a 'section'

  # Indent everything
  sed -i "s|^|  |" ./doxhtml/_doxygen.yaml
  # except for the first line
  sed -i "s|  toc:|toc:|" ./doxhtml/_doxygen.yaml
  # append title/section after initial 'toc:'
  sed -i "s|toc:|toc:\\n- title: androidx.$dir\\n  section:|" ./doxhtml/_doxygen.yaml

  # Copy generated docs out of tmp folder
  mkdir -p ../reference/androidx/$dir
  cp -a ./doxhtml/. ../reference/androidx/$dir
done