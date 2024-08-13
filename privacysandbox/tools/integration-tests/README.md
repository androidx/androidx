# Privacy Sandbox tools integration tests

Integration test (app and SDK) using the App-SDK bridge (the "shim library").

testsdk/ contains an SDK (using the API Compiler), manually setting AidlInput and FrameworkAidlInput
instead of using
androidx.privacysandbox.library (https://developer.android.com/jetpack/androidx/releases/privacysandbox-plugins)
since we want to use the HEAD versions of the shim tools.

testsdk-asb/ wraps the test SDK in an Android SDK Bundle (ASB), using
com.android.privacy-sandbox-sdk.

testapp/ contains a simple Android app with a dependency on testsdk-asb, using the HEAD version of
the API Generator. This is the project which will contain the integration tests themselves.
