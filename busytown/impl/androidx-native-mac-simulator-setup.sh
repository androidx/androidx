XCODE_SIMULATORS=$(xcrun simctl list devices | grep "iPhone" | wc -l)
if [ $XCODE_SIMULATORS == '0' ]; then
  SIMULATOR_DEVICE=$(xcrun simctl create 'iPhone 12' 'iPhone 12' 'iOS15.0')
  echo "Booting device $SIMULATOR_DEVICE"
  xcrun simctl boot $SIMULATOR_DEVICE
else
  echo "Already have $XCODE_SIMULATORS simulators set up."
fi
