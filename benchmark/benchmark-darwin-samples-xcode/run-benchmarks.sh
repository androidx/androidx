#!/bin/bash

# Runs benchmark tests.

rm -rf benchmark-darwin-sample-xcode.xcodeproj
xcodegen --spec xcodegen-project.yml

xcodebuild \
 test \
 -project benchmark-darwin-sample-xcode.xcodeproj \
 -scheme testapp-ios \
 -destination 'platform=iOS Simulator,name=iPhone 13,OS=15.2'
