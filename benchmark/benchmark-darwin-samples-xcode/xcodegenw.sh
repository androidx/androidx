#!/bin/bash
# Generates the XCode project to run / debug benchmark tests.

xcodegen --spec xcodegen-project.yml
open benchmark-darwin-sample-xcode.xcodeproj
