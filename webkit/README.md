# androidx.webkit

**See this page rendered in [Gitiles
markdown](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/webkit/README.md).**

The androidx.webkit library is a static library you can add to your Android
application in order to use android.webkit APIs that are not available for older
platform versions.

## Basic info

* [Library owners](OWNERS)
* [Release notes](https://developer.android.com/jetpack/androidx/releases/webkit)
* [Browse source](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/webkit/)
* [Reference docs and guide to import the library](https://developer.android.com/reference/androidx/webkit/package-summary)
* [Existing open bugs](https://issuetracker.google.com/issues?q=componentid:460423%20status:open)
* [File a new bug](https://issuetracker.google.com/issues/new?component=460423)

## Building the library (for local development)

If you're trying to modify the androidx.webkit library, or apply local changes
to the library, you can do so like so:

```sh
cd frameworks/support/
# Build the library/compile changes
./gradlew :webkit:webkit:assembleDebug
# Run integration tests with the WebView installed on the device
./gradlew :webkit:integration-tests:instrumentation:connectedAndroidTest
# Update API files (only necessary if you changed public APIs)
./gradlew :webkit:webkit:updateApi
```

For more a detailed developer guide, Googlers should read
http://go/wvsl-contribute.

## Instrumentation tests
The instrumentation tests for `androidx.webkit` are located in the
`:webkit:integration-tests:instrumentation` project. The tests have been split out into a separate
project to facilitate testing against different targetSdk versions.

Any new tests should be added to that project. To run the test, use the command above.

## API demo code

We also maintain a demo app ([demo
code](/webkit/integration-tests/testapp/src/main/java/com/example/androidx/webkit),
[developer guide](/webkit/integration-tests/testapp/README.md)) to demonstrate
how to properly use the latest androidx.webkit APIs in your Android app.
