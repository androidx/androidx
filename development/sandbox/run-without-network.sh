# find script (expect it is in frameworks/support/development/sandbox)
SCRIPT_DIR=$(cd "$(dirname $0)" && pwd)

PREBUILTS_DIR=$SCRIPT_DIR/../../../../prebuilts

$PREBUILTS_DIR/build-tools/linux-x86/bin/nsjail \
    --config=$SCRIPT_DIR/nsjail.cfg \
    --chroot=/ \
    --cwd=$(pwd) \
    --rw \
    -- \
    /bin/bash -c "echo EXECUTING OFFLINE: $* && $*"
