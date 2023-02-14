set -e
SCRIPT_PATH="$(cd $(dirname $0) && pwd)"

# Exclude lint tasks, as those are covered by the
# androidx-studio-integration-lint.sh script
$SCRIPT_PATH/impl/build-studio-and-androidx.sh \
  -x lint \
  -x lintDebug \
  -x lintWithExpandProjectionDebug \
  -x lintWithoutExpandProjectionDebug \
  -x lintWithNullAwareTypeConverterDebug \
  -x lintReport \
  -x verifyDependencyVersions \
  listTaskOutputs \
  bOS
