/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.common;

import static com.google.common.truth.Truth.assertThat;

import android.hardware.camera2.CaptureResult;
import androidx.camera.common.testing.FakeCaptureRequest;
import androidx.camera.common.testing.FakeCaptureResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Config.TARGET_SDK)
public final class CaptureResultWrapperJvmTest {

    @Test
    @SuppressWarnings("deprecation")
    public void fakeCaptureResultBehaviorFromJava() {
        CaptureRequestWrapper fakeRequest = FakeCaptureRequest.create();

        Map<CaptureResult.Key<?>, Object> parameters = new HashMap<>();
        parameters.put(CaptureResult.LENS_STATE, CaptureResult.LENS_STATE_STATIONARY);

        Metadata.Key<Integer> customKey = Metadata.Key.create("test.custom.key", Integer.class);
        Map<Metadata.Key<?>, Object> metadata = new HashMap<>();
        metadata.put(customKey, 42);

        // Use Java-friendly static factory method
        FakeCaptureResult fake = FakeCaptureResult.create(
            "0",
            42L,
            fakeRequest,
            parameters,
            metadata
        );

        // Test JVM getters
        assertThat(fake.getCameraId()).isEqualTo("0");

        assertThat(fake.getFrameNumber()).isEqualTo(42L);

        assertThat(fake.getCaptureRequest()).isSameInstanceAs(fakeRequest);

        assertThat(fake.get(CaptureResult.LENS_STATE))
            .isEqualTo(CaptureResult.LENS_STATE_STATIONARY);
        assertThat(fake.getOrDefault(CaptureResult.LENS_STATE, -1))
            .isEqualTo(CaptureResult.LENS_STATE_STATIONARY);
        assertThat(fake.getOrDefault(CaptureResult.CONTROL_AE_STATE, -1)).isEqualTo(-1);

        assertThat(fake.get(customKey)).isEqualTo(42);
        assertThat(fake.getOrDefault(customKey, -1)).isEqualTo(42);

        assertThat(fake.getKeys()).containsExactly(CaptureResult.LENS_STATE);
        assertThat(fake.getMetadataKeys()).containsExactly(customKey);
    }

    @Test
    public void javaImplementationCanBeInstantiatedAndUsed() {
        CaptureRequestWrapper fakeRequest = FakeCaptureRequest.create();
        Map<CaptureResult.Key<?>, Object> parameters = new HashMap<>();
        Map<Metadata.Key<?>, Object> metadata = new HashMap<>();
        Metadata.Key<Integer> customKey = Metadata.Key.create("test.custom.key", Integer.class);
        metadata.put(customKey, 100);

        CaptureResultWrapper javaMetadata = new TestJavaCaptureResult(
                "java-0", fakeRequest, 42L, parameters, metadata);

        assertThat(javaMetadata.getCameraId()).isEqualTo("java-0");
        assertThat(javaMetadata.getFrameNumber()).isEqualTo(42L);
        assertThat(javaMetadata.getCaptureRequest()).isSameInstanceAs(fakeRequest);
        assertThat(javaMetadata.get(customKey)).isEqualTo(100);
        assertThat(javaMetadata.getMetadataKeys()).containsExactly(customKey);
    }

    private static final class TestJavaCaptureResult implements CaptureResultWrapper {
        private final String mCameraId;
        private final CaptureRequestWrapper mCaptureRequest;
        private final long mFrameNumber;
        private final Map<CaptureResult.Key<?>, Object> mParameters;
        private final Map<Metadata.Key<?>, Object> mMetadata;

        TestJavaCaptureResult(
                String cameraId,
                CaptureRequestWrapper captureRequest,
                long frameNumber,
                Map<CaptureResult.Key<?>, Object> parameters,
                Map<Metadata.Key<?>, Object> metadata) {
            mCameraId = cameraId;
            mCaptureRequest = captureRequest;
            mFrameNumber = frameNumber;
            mParameters = parameters;
            mMetadata = metadata;
        }

        @Override
        public String getCameraId() {
            return mCameraId;
        }

        @Override
        public CaptureRequestWrapper getCaptureRequest() {
            return mCaptureRequest;
        }

        @Override
        public long getFrameNumber() {
            return mFrameNumber;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T get(Metadata.Key<T> key) {
            return (T) mMetadata.get(key);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T get(CaptureResult.Key<T> key) {
            return (T) mParameters.get(key);
        }

        @Override
        public Set<Metadata.Key<?>> getMetadataKeys() {
            return new HashSet<>(mMetadata.keySet());
        }

        @Override
        public List<CaptureResult.Key<?>> getKeys() {
            return new ArrayList<>(mParameters.keySet());
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T unwrapAs(Class<T> type) {
            if (type.isInstance(this)) {
                return (T) this;
            }
            return null;
        }
    }
}
