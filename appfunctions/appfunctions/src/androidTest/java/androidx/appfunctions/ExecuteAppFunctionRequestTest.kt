/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appfunctions

import android.os.Build
import android.os.Bundle
import androidx.appfunctions.ExecuteAppFunctionRequest.Companion.EXTRA_PARAMETERS
import androidx.appfunctions.ExecuteAppFunctionRequest.Companion.EXTRA_USE_JETPACK_SCHEMA
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.AssumptionViolatedException
import org.junit.Test

class ExecuteAppFunctionRequestTest {
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun toPlatformExtensionClass_success() {
        assumeAppFunctionExtensionLibraryAvailable()
        val appFunctionData = AppFunctionData.Builder("").setString("testString", "value").build()
        val request = ExecuteAppFunctionRequest("pkg", "method", appFunctionData)
        val platformRequest = request.toPlatformExtensionClass()

        assertThat(platformRequest.targetPackageName).isEqualTo("pkg")
        assertThat(platformRequest.functionIdentifier).isEqualTo("method")
        assertThat(platformRequest.parameters).isEqualTo(appFunctionData.genericDocument)
        assertThat(platformRequest.extras.getBundle(EXTRA_PARAMETERS)?.isEmpty()).isTrue()
        assertThat(platformRequest.extras.getBoolean(EXTRA_USE_JETPACK_SCHEMA)).isTrue()

        // Test with extras set
        val bundle = Bundle()
        bundle.putLong("longKey", 123L)
        val appFunctionDataWithExtras = AppFunctionData(appFunctionData.genericDocument, bundle)
        val requestWithExtras =
            ExecuteAppFunctionRequest("pkg2", "method2", appFunctionDataWithExtras)
        val platformRequestWithExtras = requestWithExtras.toPlatformExtensionClass()

        assertThat(platformRequestWithExtras.targetPackageName).isEqualTo("pkg2")
        assertThat(platformRequestWithExtras.functionIdentifier).isEqualTo("method2")
        assertThat(platformRequestWithExtras.parameters).isEqualTo(appFunctionData.genericDocument)
        assertThat(platformRequestWithExtras.extras.getBundle(EXTRA_PARAMETERS)).isEqualTo(bundle)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    fun toPlatformClass_success() {
        val appFunctionData = AppFunctionData.Builder("").setString("testString", "value").build()
        val request = ExecuteAppFunctionRequest("pkg", "method", appFunctionData)
        val platformRequest = request.toPlatformClass()

        assertThat(platformRequest.targetPackageName).isEqualTo("pkg")
        assertThat(platformRequest.functionIdentifier).isEqualTo("method")
        assertThat(platformRequest.parameters).isEqualTo(appFunctionData.genericDocument)
        assertThat(platformRequest.extras.getBundle(EXTRA_PARAMETERS)?.isEmpty()).isTrue()
        assertThat(platformRequest.extras.getBoolean(EXTRA_USE_JETPACK_SCHEMA)).isTrue()

        // Test with extras set
        val bundle = Bundle()
        bundle.putLong("longKey", 123L)
        val appFunctionDataWithExtras = AppFunctionData(appFunctionData.genericDocument, bundle)
        val requestWithExtras =
            ExecuteAppFunctionRequest("pkg2", "method2", appFunctionDataWithExtras)
        val platformRequestWithExtras = requestWithExtras.toPlatformClass()

        assertThat(platformRequestWithExtras.targetPackageName).isEqualTo("pkg2")
        assertThat(platformRequestWithExtras.functionIdentifier).isEqualTo("method2")
        assertThat(platformRequestWithExtras.parameters).isEqualTo(appFunctionData.genericDocument)
        assertThat(platformRequestWithExtras.extras.getBundle(EXTRA_PARAMETERS)).isEqualTo(bundle)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun fromPlatformExtensionClass_success() {
        assumeAppFunctionExtensionLibraryAvailable()
        val appFunctionData = AppFunctionData.Builder("").setString("testString", "value").build()
        val platformRequest =
            com.android.extensions.appfunctions.ExecuteAppFunctionRequest.Builder("pkg", "method")
                .setParameters(appFunctionData.genericDocument)
                .build()

        val request = ExecuteAppFunctionRequest.fromPlatformExtensionClass(platformRequest)

        assertThat(request.targetPackageName).isEqualTo("pkg")
        assertThat(request.functionIdentifier).isEqualTo("method")
        assertThat(request.functionParameters.genericDocument)
            .isEqualTo(appFunctionData.genericDocument)
        assertThat(request.functionParameters.extras.isEmpty).isTrue()
        assertThat(request.useJetpackSchema).isFalse()
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    fun fromPlatformClass_success() {
        val appFunctionData = AppFunctionData.Builder("").setString("testString", "value").build()
        val platformRequest =
            android.app.appfunctions.ExecuteAppFunctionRequest.Builder("pkg", "method")
                .setParameters(appFunctionData.genericDocument)
                .build()

        val request = ExecuteAppFunctionRequest.fromPlatformClass(platformRequest)

        assertThat(request.targetPackageName).isEqualTo("pkg")
        assertThat(request.functionIdentifier).isEqualTo("method")
        assertThat(request.functionParameters.genericDocument)
            .isEqualTo(appFunctionData.genericDocument)
        assertThat(request.functionParameters.extras.isEmpty).isTrue()
        assertThat(request.useJetpackSchema).isFalse()
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun fromPlatformExtensionClass_fromJetPackInExtrasIsTrue_fromJetPackIsTrue() {
        assumeAppFunctionExtensionLibraryAvailable()
        val appFunctionData = AppFunctionData.Builder("").setString("testString", "value").build()
        val platformRequest =
            com.android.extensions.appfunctions.ExecuteAppFunctionRequest.Builder("pkg", "method")
                .setParameters(appFunctionData.genericDocument)
                .setExtras(Bundle().apply { putBoolean(EXTRA_USE_JETPACK_SCHEMA, true) })
                .build()

        val request = ExecuteAppFunctionRequest.fromPlatformExtensionClass(platformRequest)

        assertThat(request.useJetpackSchema).isTrue()
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    fun fromPlatformClass_fromJetPackInExtrasIsTrue_fromJetPackIsTrue() {
        val appFunctionData = AppFunctionData.Builder("").setString("testString", "value").build()
        val platformRequest =
            android.app.appfunctions.ExecuteAppFunctionRequest.Builder("pkg", "method")
                .setParameters(appFunctionData.genericDocument)
                .setExtras(Bundle().apply { putBoolean(EXTRA_USE_JETPACK_SCHEMA, true) })
                .build()

        val request = ExecuteAppFunctionRequest.fromPlatformClass(platformRequest)

        assertThat(request.useJetpackSchema).isTrue()
    }

    private fun assumeAppFunctionExtensionLibraryAvailable() {
        try {
            Class.forName("com.android.extensions.appfunctions.ExecuteAppFunctionRequest")
            return
        } catch (e: ClassNotFoundException) {
            throw AssumptionViolatedException("Unable to find AppFunction extension library", e)
        }
    }
}
