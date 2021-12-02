set -e
SCRIPT_PATH="$(cd $(dirname $0) && pwd)"

# Exclude lint tasks, as those are covered by the
# androidx-studio-integration-lint.sh script
$SCRIPT_PATH/impl/build-studio-and-androidx.sh \
  -Pandroidx.allWarningsAsErrors \
  -Pandroidx.verifyUpToDate \
  -x lint \
  -x lintDebug \
  -x lintReport \
  -x verifyDependencyVersions \
  listTaskOutputs \
  bOS \
  --stacktrace
