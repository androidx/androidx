./gradlew :compose:ui:ui:linkInstrumentedTestDebugFrameworkUikitSimArm64

cd compose/ui/ui/src/uikitInstrumentedTest/launcher

# Force-close all simulators
xcrun simctl shutdown all
killall Simulator

## Configure configure simulators to disconnect hardware keyboard (and show on-screen keyboard).

# Export current preferences to PREF_PLIST
PREF_PLIST=~/iphonesimulator.plist
defaults export com.apple.iphonesimulator - > "$PREF_PLIST"

# Adding "ConnectHardwareKeyboard = false" for every simulator from config
/usr/libexec/PlistBuddy -c "Print :DevicePreferences" "$PREF_PLIST" | \
grep -E '^[ ]{4}[^ ]' | awk '{print $1}' | sed 's/[^a-zA-Z0-9-]//g' | while read -r UUID; do
    /usr/libexec/PlistBuddy -c "Set :DevicePreferences:$UUID:ConnectHardwareKeyboard false" "$PREF_PLIST" 2>/dev/null || \
    /usr/libexec/PlistBuddy -c "Add :DevicePreferences:$UUID:ConnectHardwareKeyboard bool false" "$PREF_PLIST"
done

# Import back the modified plist
defaults import com.apple.iphonesimulator "$PREF_PLIST"
defaults write com.apple.iphonesimulator ConnectHardwareKeyboard -bool false

xcodebuild test -scheme Launcher-CI -project Launcher.xcodeproj -destination 'platform=iOS Simulator,name=iPhone 16'
