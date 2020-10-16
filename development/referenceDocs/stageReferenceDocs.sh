#!/bin/bash
set -e

cd "$(dirname $0)"

# Save current working directory
scriptDirectory=$(pwd)

if [[ -z "$1" ]]; then
      printf "Please supply a buildID from the android build server\n"
      exit
fi

buildId=$1

if [[ -z "$2" ]]; then
      printf "Please supply a Compose buildID from the android build server\n"
      exit
fi

composeBuildId=$2

newDir="reference-docs"
# Remove the existing out directory to avoid conflicts from previous runs
rm -rf out
mkdir -p out/$newDir
cd out/$newDir

dokkaPublicDocsZip="dokkaPublicDocs-${buildId}.zip"
androidxPublicDocsZip="androidx-public-docs-${buildId}.zip"

printf "============================ STEP 1 =============================== \n"
printf "== Downloading the doc zip files from the build server... \n"
printf "== If this script hangs, try running glogin or gcert.\n"
printf "=================================================================== \n"

/google/data/ro/projects/android/fetch_artifact --bid $buildId --target androidx $dokkaPublicDocsZip
/google/data/ro/projects/android/fetch_artifact --bid $buildId --target androidx $androidxPublicDocsZip

printf "============================ STEP 2 =============================== \n"
printf "== Unzip the doc zip files \n"
printf "=================================================================== \n"

unzip $dokkaPublicDocsZip
unzip $androidxPublicDocsZip

printf "============================ STEP 3 =============================== \n"
printf "== Format the doc zip files \n"
printf "=================================================================== \n"

# Remove directories we never publish
rm en -rf
rm reference/android -rf
rm reference/java -rf
rm reference/org -rf
rm reference/hierarchy.html
rm reference/kotlin/org -rf
rm reference/kotlin/android -rf

# Move package list into the correct location
mv reference/kotlin/package-list reference/kotlin/androidx/package-list 

# Remove javascript files that have no use
rm -f reference/androidx/lists.js
rm -f reference/androidx/navtree_data.js

# Remove extraneous _book.yaml that improperly overwrites the correct one
rm -f reference/androidx/_book.yaml

# Remove the version_added labels to the _toc.yaml that don't do anything
sed -i "s/  version_added/# version_added/" reference/androidx/_toc.yaml
sed -i "s/    # version_added/#     version_added/" reference/androidx/_toc.yaml

printf "============================ STEP 4 =============================== \n"
printf "== Stitch the Compose docs to fix the issue with samples \n"
printf "=================================================================== \n"

# Save current working directory
curWorkingDir=$(pwd)

dokkaTipOfTreeDocsZip="dokkaTipOfTreeDocs-$composeBuildId.zip"

newComposeDir="reference-docs-compose-${composeBuildId}"

mkdir -p $scriptDirectory/out/$newComposeDir
cd $scriptDirectory

/google/data/ro/projects/android/fetch_artifact --bid $composeBuildId --target androidx $dokkaTipOfTreeDocsZip
mv $dokkaTipOfTreeDocsZip out/$newComposeDir/.
unzip out/$newComposeDir/$dokkaTipOfTreeDocsZip -d out/$newComposeDir

cp -r out/$newComposeDir/reference/kotlin/androidx/compose out/$newDir/reference/kotlin/androidx/.
cp -r out/$newComposeDir/reference/kotlin/androidx/ui out/$newDir/reference/kotlin/androidx/.

cd $curWorkingDir

printf "============================ STEP 5 =============================== \n"
printf "== Stitch the paging docs to fix the issue with samples \n"
printf "=================================================================== \n"

if [[ ! -z "$3" ]]; then
      # Save current working directory
      curWorkingDir=$(pwd)

      pagingBuildId=$3
      dokkaTipOfTreeDocsZip="dokkaTipOfTreeDocs-$pagingBuildId.zip"

      newPagingDir="reference-docs-paging-${pagingBuildId}"
      mkdir -p $scriptDirectory/out/$newPagingDir
      cd $scriptDirectory

      /google/data/ro/projects/android/fetch_artifact --bid $pagingBuildId --target androidx $dokkaTipOfTreeDocsZip
      mv $dokkaTipOfTreeDocsZip out/$newPagingDir/.
      unzip out/$newPagingDir/$dokkaTipOfTreeDocsZip -d out/$newPagingDir

      cp -r out/$newPagingDir/reference/kotlin/androidx/paging/* out/$newDir/reference/kotlin/androidx/paging/.

      cd $curWorkingDir
fi

printf "============================ STEP 6 =============================== \n"
printf "== Generate the language switcher \n"
printf "=================================================================== \n"

# The switcher script still requires python2 to run correctly
cd reference
python2 ./../../../switcher.py --work androidx

printf "============================ STEP 7 =============================== \n"
printf "== Run the following command to copy the docs into Google3 \n"
printf "=================================================================== \n"

printf "
\`\`\`
g4d -f androidx-ref-docs-stage && \
cd third_party/devsite/android/en/reference && \
g4 sync && \
cp -r $(pwd)/* . && \
/google/data/ro/projects/devsite/two/live/devsite2.par stage androidx && \
/google/data/ro/projects/devsite/two/live/devsite2.par stage kotlin/androidx
\`\`\`\n"

exit
