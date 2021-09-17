set -e
SCRIPT_PATH="$(cd $(dirname $0) && pwd)"

$SCRIPT_PATH/impl/build-studio-and-androidx.sh \
    --no-daemon \
    --stacktrace \
    --dependency-verification=off \
    -Pandroidx.summarizeStderr \
    -Pandroidx.allWarningsAsErrors \
    tasks
