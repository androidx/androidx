# Adding the Support Library Build Within Another Build

Sorry, this doesn't seem to be working at the moment.
For now, run `./gradlew createArchive` and copy the output to where your project can use it, as described fuller in go/support-dev

Would you like to make a change in Support Library and have it be propagated to
your downstream Gradle build (generally an app) without having to separately
build Support Library and then build your application?

## To build Support Library as part of your existing Gradle build
*   To add the Support Library build
    *   Add `apply(from: '<support-lib-repo-root>/frameworks/support/include-support-library.gradle')`
        to your settings.gradle
        *   See [include-support-library.gradle](include-support-library.gradle)
            for more information
*   If your project is an Android app, also update some dependencies:
    *   Open your local.properties file and update the value of `sdk.dir` .
        *   It should point to `<support-lib-repo-root>/prebuilts/fullsdk-<platform>` .
        *   For example, `~/support-library/prebuilts/fullsdk-linux` .
    *   In your build.gradle, update any versions that refer to previous versions of
        Support Library.
        *   To determine the correct version, find the SDK with the highest
            number among SDKs in the Support Library repo.

                echo <support-lib-repo-root>/prebuilts/fullsdk-linux/platforms/android* | xargs -n 1 echo | sed 's/.*android-//' | tail -n 1

            This should output, for example, "28"

        *   Update dependency versions
            *   For example, you may want to replace
                `com.android.support:app-compat-v7:26.0.2` with
                `com.android.support:app-compat-v7:28.0.2`
        *   Update configuration given to the Android Gradle plugin
            *   Check `compileSdkVersion` and make sure its version is correct

