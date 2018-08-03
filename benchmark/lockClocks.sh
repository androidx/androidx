#
# Copyright (C) 2018 The Android Open Source Project
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

# This script can be used to lock device clocks to stable levels for comparing
# different versions of software.  Since the clock levels are not necessarily
# indicative of real world behavior, this should **never** be used to compare
# performance between different device models.

if [ "`command -v getprop`" == "" ]; then
  echo ""
  echo "This script should run on your Android device, not on your computer."
  echo "use './gradlew lockClocks', or adb push it and then run on device"
  exit -1
fi

set -e

device=`getprop ro.product.device`

echo "it's time to lock some clocks"

# require root
if [ "`id -u`" -ne "0" ]; then
  echo "Not running as root, cannot lock clocks, aborting"
  exit -1
fi


if [ $device == "walleye" ] || [ $device == "taimen" ]; then
  stop thermal-engine
  stop perfd
  stop vendor.thermal-engine
  stop vendor.perfd

  cpubase=/sys/devices/system/cpu
  gov=cpufreq/scaling_governor

  cpu=4
  top=8

  # Enable the gold cores at medium-high frequency.
  # 1248000 1344000 1478400 1555200 1900800 2457600
  S=1555200

  while [ $((cpu < $top)) -eq 1 ]; do
    echo 1 > $cpubase/cpu${cpu}/online
    echo userspace > $cpubase/cpu${cpu}/$gov
    echo $S > $cpubase/cpu${cpu}/cpufreq/scaling_max_freq
    echo $S > $cpubase/cpu${cpu}/cpufreq/scaling_min_freq
    echo $S > $cpubase/cpu${cpu}/cpufreq/scaling_setspeed
    cpu=$(($cpu + 1))
  done

  cpu=0
  top=4
  # Disable the silver cores.
  while [ $((cpu < $top)) -eq 1 ]; do
    echo 0 > $cpubase/cpu${cpu}/online
    cpu=$(($cpu + 1))
  done

  ## TODO: had trouble with locking GPU clocks
  ## on walleye/taimen, punting on this for now

elif [ $device == "marlin" ] || [ $device == "sailfish" ]; then

  stop thermal-engine
  stop perfd
  stop vendor.thermal-engine
  stop vendor.perfd

  echo 0 > /sys/devices/system/cpu/cpu0/online
  echo 0 > /sys/devices/system/cpu/cpu1/online

  echo performance  > /sys/devices/system/cpu/cpu2/cpufreq/scaling_governor
  echo 2150400 > /sys/devices/system/cpu/cpu2/cpufreq/scaling_max_freq

  echo 13763 > /sys/class/devfreq/soc:qcom,gpubw/max_freq
  echo performance > /sys/class/kgsl/kgsl-3d0/devfreq/governor
  echo -n 624000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
else
  model=`getprop ro.product.model`
  echo "This device - $model ($device) - is not currently supported"
  exit -1
fi

echo "$device clocks have been locked - to reset, reboot the device"

