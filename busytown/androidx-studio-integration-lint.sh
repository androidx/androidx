set -e
SCRIPT_PATH="$(cd $(dirname $0) && pwd)"

$SCRIPT_PATH/impl/build-studio-and-androidx.sh \
  -Pandroidx.allWarningsAsErrors \
  -Pandroidx.summarizeStderr \
  -Pandroidx.verifyUpToDate \
  -x verifyDependencyVersions \
  lint \
  lintDebug \
  lintReport \
  --no-daemon \
  --stacktrace
