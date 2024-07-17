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
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import android.util.Size
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.impl.CameraInfoInternal
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
import androidx.camera.extensions.internal.ExtensionsUtils
import androidx.camera.extensions.internal.Version
import androidx.camera.integration.extensions.CameraExtensionsActivity
import androidx.camera.integration.extensions.CameraExtensionsActivity.CAMERA2_IMPLEMENTATION_OPTION
import androidx.camera.integration.extensions.CameraExtensionsActivity.CAMERA_PIPE_IMPLEMENTATION_OPTION
import androidx.camera.integration.extensions.IntentExtraKey
import androidx.camera.integration.extensions.utils.CameraSelectorUtil.createCameraSelectorById
import androidx.camera.integration.extensions.utils.ExtensionModeUtil.AVAILABLE_EXTENSION_MODES
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.LabTestRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import junit.framework.AssertionFailedError
import org.junit.Assume.assumeTrue

object CameraXExtensionsTestUtil {

    data class CameraXExtensionTestParams(
        val implName: String,
        val cameraXConfig: CameraXConfig,
        val cameraId: String,
        val extensionMode: Int,
    )

    /** Gets a list of all camera id and extension mode combinations. */
    @JvmStatic
    fun getAllCameraIdExtensionModeCombinations(
        context: Context = ApplicationProvider.getApplicationContext()
    ): List<CameraXExtensionTestParams> =
        filterOutUnavailableMode(
            context,
            CameraUtil.getBackwardCompatibleCameraIdListOrThrow().flatMap { cameraId ->
                AVAILABLE_EXTENSION_MODES.flatMap { extensionMode ->
                    CAMERAX_CONFIGS.map { config ->
                        CameraXExtensionTestParams(
                            config.first,
                            config.second,
                            cameraId,
                            extensionMode
                        )
                    }
                }
            }
        )

