
## Building framework with XCTests
./gradlew :compose:ui:ui:linkInstrumentedTestDebugFrameworkUikitSimArm64

# Force-close all simulators
xcrun simctl shutdown all
killall Simulator

## Configure simulators to disconnect hardware keyboard (and show on-screen keyboard).
# Get list of all DEVICES
DEVICES=$(xcrun simctl list DEVICES --json)
TEST_DEVICE="iPhone 16"

echo $DEVICES

# This assumes you have jq installed; you can also parse manually if not
if ! command -v jq &> /dev/null; then
  echo "Error: jq is not installed"
  exit 1
fi

# Export current preferences to PREF_PLIST
PREF_PLIST=~/iphonesimulator.plist
defaults export com.apple.iphonesimulator - > "$PREF_PLIST"

# Adding "ConnectHardwareKeyboard = false" for every simulator
echo "$DEVICES" | jq -r '.DEVICES | to_entries[] | select(.key | startswith("com.apple.CoreSimulator.SimRuntime.iOS")) | .value[] | "\(.udid)"' | while read -r UUID; do
    /usr/libexec/PlistBuddy -c "Set :DevicePreferences:$UUID:ConnectHardwareKeyboard false" "$PREF_PLIST" 2>/dev/null || \
    /usr/libexec/PlistBuddy -c "Add :DevicePreferences:$UUID:ConnectHardwareKeyboard bool false" "$PREF_PLIST"
done

# Import back the modified plist
defaults import com.apple.iphonesimulator "$PREF_PLIST"

## Launch Simulator app
XCODE_PATH=$(xcode-select -p)
SIMULATOR_PATH="$XCODE_PATH/Applications/Simulator.app/Contents/MacOS/Simulator"
open -a $SIMULATOR_PATH

# boot a test simulator
xcrun simctl boot "$TEST_DEVICE"
# Write the accessibility flags inside the Simulator:
xcrun simctl spawn booted defaults write com.apple.Accessibility AccessibilityEnabled -bool true
xcrun simctl spawn booted defaults write com.apple.Accessibility ApplicationAccessibilityEnabled -bool true
xcrun simctl spawn booted defaults write com.apple.Accessibility AutomationEnabled -bool true
# Restart SpringBoard (so system services pick up the change)
xcrun simctl spawn booted launchctl stop com.apple.SpringBoard

## Launch tests
cd compose/ui/ui/src/uikitInstrumentedTest/launcher
xcodebuild test -scheme Launcher-CI -project Launcher.xcodeproj -destination "platform=iOS Simulator,name=$TEST_DEVICE"
