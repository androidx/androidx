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
        adb shell rm ${dest}
        exit
    else
        echo "Could not find adb. Options are:"
        echo "  1. Ensure adb is on your \$PATH"
        echo "  2. Manually adb push this script to your device, and run it there"
        exit -1
    fi
fi

echo ""

# require root
if [[ `id` != "uid=0"* ]]; then
    echo "Not running as root, cannot disable jit, aborting"
    echo "Run 'adb root' and retry"
    exit -1
fi

setprop dalvik.vm.extra-opts "-Xusejit:false"
stop
start

## Poll for boot animation to start...
echo "  Waiting for boot animation to start..."
while [[ "`getprop init.svc.bootanim`" == "stopped" ]]; do
  sleep 0.1; # frequent polling for boot anim to start, in case it's fast
done

## And then complete
echo "  Waiting for boot animation to stop..."
while [[ "`getprop init.svc.bootanim`" == "running" ]]; do
  sleep 0.5;
done

DEVICE=`getprop ro.product.device`
echo "\nJIT compilation has been disabled on $DEVICE!"
echo "Performance will be terrible for almost everything! (except e.g. AOT benchmarks)"
echo "To reenable it (strongly recommended after benchmarking!!!), reboot or run resetDevice.sh"
