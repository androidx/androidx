set -e
SCRIPT_PATH="$(cd $(dirname $0) && pwd)"

$SCRIPT_PATH/impl/build-metalava-and-androidx.sh \
  -Pandroidx.allWarningsAsErrors \
  -Pandroidx.verifyUpToDate \
  listTaskOutputs \
  checkApi \
  --stacktrace
