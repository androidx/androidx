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
    if [ -n "`command -v adb`" ]; then
        echo Pushing $0 and running it on device
        dest=/data/local/tmp/`basename $0`
        adb push $0 ${dest}
        adb shell ${dest}
        adb shell rm ${dest}
        exit
    else
        echo "Could not find adb. Options are:"
        echo "  1. Ensure adb is on your \$PATH"
        echo "  2. Use './gradlew lockClocks'"
        echo "  3. Manually adb push this script to your device, and run it there"
        exit -1
    fi
fi

# require root
if [ "`id -u`" -ne "0" ]; then
    echo "Not running as root, cannot lock clocks, aborting"
    exit -1
fi

stop thermal-engine
stop perfd
stop vendor.thermal-engine
stop vendor.perfd

#################################################################################
# Find max cpu freq, and associated list of available freqs
#################################################################################

CPU_BASE=/sys/devices/system/cpu
GOV=cpufreq/scaling_governor

cpuMaxFreq=0
cpuAvailFreqCmpr=0
cpuAvailFreq=0
enableIndices=''
disableIndices=''

cpu=0
while [ -f ${CPU_BASE}/cpu${cpu}/online ]; do
    # enable core, so we can find its frequencies
    echo 1 > ${CPU_BASE}/cpu${cpu}/online

    maxFreq=`cat ${CPU_BASE}/cpu$cpu/cpufreq/cpuinfo_max_freq`
    availFreq=`cat ${CPU_BASE}/cpu$cpu/cpufreq/scaling_available_frequencies`
    availFreqCmpr=${availFreq// /-}

    if [ ${maxFreq} -gt ${cpuMaxFreq} ]; then
        # new highest max freq, look for cpus with same max freq and same avail freq list
        cpuMaxFreq=${maxFreq}
        cpuAvailFreq=${availFreq}
        cpuAvailFreqCmpr=${availFreqCmpr}

        if [ -z ${disableIndices} ]; then
            disableIndices="$enableIndices"
        else
            disableIndices="$disableIndices $enableIndices"
        fi
        enableIndices=${cpu}
    elif [ ${maxFreq} == ${cpuMaxFreq} ] && [ ${availFreqCmpr} == ${cpuAvailFreqCmpr} ]; then
        enableIndices="$enableIndices $cpu"
    else
        disableIndices="$disableIndices $cpu"
    fi
    cpu=$(($cpu + 1))
done

#################################################################################
# Chose a frequency to lock to that's >= 50% of max
#################################################################################

TARGET_PERCENT=50
TARGET_FREQ_SCALED=`expr ${TARGET_PERCENT} \* ${cpuMaxFreq}`
chosenFreq=0
for freq in ${cpuAvailFreq}; do
    freqScaled=`expr 100 \* $freq`
    if [ ${freqScaled} -ge ${TARGET_FREQ_SCALED} ]; then
        chosenFreq=${freq}
        break
    fi
done

#################################################################################
# Lock to target freq - note: enable before disabling!
#################################################################################

# enable 'big' CPUs
for cpu in ${enableIndices}; do
    freq=${CPU_BASE}/cpu$cpu/cpufreq

    echo 1 > ${CPU_BASE}/cpu${cpu}/online
    echo userspace > ${CPU_BASE}/cpu${cpu}/${GOV}
    echo ${chosenFreq} > ${freq}/scaling_max_freq
    echo ${chosenFreq} > ${freq}/scaling_min_freq
    echo ${chosenFreq} > ${freq}/scaling_setspeed

    # validate setting the freq worked
    obsCur=`cat ${freq}/scaling_cur_freq`
    obsMin=`cat ${freq}/scaling_min_freq`
    obsMax=`cat ${freq}/scaling_max_freq`
    if [ obsCur -ne ${chosenFreq} ] || [ obsMin -ne ${chosenFreq} ] || [ obsMax -ne ${chosenFreq} ]; then
        echo "Failed to set CPU$cpu to $chosenFreq Hz! Aborting..."
        echo "scaling_cur_freq = $obsCur"
        echo "scaling_min_freq = $obsMin"
        echo "scaling_max_freq = $obsMax"
        exit -1
    fi

done

# disable other CPUs
for cpu in ${disableIndices}; do
  echo 0 > ${CPU_BASE}/cpu${cpu}/online
done

echo ""
echo "Locked CPUs ${enableIndices// /,} to $chosenFreq / $maxFreq Hz"
echo "Disabled CPUs ${disableIndices// /,}"

#################################################################################
# Memory bus / GPU clocks - hardcoded per-device for now
#################################################################################

device=`getprop ro.product.device`
if [ ${device} == "marlin" ] || [ ${device} == "sailfish" ]; then
    echo 13763 > /sys/class/devfreq/soc:qcom,gpubw/max_freq
    echo performance > /sys/class/kgsl/kgsl-3d0/devfreq/governor
    echo -n 624000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
else
    ## TODO: had trouble with locking GPU clocks on walleye/taimen, punting for now
    model=`getprop ro.product.model`
    echo "Note: Unable to lock memory bus / GPU clocks of $model ($device)."
fi

echo "\n$device clocks have been locked - to reset, reboot the device"
