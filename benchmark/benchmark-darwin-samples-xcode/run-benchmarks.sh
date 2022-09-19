#!/bin/bash

# Runs benchmark tests.

xcodebuild \
 test \
 -project benchmark-darwin-sample-xcode.xcodeproj \
 -scheme testapp-ios \
 -destination 'platform=iOS Simulator,name=iPhone 13,OS=15.2' \
 -resultBundlePath 'benchmark-darwin-sample-xcode.xcresult'
