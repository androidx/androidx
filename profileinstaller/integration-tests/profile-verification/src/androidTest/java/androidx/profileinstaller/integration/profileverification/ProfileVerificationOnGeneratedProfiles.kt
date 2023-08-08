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
import androidx.profileinstaller.ProfileVerifier.CompilationStatus.RESULT_CODE_PROFILE_ENQUEUED_FOR_COMPILATION
import androidx.profileinstaller.ProfileVersion
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@SdkSuppress(
    minSdkVersion = android.os.Build.VERSION_CODES.P,
    maxSdkVersion = ProfileVersion.MAX_SUPPORTED_SDK
)
@RunWith(Parameterized::class)
class ProfileVerificationOnGeneratedProfiles(
    private val apk: String,
    private val packageName: String
) {

    companion object {
        private const val ACTIVITY_NAME = ".EmptyActivity"

        @Parameterized.Parameters(name = "apk={0},packageName={1}")
        @JvmStatic
        fun parameters(): List<Array<Any>> = listOf(
            arrayOf(
                "baselineprofile-consumer-release.apk",
                "androidx.benchmark.integration.baselineprofile.consumer"
            )
        )
    }

    @Before
    fun setUp() {
        // Note that this test fails on emulator api 30 (b/251540646)
        Assume.assumeTrue(!isApi30)
        withPackageName(packageName) { uninstall() }
    }

    @After
    fun tearDown() {
        withPackageName(packageName) { uninstall() }
    }

    @Test
    fun profileInstallerInstallation() = withPackageName(packageName) {

            // Installs the apk
            install(apkName = apk, withProfile = false)

            // Check that a profile exists and it's enqueued for compilation
            start(ACTIVITY_NAME)
            evaluateUI {
                profileInstalled(RESULT_CODE_PROFILE_ENQUEUED_FOR_COMPILATION)
                hasReferenceProfile(false)
                hasCurrentProfile(true)
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
            }
        }

    @Test
    fun packageManagerInstallation() = withPackageName(packageName) {

        // Install with reference profile.
        install(apkName = apk, withProfile = true)
        start(ACTIVITY_NAME)
        evaluateUI {
            hasReferenceProfile(true)
            hasCurrentProfile(true)
            profileInstalled(RESULT_CODE_COMPILED_WITH_PROFILE)
        }
    }
}
