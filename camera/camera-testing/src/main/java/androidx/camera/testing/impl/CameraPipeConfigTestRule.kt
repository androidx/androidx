/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.testing.impl

import android.util.Log
import androidx.camera.testing.impl.CameraPipeConfigTestRule.Companion.CAMERA_PIPE_TEST_FLAG
import org.junit.AssumptionViolatedException
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] to convert test failures into AssumptionViolatedException
 *
 * All the test methods will be executed as usual when [active] is set to false.
 *
 * When the [active] is true and DUT doesn't set the debug [CAMERA_PIPE_TEST_FLAG], all the test
 * failures will be converted to AssumptionViolatedException. Otherwise, test failures will be shown
 * as usual.
 *
 * The [CAMERA_PIPE_TEST_FLAG] can be enabled on the DUT by the command:
 * ```
 * adb shell setprop log.tag.CAMERA_PIPE_TESTING DEBUG
 * ```
 *
 * To apply the [TestRule] , please create the [CameraPipeConfigTestRule] directly. For Camera-Pipe
 * related tests, please set [active] to true.
 *
 * ```
 *  @get:Rule
 *  val testRule: CameraPipeConfigTestRule　= CameraPipeConfigTestRule(
 *      active = true
 *  )
 * ```
 *
 * @property active true to activate this rule.
 */
public class CameraPipeConfigTestRule(
    public val active: Boolean,
) : TestRule {

    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            private var standardHandler: Thread.UncaughtExceptionHandler? = null

            override fun evaluate() {
                if (active) {
                    if (Log.isLoggable(CAMERA_PIPE_MH_FLAG, Log.DEBUG)) {
                        testInPipeLab()
                    } else {
                        testNotInPipeLab()
                    }
                } else {
                    if (Log.isLoggable(CAMERA2_TEST_DISABLE, Log.DEBUG)) {
                        throw AssumptionViolatedException(
                            "Ignore Camera2 tests since CAMERA2_TEST_DISABLE flag is turned on."
                        )
                    }
                    base.evaluate()
                }
            }

            private fun testInPipeLab() {
                try {
                    log("started: ${description.displayName}")
                    logUncaughtExceptions()
                    base.evaluate()
                } catch (e: AssumptionViolatedException) {
                    log("AssumptionViolatedException: ${description.displayName}", e)
                    handleException(e)
                } catch (e: Throwable) {
                    log("failed: ${description.displayName}", e)
                    handleException(e)
                } finally {
                    restoreUncaughtExceptionHandler()
                    log("finished: ${description.displayName}")
                }
            }

            private fun testNotInPipeLab() {
                if (testInAllowList() && !Log.isLoggable(CAMERA_MH_FLAG, Log.DEBUG)) {
                    // Run the test when (1) In the allow list && (2) It is not MH daily test.
                    base.evaluate()
                } else {
                    throw AssumptionViolatedException(
                        "Ignore Camera-pipe tests since there's no debug flag"
                    )
                }
            }

            private fun testInAllowList() =
                allowPresubmitTests.any { description.displayName.contains(it, ignoreCase = true) }

            private fun handleException(e: Throwable) {
                if (Log.isLoggable(CAMERA_PIPE_TEST_FLAG, Log.DEBUG)) {
                    throw e
                } else {
                    throw AssumptionViolatedException("CameraPipeTestFailure", e)
                }
            }

            private fun logUncaughtExceptions() {
                standardHandler = Thread.getDefaultUncaughtExceptionHandler()
                Thread.setDefaultUncaughtExceptionHandler { _, e ->
                    log("Invoking uncaught exception handler: ${description.displayName}", e)
                    handleException(e)
                }
            }

            private fun restoreUncaughtExceptionHandler() {
                Thread.setDefaultUncaughtExceptionHandler(standardHandler)
            }
        }

    private companion object {
        private const val CAMERA2_TEST_DISABLE = "CAMERA2_TEST_DISABLE"
        private const val CAMERA_PIPE_TEST_FLAG = "CAMERA_PIPE_TESTING"
        private const val CAMERA_PIPE_MH_FLAG = "CameraPipeMH"
        private const val CAMERA_MH_FLAG = "MH"
        private const val LOG_TAG = "CameraPipeTest"

        private val allowPresubmitTests =
            listOf(
                "BasicUITest",
                "Camera2InteropIntegrationTest",
                "CameraControlDeviceTest",
                "CameraDisconnectTest",
                "CameraInfoDeviceTest",
                "CameraXInitTest",
                "CameraXServiceTest",
                "CaptureOptionSubmissionTest",
                "ExistingActivityLifecycleTest",
                "FlashTest",
                "FocusMeteringDeviceTest",
                "FovDeviceTest",
                "androidx.camera.integration.core.ImageAnalysisTest",
                "ImageCaptureLatencyTest",
                "androidx.camera.integration.core.ImageCaptureTest",
                "ImageCaptureWithoutStoragePermissionTest",
                "androidx.camera.integration.core.camera2.PreviewTest",
                "ResolutionSelectorDeviceTest",
                "StreamSharingTest",
                "SurfaceOrientedMeteringPointFactoryTest",
                "TakePictureTest",
                "ToggleButtonUITest",
                "UseCaseCombinationTest",
                "VideoRecordingEffectTest",
                "VideoRecordingFrameDropTest",
                "ZoomControlDeviceTest",
                // Camera-View
                "CameraControllerDeviceTest",
                "PreviewViewBitmapTest",
                "PreviewViewDeviceTest",
                "PreviewViewStreamStateTest",
                // Camera-Video
                "AudioEncoderConfigAudioProfileResolverTest",
                "AudioSettingsAudioProfileResolverTest",
                "VideoEncoderConfigVideoProfileResolverTest",
                "VideoEncoderTest",
                "BackupHdrProfileEncoderProfilesProviderTest",
                "AudioVideoSyncTest",
                "RecorderTest",
                "RecorderVideoCapabilitiesTest",
                "SupportedQualitiesVerificationTest",
                "VideoCaptureDeviceTest",

                // TODO(b/122975195): Re-enable test after camera-pipe implement is fixed
                // "VideoRecordingTest",
            )

        fun log(message: String, throwable: Throwable? = null) {
            if (throwable != null) {
                Log.e(LOG_TAG, message, throwable)
            } else {
                Log.i(LOG_TAG, message)
            }
        }
    }
}
