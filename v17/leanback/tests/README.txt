Test project for support leanback.

RUN TESTS
Using gradle
1. cd frameworks/support
2. ./gradlew support-leanback-v17:connectedCheck --info

Using adb
adb shell am instrument -w android.support.v17.leanback.tests/android.test.InstrumentationTestRunner
