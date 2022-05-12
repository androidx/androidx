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

package androidx.camera.extensions.impl.advanced;

import android.annotation.SuppressLint;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.util.Range;
import android.util.Size;

import androidx.camera.extensions.impl.ExtensionVersionImpl;

import java.util.List;
import java.util.Map;

/**
 * Advanced OEM contract for implementing Extensions. ImageCapture/Preview Extensions are both
 * implemented on this interface.
 *
 * <p>This advanced OEM contract empowers OEM to gain access to more Camera2 capability. This
 * includes: (1) Add custom surfaces with specific formats like YUV, RAW, RAW_DEPTH. (2) Access to
 * the capture request callbacks as well as all the images retrieved of various image formats. (3)
 * Able to triggers single or repeating request with the capabilities to specify target surfaces,
 * template id and parameters.
 *
 * <p>OEM needs to implement it with class name HdrAdvancedExtenderImpl for HDR,
 * NightAdvancedExtenderImpl for night mode, BeautyAdvancedExtenderImpl for beauty mode,
 * BokehAdvancedExtenderImpl for bokeh mode and AutoAdvancedExtenderImpl for auto mode.
 *
 * <p>OEMs are required to return true in
 * {@link ExtensionVersionImpl#isAdvancedExtenderImplemented()} in order to request CameraX to
 * use advanced extender over basic extender. OEM is okay to implement advanced
 * extender only Or basic extender only. However the caveat of advanced-only implementation is,
 * extensions will be unavailable on the apps using interfaces prior to 1.2.
 *
 * @since 1.2
 */
@SuppressLint("UnknownNullness")
public interface AdvancedExtenderImpl {

    /**
     * Indicates whether the extension is supported on the device.
     *
     * @param cameraId           The camera2 id string of the camera.
     * @param characteristicsMap A map consisting of the camera ids and the
     *                           {@link CameraCharacteristics}s. For every camera, the map
     *                           contains at least the CameraCharacteristics for the camera id.
     *                           If the camera is logical camera, it will also contain associated
     *                           physical camera ids and their CameraCharacteristics.
     * @return true if the extension is supported, otherwise false
     */
    boolean isExtensionAvailable(String cameraId,
            Map<String, CameraCharacteristics> characteristicsMap);

    /**
     * Initializes the extender to be used with the specified camera.
     *
     * <p>This should be called before any other method on the extender. The exception is {@link
     * #isExtensionAvailable}.
     *
     * @param cameraId           The camera2 id string of the camera.
     * @param characteristicsMap A map consisting of the camera ids and the
     *                           {@link CameraCharacteristics}s. For every camera, the map
     *                           contains at least the CameraCharacteristics for the camera id.
     *                           If the camera is logical camera, it will also contain associated
     *                           physical camera ids and their CameraCharacteristics.
     */
    void init(String cameraId, Map<String, CameraCharacteristics> characteristicsMap);

    /**
     * Returns the estimated capture latency range in milliseconds for the
     * target capture resolution during the calls to
     * {@link SessionProcessorImpl#startCapture}. This
     * includes the time spent processing the multi-frame capture request along with any additional
     * time for encoding of the processed buffer in the framework if necessary.
     *
     * @param cameraId          the camera id
     * @param captureOutputSize size of the capture output surface. If it is null or not in the
     *                          supported output sizes, maximum capture output size is used for
     *                          the estimation.
     * @param imageFormat the image format of the capture output surface.
     * @return the range of estimated minimal and maximal capture latency in milliseconds.
     * Returns null if no capture latency info can be provided.
     */
    Range<Long> getEstimatedCaptureLatencyRange(String cameraId,
            Size captureOutputSize, int imageFormat);

    /**
     * Returns supported output format/size map for preview. The format could be PRIVATE or
     * YUV_420_888. OEM must support PRIVATE format at least. CameraX will only use resolutions
     * for preview from the list.
     *
     * <p>The preview surface format in the CameraCaptureSession may not be identical to the
     * supported preview output format returned here. Like in the basic extender interface, the
     * preview PRIVATE surface could be added to the CameraCaptureSession and OEM processes it in
     * the HAL. Alternatively OEM can configure a intermediate YUV surface of the same size and
     * writes the output to the preview output surface.
     */
    Map<Integer, List<Size>> getSupportedPreviewOutputResolutions(String cameraId);

    /**
     * Returns supported output format/size map for image capture. OEM is required to support
     * both JPEG and YUV_420_888 format output.
     *
     * <p>Like in the basic extender interface, the surface created with this supported
     * format/size could be either added in CameraCaptureSession with HAL processing OR it
     * configures intermediate surfaces(YUV/RAW..) and writes the output to the output surface.
     */
    Map<Integer, List<Size>> getSupportedCaptureOutputResolutions(String cameraId);

    /**
     * Returns supported output sizes for Image Analysis (YUV_420_888 format).
     *
     * <p>OEM can optionally support a YUV surface for ImageAnalysis along with Preview/ImageCapture
     * output surfaces. If imageAnalysis YUV surface is not supported, OEM should return null or
     * empty list.
     */
    List<Size> getSupportedYuvAnalysisResolutions(String cameraId);

    /**
     * Returns a processor for activating extension sessions. It implements all the interactions
     * required for starting a extension and cleanup.
     */
    SessionProcessorImpl createSessionProcessor();

    /**
     * Returns a list of orthogonal capture request keys.
     *
     * <p>Any keys included in the list will be configurable by clients of the extension and will
     * affect the extension functionality.</p>
     *
     * <p>Please note that the keys {@link CaptureRequest#JPEG_QUALITY} and
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
     *
     * @return List of supported orthogonal capture keys, or an empty list if no capture settings
     * are not supported.
     * @since 1.3
     */
    List<CaptureRequest.Key> getAvailableCaptureRequestKeys();

    /**
     * Returns a list of supported capture result keys.
     *
     * <p>Any keys included in this list must be available as part of the registered
     * {@link SessionProcessorImpl.CaptureCallback#onCaptureCompleted} callback.</p>
     *
     * <p>At the very minimum, it is expected that the result key list is a superset of the
     * capture request keys.</p>
     *
     * @return List of supported capture result keys, or
     * an empty list if capture results are not supported.
     * @since 1.3
     */
    List<CaptureResult.Key> getAvailableCaptureResultKeys();
}
