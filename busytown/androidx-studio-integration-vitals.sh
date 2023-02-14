set -e
SCRIPT_PATH="$(cd $(dirname $0) && pwd)"

"$SCRIPT_PATH"/impl/build-studio-and-androidx.sh \
    -Pandroidx.summarizeStderr \
    tasks \
    :navigation:navigation-safe-args-gradle-plugin:test \
    --stacktrace \
    --no-daemon
