#
# Copyright (C) 2024 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# ADB push intro copied from lockClocks.sh
if [ "`command -v getprop`" == "" ]; then
    if [ -n "`command -v adb`" ]; then
        echo ""
        echo "Pushing $0 and running it on device..."
        dest=/data/local/tmp/`basename $0`
        adb push $0 ${dest}
        adb shell ${dest} $@
        # adb shell rm ${dest} # will fail, not very important
        exit
    else
        echo "Could not find adb. Options are:"
        echo "  1. Ensure adb is on your \$PATH"
        echo "  2. Manually adb push this script to your device, and run it there"
        exit -1
    fi
fi

DEVICE=`getprop ro.product.device`
echo ""
echo "Rebooting $DEVICE, and resetting animation scales!"
echo "This will re-lock clocks, reenable JIT, and reset animation scale to 1.0"

settings put global window_animation_scale 1.0
settings put global transition_animation_scale 1.0
settings put global animator_duration_scale 1.0

reboot # required to relock clocks, and handles reenabling jit since the property won't persist
