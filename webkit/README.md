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

## How to use this library in your app

Add this to your `build.gradle` file:

```
dependencies {
    implementation "androidx.webkit:webkit:1.13.0"
}
```

**Important:** replace `1.13.0` with the latest version from
https://developer.android.com/jetpack/androidx/releases/webkit.

## Sample apps

Please check out the WebView samples [on GitHub][GitHub demo app] for a showcase
of a handful of androidx.webkit APIs.

For more APIs, check out the sample app in the [AndroidX repo][AndroidX demo
app].

## Public bug tracker

If you find bugs in the androidx.webkit library or want to request new features,
please [file a
ticket](https://issuetracker.google.com/issues/new?component=460423).

## Building the library (contributing to the AndroidX library)

If you're trying to modify the androidx.webkit library, or apply local changes
to the library, you can do so like so:

```sh
cd frameworks/support/
# Build the library/compile changes
./gradlew :webkit:webkit:assembleDebug

# Run integration tests with the WebView installed on the device
# using this convenience script:
webkit/run_instrumentation_tests.sh
# or run the tests directly: 
./gradlew webkit:integration-tests:instrumentation:connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.webview-version=factory

# Update API files (only necessary if you changed public APIs)
./gradlew :webkit:webkit:updateApi
```

For more a detailed developer guide, Googlers should read
http://go/wvsl-contribute.

## Instrumentation tests

The instrumentation tests for `androidx.webkit` are located in the
`:webkit:integration-tests:instrumentation` project. The tests have been split
out into a separate project to facilitate testing against different targetSdk
versions.

Any new tests should be added to that project. To run the test, use the command
above.

[GitHub demo app]: https://github.com/android/views-widgets-samples/tree/main/WebView
[AndroidX demo app]: /androidx-main/webkit/integration-tests/testapp/README.md