    private fun filterOutUnavailableMode(
        context: Context,
        list: List<CameraXExtensionTestParams>
    ): List<CameraXExtensionTestParams> {
        var extensionsManager: ExtensionsManager? = null
        var cameraProvider: ProcessCameraProvider? = null
        try {
            cameraProvider = ProcessCameraProvider.getInstance(context)[2, TimeUnit.SECONDS]
            extensionsManager =
                ExtensionsManager.getInstanceAsync(context, cameraProvider)[2, TimeUnit.SECONDS]

            val result: MutableList<CameraXExtensionTestParams> = mutableListOf()
            for (item in list) {
                val cameraSelector = createCameraSelectorById(item.cameraId)
                if (extensionsManager.isExtensionAvailable(cameraSelector, item.extensionMode)) {
                    result.add(item)
                }
            }
            return result
        } catch (e: Exception) {
            return list
        } finally {
            try {
                cameraProvider?.shutdownAsync()?.get()
                extensionsManager?.shutdown()?.get()
            } catch (e: Exception) {}
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
            allModes.addAll(AVAILABLE_EXTENSION_MODES)
            CameraUtil.getBackwardCompatibleCameraIdListOrThrow().forEach { cameraId ->
                allModes.forEach { mode -> add(arrayOf(cameraId, mode)) }
            }
        }

    /**
     * Creates an [ImageCaptureExtenderImpl] object for specific [ExtensionMode] and camera id.
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
    ): ImageCaptureExtenderImpl =
        when (extensionMode) {
            ExtensionMode.HDR -> HdrImageCaptureExtenderImpl()
            ExtensionMode.BOKEH -> BokehImageCaptureExtenderImpl()
            ExtensionMode.FACE_RETOUCH -> BeautyImageCaptureExtenderImpl()
            ExtensionMode.NIGHT -> NightImageCaptureExtenderImpl()
            ExtensionMode.AUTO -> AutoImageCaptureExtenderImpl()
            else -> throw AssertionFailedError("No such ImageCapture extender implementation")
        }.apply { init(cameraId, cameraCharacteristics) }

    /**
     * Creates a [PreviewExtenderImpl] object for specific [ExtensionMode] and camera id.
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
    ): PreviewExtenderImpl =
        when (extensionMode) {
            ExtensionMode.HDR -> HdrPreviewExtenderImpl()
            ExtensionMode.BOKEH -> BokehPreviewExtenderImpl()
            ExtensionMode.FACE_RETOUCH -> BeautyPreviewExtenderImpl()
            ExtensionMode.NIGHT -> NightPreviewExtenderImpl()
            ExtensionMode.AUTO -> AutoPreviewExtenderImpl()
            else -> throw AssertionFailedError("No such Preview extender implementation")
        }.apply { init(cameraId, cameraCharacteristics) }

    /**
     * Creates a [AdvancedExtenderImpl] object for specific [ExtensionMode] and camera id.
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
    ): AdvancedExtenderImpl =
        when (extensionMode) {
            ExtensionMode.HDR -> HdrAdvancedExtenderImpl()
            ExtensionMode.BOKEH -> BokehAdvancedExtenderImpl()
            ExtensionMode.FACE_RETOUCH -> BeautyAdvancedExtenderImpl()
            ExtensionMode.NIGHT -> NightAdvancedExtenderImpl()
            ExtensionMode.AUTO -> AutoAdvancedExtenderImpl()
            else -> throw AssertionFailedError("No such Preview extender implementation")
        }.apply {
            val cameraCharacteristicsMap =
                ExtensionsUtils.getCameraCharacteristicsMap(cameraInfo as CameraInfoInternal)
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
    fun assumeAnyExtensionModeSupported(extensionsManager: ExtensionsManager, cameraId: String) {
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
        val intent =
            ApplicationProvider.getApplicationContext<Context>()
                .packageManager
                .getLaunchIntentForPackage(BASIC_SAMPLE_PACKAGE)
                ?.apply {
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

    /**
     * Obtains the ImageCapture supported resolutions according to the provided
     * ImageCaptureExtenderImpl.
     */
    @JvmStatic
    fun getImageCaptureSupportedResolutions(
        impl: ImageCaptureExtenderImpl,
        cameraCharacteristics: CameraCharacteristics
    ): List<Size> {
        // Returns the supported resolutions list from ImageCaptureExtenderImpl if it provides the
        // info.
        impl.supportedResolutions?.forEach {
            // When there is no capture processor, the image format is JPEG.
            // When there is capture processor for post-processing, the image format is YUV_420_888.
            if (
                (impl.captureProcessor == null && it.first == ImageFormat.JPEG) ||
                    (impl.captureProcessor != null && it.first == ImageFormat.YUV_420_888)
            ) {
                return it.second.toList()
            }
        }

        // Returns the supported resolutions list from StreamConfigurationMap if
        // ImageCaptureExtenderImpl doesn't provide the info.
        val map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        return map!!.getOutputSizes(ImageFormat.JPEG).toList()
    }

    /**
     * Obtains the ImageCapture supported resolutions according to the provided
     * AdvancedExtenderImpl.
     */
    @JvmStatic
    fun getImageCaptureSupportedResolutions(
        impl: AdvancedExtenderImpl,
        cameraId: String,
        cameraCharacteristics: CameraCharacteristics
    ): List<Size> {
        // Returns the supported resolutions list from AdvancedExtenderImpl if it provides the
        // info.
        impl.getSupportedCaptureOutputResolutions(cameraId).forEach {
            if (it.key == ImageFormat.JPEG) {
                return it.value
            }
        }

        // Returns the supported resolutions list from StreamConfigurationMap if
        // ImageCaptureExtenderImpl doesn't provide the info.
        val map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        return map!!.getOutputSizes(ImageFormat.JPEG).toList()
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
     * <li> Open and close camera
     * <li> Open and close capture session
     * <li> Bind and unbind use cases
     * <li> Pause and resume lifecycle owner
     * <li> Switch cameras
     * <li> Switch extension modes
     * </ul>
     */
    private const val LAB_STRESS_TEST_OPERATION_REPEAT_COUNT = 10
    private const val STRESS_TEST_OPERATION_REPEAT_COUNT = 3

    /** Constant to specify that the verification target is [Preview]. */
    const val VERIFICATION_TARGET_PREVIEW = 0x1

    /** Constant to specify that the verification target is [ImageCapture]. */
    const val VERIFICATION_TARGET_IMAGE_CAPTURE = 0x2

    /** A list of supported implementation options and their respective [CameraXConfig]. */
    private val CAMERAX_CONFIGS =
        listOf(
            Pair(CAMERA2_IMPLEMENTATION_OPTION, Camera2Config.defaultConfig()),
            Pair(CAMERA_PIPE_IMPLEMENTATION_OPTION, CameraPipeConfig.defaultConfig())
        )
}
