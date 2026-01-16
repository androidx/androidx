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

import android.util.Log;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;

/** Base class for all JNI Marshalling tests. */
public abstract class BaseJniMarshallingTest {
    private static final byte TEST_PATTERN_BYTE = (byte) 0xcd;
    private static final byte TEST_PATTERN_SENTINEL = (byte) 0xee;
    protected ImpressApi mImpressApi;
    protected long mTestViewHandle;

    @SuppressWarnings("VisiblySynchronized")
    // It is safe to suppress this warning because this is a test class. There is no risk of
    // external callers causing deadlocks by synchronizing on this.
    protected static synchronized void loadLibraryAsync(@NonNull String nativeLibraryName) {
        try {
            System.loadLibrary(nativeLibraryName);
        } catch (UnsatisfiedLinkError e) {
            Log.e(
                    "BaseJniMarshallingTest",
                    "Unable to load " + nativeLibraryName + " " + e.getMessage());
            return;
        }
    }

    @Before
    public void setUp() {
        loadLibraryAsync("test_impress_api_jni");
        ImpressApiTestHelper.nativeResetTestState();

        mTestViewHandle = ImpressApiTestHelper.nativeCreateTestView();
        assertThat(mTestViewHandle).isNotEqualTo(0);
        assertThat(mTestViewHandle).isNotEqualTo(-1);

        mImpressApi = new ImpressApiImpl();
        mImpressApi.setup(mTestViewHandle);
    }

    @After
    public void tearDown() {
        if (mTestViewHandle != 0) {
            ImpressApiTestHelper.nativeDestroyTestView(mTestViewHandle);
        }
        ImpressApiTestHelper.nativeResetTestState();
    }

    protected byte[] generateTestPattern(int size) {
        byte[] data = new byte[size];
        for (int i = 0; i < size - 1; i++) {
            data[i] = TEST_PATTERN_BYTE;
        }
        if (size > 0) {
            data[size - 1] = TEST_PATTERN_SENTINEL;
        }
        return data;
    }
}
