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
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil.VERIFICATION_TARGET_IMAGE_CAPTURE
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil.VERIFICATION_TARGET_PREVIEW
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil.launchCameraExtensionsActivity
import androidx.camera.integration.extensions.util.takePictureAndWaitForImageSavedIdle
import androidx.camera.integration.extensions.util.waitForPreviewViewIdle
import androidx.camera.integration.extensions.util.waitForPreviewViewStreaming
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.CoreAppTestUtil
import androidx.camera.testing.impl.ExtensionsUtil.assumePcsSupportedForImageCapture
import androidx.camera.testing.impl.PriorityRuleChain
import androidx.camera.testing.impl.RequireForegroundRule
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import androidx.testutils.withActivity
import java.util.concurrent.TimeUnit
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Stress tests to verify that Preview and ImageCapture can work well when changing lifecycle
 * status.
 */
@LargeTest
@RunWith(Parameterized::class)
class LifecycleStatusChangeStressTest(
    private val cameraId: String,
    private val extensionMode: Int,
) {
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
                        assumeTrue(CameraXExtensionsTestUtil.isTargetDeviceAvailableForExtensions())
                        CoreAppTestUtil.assumeCompatibleDevice()

                        cameraProvider =
                            ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]

                        val extensionsManager =
                            ExtensionsManager.getInstance(context, cameraProvider)

                        // Checks whether the extension mode can be supported first before launching
                        // the activity.
                        CameraXExtensionsTestUtil.assumeExtensionModeSupported(
                            extensionsManager,
                            cameraId,
                            extensionMode,
                        )
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

    private lateinit var cameraProvider: ProcessCameraProvider

    companion object {
        val context = ApplicationProvider.getApplicationContext<Context>()

        @Parameterized.Parameters(name = "cameraId = {0}, extensionMode = {1}")
        @JvmStatic
        fun parameters() = CameraXExtensionsTestUtil.getAllCameraIdExtensionModeCombinations()
    }

    @Test
    fun pauseResumeActivity_checkPreviewInEachTime() {
        pauseResumeActivity_checkOutput_repeatedly(VERIFICATION_TARGET_PREVIEW)
    }

    @Test
    fun pauseResumeActivity_checkImageCaptureInEachTime() {
        assumePcsSupportedForImageCapture(context)
        pauseResumeActivity_checkOutput_repeatedly(VERIFICATION_TARGET_IMAGE_CAPTURE)
    }

    private fun pauseResumeActivity_checkOutput_repeatedly(
        verificationTarget: Int,
        repeatCount: Int = CameraXExtensionsTestUtil.getStressTestRepeatingCount(),
    ) {
        val activityScenario = launchCameraExtensionsActivity(cameraId, extensionMode)

        with(activityScenario) {
            use {
                waitForPreviewViewStreaming()
                repeat(repeatCount) {
                    withActivity {
                        resetPreviewViewIdleStateIdlingResource()
                        resetPreviewViewStreamingStateIdlingResource()
                    }
                    moveToState(Lifecycle.State.CREATED)
                    waitForPreviewViewIdle()
                    moveToState(Lifecycle.State.RESUMED)

                    if (verificationTarget.and(VERIFICATION_TARGET_PREVIEW) != 0) {
                        waitForPreviewViewStreaming()
                    }

                    if (verificationTarget.and(VERIFICATION_TARGET_IMAGE_CAPTURE) != 0) {
                        takePictureAndWaitForImageSavedIdle()
                    }
                }
            }
        }
    }

    @Test
    fun checkPreview_afterPauseResumeActivityRepeatedly() {
        pauseResumeActivityRepeatedly_thenCheckOutput(VERIFICATION_TARGET_PREVIEW)
    }

    @Test
    fun checkImageCapture_afterPauseResumeActivityRepeatedly() {
        assumePcsSupportedForImageCapture(context)
        pauseResumeActivityRepeatedly_thenCheckOutput(VERIFICATION_TARGET_IMAGE_CAPTURE)
    }

    private fun pauseResumeActivityRepeatedly_thenCheckOutput(
        verificationTarget: Int,
        repeatCount: Int = CameraXExtensionsTestUtil.getStressTestRepeatingCount(),
    ) {
        val activityScenario = launchCameraExtensionsActivity(cameraId, extensionMode)

        with(activityScenario) {
            use {
                waitForPreviewViewStreaming()

                repeat(repeatCount) {
                    withActivity {
                        resetPreviewViewIdleStateIdlingResource()
                        resetPreviewViewStreamingStateIdlingResource()
                    }
                    moveToState(Lifecycle.State.CREATED)
                    waitForPreviewViewIdle()
                    moveToState(Lifecycle.State.RESUMED)
                    waitForPreviewViewStreaming()
                }

                if (verificationTarget.and(VERIFICATION_TARGET_PREVIEW) != 0) {
                    waitForPreviewViewStreaming()
                }

                if (verificationTarget.and(VERIFICATION_TARGET_IMAGE_CAPTURE) != 0) {
                    takePictureAndWaitForImageSavedIdle()
                }
            }
        }
    }
}
