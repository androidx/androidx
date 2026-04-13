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

/** JNI marshalling tests for Texture operations. */
@SdkSuppress(minSdkVersion = 29)
@RunWith(AndroidJUnit4::class)
class TextureJniMarshallingTest : BaseJniMarshallingTest() {

    companion object {
        private const val TEST_TEXTURE_PATH = "assets/texture.png"
        private const val TEST_NATIVE_TOKEN = 12345L
        private const val TEST_IBL_TOKEN = 789L
        private const val TEST_ERROR_MESSAGE = "Test C++ Failure From Marshalling Test"
    }

    @Test
    fun loadTexture_withPath_marshallsCorrectly() = runBlocking {
        ImpressApiTestHelper.nativeSetExpectedLoadTexturePath(TEST_TEXTURE_PATH)
        ImpressApiTestHelper.nativeSetLoadTextureAssetSuccess(TEST_NATIVE_TOKEN)

        val texture = mImpressApi.loadTexture(TEST_TEXTURE_PATH)

        assertThat(texture).isNotNull()
        assertThat(texture.nativeHandle).isEqualTo(TEST_NATIVE_TOKEN)
    }

    @Test
    fun loadTexture_withPath_propagatesFailure() = runBlocking {
        ImpressApiTestHelper.nativeSetExpectedLoadTexturePath(TEST_TEXTURE_PATH)
        ImpressApiTestHelper.nativeSetLoadTextureAssetFailure(TEST_ERROR_MESSAGE)

        val exception = assertFailsWith<Exception> { mImpressApi.loadTexture(TEST_TEXTURE_PATH) }

        assertThat(exception).hasMessageThat().contains(TEST_ERROR_MESSAGE)
    }

    @Test
    fun borrowReflectionTexture_marshallsCorrectly() {
        ImpressApiTestHelper.nativeSetExpectedBorrowReflectionTexture()
        ImpressApiTestHelper.nativeSetBorrowReflectionTextureSuccessToken(TEST_NATIVE_TOKEN)

        val texture = mImpressApi.borrowReflectionTexture()

        assertThat(texture).isNotNull()
        assertThat(texture.nativeHandle).isEqualTo(TEST_NATIVE_TOKEN)
    }

    @Test
    fun getReflectionTextureFromIbl_marshallsCorrectly() {
        ImpressApiTestHelper.nativeSetExpectedGetReflectionTextureFromIbl(TEST_IBL_TOKEN)
        ImpressApiTestHelper.nativeSetGetReflectionTextureFromIblSuccessToken(TEST_NATIVE_TOKEN)

        val texture = mImpressApi.getReflectionTextureFromIbl(TEST_IBL_TOKEN)

        assertThat(texture).isNotNull()
        assertThat(texture.nativeHandle).isEqualTo(TEST_NATIVE_TOKEN)
    }
}
