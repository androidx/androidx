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

import android.hardware.camera2.CaptureRequest;
import androidx.camera.common.testing.FakeCaptureRequest;
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
public final class CaptureRequestWrapperJvmTest {

    @Test
    public void fakeCaptureRequestBehaviorFromJava() {
        Map<CaptureRequest.Key<?>, Object> parameters = new HashMap<>();
        parameters.put(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);

        Metadata.Key<Integer> customKey = Metadata.Key.create("test.custom.key", Integer.class);
        Map<Metadata.Key<?>, Object> metadata = new HashMap<>();
        metadata.put(customKey, 42);

        FakeCaptureRequest fake = FakeCaptureRequest.create(parameters, metadata);

        assertThat(fake.get(CaptureRequest.CONTROL_AE_MODE))
            .isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON);
        assertThat(fake.getOrDefault(CaptureRequest.CONTROL_AE_MODE, -1))
            .isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON);
        assertThat(fake.getOrDefault(CaptureRequest.CONTROL_AF_MODE, -1)).isEqualTo(-1);

        assertThat(fake.get(customKey)).isEqualTo(42);
        assertThat(fake.getOrDefault(customKey, -1)).isEqualTo(42);

        assertThat(fake.getKeys()).containsExactly(CaptureRequest.CONTROL_AE_MODE);
        assertThat(fake.getMetadataKeys()).containsExactly(customKey);
    }

    @Test
    public void javaImplementationCanBeInstantiatedAndUsed() {
        Map<CaptureRequest.Key<?>, Object> parameters = new HashMap<>();
        Map<Metadata.Key<?>, Object> metadata = new HashMap<>();
        Metadata.Key<Integer> customKey = Metadata.Key.create("test.custom.key", Integer.class);
        metadata.put(customKey, 100);

        CaptureRequestWrapper javaMetadata =
                new TestJavaCaptureRequest(parameters, metadata);

        assertThat(javaMetadata.get(customKey)).isEqualTo(100);
        assertThat(javaMetadata.getMetadataKeys()).containsExactly(customKey);
    }

    private static final class TestJavaCaptureRequest implements CaptureRequestWrapper {
        private final Map<CaptureRequest.Key<?>, Object> mParameters;
        private final Map<Metadata.Key<?>, Object> mMetadata;

        TestJavaCaptureRequest(
                Map<CaptureRequest.Key<?>, Object> parameters,
                Map<Metadata.Key<?>, Object> metadata) {
            mParameters = parameters;
            mMetadata = metadata;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T get(Metadata.Key<T> key) {
            return (T) mMetadata.get(key);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T get(CaptureRequest.Key<T> key) {
            return (T) mParameters.get(key);
        }

        @Override
        public Set<Metadata.Key<?>> getMetadataKeys() {
            return new HashSet<>(mMetadata.keySet());
        }

        @Override
        public List<CaptureRequest.Key<?>> getKeys() {
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
