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

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;

/** JNI Marshaling Tests for the Impress API bindings. */
@SdkSuppress(minSdkVersion = 29)
@RunWith(AndroidJUnit4.class)
public class ImpressApiMarshallingTest {
    private ImpressApi mImpressApi;

    @Before
    public void setUp() {
        loadLibraryAsync("test_impress_api_jni");
        ImpressApiTestHelper.nativeResetTestState();
        mImpressApi = new ImpressApiImpl();
    }

    @After
    public void tearDown() {
        ImpressApiTestHelper.nativeResetTestState();
    }

    @Test
    public void loadGltfAsset_marshalsPath_invokesCallbackOnSuccess() throws Exception {
        String expectedPath = "test/model.gltf";
        long expectedToken = 12345L;
        ImpressApiTestHelper.nativeSetExpectedLoadGltfPath(expectedPath);
        ImpressApiTestHelper.nativeSetLoadGltfAssetSuccess(expectedToken);
        // TODO(b/446592272): Re-enable this assertion when the JNI marshaling refactor is complete.
        // ListenableFuture<GltfModel> future = mImpressApi.loadGltfAsset(expectedPath);
        // GltfModel actualModel = future.get(5, SECONDS);
        // Long actualToken = actualModel.getNativeHandle();
        // assertThat(actualToken).isEqualTo(expectedToken);
    }

    @Test
    public void loadGltfAsset_marshalsPath_invokesCallbackOnFailure() {
        String expectedPath = "another/model.gltf";
        String expectedErrorMessage = "Test C++ Failure From Marshalling Test";
        ImpressApiTestHelper.nativeSetExpectedLoadGltfPath(expectedPath);
        ImpressApiTestHelper.nativeSetLoadGltfAssetFailure(expectedErrorMessage);
        ListenableFuture<GltfModel> future = mImpressApi.loadGltfAsset(expectedPath);
        ExecutionException exception =
                assertThrows(ExecutionException.class, () -> future.get(5, SECONDS));
        assertThat(exception).hasCauseThat().isInstanceOf(Exception.class);
        assertThat(exception).hasMessageThat().contains(expectedErrorMessage);
    }

    @SuppressWarnings("VisiblySynchronized")
    protected static synchronized void loadLibraryAsync(@NonNull String nativeLibraryName) {
        try {
            System.loadLibrary(nativeLibraryName);
        } catch (UnsatisfiedLinkError e) {
            Log.e(
                    "ImpressApiMarshallingTest",
                    "Unable to load " + nativeLibraryName + " " + e.getMessage());
            return;
        }
    }
}
