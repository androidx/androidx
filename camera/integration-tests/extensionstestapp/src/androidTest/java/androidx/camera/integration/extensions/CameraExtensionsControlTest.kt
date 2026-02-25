/*
 * Copyright 2025 The Android Open Source Project
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
import android.graphics.SurfaceTexture
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.extensions.CameraExtensionsControl
import androidx.camera.extensions.CameraExtensionsInfo
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil
import androidx.camera.integration.extensions.utils.CameraSelectorUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class CameraExtensionsControlTest(private val cameraId: String, private val extensionMode: Int) {

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
        @Parameterized.Parameters(name = "cameraId = {0}, extensionMode = {1}")
        @JvmStatic
        fun parameters() = CameraXExtensionsTestUtil.getAllCameraIdExtensionModeCombinations()
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var extensionsManager: ExtensionsManager
    private lateinit var cameraExtensionsInfo: CameraExtensionsInfo
    private lateinit var cameraExtensionsControl: CameraExtensionsControl
    private lateinit var baseCameraSelector: CameraSelector
    private lateinit var extensionCameraSelector: CameraSelector
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner
    private lateinit var camera: Camera
    private lateinit var preview: Preview
    private lateinit var imageCapture: ImageCapture
    private val frameAvailableCountDownLatch = CountDownLatch(1)

    @Before
    fun setup() {
        assumeTrue(CameraXExtensionsTestUtil.isTargetDeviceAvailableForExtensions())

        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]

        extensionsManager = runBlocking { ExtensionsManager.getInstance(context, cameraProvider) }

        baseCameraSelector = CameraSelectorUtil.createCameraSelectorById(cameraId)
        assumeTrue(extensionsManager.isExtensionAvailable(baseCameraSelector, extensionMode))

        extensionCameraSelector =
            extensionsManager.getExtensionEnabledCameraSelector(baseCameraSelector, extensionMode)

        instrumentation.runOnMainSync {
            fakeLifecycleOwner = FakeLifecycleOwner().apply { startAndResume() }
            preview = Preview.Builder().build()
            preview.surfaceProvider =
                SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider {
                    SurfaceTexture.OnFrameAvailableListener {
                        frameAvailableCountDownLatch.countDown()
                    }
                }
            imageCapture = ImageCapture.Builder().build()
            camera =
                cameraProvider.bindToLifecycle(
                    fakeLifecycleOwner,
                    extensionCameraSelector,
                    preview,
                    imageCapture,
                )
        }

        cameraExtensionsInfo = extensionsManager.getCameraExtensionsInfo(camera.cameraInfo)
        cameraExtensionsControl =
            extensionsManager.getCameraExtensionsControl(camera.cameraControl)!!
    }

    @After
    fun tearDown() {
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync()
        }

        if (::extensionsManager.isInitialized) {
            extensionsManager.shutdown()
        }
    }

    @Test
    fun canSetExtensionStrength() {
        assumeTrue(cameraExtensionsInfo.isExtensionStrengthAvailable)
        // Wait for the frame be available before setting the extension strength.
        frameAvailableCountDownLatch.await(3, TimeUnit.SECONDS)

        val oldStrength = cameraExtensionsInfo.extensionStrength!!.value!!
        val newStrength = (oldStrength + 50) % 100
        val countDownLatch = CountDownLatch(1)

        instrumentation.runOnMainSync {
            cameraExtensionsInfo.extensionStrength!!.observeForever { strength ->
                if (strength == newStrength) {
                    countDownLatch.countDown()
                }
            }
        }

        cameraExtensionsControl.setExtensionStrength(newStrength)
        // It might take some time to reach the new strength value.
        assertThat(countDownLatch.await(5, TimeUnit.SECONDS)).isTrue()
    }
}
