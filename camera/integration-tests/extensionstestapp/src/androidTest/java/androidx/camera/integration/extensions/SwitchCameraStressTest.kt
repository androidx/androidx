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

package androidx.camera.integration.extensions

import android.Manifest
import android.content.Context
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil.launchCameraExtensionsActivity
import androidx.camera.integration.extensions.util.takePictureAndWaitForImageSavedIdle
import androidx.camera.integration.extensions.util.waitForPreviewViewStreaming
import androidx.camera.integration.extensions.utils.ExtensionModeUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.ExtensionsUtil.assumePcsSupportedForImageCapture
import androidx.camera.testing.impl.PriorityRuleChain
import androidx.camera.testing.impl.RequireForegroundRule
import androidx.camera.testing.impl.StressTestRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import androidx.testutils.withActivity
import java.util.concurrent.TimeUnit
import org.junit.Assume.assumeTrue
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private const val DEFAULT_BACK_CAMERA_ID = "0"

/** Stress tests to verify that Preview and ImageCapture can work well when switching cameras. */
@LargeTest
@RunWith(Parameterized::class)
class SwitchCameraStressTest(private val extensionMode: Int) {
    @get:Rule
    val priorityChain =
        PriorityRuleChain()
            .add(
                1,
                GrantPermissionRule.grant(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO,
                ),
            )
            .add(
                2,
                CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
                    PreTestCameraIdList(Camera2Config.defaultConfig())
                ),
            )
            .add(
                3,
                RequireForegroundRule {
                        assumeTrue(CameraUtil.deviceHasCamera())
                        assumeTrue(CameraXExtensionsTestUtil.isTargetDeviceAvailableForExtensions())
                        assumePcsSupportedForImageCapture(context)

                        cameraProvider =
                            ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]

                        val extensionsManager =
                            ExtensionsManager.getInstance(context, cameraProvider)

                        val isBackCameraSupported =
                            extensionsManager.isExtensionAvailable(
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                extensionMode,
                            )
                        val isFrontCameraSupported =
                            extensionsManager.isExtensionAvailable(
                                CameraSelector.DEFAULT_FRONT_CAMERA,
                                extensionMode,
                            )

                        // Checks whether the extension mode can be supported first before launching
                        // the activity.
                        // Only runs the test when at least one of the back or front cameras support
                        // the target testing extension mode
                        assumeTrue(isBackCameraSupported || isFrontCameraSupported)

                        if (!isBackCameraSupported) {
                            startingExtensionMode =
                                CameraXExtensionsTestUtil.getFirstSupportedExtensionMode(
                                    extensionsManager,
                                    DEFAULT_BACK_CAMERA_ID,
                                )
                        }
                    }
                    .withCleanup {
                        if (::cameraProvider.isInitialized) {
                            cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS]
                            val extensionsManager =
                                ExtensionsManager.getInstance(context, cameraProvider)
                            extensionsManager.shutdown()
                        }
                    },
            )

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private var startingExtensionMode: Int = extensionMode

    private lateinit var cameraProvider: ProcessCameraProvider

    companion object {
        @ClassRule @JvmField val stressTest = StressTestRule()

        @Parameterized.Parameters(name = "extensionMode = {0}")
        @JvmStatic
        fun parameters(): List<Array<Any>> {
            return ExtensionModeUtil.AVAILABLE_EXTENSION_MODES.map { arrayOf(it) }
        }
    }

    @Test
    fun switchCameraTenTimes_canCaptureImageInEachTime() {
        val activityScenario =
            launchCameraExtensionsActivity(DEFAULT_BACK_CAMERA_ID, startingExtensionMode)

        with(activityScenario) {
            use {
                repeat(CameraXExtensionsTestUtil.getStressTestRepeatingCount()) {
                    // Waits for the take picture success callback.
                    takePictureAndWaitForImageSavedIdle()

                    withActivity {
                        // Switches camera
                        switchCameras()
                        // Switches to the target testing extension mode as possible because some
                        // extension modes may not be supported in some lens facing of cameras.
                        switchToExtensionMode(extensionMode)
                    }

                    // Waits for preview view turned to STREAMING state after switching camera
                    waitForPreviewViewStreaming()
                }
            }
        }
    }

    @Test
    fun canCaptureImage_afterSwitchCameraTenTimes() {
        val activityScenario =
            launchCameraExtensionsActivity(DEFAULT_BACK_CAMERA_ID, startingExtensionMode)

        with(activityScenario) {
            use {
                repeat(CameraXExtensionsTestUtil.getStressTestRepeatingCount()) {
                    withActivity {
                        // Switches camera
                        switchCameras()
                        // Switches to the target testing extension mode as possible because some
                        // extension modes may not be supported in some lens facing of cameras.
                        switchToExtensionMode(extensionMode)
                    }
                }

                // Waits for the take picture success callback.
                takePictureAndWaitForImageSavedIdle()
            }
        }
    }
}
