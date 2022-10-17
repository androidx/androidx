#!/bin/bash

rm -r collection-benchmark-ios.xcresult || true
xcodebuild \
 test \
 -project CollectionBenchmarkApp.xcodeproj \
 -scheme testapp-ios-benchmarks \
 -destination 'platform=iOS Simulator,name=iPhone 13,OS=15.5' \
 -resultBundlePath 'collection-benchmark-ios.xcresult'
