// Check if there are any simulators set up in Xcode
XCODE_SIMULATORS=$(xcrun simctl list devices | grep "iPhone" | wc -l)
if [ $XCODE_SIMULATORS = '0' ]; then
  // Get the latest iOS version installed on the machine
  // -k2,2 Specifies that the sort key is the second column of output
  // -V Enables "version sort" to handle sorting of version numbers
  // -r Reverses the result of the sort, so the first is the latest version
  // exit causes the awk command to exit after printing the first result
  XCODE_IOS_VERSION=$(xcrun simctl list runtimes | grep "iOS" | sort -k2,2Vr | awk  '{print $2; exit}')
  if [ -z $XCODE_IOS_VERSION ]; then
    echo "No iOS runtimes installed. Run xcodebuild -downloadPlatform iOS"
  else
    echo "Found iOS $XCODE_IOS_VERSION"
    SIMULATOR_DEVICE=$(xcrun simctl create 'iPhone 12' 'iPhone 12' iOS$XCODE_IOS_VERSION)
    echo "Booting device $SIMULATOR_DEVICE"
    xcrun simctl boot $SIMULATOR_DEVICE
  fi
else
  echo "Already have $XCODE_SIMULATORS simulators set up."
fi
