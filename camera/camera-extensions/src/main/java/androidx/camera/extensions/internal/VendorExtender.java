/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.extensions.internal;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureResult;
import android.util.Pair;
import android.util.Range;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.impl.SessionProcessor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A unified vendor extensions interface which interacts with both basic and advanced extender
 * vendor implementation.
 */
public interface VendorExtender {
    /**
     * Indicates whether the extension is supported on the device.
     *
     * <p>isExtensionAvailable is the only method that can be called ahead of init().
     *
     * @param cameraId           The camera2 id string of the camera.
     * @param characteristicsMap A map consisting of the camera ids and the
     *                           {@link CameraCharacteristics}s. For every camera, the map
     *                           contains at least the CameraCharacteristics for the camera id.
     *                           If the camera is logical camera, it will also contain associated
     *                           physical camera ids and their CameraCharacteristics.
     * @return true if the extension is supported, otherwise false
     */
    default boolean isExtensionAvailable(@NonNull String cameraId,
            @NonNull Map<String, CameraCharacteristics> characteristicsMap) {
        return false;
    }


    /**
     * Initializes the extender to be used with the specified camera.
     */
    default void init(@NonNull CameraInfo cameraInfo) {
    }

    /**
     * Gets the estimated latency range of image capture.
     *
     * <p>It must be called after init() is called.
     */
    @Nullable
    default Range<Long> getEstimatedCaptureLatencyRange(@Nullable Size size) {
        return null;
    }

    /**
     * Gets the supported output resolutions for preview. PRIVATE format must be supported.
     *
     * <p>Pair list composed with {@link ImageFormat} and {@link Size} array will be returned.
     *
     * <p>The returned resolutions should be subset of the supported sizes retrieved from
     * {@link android.hardware.camera2.params.StreamConfigurationMap} for the camera device.
     *
     * <p>The returned size array must contain all supported resolutions. It cannot be null.
     *
     * <p>It must be called after init() is called.
     */
    @NonNull
    default List<Pair<Integer, Size[]>> getSupportedPreviewOutputResolutions() {
        return Collections.emptyList();
    }

    /**
     * Gets the supported output resolutions for image capture. JPEG format must be supported.
     *
     * <p>Pair list composed with {@link ImageFormat} and {@link Size} array will be returned.
     *
     * <p>The returned resolutions should be subset of the supported sizes retrieved from
     * {@link android.hardware.camera2.params.StreamConfigurationMap} for the camera device.
     *
     * <p>The returned size array must contain all supported resolutions. It cannot be null.
     *
     * <p>It must be called after init() is called.
     */
    @NonNull
    default List<Pair<Integer, Size[]>> getSupportedCaptureOutputResolutions() {
        return Collections.emptyList();
    }

    /**
     * Gets the supported output resolutions for image analysis (YUV_420_888).
     *
     * <p>The returned resolutions should be subset of the supported sizes retrieved from
     * {@link android.hardware.camera2.params.StreamConfigurationMap} for the camera device.
     *
     * <p>The returned size array must contain all supported resolutions. It cannot be null.
     *
     * <p>It must be called after init() is called.
     */
    @NonNull
    default Size[] getSupportedYuvAnalysisResolutions() {
        return new Size[0];
    }

    /**
     * Returns supported output format/size map for postview image.
     *
     * <p>The returned sizes must be smaller than or equal to the provided capture size and have the
     * same aspect ratio as the given capture size. If no supported resolution exists for the
     * provided capture size then an empty map is returned.
     */
    @NonNull
    default Map<Integer, List<Size>> getSupportedPostviewResolutions(@NonNull Size captureSize) {
        return Collections.emptyMap();
    }

    /**
     * Returns if postview is supported or not.
     */
    default boolean isPostviewAvailable() {
        return false;
    }

    /**
     * Returns if the capture process progress is supported or not.
     */
    default boolean isCaptureProcessProgressAvailable() {
        return false;
    }

    /**
     * Returns if extension strength is supported or not.
     */
    default boolean isExtensionStrengthAvailable() {
        return false;
    }

    /**
     * Returns if reporting current extension mode is supported or not.
     */
    default boolean isCurrentExtensionModeAvailable() {
        return false;
    }

    /**
     * Creates a {@link SessionProcessor} that is responsible for (1) determining the stream
     * configuration based on given output surfaces (2) Requesting OEM implementation to start
     * repeating request and performing a still image capture.
     */
    @Nullable
    default SessionProcessor createSessionProcessor(@NonNull Context context) {
        return null;
    }

    /**
     * Return the list of supported {@link CaptureResult.Key}s that will be contained in the
     * onCaptureCompleted callback.
     */
    @NonNull
    default List<CaptureResult.Key> getSupportedCaptureResultKeys() {
        return Collections.emptyList();
    }

    /**
     * Returns if the onCaptureCompleted with capture result will be invoked or not.
     */
    default boolean willReceiveOnCaptureCompleted() {
        if (ClientVersion.isMaximumCompatibleVersion(Version.VERSION_1_2)
                || ExtensionVersion.isMaximumCompatibleVersion(Version.VERSION_1_2)) {
            // For OEM implementing v1.2 or below, onCaptureCompleted won't be invoked.
            return false;
        }

        // onCaptureCompleted is invoked when available captureResult keys are not empty.
        return !getSupportedCaptureResultKeys().isEmpty();
    }
}
