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
import android.os.SystemClock
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.extensions.internal.ExtensionVersion
import androidx.camera.extensions.internal.Version
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil.assumeExtensionModeSupported
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil.launchCameraExtensionsActivity
import androidx.camera.integration.extensions.util.HOME_TIMEOUT_MS
import androidx.camera.integration.extensions.util.takePictureAndWaitForImageSavedIdle
import androidx.camera.integration.extensions.util.waitForPreviewViewStreaming
import androidx.camera.integration.extensions.utils.CameraIdExtensionModePair
import androidx.camera.integration.extensions.utils.CameraSelectorUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.CoreAppTestUtil
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * The tests to verify that ImageCapture can work well when extension modes are enabled.
 */
@LargeTest
@RunWith(Parameterized::class)
class ImageCaptureTest(private val config: CameraIdExtensionModePair) {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    @get:Rule
    val permissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private val context = ApplicationProvider.getApplicationContext<Context>()

    companion object {
        @Parameterized.Parameters(name = "config = {0}")
        @JvmStatic
        fun parameters() = CameraXExtensionsTestUtil.getAllCameraIdExtensionModeCombinations()
    }

    @Before
    fun setup() {
        assumeTrue(CameraXExtensionsTestUtil.isTargetDeviceAvailableForExtensions())
        // Clear the device UI and check if there is no dialog or lock screen on the top of the
        // window before start the test.
        CoreAppTestUtil.prepareDeviceUI(InstrumentationRegistry.getInstrumentation())
        // Use the natural orientation throughout these tests to ensure the activity isn't
        // recreated unexpectedly. This will also freeze the sensors until
        // mDevice.unfreezeRotation() in the tearDown() method. Any simulated rotations will be
        // explicitly initiated from within the test.
        device.setOrientationNatural()

        val cameraProvider =
            ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]

        val extensionsManager = ExtensionsManager.getInstanceAsync(
            context,
            cameraProvider
        )[10000, TimeUnit.MILLISECONDS]

        assumeExtensionModeSupported(extensionsManager, config.cameraId, config.extensionMode)
    }

    @After
    fun tearDown() {
        val cameraProvider =
            ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        cameraProvider.shutdownAsync()

        val extensionsManager = ExtensionsManager.getInstanceAsync(
            context,
            cameraProvider
        )[10000, TimeUnit.MILLISECONDS]
        extensionsManager.shutdown()

        // Unfreeze rotation so the device can choose the orientation via its own policy. Be nice
        // to other tests :)
        device.unfreezeRotation()
        device.pressHome()
        device.waitForIdle(HOME_TIMEOUT_MS)
    }

    /**
     * Checks that ImageCapture can successfully take a picture when an extension mode is enabled.
     */
    @Test
    fun takePictureWithExtensionMode() {
        val activityScenario = launchCameraExtensionsActivity(config.cameraId, config.extensionMode)

        with(activityScenario) {
            use {
                takePictureAndWaitForImageSavedIdle()
            }
        }
    }

    /**
     * The following 1.4 interface methods are validated by this test.
     * <ol>
     *   <li>ImageCaptureExtenderImpl#getRealtimeCaptureLatency()
     *   <li>SessionProcessorImpl#getRealtimeCaptureLatency()
     * </ol>
     *
     * According to the javadoc description, this method is guaranteed to be called after the
     * camera capture session is initialized and camera preview is enabled for the
     * ImageCaptureExtenderImpl implementation, or, after onCaptureSessionStart is called for the
     * SessionProcessorImpl implementation. Using ActivityScenario to launch the extensions
     * activity and waiting for its preview being ready can make sure that the calling timing can
     * meet the javadoc description.
     */
    @Test
    fun validateRealtimeCaptureLatencySupport_sinceVersion_1_4() {
        assumeTrue(ExtensionVersion.getRuntimeVersion()!! >= Version.VERSION_1_4)
        val activityScenario = launchCameraExtensionsActivity(config.cameraId, config.extensionMode)

        with(activityScenario) {
            use {
                waitForPreviewViewStreaming()
                val camera = withActivity { mCamera }
                // Retrieves the session processor from the camera's extended config
                val sessionProcessor = camera.extendedConfig.sessionProcessor
                // getRealtimeCaptureLatency is allowed to return null, therefore, we can only try
                // to invoke this method to make sure that this method correctly exists in the
                // vendor library implementation.
                sessionProcessor.realtimeCaptureLatency
            }
        }
    }

    @Test
    fun validateCaptureProcessProgressSupport_sinceVersion_1_4(): Unit = runBlocking {
        assumeTrue(ExtensionVersion.getRuntimeVersion()!! >= Version.VERSION_1_4)

        val cameraProvider =
            ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]

        val extensionsManager = ExtensionsManager.getInstanceAsync(
            context,
            cameraProvider
        )[10000, TimeUnit.MILLISECONDS]

        val baseCameraSelector = CameraSelectorUtil.createCameraSelectorById(config.cameraId)
        val cameraSelector = extensionsManager.getExtensionEnabledCameraSelector(
            baseCameraSelector,
            config.extensionMode
        )

        val fakeLifecycleOwner = withContext(Dispatchers.Main) {
            FakeLifecycleOwner().apply {
                startAndResume()
            }
        }

        val camera = withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                cameraSelector
            )
        }
        val isCaptureProcessProgressSupported = ImageCapture.getImageCaptureCapabilities(
            camera.cameraInfo
        ).isCaptureProcessProgressSupported

        assumeTrue(isCaptureProcessProgressSupported)

        val imageCapture = ImageCapture.Builder().build()

        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, imageCapture)
        }

        val progress100Latch = CountDownLatch(1)
        var progress100Timestamp = 0L
        val processProgressData = mutableListOf<Int>()
        val imageCapturedLatch = CountDownLatch(1)
        var imageCapturedTimestamp = 0L

        // Using OnImageCapturedCallback to monitor the callback events so that we can make sure
        // that the progress 100 result comes before the captured (post-processed) image is
        // received.
        imageCapture.takePicture(
            CameraXExecutors.mainThreadExecutor(),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    imageCapturedTimestamp = SystemClock.elapsedRealtime()
                    imageCapturedLatch.countDown()
                    image.close()
                }

                override fun onCaptureProcessProgressed(progress: Int) {
                    processProgressData.add(progress)
                    if (progress == 100) {
                        progress100Timestamp = SystemClock.elapsedRealtime()
                        progress100Latch.countDown()
                    }
                }
            }
        )

        assertThat(progress100Latch.await(10, TimeUnit.SECONDS)).isTrue()
        assertThat(imageCapturedLatch.await(10, TimeUnit.SECONDS)).isTrue()
        // Makes sure that progress 100 can be received before the image captured callback is
        // invoked
        assertThat(progress100Timestamp).isLessThan(imageCapturedTimestamp)
        // Makes sure that the received progress data is in increasing order
        assertThat(processProgressData).isInOrder(object : Comparator<Int> {
            override fun compare(progress1: Int, progress2: Int): Int {
                return if (progress1 > progress2) {
                    1
                } else if (progress1 == progress2) {
                    0
                } else {
                    -1
                }
            }
        })
    }
}
