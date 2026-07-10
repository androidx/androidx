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
import androidx.camera.core.ImageCapture
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil.assumeExtensionModeOutputFormatSupported
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil.assumeExtensionModeSupported
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil.launchCameraExtensionsActivity
import androidx.camera.integration.extensions.util.takePictureAndWaitForImageSavedIdle
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.ExtensionsUtil.assumePcsSupportedForImageCapture
import androidx.camera.testing.impl.RequireForegroundRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** The tests to verify that ImageCapture can work well when extension modes are enabled. */
@LargeTest
@RunWith(Parameterized::class)
class ImageCaptureTest(private val cameraId: String, private val extensionMode: Int) {
    @get:Rule
    val requireForegroundRule = RequireForegroundRule {
        assumeTrue(CameraXExtensionsTestUtil.isTargetDeviceAvailableForExtensions())
        assumePcsSupportedForImageCapture(context)
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

    companion object {
        val context = ApplicationProvider.getApplicationContext<Context>()

        @Parameterized.Parameters(name = "cameraId = {0}, extensionMode = {1}")
        @JvmStatic
        fun parameters() = CameraXExtensionsTestUtil.getAllCameraIdExtensionModeCombinations()
    }

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var extensionsManager: ExtensionsManager

    @Before
    fun setup(): Unit = runBlocking {
        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]

        extensionsManager = ExtensionsManager.getInstance(context, cameraProvider)

        assumeExtensionModeSupported(extensionsManager, cameraId, extensionMode)

        requireForegroundRule.deferCleanup {
            if (::cameraProvider.isInitialized) {
                cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS]
            }

            if (::extensionsManager.isInitialized) {
                extensionsManager.shutdown()
            }
        }
    }

    /**
     * Checks that ImageCapture can successfully take a JPEG picture when an extension mode is
     * enabled.
     */
    @Test
    fun takeJpegPictureWithExtensionMode() {
        takePictureWithExtensionMode(ImageCapture.OUTPUT_FORMAT_JPEG, false)
    }

    /**
     * Checks that ImageCapture can successfully take a picture when an extension mode is enabled
     * and VideoCapture is bound together.
     */
    @Test
    fun takeJpegPictureWithExtensionModeAndVideoCaptureOn() {
        takePictureWithExtensionMode(ImageCapture.OUTPUT_FORMAT_JPEG, true)
    }

    /**
     * Checks that ImageCapture can successfully take a JPEG_R picture when an extension mode is
     * enabled.
     */
    @Test
    fun takeJpegUltraHdrPictureWithExtensionMode() {
        takePictureWithExtensionMode(ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR, false)
    }

    /**
     * Checks that ImageCapture can successfully take a JPEG_R picture when an extension mode is
     * enabled and VideoCapture is bound together.
     */
    @Test
    fun takeJpegUltraHdrPictureWithExtensionModeAndVideoCaptureOn() {
        takePictureWithExtensionMode(ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR, true)
    }

    private fun takePictureWithExtensionMode(
        outputFormat: Int = ImageCapture.OUTPUT_FORMAT_JPEG,
        videoCaptureEnabled: Boolean = false,
    ) {
        if (outputFormat == ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR) {
            assumeExtensionModeOutputFormatSupported(
                cameraProvider,
                extensionsManager,
                cameraId,
                extensionMode,
                ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR,
            )
        }

        val activityScenario =
            launchCameraExtensionsActivity(
                cameraId,
                extensionMode,
                outputFormat = outputFormat,
                videoCaptureEnabled = videoCaptureEnabled,
            )

        with(activityScenario) { use { takePictureAndWaitForImageSavedIdle() } }
    }
}
