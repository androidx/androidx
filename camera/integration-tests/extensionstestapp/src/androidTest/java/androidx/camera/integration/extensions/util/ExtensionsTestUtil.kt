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

package androidx.camera.integration.extensions.util

import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.impl.AutoImageCaptureExtenderImpl
import androidx.camera.extensions.impl.AutoPreviewExtenderImpl
import androidx.camera.extensions.impl.BeautyImageCaptureExtenderImpl
import androidx.camera.extensions.impl.BeautyPreviewExtenderImpl
import androidx.camera.extensions.impl.BokehImageCaptureExtenderImpl
import androidx.camera.extensions.impl.BokehPreviewExtenderImpl
import androidx.camera.extensions.impl.HdrImageCaptureExtenderImpl
import androidx.camera.extensions.impl.HdrPreviewExtenderImpl
import androidx.camera.extensions.impl.ImageCaptureExtenderImpl
import androidx.camera.extensions.impl.NightImageCaptureExtenderImpl
import androidx.camera.extensions.impl.NightPreviewExtenderImpl
import androidx.camera.extensions.impl.PreviewExtenderImpl
import androidx.camera.extensions.internal.ExtensionVersion
import androidx.camera.integration.extensions.utils.ExtensionModeUtil
import androidx.camera.testing.CameraUtil
import junit.framework.AssertionFailedError

object ExtensionsTestUtil {

    /**
     * Gets a list of all camera id and extension mode combinations.
     */
    @JvmStatic
    fun getAllCameraIdExtensionModeCombinations(): List<Array<Any>> =
        arrayListOf<Array<Any>>().apply {
            CameraUtil.getBackwardCompatibleCameraIdListOrThrow().forEach { cameraId ->
                ExtensionModeUtil.AVAILABLE_EXTENSION_MODES.forEach { mode ->
                    add(arrayOf(cameraId, mode))
                }
            }
        }

    /**
     * Gets a list of all camera id and mode combinations. Normal mode and all extension modes will
     * be included.
     */
    @JvmStatic
    fun getAllCameraIdModeCombinations(): List<Array<Any>> =
        arrayListOf<Array<Any>>().apply {
            val allModes = mutableListOf<Int>()
            allModes.add(0, ExtensionMode.NONE)
            allModes.addAll(ExtensionModeUtil.AVAILABLE_EXTENSION_MODES)
            CameraUtil.getBackwardCompatibleCameraIdListOrThrow().forEach { cameraId ->
                allModes.forEach { mode ->
                    add(arrayOf(cameraId, mode))
                }
            }
        }

    /**
     * Creates an [ImageCaptureExtenderImpl] object for specific [ExtensionMode] and
     * camera id.
     *
     * @param extensionMode The extension mode for the created object.
     * @param cameraId The target camera id.
     * @param cameraCharacteristics The camera characteristics of the target camera.
     * @return An [ImageCaptureExtenderImpl] object.
     */
    @JvmStatic
    fun createImageCaptureExtenderImpl(
        @ExtensionMode.Mode extensionMode: Int,
        cameraId: String,
        cameraCharacteristics: CameraCharacteristics
    ): ImageCaptureExtenderImpl = when (extensionMode) {
        ExtensionMode.HDR -> HdrImageCaptureExtenderImpl()
        ExtensionMode.BOKEH -> BokehImageCaptureExtenderImpl()
        ExtensionMode.FACE_RETOUCH -> BeautyImageCaptureExtenderImpl()
        ExtensionMode.NIGHT -> NightImageCaptureExtenderImpl()
        ExtensionMode.AUTO -> AutoImageCaptureExtenderImpl()
        else -> throw AssertionFailedError("No such ImageCapture extender implementation")
    }.apply { init(cameraId, cameraCharacteristics) }

    /**
     * Creates a [PreviewExtenderImpl] object for specific [ExtensionMode] and
     * camera id.
     *
     * @param extensionMode The extension mode for the created object.
     * @param cameraId The target camera id.
     * @param cameraCharacteristics The camera characteristics of the target camera.
     * @return A [PreviewExtenderImpl] object.
     */
    @JvmStatic
    fun createPreviewExtenderImpl(
        @ExtensionMode.Mode extensionMode: Int,
        cameraId: String,
        cameraCharacteristics: CameraCharacteristics
    ): PreviewExtenderImpl = when (extensionMode) {
        ExtensionMode.HDR -> HdrPreviewExtenderImpl()
        ExtensionMode.BOKEH -> BokehPreviewExtenderImpl()
        ExtensionMode.FACE_RETOUCH -> BeautyPreviewExtenderImpl()
        ExtensionMode.NIGHT -> NightPreviewExtenderImpl()
        ExtensionMode.AUTO -> AutoPreviewExtenderImpl()
        else -> throw AssertionFailedError("No such Preview extender implementation")
    }.apply {
        init(cameraId, cameraCharacteristics)
    }

    /**
     * Returns whether the target camera device can support the test for a specific extension mode.
     */
    @JvmStatic
    fun isTargetDeviceAvailableForExtensions(): Boolean {
        // Runtime version must be non-null if the device supports extensions.
        if (ExtensionVersion.getRuntimeVersion() == null) {
            return false
        }

        // Skips Cuttlefish device since actually it is not a real marketing device which supports
        // extensions and it will cause pre-submit failures.
        return !Build.MODEL.contains("Cuttlefish", true)
    }

    /**
     * Stress test repeat count to run the test
     */
    const val STRESS_TEST_REPEAT_COUNT = 2

    /**
     * Stress test target testing operation count.
     *
     * <p>The target testing operation might be:
     * <ul>
     *     <li> Open and close camera
     *     <li> Open and close capture session
     *     <li> Bind and unbind use cases
     *     <li> Pause and resume lifecycle owner
     *     <li> Switch cameras
     *     <li> Switch extension modes
     * </ul>
     *
     */
    const val STRESS_TEST_OPERATION_REPEAT_COUNT = 10

    /**
     * Constant to specify that the verification target is [Preview].
     */
    const val VERIFICATION_TARGET_PREVIEW = 0x1

    /**
     * Constant to specify that the verification target is [ImageCapture].
     */
    const val VERIFICATION_TARGET_IMAGE_CAPTURE = 0x2

    /**
     * Constant to specify that the verification target is [ImageAnalysis].
     */
    const val VERIFICATION_TARGET_IMAGE_ANALYSIS = 0x4
}