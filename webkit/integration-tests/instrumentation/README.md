# Webkit library instrumentation tests

This project contains the instrumentation tests for the [webkit](/webkit/webkit) library.

The tests are located in a separate module to allow the use of multiple
[product flavors](https://developer.android.com/build/build-variants#product-flavors)
to build and run the tests against different targetSdk versions.

This is necessary in order to test dark mode functionality, which changes depending on `targetSdk`
between `32` and `33`.


## Source sets
Tests that do not depend on a particular `targetSdk` version should be added to the default `androidTest` source set.

Tests that require a particular `targetSdk` version should be added to the appropriate
[source set](https://developer.android.com/build/build-variants#sourcesets).

## Running tests from Android Studio
Tests can be run as normal in Android Studio. You must use the "Build Variants" menu to select
the product flavor to run. Use one of

* `targetSdkLatestDebug`
* `targetSdk32Debug`

You must select the corresponding build variant in order to run tests located outside the shared
source set.