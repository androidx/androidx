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
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.util.Pair;
import android.util.Range;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * Provides abstract methods that the OEM needs to implement to enable extensions for image capture.
 *
 * @since 1.0
 */
public interface ImageCaptureExtenderImpl extends ExtenderStateListener {
    /**
     * Indicates whether the extension is supported on the device.
     *
     * @param cameraId The camera2 id string of the camera.
     * @param cameraCharacteristics The {@link CameraCharacteristics} of the camera.
     * @return true if the extension is supported, otherwise false
     */
    boolean isExtensionAvailable(@NonNull String cameraId,
            @NonNull CameraCharacteristics cameraCharacteristics);

    /**
     * Initializes the extender to be used with the specified camera.
     *
     * <p>This should be called before any other method on the extender. The exception is {@link
     * #isExtensionAvailable(String, CameraCharacteristics)}.
     *
     * @param cameraId The camera2 id string of the camera.
     * @param cameraCharacteristics The {@link CameraCharacteristics} of the camera.
     */
    void init(@NonNull String cameraId, @NonNull CameraCharacteristics cameraCharacteristics);

    /**
     * The processing that will be done on a set of captures to create and image with the effect.
     */
    @Nullable
    CaptureProcessorImpl getCaptureProcessor();

    /** The set of captures that are needed to create an image with the effect. */
    @NonNull
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
     * @return the customized supported resolutions, or null to support all sizes retrieved from
     *         {@link android.hardware.camera2.params.StreamConfigurationMap}.
     * @since 1.1
     */
    @Nullable
    List<Pair<Integer, Size[]>> getSupportedResolutions();

    /**
     * Returns supported output format/size map for postview image. OEM is required to support
     * both JPEG and YUV_420_888 format output.
     *
     * <p>Pair list composed with {@link ImageFormat} and {@link Size} array will be returned.
     * The sizes must be smaller than or equal to the provided capture size and have the same
     * aspect ratio as the given capture size.
     *
     * @since 1.4
     */
    @Nullable
    List<Pair<Integer, Size[]>> getSupportedPostviewResolutions(@NonNull Size captureSize);

    /**
     * Returns the estimated capture latency range in milliseconds for the target capture
     * resolution.
     *
     * <p>This includes the time spent processing the multi-frame capture request along with any
     * additional time for encoding of the processed buffer in the framework if necessary.</p>
     *
     * @param captureOutputSize size of the capture output surface. If it is null or not in the
     *                          supported output sizes, maximum capture output size is used for
     *                          the estimation.
     * @return the range of estimated minimal and maximal capture latency in milliseconds, or
     * null if no capture latency info can be provided.
     * @since 1.2
     */
    @Nullable
    Range<Long> getEstimatedCaptureLatencyRange(@Nullable Size captureOutputSize);

    /**
     * Return a list of orthogonal capture request keys.
     *
     * <p>Any keys included in the list will be configurable by clients of the extension and will
     * affect the extension functionality.</p>
     *
     * <p>Do note that the list of keys applies to {@link PreviewExtenderImpl} as well.</p>
     *
     * <p>Also note that the keys {@link CaptureRequest#JPEG_QUALITY} and
     * {@link CaptureRequest#JPEG_ORIENTATION} are always supported regardless being added in the
     * list or not. To support common camera operations like zoom, tap-to-focus, flash and
     * exposure compensation, we recommend supporting the following keys if possible.
     * <pre>
     *  zoom:  {@link CaptureRequest#CONTROL_ZOOM_RATIO}
     *         {@link CaptureRequest#SCALER_CROP_REGION}
     *  tap-to-focus:
     *         {@link CaptureRequest#CONTROL_AF_MODE}
     *         {@link CaptureRequest#CONTROL_AF_TRIGGER}
     *         {@link CaptureRequest#CONTROL_AF_REGIONS}
     *         {@link CaptureRequest#CONTROL_AE_REGIONS}
     *         {@link CaptureRequest#CONTROL_AWB_REGIONS}
     *  flash:
     *         {@link CaptureRequest#CONTROL_AE_MODE}
     *         {@link CaptureRequest#CONTROL_AE_PRECAPTURE_TRIGGER}
     *         {@link CaptureRequest#FLASH_MODE}
     *  exposure compensation:
     *         {@link CaptureRequest#CONTROL_AE_EXPOSURE_COMPENSATION}
     * </pre>
     * On basic extensions that implement 1.2 or prior version, the above keys are all supported
     * explicitly. When migrating from 1.2 or prior to 1.3, please note that both CameraX and
     * Camera2 will honor the returned list and support only the keys contained in it. For
     * example, if OEM decides to return only {@link CaptureRequest#CONTROL_ZOOM_RATIO} and
     * {@link CaptureRequest#SCALER_CROP_REGION} in the 1.3 implementation, it means only zoom is
     * supported for the app while tap-to-focus , flash and exposure compensation are not allowed.
     *
     * @return List of supported orthogonal capture keys, or an empty list if no capture settings
     * are not supported.
     * @since 1.3
     */
    @NonNull
    List<CaptureRequest.Key> getAvailableCaptureRequestKeys();

    /**
     * Return a list of supported capture result keys.
     *
     * <p>Any keys included in this list must be available as part of the registered
     * {@link ProcessResultImpl} callback. In case frame processing is not supported,
     * then the Camera2/CameraX framework will use the list to filter and notify camera clients
     * using the respective camera results.</p>
     *
     * <p>At the very minimum, it is expected that the result key list is a superset of the
     * capture request keys.</p>
     *
     * <p>Do note that the list of keys applies to {@link PreviewExtenderImpl} as well.</p>
     *
     * @return List of supported capture result keys, or an empty list if capture results are not
     * supported.
     * @since 1.3
     */
    @NonNull
    List<CaptureResult.Key> getAvailableCaptureResultKeys();

    /**
     * Advertise support for {@link ProcessResultImpl#onCaptureProcessProgressed}.
     *
     * @return {@code true} in case the process progress callback is supported and is expected to
     * be triggered, {@code false} otherwise.
     * @since 1.4
     */
    boolean isCaptureProcessProgressAvailable();

    /**
     * Returns the dynamically calculated capture latency pair in milliseconds.
     *
     * <p>In contrast to {@link #getEstimatedCaptureLatencyRange} this method is guaranteed to be
     * called after the camera capture session is initialized and camera preview is enabled.
     * The measurement is expected to take in to account dynamic parameters such as the current
     * scene, the state of 3A algorithms, the state of internal HW modules and return a more
     * accurate assessment of the still capture latency.</p>
     *
     * @return pair that includes the estimated input frame/frames camera capture latency as the
     * first field and the estimated post-processing latency {@link CaptureProcessorImpl#process}
     * as the second pair field. Both first and second fields will be in milliseconds. The total
     * still capture latency will be the sum of both the first and second values.
     * The pair is expected to be null if the dynamic latency estimation is not supported.
     * If clients have not configured a still capture output, then this method can also return a
     * null pair.
     * @since 1.4
     */
    @Nullable
    Pair<Long, Long> getRealtimeCaptureLatency();

    /**
     * Indicates whether the extension supports the postview for still capture feature.
     * If the extension is using HAL processing, false should be returned since the
     * postview feature is not currently supported for this case.
     *
     * @return {@code true} in case postview for still capture is supported
     * {@code false} otherwise.
     * @since 1.4
     */
    boolean isPostviewAvailable();
}
