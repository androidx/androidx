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

package androidx.xr.scenecore.impl.impress;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import static java.util.concurrent.TimeUnit.SECONDS;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;

/** JNI Marshaling Tests for the Skybox endpoints of the Impress API bindings. */
@SdkSuppress(minSdkVersion = 29)
@RunWith(AndroidJUnit4.class)
public class SkyboxJniMarshallingTest extends BaseJniMarshallingTest {
    private static final String TEST_IBL_PATH = "skybox/sky.zip";
    private static final String TEST_IBL_KEY = "ibl_key";
    private static final long TEST_NATIVE_TOKEN = 67890L;
    private static final String TEST_ERROR_MESSAGE = "Test C++ IBL Failure";

    @Test
    public void loadImageBasedLightingAsset_marshalsPath_invokesCallbackOnSuccess()
            throws Exception {
        ImpressApiTestHelper.nativeSetExpectedLoadIblPath(TEST_IBL_PATH);
        ImpressApiTestHelper.nativeSetLoadIblAssetSuccess(TEST_NATIVE_TOKEN);

        ListenableFuture<ExrImage> future = mImpressApi.loadImageBasedLightingAsset(TEST_IBL_PATH);
        ExrImage actualImage = future.get(5, SECONDS);

        Long actualToken = actualImage.getNativeHandle();
        assertThat(actualToken).isEqualTo(TEST_NATIVE_TOKEN);
    }

    @Test
    public void loadImageBasedLightingAsset_fromByteArray_invokesCallbackOnSuccess()
            throws Exception {
        int testSize = 1024;
        byte[] testData = generateTestPattern(testSize);
        ImpressApiTestHelper.nativeSetExpectedLoadIblAssetTestPattern(testSize, TEST_IBL_KEY);
        ImpressApiTestHelper.nativeSetLoadIblAssetSuccess(TEST_NATIVE_TOKEN);

        ListenableFuture<ExrImage> future =
                mImpressApi.loadImageBasedLightingAsset(testData, TEST_IBL_KEY);
        ExrImage actualImage = future.get(5, SECONDS);

        Long actualToken = actualImage.getNativeHandle();
        assertThat(actualToken).isEqualTo(TEST_NATIVE_TOKEN);
    }

    @Test
    public void loadImageBasedLightingAsset_marshalsPath_invokesCallbackOnFailure() {
        ImpressApiTestHelper.nativeSetExpectedLoadIblPath(TEST_IBL_PATH);
        ImpressApiTestHelper.nativeSetLoadIblAssetFailure(TEST_ERROR_MESSAGE);

        ListenableFuture<ExrImage> future = mImpressApi.loadImageBasedLightingAsset(TEST_IBL_PATH);
        ExecutionException exception =
                assertThrows(ExecutionException.class, () -> future.get(5, SECONDS));

        assertThat(exception).hasCauseThat().isInstanceOf(Exception.class);
        assertThat(exception).hasMessageThat().contains(TEST_ERROR_MESSAGE);
    }

    @Test
    public void loadImageBasedLightingAsset_fromByteArray_invokesCallbackOnFailure() {
        int testSize = 1024;
        byte[] testData = generateTestPattern(testSize);
        ImpressApiTestHelper.nativeSetExpectedLoadIblAssetTestPattern(testSize, TEST_IBL_KEY);
        ImpressApiTestHelper.nativeSetLoadIblAssetFailure(TEST_ERROR_MESSAGE);

        ListenableFuture<ExrImage> future =
                mImpressApi.loadImageBasedLightingAsset(testData, TEST_IBL_KEY);
        ExecutionException exception =
                assertThrows(ExecutionException.class, () -> future.get(5, SECONDS));

        assertThat(exception).hasCauseThat().isInstanceOf(Exception.class);
        assertThat(exception).hasMessageThat().contains(TEST_ERROR_MESSAGE);
    }

    @Test
    public void releaseImageBasedLightingAsset_marshalsToken() {
        ImpressApiTestHelper.nativeSetExpectedReleaseIblAsset(TEST_NATIVE_TOKEN);

        mImpressApi.releaseImageBasedLightingAsset(TEST_NATIVE_TOKEN);

        // This JNI call does not return any data, so the only assertion is on the native side.
    }

    @Test
    public void setPreferredEnvironmentLight_marshalsToken() {
        ImpressApiTestHelper.nativeSetExpectedSetEnvironmentLight(TEST_NATIVE_TOKEN);

        mImpressApi.setPreferredEnvironmentLight(TEST_NATIVE_TOKEN);

        // This JNI call does not return any data, so the only assertion is on the native side.
    }

    @Test
    public void clearPreferredEnvironmentIblAsset_marshalsCall() {
        ImpressApiTestHelper.nativeSetExpectedClearEnvironmentLight();

        mImpressApi.clearPreferredEnvironmentIblAsset();

        // This JNI call does not return any data, so the only assertion is on the native side.
    }
}
