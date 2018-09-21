set -e

# parse args
inputFile="$1"
oldVersion="$2"
newVersion="$3"

function usage() {
  echo "re-version-repo.sh takes a .zip of a Maven repo and rewrites the versions inside it to a new version

Usage: re-version-repo.sh <input-zip> <old-version> <new-version>
"
  exit 1
}

if [ "${inputFile}" == "" ]; then
  usage
fi
if [ "${oldVersion}" == "" ]; then
  usage
fi
if [ "${newVersion}" == "" ]; then
  usage
fi

# setup
if stat "${inputFile}" > /dev/null 2>/dev/null; then
  echo
else
  echo "Input file ${inputFile} does not exist"
  exit 1
fi
inputFile="$(readlink -f ${inputFile})"

if echo "${inputFile}" | grep -v "${oldVersion}" >/dev/null; then
  echo "The text '${oldVersion}' does not appear in the name of the file, ${inputFile}."
  echo "This is required as a sanity check (and also facilitates computing the output file name)"
  exit 1
fi
outputFile="$(echo ${inputFile} | sed "s/${oldVersion}/${newVersion}/g")"

tempDir="/tmp/repo"
rm "${tempDir}" -rf
mkdir -p "${tempDir}"
cd "${tempDir}"

# unzip dir
echo
echo unzipping "${inputFile}"
unzippedDir="${tempDir}/unzipped"
unzip -q "${inputFile}" -d "${unzippedDir}"
cd "${unzippedDir}"

# make new dirs for new files
echo
oldDirs="$(find -type d)"
newDirs="$(echo ${oldDirs} | sed "s/${oldVersion}/${newVersion}/g")"
echo "Making new dirs: ${newDirs}"
echo "${newDirs}" | xargs --no-run-if-empty mkdir -p

# move every file
echo
echo moving files
oldFiles="$(find -type f)"
moveCommand=""
for oldFile in ${oldFiles}; do
  if echo "${oldFile}" | grep "${oldVersion}">/dev/null; then
    newFile="$(echo ${oldFile} | sed "s/${oldVersion}/${newVersion}/g")"
    echo "moving ${oldFile} -> ${newFile}"
    mv "${oldFile}" "${newFile}"
  fi
done

# remove old dirs
echo
obsoleteDirs="$(find -type d | grep ${oldVersion} | grep -v ${newVersion} | sort -r)"
echo "Removing dirs: ${obsoleteDirs}"
echo "${obsoleteDirs}" | xargs -n 1 --no-run-if-empty rmdir

# rewrite .pom files
echo
echo rewriting poms
find -name "*.pom" | xargs sed -i "s/${oldVersion}/${newVersion}/g"

# regenerate .md5 and .sha1 files
for f in $(find -type f | grep -v '\.sha1$' | grep -v '\.md5'); do
  md5=$(md5sum $f | sed 's/ .*//')
  sha1=$(sha1sum $f | sed 's/ .*//')

  echo -n $md5 > "${f}.md5"
  echo -n $sha1 > "${f}.sha1"
done


echo
echo rezipping
rm -f "${outputFile}"
zip -qr "${outputFile}" .

echo "Done transforming ${inputFile} (${oldVersion}) ->  ${outputFile} (${newVersion})"
