# Test Data

## [sample-xcode.xcresult](./sample-xcode.xcresult)

This is an example benchmark `xcresult` directory that is the result of running an `xcodebuild`.

An example invocation might look something like:

```bash
xcodebuild test -project $SRCROOT/benchmark-darwin-samples-xcode.xcodeproj \
    -scheme testapp-ios \
    -destination id=7F61C467-4E4A-437C-B6EF-026FEEF3904C \
    -resultBundlePath $SRCROOT/benchmark-darwin-samples-xcode.xcresult
```

The `xcresult` output directory stores results in a nested `plist` format. Entities in the top-level
`plist` file point to other entities stored in other`plist` files (inside the `Data` directory),
using a unique filename identifier.
