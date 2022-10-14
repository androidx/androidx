/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.profileinstaller.integration.profileverification

import androidx.profileinstaller.ProfileVerifier.CompilationStatus.RESULT_CODE_COMPILED_WITH_PROFILE
import androidx.profileinstaller.ProfileVerifier.CompilationStatus.RESULT_CODE_NO_PROFILE
import androidx.profileinstaller.ProfileVerifier.CompilationStatus.RESULT_CODE_PROFILE_ENQUEUED_FOR_COMPILATION
import androidx.profileinstaller.ProfileVersion
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/**
 * This test uses the project
 * "profileinstaller:integration-tests:profile-verification-sample-no-initializer". The release
 * version of it has been embedded in the assets in 3 different versions with increasing
 * versionCode to allow updating the app through pm command. In this test the
 * ProfileInstallerInitializer has been disabled from the target app manifest.
 *
 * The SampleActivity invoked displays the status of the reference profile install on the UI after
 * the callback from {@link ProfileVerifier} returns. This test checks the status visualized to
 * confirm if the reference profile has been installed.
 *
 * This test needs min sdk version `P` because it's first version to introduce support for dm file:
 * https://googleplex-android-review.git.corp.google.com/c/platform/frameworks/base/+/3368431/
 */
@SdkSuppress(
    minSdkVersion = android.os.Build.VERSION_CODES.P,
    maxSdkVersion = ProfileVersion.MAX_SUPPORTED_SDK
)
@LargeTest
class ProfileVerificationTestWithoutProfileInstallerInitializer {

    @Before
    fun setUp() = withPackageName(PACKAGE_NAME_WITHOUT_INITIALIZER) {
        // Note that this test fails on emulator api 30 (b/251540646)
        assumeTrue(!isApi30)
        uninstall()
    }

    @After
    fun tearDown() = withPackageName(PACKAGE_NAME_WITHOUT_INITIALIZER) {
        uninstall()
    }

    @Test
    fun installNewAppWithoutReferenceProfile() = withPackageName(PACKAGE_NAME_WITHOUT_INITIALIZER) {

        // Install without reference profile
        install(apkName = APK_WITHOUT_INITIALIZER_V1, withProfile = false)
        start(ACTIVITY_NAME)
        evaluateUI {
            profileInstalled(RESULT_CODE_NO_PROFILE)
            hasReferenceProfile(false)
            hasCurrentProfile(false)
        }
    }

    @Test
    fun installNewAppAndWaitForCompilation() = withPackageName(PACKAGE_NAME_WITHOUT_INITIALIZER) {

        // Install without reference profile
        install(apkName = APK_WITHOUT_INITIALIZER_V1, withProfile = false)

        // Start once to check there is no profile
        start(ACTIVITY_NAME)
        evaluateUI {
            profileInstalled(RESULT_CODE_NO_PROFILE)
            hasReferenceProfile(false)
            hasCurrentProfile(false)
        }
        stop()

        // Start again to check there is no profile
        start(ACTIVITY_NAME)
        evaluateUI {
            profileInstalled(RESULT_CODE_NO_PROFILE)
            hasReferenceProfile(false)
            hasCurrentProfile(false)
        }
        stop()

        // Install profile through broadcast receiver
        broadcastProfileInstallAction()

        // Start again to check there it's now awaiting compilation
        start(ACTIVITY_NAME)
        evaluateUI {
            profileInstalled(RESULT_CODE_PROFILE_ENQUEUED_FOR_COMPILATION)
            hasReferenceProfile(false)
            hasCurrentProfile(true)
        }
        stop()

        // Start again to check there it's now awaiting compilation
        start(ACTIVITY_NAME)
        evaluateUI {
            profileInstalled(RESULT_CODE_PROFILE_ENQUEUED_FOR_COMPILATION)
            hasReferenceProfile(false)
            hasCurrentProfile(true)
        }
        stop()

        // Compile
        compileCurrentProfile()

        // Start again to check profile is compiled
        start(ACTIVITY_NAME)
        evaluateUI {
            profileInstalled(RESULT_CODE_COMPILED_WITH_PROFILE)
            hasReferenceProfile(true)
            hasCurrentProfile(false)
        }

        // Start again to check profile is compiled
        start(ACTIVITY_NAME)
        evaluateUI {
            profileInstalled(RESULT_CODE_COMPILED_WITH_PROFILE)
            hasReferenceProfile(true)
            hasCurrentProfile(false)
        }
    }

    @Test
    fun installAppWithReferenceProfile() = withPackageName(PACKAGE_NAME_WITHOUT_INITIALIZER) {

        // Install with reference profile
        install(apkName = APK_WITHOUT_INITIALIZER_V1, withProfile = true)
        start(ACTIVITY_NAME)
        evaluateUI {
            profileInstalled(RESULT_CODE_COMPILED_WITH_PROFILE)
            hasReferenceProfile(true)
            hasCurrentProfile(false)
        }
    }

