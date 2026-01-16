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

package androidx.xr.scenecore.impl.impress

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/** JNI Marshaling Tests for the Skybox endpoints of the Impress API bindings. */
@SdkSuppress(minSdkVersion = 29)
@RunWith(AndroidJUnit4::class)
class SkyboxJniMarshallingTest : BaseJniMarshallingTest() {

    companion object {
        private const val TEST_IBL_PATH = "skybox/sky.zip"
        private const val TEST_IBL_KEY = "ibl_key"
        private const val TEST_NATIVE_TOKEN = 67890L
        private const val TEST_ERROR_MESSAGE = "Test C++ IBL Failure"
    }

    @Test
    fun loadImageBasedLightingAsset_marshalsPath_invokesCallbackOnSuccess() = runBlocking {
        ImpressApiTestHelper.nativeSetExpectedLoadIblPath(TEST_IBL_PATH)
        ImpressApiTestHelper.nativeSetLoadIblAssetSuccess(TEST_NATIVE_TOKEN)

        val actualImage = mImpressApi.loadImageBasedLightingAsset(TEST_IBL_PATH)

        val actualToken = actualImage.nativeHandle
        assertThat(actualToken).isEqualTo(TEST_NATIVE_TOKEN)
    }

    @Test
    fun loadImageBasedLightingAsset_fromByteArray_invokesCallbackOnSuccess() = runBlocking {
        val testSize = 1024
        val testData = generateTestPattern(testSize)
        ImpressApiTestHelper.nativeSetExpectedLoadIblAssetTestPattern(testSize, TEST_IBL_KEY)
        ImpressApiTestHelper.nativeSetLoadIblAssetSuccess(TEST_NATIVE_TOKEN)

        val actualImage = mImpressApi.loadImageBasedLightingAsset(testData, TEST_IBL_KEY)

        val actualToken = actualImage.nativeHandle
        assertThat(actualToken).isEqualTo(TEST_NATIVE_TOKEN)
    }

    @Test
    fun loadImageBasedLightingAsset_marshalsPath_invokesCallbackOnFailure() = runBlocking {
        ImpressApiTestHelper.nativeSetExpectedLoadIblPath(TEST_IBL_PATH)
        ImpressApiTestHelper.nativeSetLoadIblAssetFailure(TEST_ERROR_MESSAGE)

        val exception =
            assertFailsWith<Exception> { mImpressApi.loadImageBasedLightingAsset(TEST_IBL_PATH) }

        assertThat(exception).hasMessageThat().contains(TEST_ERROR_MESSAGE)
    }

    @Test
    fun loadImageBasedLightingAsset_fromByteArray_invokesCallbackOnFailure() = runBlocking {
        val testSize = 1024
        val testData = generateTestPattern(testSize)
        ImpressApiTestHelper.nativeSetExpectedLoadIblAssetTestPattern(testSize, TEST_IBL_KEY)
        ImpressApiTestHelper.nativeSetLoadIblAssetFailure(TEST_ERROR_MESSAGE)

        val exception =
            assertFailsWith<Exception> {
                mImpressApi.loadImageBasedLightingAsset(testData, TEST_IBL_KEY)
            }

        assertThat(exception).hasMessageThat().contains(TEST_ERROR_MESSAGE)
    }

    @Test
    fun releaseImageBasedLightingAsset_marshalsToken() {
        ImpressApiTestHelper.nativeSetExpectedReleaseIblAsset(TEST_NATIVE_TOKEN)

        mImpressApi.releaseImageBasedLightingAsset(TEST_NATIVE_TOKEN)

        // This JNI call does not return any data, so the only assertion is on the native side.
    }

    @Test
    fun setPreferredEnvironmentLight_marshalsToken() {
        ImpressApiTestHelper.nativeSetExpectedSetEnvironmentLight(TEST_NATIVE_TOKEN)

        mImpressApi.setPreferredEnvironmentLight(TEST_NATIVE_TOKEN)

        // This JNI call does not return any data, so the only assertion is on the native side.
    }

    @Test
    fun clearPreferredEnvironmentIblAsset_marshalsCall() {
        ImpressApiTestHelper.nativeSetExpectedClearEnvironmentLight()

        mImpressApi.clearPreferredEnvironmentIblAsset()

        // This JNI call does not return any data, so the only assertion is on the native side.
    }
}
