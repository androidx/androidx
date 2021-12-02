/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.extensions.impl;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.util.Pair;
import android.util.Range;
import android.util.Size;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.List;

/**
 * Provides abstract methods that the OEM needs to implement to enable extensions for image capture.
 *
 * @since 1.0
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface ImageCaptureExtenderImpl extends ExtenderStateListener {
    /**
     * Indicates whether the extension is supported on the device.
     *
     * @param cameraId The camera2 id string of the camera.
     * @param cameraCharacteristics The {@link CameraCharacteristics} of the camera.
     * @return true if the extension is supported, otherwise false
     */
    boolean isExtensionAvailable(String cameraId, CameraCharacteristics cameraCharacteristics);

    /**
     * Initializes the extender to be used with the specified camera.
     *
     * <p>This should be called before any other method on the extender. The exception is {@link
     * #isExtensionAvailable(String, CameraCharacteristics)}.
     *
     * @param cameraId The camera2 id string of the camera.
     * @param cameraCharacteristics The {@link CameraCharacteristics} of the camera.
     */
    void init(String cameraId, CameraCharacteristics cameraCharacteristics);

    /**
     * The processing that will be done on a set of captures to create and image with the effect.
     */
    CaptureProcessorImpl getCaptureProcessor();

    /** The set of captures that are needed to create an image with the effect. */
    List<CaptureStageImpl> getCaptureStages();

    /**
     * Returns the maximum size of the list returned by {@link #getCaptureStages()}.
     * @return the maximum count.
     */
    int getMaxCaptureStage();

    /**
     * Returns the customized supported resolutions.
     *
     * <p>Pair list composed with {@link ImageFormat} and {@link Size} array will be returned.
     *
     * <p>The returned resolutions should be subset of the supported sizes retrieved from
     * {@link android.hardware.camera2.params.StreamConfigurationMap} for the camera device. If the
     * returned list is not null, it will be used to find the best resolutions combination for
     * the bound use cases.
     *
     * @return the customized supported resolutions.
     * @since 1.1
     */
    @Nullable
    List<Pair<Integer, Size[]>> getSupportedResolutions();

    /**
     * Returns the estimated capture latency range in milliseconds for the target capture
     * resolution.
     *
     * <p> This includes the time spent processing the multi-frame capture request along with any
     * additional time for encoding of the processed buffer in the framework if necessary.
     *
     * @param captureOutputSize size of the capture output surface. If it is null or not in the
     *                          supported output sizes, maximum capture output size is used for
     *                          the estimation.
     * @return the range of estimated minimal and maximal capture latency in milliseconds.
     * Returns null if no capture latency info can be provided.
     * @since 1.2
     */
    @Nullable
    Range<Long> getEstimatedCaptureLatencyRange(@Nullable Size captureOutputSize);
}

