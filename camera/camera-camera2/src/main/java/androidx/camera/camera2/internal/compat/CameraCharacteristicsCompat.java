/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.internal.compat;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.camera.camera2.internal.compat.workaround.OutputSizesCorrector;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A wrapper for {@link CameraCharacteristics} which caches the retrieved values to optimize
 * the latency and might contain backward compatible fixes for certain parameters.
 */
@RequiresApi(21)
public class CameraCharacteristicsCompat {
    @NonNull
    @GuardedBy("this")
    private final Map<CameraCharacteristics.Key<?>, Object> mValuesCache = new HashMap<>();
    @NonNull
    private final CameraCharacteristicsCompatImpl mCameraCharacteristicsImpl;
    @NonNull
    private final String mCameraId;

    @Nullable
    private StreamConfigurationMapCompat mStreamConfigurationMapCompat = null;

    private CameraCharacteristicsCompat(@NonNull CameraCharacteristics cameraCharacteristics,
            @NonNull String cameraId) {
        if (Build.VERSION.SDK_INT >= 28) {
            mCameraCharacteristicsImpl = new CameraCharacteristicsApi28Impl(cameraCharacteristics);
        } else {
            mCameraCharacteristicsImpl = new CameraCharacteristicsBaseImpl(cameraCharacteristics);
        }
        mCameraId = cameraId;
    }

    /**
     * Tests might need to create CameraCharacteristicsCompat directly for convenience. Elsewhere
     * we should get the CameraCharacteristicsCompat instance from {@link CameraManagerCompat}.
     */
    @VisibleForTesting
    @NonNull
    public static CameraCharacteristicsCompat toCameraCharacteristicsCompat(
            @NonNull CameraCharacteristics characteristics, @NonNull String cameraId) {
        return new CameraCharacteristicsCompat(characteristics, cameraId);
    }

    /**
     * Return true if the key should be retrieved from {@link CameraCharacteristics} without
     * caching it.
     */
    private boolean isKeyNonCacheable(@NonNull CameraCharacteristics.Key<?> key) {
        // SENSOR_ORIENTATION value scould change in some circumstances.
        if (key.equals(CameraCharacteristics.SENSOR_ORIENTATION)) {
            return true;
        }
        return false;
    }

    /**
     * Gets a camera characteristics field value and caches the value for later use.
     *
     * <p>It will cache the value once get() is called. If get() is called more than once using
     * the same key, it will return instantly.
     *
     * @param key The characteristics field to read.
     * @return The value of that key, or null if the field is not set.
     */
    @Nullable
    public <T> T get(@NonNull CameraCharacteristics.Key<T> key) {
        // For some keys that will have varying value and cannot be cached, we need to always
        // retrieve the key from the CameraCharacteristics.
        if (isKeyNonCacheable(key)) {
            return mCameraCharacteristicsImpl.get(key);
        }

        synchronized (this) {
            @SuppressWarnings("unchecked") // The value type always matches the key type.
            T value = (T) mValuesCache.get(key);
            if (value != null) {
                return value;
            }

            value = mCameraCharacteristicsImpl.get(key);
            if (value != null) {
                mValuesCache.put(key, value);
            }
            return value;
        }
    }

    /**
     * Returns the physical camera Ids if it is a logical camera. Otherwise it would
     * return an empty set.
     */
    @NonNull
    public Set<String> getPhysicalCameraIds() {
        return mCameraCharacteristicsImpl.getPhysicalCameraIds();
    }

    /**
     * Obtains the {@link StreamConfigurationMapCompat} which contains the output sizes related
     * workarounds in it.
     */
    @NonNull
    public StreamConfigurationMapCompat getStreamConfigurationMapCompat() {
        if (mStreamConfigurationMapCompat == null) {
            StreamConfigurationMap map = get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) {
                throw new IllegalArgumentException("StreamConfigurationMap is null!");
            }
            OutputSizesCorrector outputSizesCorrector = new OutputSizesCorrector(mCameraId);
            mStreamConfigurationMapCompat =
                    StreamConfigurationMapCompat.toStreamConfigurationMapCompat(map,
                            outputSizesCorrector);
        }

        return mStreamConfigurationMapCompat;
    }

    /**
     * Returns the {@link CameraCharacteristics} represented by this object.
     */
    @NonNull
    public CameraCharacteristics toCameraCharacteristics() {
        return mCameraCharacteristicsImpl.unwrap();
    }

    /**
     * CameraCharacteristic Implementation Interface
     */
    public interface CameraCharacteristicsCompatImpl {
        /**
         * Gets the key/values from the CameraCharacteristics .
         */
        @Nullable
        <T> T get(@NonNull CameraCharacteristics.Key<T> key);

        /**
         * Get physical camera ids.
         */
        @NonNull
        Set<String> getPhysicalCameraIds();

        /**
         * Returns the underlying {@link CameraCharacteristics} instance.
         */
        @NonNull
        CameraCharacteristics unwrap();
    }
}