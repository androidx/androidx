set -e
SCRIPT_PATH="$(cd $(dirname $0) && pwd)"

$SCRIPT_PATH/impl/build-studio-and-androidx.sh \
  --ci \
  -x verifyDependencyVersions \
  lintReportJvm \
  lintReportDebug
