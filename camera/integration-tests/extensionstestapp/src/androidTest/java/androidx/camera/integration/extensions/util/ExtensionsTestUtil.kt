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
}