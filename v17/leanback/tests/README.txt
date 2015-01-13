Test project for support leanback.

INSTALLATION
adb install -r AndroidLeanbackTests.apk

RUN TESTS
adb shell am instrument -w android.support.v17.leanback.tests/android.test.InstrumentationTestRunner