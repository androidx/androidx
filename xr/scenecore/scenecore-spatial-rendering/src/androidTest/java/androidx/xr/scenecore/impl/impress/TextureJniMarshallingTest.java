/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.xr.scenecore.impl.impress;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;

/** JNI marshalling tests for Texture operations. */
@SdkSuppress(minSdkVersion = 29)
@RunWith(AndroidJUnit4.class)
public final class TextureJniMarshallingTest extends BaseJniMarshallingTest {
    private static final String TEST_TEXTURE_PATH = "assets/texture.png";
    private static final long TEST_NATIVE_TOKEN = 12345L;
    private static final long TEST_IBL_TOKEN = 789L;
    private static final String TEST_ERROR_MESSAGE = "Test C++ Failure From Marshalling Test";

    @Test
    public void loadTexture_withPath_marshallsCorrectly() throws Exception {
        ImpressApiTestHelper.nativeSetExpectedLoadTexturePath(TEST_TEXTURE_PATH);
        ImpressApiTestHelper.nativeSetLoadTextureAssetSuccess(TEST_NATIVE_TOKEN);

        ListenableFuture<Texture> future = mImpressApi.loadTexture(TEST_TEXTURE_PATH);
        Texture texture = future.get();

        assertThat(texture).isNotNull();
        assertThat(texture.getNativeHandle()).isEqualTo(TEST_NATIVE_TOKEN);
    }

    @Test
    public void loadTexture_withPath_propagatesFailure() {
        ImpressApiTestHelper.nativeSetExpectedLoadTexturePath(TEST_TEXTURE_PATH);
        ImpressApiTestHelper.nativeSetLoadTextureAssetFailure(TEST_ERROR_MESSAGE);

        ListenableFuture<Texture> future = mImpressApi.loadTexture(TEST_TEXTURE_PATH);

        ExecutionException exception = assertThrows(ExecutionException.class, future::get);
        assertThat(exception).hasCauseThat().hasMessageThat().contains(TEST_ERROR_MESSAGE);
    }

    @Test
    public void borrowReflectionTexture_marshallsCorrectly() {
        ImpressApiTestHelper.nativeSetExpectedBorrowReflectionTexture();
        ImpressApiTestHelper.nativeSetBorrowReflectionTextureSuccessToken(TEST_NATIVE_TOKEN);

        Texture texture = mImpressApi.borrowReflectionTexture();

        assertThat(texture).isNotNull();
        assertThat(texture.getNativeHandle()).isEqualTo(TEST_NATIVE_TOKEN);
    }

    @Test
    public void getReflectionTextureFromIbl_marshallsCorrectly() {
        ImpressApiTestHelper.nativeSetExpectedGetReflectionTextureFromIbl(TEST_IBL_TOKEN);
        ImpressApiTestHelper.nativeSetGetReflectionTextureFromIblSuccessToken(TEST_NATIVE_TOKEN);

        Texture texture = mImpressApi.getReflectionTextureFromIbl(TEST_IBL_TOKEN);

        assertThat(texture).isNotNull();
        assertThat(texture.getNativeHandle()).isEqualTo(TEST_NATIVE_TOKEN);
    }
}
