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

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraInfo
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
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
import androidx.camera.extensions.impl.advanced.AdvancedExtenderImpl
import androidx.camera.extensions.impl.advanced.AutoAdvancedExtenderImpl
import androidx.camera.extensions.impl.advanced.BeautyAdvancedExtenderImpl
import androidx.camera.extensions.impl.advanced.BokehAdvancedExtenderImpl
import androidx.camera.extensions.impl.advanced.HdrAdvancedExtenderImpl
import androidx.camera.extensions.impl.advanced.NightAdvancedExtenderImpl
import androidx.camera.extensions.internal.ExtensionVersion
import androidx.camera.extensions.internal.Version
import androidx.camera.integration.extensions.CameraExtensionsActivity
import androidx.camera.integration.extensions.IntentExtraKey
import androidx.camera.integration.extensions.utils.CameraIdExtensionModePair
import androidx.camera.integration.extensions.utils.CameraSelectorUtil.createCameraSelectorById
import androidx.camera.integration.extensions.utils.ExtensionModeUtil
import androidx.camera.integration.extensions.utils.ExtensionModeUtil.AVAILABLE_EXTENSION_MODES
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.LabTestRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import junit.framework.AssertionFailedError
import org.junit.Assume.assumeTrue

object CameraXExtensionsTestUtil {

    /**
     * Gets a list of all camera id and extension mode combinations.
     */
    @JvmStatic
    fun getAllCameraIdExtensionModeCombinations(): List<CameraIdExtensionModePair> =
        CameraUtil.getBackwardCompatibleCameraIdListOrThrow().flatMap { cameraId ->
            ExtensionModeUtil.AVAILABLE_EXTENSION_MODES.map { extensionMode ->
                CameraIdExtensionModePair(cameraId, extensionMode)
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
     * Creates a [AdvancedExtenderImpl] object for specific [ExtensionMode] and
     * camera id.
     *
     * @param extensionMode The extension mode for the created object.
     * @param cameraId The target camera id.
     * @param cameraCharacteristics The camera characteristics of the target camera.
     * @return A [AdvancedExtenderImpl] object.
     */
    @JvmStatic
    fun createAdvancedExtenderImpl(
        @ExtensionMode.Mode extensionMode: Int,
        cameraId: String,
        cameraInfo: CameraInfo
    ): AdvancedExtenderImpl = when (extensionMode) {
        ExtensionMode.HDR -> HdrAdvancedExtenderImpl()
        ExtensionMode.BOKEH -> BokehAdvancedExtenderImpl()
        ExtensionMode.FACE_RETOUCH -> BeautyAdvancedExtenderImpl()
        ExtensionMode.NIGHT -> NightAdvancedExtenderImpl()
        ExtensionMode.AUTO -> AutoAdvancedExtenderImpl()
        else -> throw AssertionFailedError("No such Preview extender implementation")
    }.apply {
        val cameraCharacteristicsMap = Camera2CameraInfo.from(cameraInfo).cameraCharacteristicsMap
        init(cameraId, cameraCharacteristicsMap)
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

    @JvmStatic
    fun assumeExtensionModeSupported(
        extensionsManager: ExtensionsManager,
        cameraId: String,
        extensionMode: Int
    ) {
        val cameraIdCameraSelector = createCameraSelectorById(cameraId)
        assumeTrue(
            "Extensions mode($extensionMode) not supported",
            extensionsManager.isExtensionAvailable(cameraIdCameraSelector, extensionMode)
        )
    }

    @JvmStatic
    fun assumeAnyExtensionModeSupported(
        extensionsManager: ExtensionsManager,
        cameraId: String
    ) {
        val cameraIdCameraSelector = createCameraSelectorById(cameraId)
        var anyExtensionModeSupported = false

        AVAILABLE_EXTENSION_MODES.forEach { mode ->
            if (extensionsManager.isExtensionAvailable(cameraIdCameraSelector, mode)) {
                anyExtensionModeSupported = true
                return@forEach
            }
        }

        assumeTrue(anyExtensionModeSupported)
    }

    @JvmStatic
    fun getFirstSupportedExtensionMode(
        extensionsManager: ExtensionsManager,
        cameraId: String
    ): Int {
        val cameraIdCameraSelector = createCameraSelectorById(cameraId)

        AVAILABLE_EXTENSION_MODES.forEach { mode ->
            if (extensionsManager.isExtensionAvailable(cameraIdCameraSelector, mode)) {
                return mode
            }
        }

        return ExtensionMode.NONE
    }

    @JvmStatic
    fun isAdvancedExtenderImplemented(): Boolean {
        if (!isTargetDeviceAvailableForExtensions()) {
            return false
        }
        if (ExtensionVersion.getRuntimeVersion()!! < Version.VERSION_1_2) {
            return false
        }

        return ExtensionVersion.isAdvancedExtenderSupported()
    }

    @JvmStatic
    fun launchCameraExtensionsActivity(
        cameraId: String,
        extensionMode: Int,
        deleteCapturedImages: Boolean = true,
    ): ActivityScenario<CameraExtensionsActivity> {
        val intent = ApplicationProvider.getApplicationContext<Context>().packageManager
            .getLaunchIntentForPackage(BASIC_SAMPLE_PACKAGE)?.apply {
                putExtra(IntentExtraKey.INTENT_EXTRA_KEY_CAMERA_ID, cameraId)
                putExtra(IntentExtraKey.INTENT_EXTRA_KEY_EXTENSION_MODE, extensionMode)
                putExtra(
                    IntentExtraKey.INTENT_EXTRA_KEY_DELETE_CAPTURED_IMAGE,
                    deleteCapturedImages
                )
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

        val activityScenario: ActivityScenario<CameraExtensionsActivity> =
            ActivityScenario.launch(intent)

        activityScenario.waitForInitializationIdle()

        // Ensure ActivityScenario is cleaned up properly
        // Wait for PreviewView to become STREAMING state and its IdlingResource to become idle.
        activityScenario.onActivity {
            // Checks that CameraExtensionsActivity's current extension mode is correct.
            assertThat(it.currentExtensionMode).isEqualTo(extensionMode)
        }

        return activityScenario
    }

    @JvmStatic
    fun getStressTestRepeatingCount() =
        if (LabTestRule.isInLabTest()) {
            LAB_STRESS_TEST_OPERATION_REPEAT_COUNT
        } else {
            STRESS_TEST_OPERATION_REPEAT_COUNT
        }

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
    private const val LAB_STRESS_TEST_OPERATION_REPEAT_COUNT = 10
    private const val STRESS_TEST_OPERATION_REPEAT_COUNT = 3

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
