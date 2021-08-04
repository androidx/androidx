set -e
SCRIPT_PATH="$(cd $(dirname $0) && pwd)"

# runErrorProne is disabled due to I77d9800990e2a46648f7ed2713c54398cd798a0d in AGP
$SCRIPT_PATH/impl/build-studio-and-androidx.sh -Pandroidx.summarizeStderr --no-daemon -Pandroidx.allWarningsAsErrors listTaskOutputs bOS -Pandroidx.verifyUpToDate -x verifyDependencyVersions -x runErrorProne --stacktrace