    @Test
    fun updateFromNoReferenceProfileToReferenceProfile() =
        withPackageName(PACKAGE_NAME_WITHOUT_INITIALIZER) {

            // Install without reference profile
            install(apkName = APK_WITHOUT_INITIALIZER_V2, withProfile = false)
            start(ACTIVITY_NAME)
            evaluateUI {
                profileInstalled(RESULT_CODE_NO_PROFILE)
                hasReferenceProfile(false)
                hasCurrentProfile(false)
            }

            // Updates adding reference profile
            install(apkName = APK_WITHOUT_INITIALIZER_V3, withProfile = true)
            start(ACTIVITY_NAME)
            evaluateUI {
                profileInstalled(RESULT_CODE_COMPILED_WITH_PROFILE)
                hasReferenceProfile(true)
                hasCurrentProfile(false)
            }
        }

    @Test
    fun updateFromReferenceProfileToNoReferenceProfile() =
        withPackageName(PACKAGE_NAME_WITHOUT_INITIALIZER) {

            // Install with reference profile
            install(apkName = APK_WITHOUT_INITIALIZER_V1, withProfile = true)
            start(ACTIVITY_NAME)
            evaluateUI {
                profileInstalled(RESULT_CODE_COMPILED_WITH_PROFILE)
                hasReferenceProfile(true)
                hasCurrentProfile(false)
            }

            // Updates removing reference profile
            install(apkName = APK_WITHOUT_INITIALIZER_V2, withProfile = false)
            start(ACTIVITY_NAME)
            evaluateUI {
                profileInstalled(RESULT_CODE_NO_PROFILE)
                hasReferenceProfile(false)
                hasCurrentProfile(false)
            }
        }

    @Test
    fun installWithReferenceProfileThenUpdateNoProfileThenUpdateProfileAgain() =
        withPackageName(PACKAGE_NAME_WITHOUT_INITIALIZER) {

            // Install with reference profile
            install(apkName = APK_WITHOUT_INITIALIZER_V1, withProfile = true)
            start(ACTIVITY_NAME)
            evaluateUI {
                profileInstalled(RESULT_CODE_COMPILED_WITH_PROFILE)
                hasReferenceProfile(true)
                hasCurrentProfile(false)
            }

            // Updates removing reference profile
            install(apkName = APK_WITHOUT_INITIALIZER_V2, withProfile = false)
            start(ACTIVITY_NAME)
            evaluateUI {
                profileInstalled(RESULT_CODE_NO_PROFILE)
                hasReferenceProfile(false)
                hasCurrentProfile(false)
            }

            // Reinstall with reference profile
            install(apkName = APK_WITHOUT_INITIALIZER_V3, withProfile = true)
            start(ACTIVITY_NAME)
            evaluateUI {
                profileInstalled(RESULT_CODE_COMPILED_WITH_PROFILE)
                hasReferenceProfile(true)
                hasCurrentProfile(false)
            }
        }

    @Test
    fun forceInstallCurrentProfileThroughBroadcastReceiver() =
        withPackageName(PACKAGE_NAME_WITHOUT_INITIALIZER) {

            // Install without reference profile
            install(apkName = APK_WITHOUT_INITIALIZER_V1, withProfile = false)

            // Start and assess there is no profile
            start(ACTIVITY_NAME)
            evaluateUI {
                profileInstalled(RESULT_CODE_NO_PROFILE)
                hasReferenceProfile(false)
                hasCurrentProfile(false)
            }
            stop()

            // Force update through broadcast receiver
            broadcastProfileInstallAction()

            // Start and assess there is a current profile
            start(ACTIVITY_NAME)
            evaluateUI {
                profileInstalled(RESULT_CODE_PROFILE_ENQUEUED_FOR_COMPILATION)
                hasReferenceProfile(false)
                hasCurrentProfile(true)
            }
        }

    @Test
    fun forceInstallCurrentProfileThroughBroadcastReceiverAndUpdateWithReference() =
        withPackageName(PACKAGE_NAME_WITHOUT_INITIALIZER) {

            // Install without reference profile, start and assess there is no profile
            install(apkName = APK_WITHOUT_INITIALIZER_V1, withProfile = false)
            start(ACTIVITY_NAME)
            evaluateUI {
                profileInstalled(RESULT_CODE_NO_PROFILE)
                hasReferenceProfile(false)
                hasCurrentProfile(false)
            }
            stop()

            // Force update through ProfileInstallerReceiver
            broadcastProfileInstallAction()

            // Start again and assess there is a current profile now installed
            start(ACTIVITY_NAME)
            evaluateUI {
                profileInstalled(RESULT_CODE_PROFILE_ENQUEUED_FOR_COMPILATION)
                hasReferenceProfile(false)
                hasCurrentProfile(true)
            }

            // Update to v2 and assert that the current profile was uninstalled
            install(apkName = APK_WITHOUT_INITIALIZER_V2, withProfile = false)
            start(ACTIVITY_NAME)
            evaluateUI {
                profileInstalled(RESULT_CODE_NO_PROFILE)
                hasReferenceProfile(false)
                hasCurrentProfile(false)
            }

            // Update to v3 with reference profile and assess this is correctly recognized
            install(apkName = APK_WITHOUT_INITIALIZER_V3, withProfile = true)
            start(ACTIVITY_NAME)
            evaluateUI {
                profileInstalled(RESULT_CODE_COMPILED_WITH_PROFILE)
                hasReferenceProfile(true)
                hasCurrentProfile(false)
            }
        }
}
