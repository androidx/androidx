set -e
SCRIPT_PATH="$(cd $(dirname $0) && pwd)"

$SCRIPT_PATH/impl/build-studio-and-androidx.sh -Pandroidx.allWarningsAsErrors listTaskOutputs bOS -Pandroidx.verifyUpToDate -x verifyDependencyVersions --stacktrace
