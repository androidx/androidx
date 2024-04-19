#!/usr/bin/env bash

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")"; pwd)"
pushd "$script_dir" >/dev/null

./gradlew --quiet installDist && ./app/build/install/bench-flame-diff/bin/bench-flame-diff "$@"
exit_code=$?

popd >/dev/null

exit $exit_code
