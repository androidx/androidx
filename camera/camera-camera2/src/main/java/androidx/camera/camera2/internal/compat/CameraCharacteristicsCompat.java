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
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;

import androidx.annotation.GuardedBy;
import androidx.annotation.IntRange;
import androidx.annotation.VisibleForTesting;
import androidx.camera.camera2.internal.compat.workaround.OutputSizesCorrector;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A wrapper for {@link CameraCharacteristics} which caches the retrieved values to optimize
 * the latency and might contain backward compatible fixes for certain parameters.
 */
public class CameraCharacteristicsCompat {
    @GuardedBy("this")
    private final @NonNull Map<CameraCharacteristics.Key<?>, Object> mValuesCache = new HashMap<>();
    private final @NonNull CameraCharacteristicsCompatImpl mCameraCharacteristicsImpl;
    private final @NonNull String mCameraId;

    private @Nullable StreamConfigurationMapCompat mStreamConfigurationMapCompat = null;

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
    public static @NonNull CameraCharacteristicsCompat toCameraCharacteristicsCompat(
            @NonNull CameraCharacteristics characteristics, @NonNull String cameraId) {
        return new CameraCharacteristicsCompat(characteristics, cameraId);
    }

    /**
     * Return true if the key should be retrieved from {@link CameraCharacteristics} without
     * caching it.
     */
    private boolean isKeyNonCacheable(CameraCharacteristics.@NonNull Key<?> key) {
        // SENSOR_ORIENTATION value should change in some circumstances.
        return key.equals(CameraCharacteristics.SENSOR_ORIENTATION);
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
    public <T> @Nullable T get(CameraCharacteristics.@NonNull Key<T> key) {
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
    public @NonNull Set<String> getPhysicalCameraIds() {
        return mCameraCharacteristicsImpl.getPhysicalCameraIds();
    }

    /**
     * Returns {@code true} if overriding zoom setting is available, otherwise {@code false}.
     */
    public boolean isZoomOverrideAvailable() {
        if (Build.VERSION.SDK_INT >= 34) {
            int[] availableSettingsOverrides = mCameraCharacteristicsImpl.get(
                    CameraCharacteristics.CONTROL_AVAILABLE_SETTINGS_OVERRIDES);
            if (availableSettingsOverrides != null) {
                for (int i : availableSettingsOverrides) {
                    if (i == CameraMetadata.CONTROL_SETTINGS_OVERRIDE_ZOOM) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns the default torch strength level.
     */
    public int getDefaultTorchStrengthLevel() {
        Integer defaultLevel = null;
        if (hasFlashUnit() && Build.VERSION.SDK_INT >= 35) {
            defaultLevel = get(CameraCharacteristics.FLASH_TORCH_STRENGTH_DEFAULT_LEVEL);
        }
        // The framework returns 1 when the device doesn't support configuring torch strength. So
        // also return 1 if the device doesn't have flash unit or is unable to provide the
        // information.
        return defaultLevel == null ? 1 : defaultLevel;
    }

    /**
     * Returns the maximum torch strength level.
     */
    @IntRange(from = 1)
    public int getMaxTorchStrengthLevel() {
        Integer maxLevel = null;
        if (hasFlashUnit() && Build.VERSION.SDK_INT >= 35) {
            maxLevel = get(CameraCharacteristics.FLASH_TORCH_STRENGTH_MAX_LEVEL);
        }
        // The framework returns 1 when the device doesn't support configuring torch strength. So
        // also return 1 if the device doesn't have flash unit or is unable to provide the
        // information.
        return maxLevel == null ? 1 : maxLevel;
    }

    public boolean isTorchStrengthLevelSupported() {
        return hasFlashUnit() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
                && getMaxTorchStrengthLevel() > 1;
    }

    private boolean hasFlashUnit() {
        Boolean flashInfoAvailable = get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        return flashInfoAvailable != null && flashInfoAvailable;
    }

    /**
     * Obtains the {@link StreamConfigurationMapCompat} which contains the output sizes related
     * workarounds in it.
     */
    public @NonNull StreamConfigurationMapCompat getStreamConfigurationMapCompat() {
        if (mStreamConfigurationMapCompat == null) {
            StreamConfigurationMap map;
            try {
                map = get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            } catch (NullPointerException | AssertionError e) {
                // Some devices may throw AssertionError when querying stream configuration map
                // from CameraCharacteristics during bindToLifecycle. Catch the AssertionError and
                // throw IllegalArgumentException so app level can decide how to handle.
                throw new IllegalArgumentException(e.getMessage());
            }
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
    public @NonNull CameraCharacteristics toCameraCharacteristics() {
        return mCameraCharacteristicsImpl.unwrap();
    }

    /**
     * Returns the camera id associated with the camera characteristics.
     */
    public @NonNull String getCameraId() {
        return mCameraId;
    }

    /**
     * CameraCharacteristic Implementation Interface
     */
    public interface CameraCharacteristicsCompatImpl {
        /**
         * Gets the key/values from the CameraCharacteristics.
         */
        <T> @Nullable T get(CameraCharacteristics.@NonNull Key<T> key);

        /**
         * Gets physical camera ids.
         */
        @NonNull Set<String> getPhysicalCameraIds();

        /**
         * Returns the underlying {@link CameraCharacteristics} instance.
         */
        @NonNull CameraCharacteristics unwrap();
    }
}