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
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil.assumeExtensionModeSupported
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil.launchCameraExtensionsActivity
import androidx.camera.integration.extensions.util.waitForPreviewViewStreaming
import androidx.camera.integration.extensions.utils.CameraSelectorUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.RequireForegroundRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import androidx.testutils.withActivity
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** The tests to verify that Preview can work well when extension modes are enabled. */
@LargeTest
@RunWith(Parameterized::class)
class PreviewTest(private val cameraId: String, private val extensionMode: Int) {
    @get:Rule
    val requireForegroundRule = RequireForegroundRule {
        assumeTrue(CameraUtil.deviceHasCamera())
        assumeTrue(CameraXExtensionsTestUtil.isTargetDeviceAvailableForExtensions())
    }

    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            PreTestCameraIdList(Camera2Config.defaultConfig())
        )

    @get:Rule
    val permissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
        )

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var extensionsManager: ExtensionsManager

    companion object {
        val context = ApplicationProvider.getApplicationContext<Context>()

        @Parameterized.Parameters(name = "cameraId = {0}, extensionMode = {1}")
        @JvmStatic
        fun parameters() = CameraXExtensionsTestUtil.getAllCameraIdExtensionModeCombinations()
    }

    @Before
    fun setup(): Unit = runBlocking {
        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]

        extensionsManager = ExtensionsManager.getInstance(context, cameraProvider)

        assumeExtensionModeSupported(extensionsManager, cameraId, extensionMode)

        requireForegroundRule.deferCleanup {
            if (::cameraProvider.isInitialized) {
                cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS]
                val extensionsManager = ExtensionsManager.getInstance(context, cameraProvider)
                extensionsManager.shutdown()
            }
        }
    }

    /**
     * Checks that Preview can successfully enter the STREAMING state to show the preview when an
     * extension mode is enabled.
     */
    @Test
    fun previewWithExtensionModeCanEnterStreamingState() {
        val activityScenario = launchCameraExtensionsActivity(cameraId, extensionMode)

        with(activityScenario) { use { waitForPreviewViewStreaming() } }
    }

    private fun assumeNextCameraIdExtensionModeSupported(cameraId: String, extensionsMode: Int) {
        val nextCameraId =
            CameraSelectorUtil.findNextSupportedCameraId(
                ApplicationProvider.getApplicationContext(),
                extensionsManager,
                cameraId,
                extensionMode,
            )
        assumeTrue(
            "Cannot find next camera id that supports extensions mode($extensionsMode)",
            nextCameraId != null,
        )
    }

    /**
     * Checks that Preview can successfully enter the STREAMING state to show the preview when an
     * extension mode is enabled and after switching cameras.
     */
    @Test
    fun previewCanEnterStreamingStateAfterSwitchingCamera() {
        assumeNextCameraIdExtensionModeSupported(cameraId, extensionMode)
        val activityScenario = launchCameraExtensionsActivity(cameraId, extensionMode)

        with(activityScenario) {
            use {
                waitForPreviewViewStreaming()
                withActivity {
                    resetPreviewViewStreamingStateIdlingResource()
                    switchCameras()
                }
                waitForPreviewViewStreaming()
            }
        }
    }
}
