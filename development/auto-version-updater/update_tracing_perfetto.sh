#!/usr/bin/env bash

# Usage:
# ./update_perfetto.sh ANDROIDX_CHECKOUT CURRENT_VERSION NEW_VERSION FORCE_UNSTRIPPED_BINARIES
#
# Example:
# ./update_perfetto.sh ~/android/androidx-main/frameworks/support 1.0.0-alpha04 1.0.0-alpha05 True

set -euo pipefail

ANDROIDX_CHECKOUT="$(cd "$1"; pwd .)" # gets absolute path of root dir
CURRENT_VERSION="$2"
NEW_VERSION="$3"
FORCE_UNSTRIPPED_BINARIES="$4"

/usr/bin/env python3 <<EOF
from update_versions_for_release import update_tracing_perfetto
update_tracing_perfetto('$CURRENT_VERSION', '$NEW_VERSION', '$ANDROIDX_CHECKOUT',
                        $FORCE_UNSTRIPPED_BINARIES)
EOF
