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

package androidx.camera.core.impl;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraExtensionSession;
import android.os.Build;
import android.util.Pair;
import android.util.Range;
import android.util.Size;

import androidx.annotation.IntRange;
import androidx.camera.core.CameraInfo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A session processor for transforming session info or operations.
 */
public interface SessionProcessor {
    /**
     * The default type for the session processor.
     */
    int TYPE_DEFAULT = 0;

    /**
     * The session processor is used for Camera2 extension modes that should create the capture
     * session via Camera2 Extensions API.
     */
    int TYPE_CAMERA2_EXTENSION = 1;

    /**
     * Initializes the session and returns a transformed {@link SessionConfig} which should be
     * used to configure the camera instead of original one.
     *
     * <p>Output surfaces of preview, image capture and imageAnalysis should be passed in. The
     * SessionProcessor is responsible to write the output to this given output surfaces.
     *
     * @param cameraInfo                 cameraInfo for querying the camera info
     * @param outputSurfaceConfig output surface configuration for preview, image capture,
     *                                  image analysis and the postview. This can be null under
     *                                  Camera2 Extensions implementation mode. In that case, this
     *                                  function is invoked to setup the necessary stuffs only.
     * @return a {@link SessionConfig} that contains the surfaces and the session parameters and
     * should be used to configure the camera session. Return null when the input
     * <code>outputSurfaceConfig</code> is null.
     */
    @Nullable SessionConfig initSession(@NonNull CameraInfo cameraInfo,
            @Nullable OutputSurfaceConfiguration outputSurfaceConfig);

    /**
     * De-initializes the session. This is called after the camera session is closed.
     */
    void deInitSession();

    /**
     * Returns supported output format/size map for postview image. The API is provided
     * for camera-core to query the supported postview sizes from SessionProcessor.
     */
    default @NonNull Map<Integer, List<Size>> getSupportedPostviewSize(@NonNull Size captureSize) {
        return Collections.emptyMap();
    }

    /**
     * Returns the supported camera operations when the SessionProcessor is enabled.
     */
    default @AdapterCameraInfo.CameraOperation @NonNull Set<Integer>
            getSupportedCameraOperations() {
        return Collections.emptySet();
    }

    default @NonNull List<Pair<CameraCharacteristics.Key, Object>>
            getAvailableCharacteristicsKeyValues() {
        return Collections.emptyList();
    }

    /**
     * Returns the extensions-specific zoom range
     */
    @SuppressWarnings("unchecked")
    default @Nullable Range<Float> getExtensionZoomRange() {
        if (Build.VERSION.SDK_INT >= 30) {
            List<Pair<CameraCharacteristics.Key, Object>> keyValues =
                    getAvailableCharacteristicsKeyValues();
            for (Pair<CameraCharacteristics.Key, Object> keyValue : keyValues) {
                if (keyValue.first.equals(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)) {
                    return (Range<Float>) keyValue.second;
                }
            }
        }
        return null;
    }

    /**
     * Returns the extensions-specific available stabilization modes.
     */
    @SuppressWarnings("unchecked")
    default int @Nullable [] getExtensionAvailableStabilizationModes() {
        List<Pair<CameraCharacteristics.Key, Object>> keyValues =
                getAvailableCharacteristicsKeyValues();
        for (Pair<CameraCharacteristics.Key, Object> keyValue : keyValues) {
            if (keyValue.first.equals(
                    CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)) {
                return (int[]) keyValue.second;
            }
        }
        return null;
    }

    /**
     * Returns the dynamically calculated capture latency pair in milliseconds.
     *
     * The measurement is expected to take in to account dynamic parameters such as the current
     * scene, the state of 3A algorithms, the state of internal HW modules and return a more
     * accurate assessment of the capture and/or processing latency.</p>
     *
     * @return pair that includes the estimated input frame/frames camera capture latency as the
     * first field. The second field value includes the estimated post-processing latency. Both
     * first and second values will be in milliseconds. The total still capture latency will be the
     * sum of both the first and second values of the pair. The pair is expected to be null if the
     * dynamic latency estimation is not supported. If clients have not configured a still capture
     * output, then this method can also return a null pair.
     */
    default @Nullable Pair<Long, Long> getRealtimeCaptureLatency() {
        return null;
    }

    /**
     * Returns the implementation type info composited by the extension impl type and the
     * extension mode.
     *
     * <p>The first value of the returned {@link Pair} can be {@link #TYPE_DEFAULT} or
     * {@link #TYPE_CAMERA2_EXTENSION} that can let the caller know how to use the
     * SessionProcessor to create the capture session. The second value is the mode under the impl
     * type.
     *
     * @return a {@link Pair} composited by the extension impl type and the extension mode.
     */
    @NonNull
    default Pair<Integer, Integer> getImplementationType() {
        return Pair.create(TYPE_DEFAULT, 0 /* ExtensionMode.None */);
    }

    /**
     * Sets a {@link CaptureSessionRequestProcessor} for retrieving specific information from the
     * camera capture session or submitting requests.
     *
     * <p>This is used for the SessionProcessor implementation that needs to directly interact
     * with the camera capture session to retrieve specific information or submit requests.
     *
     * <p>Callers should clear this by calling with null to avoid the session processor to hold
     * the camera capture session related resources.
     */
    default void setCaptureSessionRequestProcessor(
            @Nullable CaptureSessionRequestProcessor processor) {
    }

    /**
     * An interface for retrieving specific information from the camera capture session or
     * submitting requests.
     */
    interface CaptureSessionRequestProcessor {
        /**
         * Returns the realtime still capture latency information.
         *
         * @see CameraExtensionSession#getRealtimeStillCaptureLatency()
         */
        @Nullable
        Pair<Long, Long> getRealtimeStillCaptureLatency();

        /**
         * Sets the strength of the extension post-processing effect.
         *
         * @param strength the new extension strength value
         * @see android.hardware.camera2.CaptureRequest#EXTENSION_STRENGTH
         */
        void setExtensionStrength(@IntRange(from = 0, to = 100) int strength);
    }
}
