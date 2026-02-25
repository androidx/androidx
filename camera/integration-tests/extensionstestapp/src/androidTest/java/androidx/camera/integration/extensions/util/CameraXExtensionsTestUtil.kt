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
import android.os.Build
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.integration.extensions.CameraExtensionsActivity
import androidx.camera.integration.extensions.IntentExtraKey
import androidx.camera.integration.extensions.IntentExtraKey.INTENT_EXTRA_KEY_VIDEO_CAPTURE_ENABLED
import androidx.camera.integration.extensions.utils.CameraSelectorUtil.createCameraSelectorById
import androidx.camera.integration.extensions.utils.ExtensionModeUtil.AVAILABLE_EXTENSION_MODES
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.LabTestRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.Assume.assumeTrue

object CameraXExtensionsTestUtil {

    /** Gets a list of all camera id and extension mode combinations. */
    @JvmStatic
    fun getAllCameraIdExtensionModeCombinations(
        context: Context = ApplicationProvider.getApplicationContext()
    ): Collection<Array<Any>> =
        filterOutUnavailableMode(
            context,
            CameraUtil.getBackwardCompatibleCameraIdListOrThrow().flatMap { cameraId ->
                AVAILABLE_EXTENSION_MODES.map { extensionMode -> arrayOf(cameraId, extensionMode) }
            },
        )

    private fun filterOutUnavailableMode(
        context: Context,
        list: List<Array<Any>>,
    ): Collection<Array<Any>> {
        var extensionsManager: ExtensionsManager? = null
        var cameraProvider: ProcessCameraProvider? = null
        try {
            cameraProvider = ProcessCameraProvider.getInstance(context)[2, TimeUnit.SECONDS]
            extensionsManager =
                ExtensionsManager.getInstanceAsync(context, cameraProvider)[2, TimeUnit.SECONDS]

            val result: MutableList<Array<Any>> = mutableListOf()
            for ((cameraId, extensionMode) in list) {
                val cameraSelector = createCameraSelectorById(cameraId as String)
                if (extensionsManager.isExtensionAvailable(cameraSelector, extensionMode as Int)) {
                    result.add(arrayOf(cameraId, extensionMode))
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
     * Returns whether the target camera device can support the test for a specific extension mode.
     */
    @JvmStatic
    fun isTargetDeviceAvailableForExtensions(): Boolean {
        // Skips Cuttlefish device since actually it is not a real marketing device which supports
        // extensions and it will cause pre-submit failures.
        return !Build.MODEL.contains("Cuttlefish", true)
    }

    @JvmStatic
    fun assumeExtensionModeSupported(
        extensionsManager: ExtensionsManager,
        cameraId: String,
        extensionMode: Int,
    ) {
        val cameraIdCameraSelector = createCameraSelectorById(cameraId)
        assumeTrue(
            "Extensions mode($extensionMode) not supported",
            extensionsManager.isExtensionAvailable(cameraIdCameraSelector, extensionMode),
        )
    }

    @JvmStatic
    fun assumeExtensionModeOutputFormatSupported(
        cameraProvider: ProcessCameraProvider,
        extensionsManager: ExtensionsManager,
        cameraId: String,
        extensionMode: Int,
        outputFormat: Int,
    ) {
        val cameraIdCameraSelector = createCameraSelectorById(cameraId)
        val extensionsEnabledCameraSelector =
            extensionsManager.getExtensionEnabledCameraSelector(
                cameraIdCameraSelector,
                extensionMode,
            )
        val imageCaptureCapabilities =
            ImageCapture.getImageCaptureCapabilities(
                cameraProvider.getCameraInfo(extensionsEnabledCameraSelector)
            )
        assumeTrue(
            "Extensions mode($extensionMode) does not supported output format $outputFormat still" +
                " image capture",
            imageCaptureCapabilities.supportedOutputFormats.contains(outputFormat),
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
        cameraId: String,
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
    fun launchCameraExtensionsActivity(
        cameraId: String,
        extensionMode: Int,
        outputFormat: Int = ImageCapture.OUTPUT_FORMAT_JPEG,
        videoCaptureEnabled: Boolean? = null,
        deleteCapturedImages: Boolean = true,
    ): ActivityScenario<CameraExtensionsActivity> {
        val intent =
            ApplicationProvider.getApplicationContext<Context>()
                .packageManager
                .getLaunchIntentForPackage(BASIC_SAMPLE_PACKAGE)
                ?.apply {
                    putExtra(IntentExtraKey.INTENT_EXTRA_KEY_CAMERA_ID, cameraId)
                    putExtra(IntentExtraKey.INTENT_EXTRA_KEY_EXTENSION_MODE, extensionMode)
                    putExtra(IntentExtraKey.INTENT_EXTRA_KEY_OUTPUT_FORMAT, outputFormat)
                    putExtra(
                        IntentExtraKey.INTENT_EXTRA_KEY_DELETE_CAPTURED_IMAGE,
                        deleteCapturedImages,
                    )
                    videoCaptureEnabled?.let {
                        putExtra(INTENT_EXTRA_KEY_VIDEO_CAPTURE_ENABLED, it)
                    }
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
}
