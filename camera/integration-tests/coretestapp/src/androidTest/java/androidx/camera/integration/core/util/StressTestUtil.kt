/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.integration.core.util

import android.content.Context
import android.content.Intent
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.Camera
import androidx.camera.core.CameraFilter
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.integration.core.CameraXActivity
import androidx.camera.integration.core.CameraXActivity.BIND_IMAGE_ANALYSIS
import androidx.camera.integration.core.CameraXActivity.BIND_IMAGE_CAPTURE
import androidx.camera.integration.core.CameraXActivity.BIND_PREVIEW
import androidx.camera.integration.core.CameraXActivity.BIND_VIDEO_CAPTURE
import androidx.camera.integration.core.CameraXActivity.INTENT_EXTRA_CAMERA_ID
import androidx.camera.integration.core.CameraXActivity.INTENT_EXTRA_USE_CASE_COMBINATION
import androidx.camera.integration.core.waitForViewfinderIdle
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.Assume.assumeTrue

private const val CORE_TEST_APP_PACKAGE = "androidx.camera.integration.core"

object StressTestUtil {

    /**
     * Launches CameraXActivity and wait for the preview ready.
     *
     * <p>Test cases can start activity by this function and then add other specific test
     * operations after the activity is launched.
     *
     * <p>If the target camera device can't support the specified use case combination, an
     * AssumptionViolatedException will be thrown to skip the test.
     *
     * @param cameraId Launches the activity with the specified camera id
     * @param useCaseCombination Launches the activity with the specified use case combination.
     * [BIND_PREVIEW], [BIND_IMAGE_CAPTURE], [BIND_VIDEO_CAPTURE] and [BIND_IMAGE_ANALYSIS] can be
     * used to set the combination.
     */
    @JvmStatic
    fun launchCameraXActivityAndWaitForPreviewReady(
        cameraId: String,
        useCaseCombination: Int
    ): ActivityScenario<CameraXActivity> {
        if (useCaseCombination.and(BIND_PREVIEW) == 0) {
            throw IllegalArgumentException("Preview must be included!")
        }

        val intent = ApplicationProvider.getApplicationContext<Context>().packageManager
            .getLaunchIntentForPackage(CORE_TEST_APP_PACKAGE)!!.apply {
                putExtra(INTENT_EXTRA_CAMERA_ID, cameraId)
                putExtra(INTENT_EXTRA_USE_CASE_COMBINATION, useCaseCombination)
                setClassName(CORE_TEST_APP_PACKAGE, CameraXActivity::class.java.name)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

        val activityScenario: ActivityScenario<CameraXActivity> = ActivityScenario.launch(intent)

        activityScenario.onActivity {
            // Checks that the camera id is correct
            if ((it.camera!!.cameraInfo as CameraInfoInternal).cameraId != cameraId) {
                it.finish()
                throw IllegalArgumentException("The activity is not launched with the correct" +
                    " camera of expected id.")
            }
        }

        // Ensure ActivityScenario is cleaned up properly
        // Wait for viewfinder to receive enough frames for its IdlingResource to idle.
        activityScenario.waitForViewfinderIdle()

        return activityScenario
    }

    /**
     * Checks and skips the test if the target camera can't support the use case combination.
     */
    @JvmStatic
    fun assumeCameraSupportUseCaseCombination(camera: Camera, useCaseCombination: Int) {
        val preview = Preview.Builder().build()
        val imageCapture = if (useCaseCombination.and(BIND_IMAGE_CAPTURE) != 0) {
            ImageCapture.Builder().build()
        } else {
            null
        }
        val videoCapture = if (useCaseCombination.and(BIND_VIDEO_CAPTURE) != 0) {
            VideoCapture.withOutput(Recorder.Builder().build())
        } else {
            null
        }
        val imageAnalysis = if (useCaseCombination.and(BIND_IMAGE_ANALYSIS) != 0) {
            ImageAnalysis.Builder().build()
        } else {
            null
        }

        assumeTrue(
            camera.isUseCasesCombinationSupported(
                *listOfNotNull(
                    preview,
                    imageCapture,
                    videoCapture,
                    imageAnalysis
                ).toTypedArray()
            )
        )
    }

    @JvmStatic
    fun createCameraSelectorById(cameraId: String) =
        CameraSelector.Builder().addCameraFilter(CameraFilter { cameraInfos ->
            cameraInfos.forEach {
                if ((it as CameraInfoInternal).cameraId == cameraId) {
                    return@CameraFilter listOf<CameraInfo>(it)
                }
            }

            throw IllegalArgumentException("No camera can be find for id: $cameraId")
        }).build()

    @JvmStatic
    fun getAllCameraXConfigCameraIdCombinations() = mutableListOf<Array<Any?>>().apply {
        val cameraxConfigs =
            listOf(Camera2Config::class.simpleName, CameraPipeConfig::class.simpleName)

        cameraxConfigs.forEach { configImplName ->
            CameraUtil.getBackwardCompatibleCameraIdListOrThrow().forEach { cameraId ->
                add(
                    arrayOf(
                        configImplName,
                        when (configImplName) {
                            CameraPipeConfig::class.simpleName ->
                                CameraPipeConfig.defaultConfig()
                            Camera2Config::class.simpleName ->
                                Camera2Config.defaultConfig()
                            else -> Camera2Config.defaultConfig()
                        },
                        cameraId
                    )
                )
            }
        }
    }

    /**
     * Large stress test repeat count to run the test
     */
    const val LARGE_STRESS_TEST_REPEAT_COUNT = 1

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
     * Timeout duration to wait for idle after pressing HOME key
     */
    const val HOME_TIMEOUT_MS = 3000L

    /**
     * Auto-stop duration for video capture related tests.
     */
    const val VIDEO_CAPTURE_AUTO_STOP_LENGTH_MS = 1000L

    /**
     * Constant to specify that the verification target is [Preview].
     */
    const val VERIFICATION_TARGET_PREVIEW = 0x1

    /**
     * Constant to specify that the verification target is [ImageCapture].
     */
    const val VERIFICATION_TARGET_IMAGE_CAPTURE = 0x2

    /**
     * Constant to specify that the verification target is [VideoCapture].
     */
    const val VERIFICATION_TARGET_VIDEO_CAPTURE = 0x4

    /**
     * Constant to specify that the verification target is [ImageAnalysis].
     */
    const val VERIFICATION_TARGET_IMAGE_ANALYSIS = 0x8
}
