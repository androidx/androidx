/*
 * Copyright 2023 The Android Open Source Project
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
import androidx.profileinstaller.ProfileVerifier.CompilationStatus.RESULT_CODE_ERROR_NO_PROFILE_EMBEDDED
import androidx.profileinstaller.ProfileVerifier.CompilationStatus.RESULT_CODE_PROFILE_ENQUEUED_FOR_COMPILATION
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Test

@LargeTest
@SdkSuppress(minSdkVersion = android.os.Build.VERSION_CODES.P)
class ProfileVerificationOnGeneratedProfiles {

    companion object {
        const val APK_DEBUG_BASELINE_PROFILE_CONSUMER = "baselineprofile-consumer-debug.apk"
        const val APK_RELEASE_BASELINE_PROFILE_CONSUMER = "baselineprofile-consumer-release.apk"
        const val PACKAGE_NAME = "androidx.benchmark.integration.baselineprofile.consumer"
        const val ACTIVITY_NAME = ".EmptyActivity"
    }

    @Before
    fun setUp() {
        // Note that this test fails on emulator api 30 (b/251540646)
        Assume.assumeTrue(!isApi30)
        withPackageName(PACKAGE_NAME) { uninstall() }
    }

    @After
    fun tearDown() {
        withPackageName(PACKAGE_NAME) { uninstall() }
    }

    @Test
    fun releaseProfileInstallerInstallation() =
        withPackageName(PACKAGE_NAME) {

            // Installs the apk
            install(apkName = APK_RELEASE_BASELINE_PROFILE_CONSUMER, withProfile = false)

            // Check that a profile exists and it's enqueued for compilation
            start(ACTIVITY_NAME)
            evaluateUI {
                profileInstalled(RESULT_CODE_PROFILE_ENQUEUED_FOR_COMPILATION)
                hasReferenceProfile(false)
                hasCurrentProfile(true)
                hasEmbeddedProfile(true)
            }
            stop()

            // Compile app with enqueued profile
            compileCurrentProfile()

            // Checks that the app has been compiled with a profile
            start(ACTIVITY_NAME)
            evaluateUI {
                profileInstalled(RESULT_CODE_COMPILED_WITH_PROFILE)
                hasReferenceProfile(true)
                hasCurrentProfile(false)
                hasEmbeddedProfile(true)
            }
        }

    @Test
    fun releasePackageManagerInstallation() =
        withPackageName(PACKAGE_NAME) {

            // Install with reference profile.
            install(apkName = APK_RELEASE_BASELINE_PROFILE_CONSUMER, withProfile = true)
            start(ACTIVITY_NAME)
            evaluateUI {
                profileInstalled(RESULT_CODE_COMPILED_WITH_PROFILE)
                hasReferenceProfile(true)
                hasCurrentProfile(true)
                hasEmbeddedProfile(true)
            }
        }

    @Test
    fun debugShouldNotHaveEmbeddedProfile() =
        withPackageName(PACKAGE_NAME) {

            // Installs the apk
            install(apkName = APK_DEBUG_BASELINE_PROFILE_CONSUMER, withProfile = false)

            // Check that a profile exists and it's enqueued for compilation
            start(ACTIVITY_NAME)
            evaluateUI {
                profileInstalled(RESULT_CODE_ERROR_NO_PROFILE_EMBEDDED)
                hasReferenceProfile(false)
                hasCurrentProfile(false)
                hasEmbeddedProfile(false)
            }
        }
}
