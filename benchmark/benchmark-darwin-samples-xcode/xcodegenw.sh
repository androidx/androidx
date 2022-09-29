#!/bin/bash
# Generates the XCode project to run / debug benchmark tests.

rm -rf benchmark-darwin-sample-xcode.xcodeproj
xcodegen --spec xcodegen-project.yml
open benchmark-darwin-sample-xcode.xcodeproj
