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
import androidx.profileinstaller.ProfileVerifier.CompilationStatus.RESULT_CODE_COMPILED_WITH_PROFILE_NON_MATCHING
import androidx.profileinstaller.ProfileVerifier.CompilationStatus.RESULT_CODE_PROFILE_ENQUEUED_FOR_COMPILATION
import androidx.profileinstaller.ProfileVersion
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/**
 * This test uses the project "profileinstaller:integration-tests:profile-verification-sample". The
 * release version of it has been embedded in the assets in 3 different versions with increasing
 * versionCode to allow updating the app through pm command.
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
class ProfileVerificationTestWithProfileInstallerInitializer {

    @Before
    fun setUp() = withPackageName(PACKAGE_NAME_WITH_INITIALIZER) {
        // Note that this test fails on emulator api 30 (b/251540646)
        assumeTrue(!isApi30)
        uninstall()
    }

    @After
    fun tearDown() = withPackageName(PACKAGE_NAME_WITH_INITIALIZER) {
        uninstall()
    }

    @Test
    fun installNewApp() = withPackageName(PACKAGE_NAME_WITH_INITIALIZER) {
        // Install without reference profile
        install(apkName = APK_WITH_INITIALIZER_V1, withProfile = false)

        // Start
        start(ACTIVITY_NAME)
        evaluateUI {
            profileInstalled(RESULT_CODE_PROFILE_ENQUEUED_FOR_COMPILATION)
            hasReferenceProfile(false)
            hasCurrentProfile(true)
        }
    }

    @Test
    fun installNewAppAndWaitForCompilation() = withPackageName(PACKAGE_NAME_WITH_INITIALIZER) {

        // Install without reference profile
        install(apkName = APK_WITH_INITIALIZER_V1, withProfile = false)

        // Start once to install profile
        start(ACTIVITY_NAME)
        evaluateUI {
            profileInstalled(RESULT_CODE_PROFILE_ENQUEUED_FOR_COMPILATION)
            hasReferenceProfile(false)
            hasCurrentProfile(true)
        }
        stop()

        // Start again, should still be awaiting compilation
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

        // Profile should still be compiled
        start(ACTIVITY_NAME)
        evaluateUI {
            profileInstalled(RESULT_CODE_COMPILED_WITH_PROFILE)
            hasReferenceProfile(true)
            hasCurrentProfile(false)
        }
    }

    @Test
    fun installAppWithReferenceProfile() = withPackageName(PACKAGE_NAME_WITH_INITIALIZER) {

        // Install with reference profile.
        install(apkName = APK_WITH_INITIALIZER_V1, withProfile = true)
        start(ACTIVITY_NAME)
        evaluateUI {
            profileInstalled(RESULT_CODE_COMPILED_WITH_PROFILE)
            hasReferenceProfile(true)
            hasCurrentProfile(true)
        }
    }

    @Test
    fun updateFromReferenceProfileToReferenceProfile() =
        withPackageName(PACKAGE_NAME_WITH_INITIALIZER) {

            // Install without reference profile
            install(apkName = APK_WITH_INITIALIZER_V1, withProfile = true)
            start(ACTIVITY_NAME)
            evaluateUI {
                profileInstalled(RESULT_CODE_COMPILED_WITH_PROFILE)
                hasReferenceProfile(true)
                hasCurrentProfile(true)
            }

            // Updates adding reference profile
            install(apkName = APK_WITH_INITIALIZER_V2, withProfile = true)
            start(ACTIVITY_NAME)
            evaluateUI {
                profileInstalled(RESULT_CODE_COMPILED_WITH_PROFILE)
                hasReferenceProfile(true)
                hasCurrentProfile(true)
            }
        }

    @Test
    fun updateFromNoReferenceProfileToReferenceProfile() =
        withPackageName(PACKAGE_NAME_WITH_INITIALIZER) {

            // Install without reference profile
            install(apkName = APK_WITH_INITIALIZER_V2, withProfile = false)
            start(ACTIVITY_NAME)
            evaluateUI {
                profileInstalled(RESULT_CODE_PROFILE_ENQUEUED_FOR_COMPILATION)
                hasReferenceProfile(false)
                hasCurrentProfile(true)
            }

            // Updates adding reference profile
            install(apkName = APK_WITH_INITIALIZER_V3, withProfile = true)
            start(ACTIVITY_NAME)
            evaluateUI {

                // Taimen Api 28 and Cuttlefish Api 29 behave differently.
                if ((isApi29 && isCuttlefish) || (isApi28 && !isCuttlefish)) {
                    profileInstalled(RESULT_CODE_COMPILED_WITH_PROFILE_NON_MATCHING)
                } else {
                    profileInstalled(RESULT_CODE_COMPILED_WITH_PROFILE)
                }

                hasReferenceProfile(true)
                hasCurrentProfile(true)
            }
        }

    @Test
    fun updateFromReferenceProfileToNoReferenceProfile() =
        withPackageName(PACKAGE_NAME_WITH_INITIALIZER) {

            // Install with reference profile
            install(apkName = APK_WITH_INITIALIZER_V1, withProfile = true)
            start(ACTIVITY_NAME)
            evaluateUI {
                profileInstalled(RESULT_CODE_COMPILED_WITH_PROFILE)
                hasReferenceProfile(true)
                hasCurrentProfile(true)
            }

            // Updates removing reference profile
            install(apkName = APK_WITH_INITIALIZER_V2, withProfile = false)
            start(ACTIVITY_NAME)
            evaluateUI {
                profileInstalled(RESULT_CODE_PROFILE_ENQUEUED_FOR_COMPILATION)
                hasReferenceProfile(false)
                hasCurrentProfile(true)
            }
        }

    @Test
    fun installWithReferenceProfileThenUpdateNoProfileThenUpdateProfileAgain() =
        withPackageName(PACKAGE_NAME_WITH_INITIALIZER) {

            // Install with reference profile
            install(apkName = APK_WITH_INITIALIZER_V1, withProfile = true)
            start(ACTIVITY_NAME)
            evaluateUI {
                profileInstalled(RESULT_CODE_COMPILED_WITH_PROFILE)
                hasReferenceProfile(true)
                hasCurrentProfile(true)
            }

            // Updates removing reference profile
            install(apkName = APK_WITH_INITIALIZER_V2, withProfile = false)
            start(ACTIVITY_NAME)
            evaluateUI {
                profileInstalled(RESULT_CODE_PROFILE_ENQUEUED_FOR_COMPILATION)
                hasReferenceProfile(false)
                hasCurrentProfile(true)
            }

            // Reinstall with reference profile
            install(apkName = APK_WITH_INITIALIZER_V3, withProfile = true)
            start(ACTIVITY_NAME)
            evaluateUI {

                // Taimen Api 28 and Cuttlefish Api 29 behave differently.
                if ((isApi29 && isCuttlefish) || (isApi28 && !isCuttlefish)) {
                    profileInstalled(RESULT_CODE_COMPILED_WITH_PROFILE_NON_MATCHING)
                } else {
                    profileInstalled(RESULT_CODE_COMPILED_WITH_PROFILE)
                }
                hasReferenceProfile(true)
                hasCurrentProfile(true)
            }
        }
}
