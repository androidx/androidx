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

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.content.Context;
import android.hardware.camera2.CameraManager;
import androidx.camera.common.testing.FakeCameraCharacteristics;
import androidx.test.core.app.ApplicationProvider;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowCameraCharacteristics;
import org.robolectric.shadows.ShadowCameraManager;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Config.TARGET_SDK)
public final class CameraCharacteristicsWrapperJvmTest {

    @Test
    @SuppressWarnings("deprecation")
    public void fakeCameraCharacteristicsBehaviorFromJava() {
        Map<CameraCharacteristics.Key<?>, Object> characteristics = new HashMap<>();
        characteristics.put(
            CameraCharacteristics.LENS_FACING,
            CameraCharacteristics.LENS_FACING_FRONT
        );

        Metadata.Key<Integer> customKey = Metadata.Key.create("test.custom.key", Integer.class);
        Map<Metadata.Key<?>, Object> metadata = new HashMap<>();
        metadata.put(customKey, 42);

        // Use Java-friendly static factory method
        FakeCameraCharacteristics fake =
            FakeCameraCharacteristics.create("0", characteristics, metadata);

        // Test JVM getters
        assertThat(fake.getCameraId()).isEqualTo("0");

        assertThat(fake.get(CameraCharacteristics.LENS_FACING))
            .isEqualTo(CameraCharacteristics.LENS_FACING_FRONT);
        assertThat(fake.getOrDefault(CameraCharacteristics.LENS_FACING, -1))
            .isEqualTo(CameraCharacteristics.LENS_FACING_FRONT);
        assertThat(fake.getOrDefault(CameraCharacteristics.SENSOR_ORIENTATION, 90)).isEqualTo(90);

        assertThat(fake.get(customKey)).isEqualTo(42);
        assertThat(fake.getOrDefault(customKey, -1)).isEqualTo(42);

        assertThat(fake.getKeys()).containsExactly(CameraCharacteristics.LENS_FACING);
        assertThat(fake.getMetadataKeys()).containsExactly(customKey);
    }

    @Test
    public void javaImplementationCanBeInstantiatedAndUsed() {
        Map<CameraCharacteristics.Key<?>, Object> characteristics = new HashMap<>();
        Map<Metadata.Key<?>, Object> metadata = new HashMap<>();
        Metadata.Key<Integer> customKey = Metadata.Key.create("test.custom.key", Integer.class);
        metadata.put(customKey, 100);

        CameraCharacteristicsWrapper javaMetadata = new TestJavaCameraCharacteristics(
                "java-0", characteristics, metadata);

        assertThat(javaMetadata.getCameraId()).isEqualTo("java-0");
        assertThat(javaMetadata.get(customKey)).isEqualTo(100);
        assertThat(javaMetadata.getMetadataKeys()).containsExactly(customKey);
    }

    @Test
    public void testLoadFromContext() {
        Context context = ApplicationProvider.getApplicationContext();
        CameraManager cameraManager = context.getSystemService(CameraManager.class);
        ShadowCameraManager shadowCameraManager = Shadows.shadowOf(cameraManager);
        shadowCameraManager.addCamera(
                "0", ShadowCameraCharacteristics.newCameraCharacteristics());

        CameraCharacteristicsWrapper wrapper =
                androidx.camera.common.CameraCharacteristics.loadFrom(context, "0");
        assertThat(wrapper).isNotNull();
        assertThat(wrapper.getCameraId()).isEqualTo("0");
    }

    @Test
    public void testLoadFromCameraManager() {
        Context context = ApplicationProvider.getApplicationContext();
        CameraManager cameraManager = context.getSystemService(CameraManager.class);
        ShadowCameraManager shadowCameraManager = Shadows.shadowOf(cameraManager);
        shadowCameraManager.addCamera(
                "0", ShadowCameraCharacteristics.newCameraCharacteristics());

        CameraCharacteristicsWrapper wrapper =
                androidx.camera.common.CameraCharacteristics.loadFrom(cameraManager, "0");
        assertThat(wrapper).isNotNull();
        assertThat(wrapper.getCameraId()).isEqualTo("0");
    }

    private static final class TestJavaCameraCharacteristics
            implements CameraCharacteristicsWrapper {
        private final String mCameraId;
        private final Map<CameraCharacteristics.Key<?>, Object> mCharacteristics;
        private final Map<Metadata.Key<?>, Object> mMetadata;

        TestJavaCameraCharacteristics(
                String cameraId,
                Map<CameraCharacteristics.Key<?>, Object> characteristics,
                Map<Metadata.Key<?>, Object> metadata) {
            mCameraId = cameraId;
            mCharacteristics = characteristics;
            mMetadata = metadata;
        }

        @Override
        public String getCameraId() {
            return mCameraId;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T get(Metadata.Key<T> key) {
            return (T) mMetadata.get(key);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T get(CameraCharacteristics.Key<T> key) {
            return (T) mCharacteristics.get(key);
        }

        @Override
        public Set<Metadata.Key<?>> getMetadataKeys() {
            return new HashSet<>(mMetadata.keySet());
        }

        @Override
        public Set<CameraCharacteristics.Key<?>> getKeys() {
            return new HashSet<>(mCharacteristics.keySet());
        }

        @Override
        public Set<CaptureRequest.Key<?>> getCaptureRequestKeys() {
            return Collections.emptySet();
        }

        @Override
        public Set<CaptureResult.Key<?>> getCaptureResultKeys() {
            return Collections.emptySet();
        }

        @Override
        public Set<CaptureRequest.Key<?>> getPhysicalCaptureRequestKeys() {
            return Collections.emptySet();
        }

        @Override
        public Set<CameraCharacteristics.Key<?>> getSessionKeys() {
            return Collections.emptySet();
        }

        @Override
        public Set<CaptureRequest.Key<?>> getSessionCaptureRequestKeys() {
            return Collections.emptySet();
        }

        @Override
        public Set<CameraCharacteristics.Key<?>> getRestrictedKeys() {
            return Collections.emptySet();
        }

        @Override
        public boolean isRestricted() {
            return false;
        }

        @Override
        public Set<CameraCharacteristics.Key<?>> getDynamicKeys() {
            return Collections.emptySet();
        }

        @Override
        public Set<CameraId> getPhysicalCameraIds() {
            return Collections.emptySet();
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
