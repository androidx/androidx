set -e
set -o pipefail

showPresubmit=false
while [ "$1" != "" ]; do
  arg="$1"
  shift
  if [ "$arg" == "--presubmit" ]; then
    showPresubmit=true
    continue
  fi
done

function getFailures() {
  if [ "$showPresubmit" == "true" ]; then
    buildType="pending"
  else
    buildType="submitted"
  fi
  /google/data/ro/projects/android/bass --query="fail" -branch="aosp-androidx-main" -days="30" -successful="false" --build-type="$buildType" 2>&1 | tee /tmp/failure-output
}
getFailures

function stripExtraOutput() {
  cat /tmp/failure-output | grep -v "Branch" | grep -v "Machine Name" | grep -v "Completed At" | grep -v "Snippet" | sed 's/.*BuildID: \([0-9]*\).*/BuildID \1/g' | sed 's/.*Target: \(.*\)/Target: \1/g' | sed 's/.*---.*//' | uniq | tee /tmp/failure-output-reduced
}
stripExtraOutput

function consolidateLines() {
cat /tmp/failure-output-reduced | sed 's/$/ /g' | tr -d '\n' | sed 's/BuildID/\nBuildID/g' | sed 's/BuildID \([0-9]*\) Target: \([^ ]*\) \(.*\)/\3 BuildID \1 \/ \2/g' | tee /tmp/failures
}
consolidateLines

function sortFailures() {
  cat /tmp/failures | sort | tee /tmp/failures-sorted
}
sortFailures

function showMostCommonFailures() {
  echo
  echo Most common build failures:
  cat /tmp/failures-sorted | sed 's/BuildID.*//' | uniq -c | sort -n | tail
}
showMostCommonFailures
