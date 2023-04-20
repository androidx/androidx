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

import android.os.Build
import androidx.profileinstaller.ProfileVerifier.CompilationStatus.RESULT_CODE_ERROR_UNSUPPORTED_API_VERSION
import androidx.test.filters.LargeTest
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Test

@LargeTest
class ProfileVerificationOnUnsupportedApiVersions {

    @Before
    fun setUp() {
        // This test runs only on selected api version currently unsupported by profile verifier
        Assume.assumeTrue(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.P ||
                Build.VERSION.SDK_INT == Build.VERSION_CODES.R
        )

        withPackageName(PACKAGE_NAME_WITH_INITIALIZER) { uninstall() }
        withPackageName(PACKAGE_NAME_WITHOUT_INITIALIZER) { uninstall() }
    }

    @After
    fun tearDown() {
        withPackageName(PACKAGE_NAME_WITH_INITIALIZER) { uninstall() }
        withPackageName(PACKAGE_NAME_WITHOUT_INITIALIZER) { uninstall() }
    }

    @Test
    fun unsupportedApiWithInitializer() = withPackageName(PACKAGE_NAME_WITH_INITIALIZER) {
        install(apkName = APK_WITH_INITIALIZER_V1, withProfile = false)
        start(ACTIVITY_NAME)
        evaluateUI {
            profileInstalled(RESULT_CODE_ERROR_UNSUPPORTED_API_VERSION)
            hasReferenceProfile(false)
            hasCurrentProfile(false)
        }
    }

    @Test
    fun unsupportedApiWithoutInitializer() = withPackageName(PACKAGE_NAME_WITHOUT_INITIALIZER) {
        install(apkName = APK_WITHOUT_INITIALIZER_V1, withProfile = false)
        start(ACTIVITY_NAME)
        evaluateUI {
            profileInstalled(RESULT_CODE_ERROR_UNSUPPORTED_API_VERSION)
            hasReferenceProfile(false)
            hasCurrentProfile(false)
        }
    }
}