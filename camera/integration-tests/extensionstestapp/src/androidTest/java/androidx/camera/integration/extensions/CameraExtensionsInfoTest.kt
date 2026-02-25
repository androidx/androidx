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
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.os.Build
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.adapter.awaitUntil
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.impl.utils.ContextUtil
import androidx.camera.extensions.CameraExtensionsInfo
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.extensions.internal.Camera2ExtensionsUtil
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil
import androidx.camera.integration.extensions.utils.CameraSelectorUtil
import androidx.camera.integration.extensions.utils.ExtensionModeUtil.AVAILABLE_EXTENSION_MODES
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class CameraExtensionsInfoTest(private val cameraId: String, private val extensionMode: Int) {

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
    private val context =
        ContextUtil.getPersistentApplicationContext(ApplicationProvider.getApplicationContext())
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var extensionsManager: ExtensionsManager
    private lateinit var cameraExtensionsInfo: CameraExtensionsInfo
    private lateinit var baseCameraSelector: CameraSelector
    private lateinit var extensionCameraSelector: CameraSelector
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner
    private lateinit var camera: Camera
    private lateinit var preview: Preview
    private lateinit var imageCapture: ImageCapture

    @Before
    fun setup() {
        assumeTrue(CameraXExtensionsTestUtil.isTargetDeviceAvailableForExtensions())

        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]

        extensionsManager = runBlocking { ExtensionsManager.getInstance(context, cameraProvider) }

        baseCameraSelector = CameraSelectorUtil.createCameraSelectorById(cameraId)
        assumeTrue(extensionsManager.isExtensionAvailable(baseCameraSelector, extensionMode))

        extensionCameraSelector =
            extensionsManager.getExtensionEnabledCameraSelector(baseCameraSelector, extensionMode)
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
    fun isExtensionStrengthAvailable_returnCorrectValue() {
        val available = isCamera2ExtensionStrengthSupported()
        bindAndRetrieveExtensionsInfo()
        assertThat(cameraExtensionsInfo.isExtensionStrengthAvailable).isEqualTo(available)

        if (available) {
            // Getting the extension strength value should not cause exception
            cameraExtensionsInfo.extensionStrength!!.value
        } else {
            assertThat(cameraExtensionsInfo.extensionStrength).isNull()
        }
    }

    private fun isCamera2ExtensionStrengthSupported(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            (context.getSystemService(Context.CAMERA_SERVICE) as CameraManager).let {
                val characteristics = it.getCameraExtensionCharacteristics(cameraId)
                val camera2ExtensionMode =
                    Camera2ExtensionsUtil.convertCameraXModeToCamera2Mode(extensionMode)

                return characteristics
                    .getAvailableCaptureRequestKeys(camera2ExtensionMode)
                    .contains(CaptureRequest.EXTENSION_STRENGTH)
            }
        }
        return false
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun isCurrentExtensionModeAvailable_returnCorrectValue(): Unit = runBlocking {
        val available = isCamera2CurrentExtensionModeSupported()
        bindAndRetrieveExtensionsInfo()
        assertThat(cameraExtensionsInfo.isCurrentExtensionModeAvailable).isEqualTo(available)

        if (available) {
            // Getting the current extension type value should not cause exception
            cameraExtensionsInfo.currentExtensionMode!!.value

            val completableDeferred = CompletableDeferred<Int>()
            val observer =
                Observer<Int> { currentExtensionType ->
                    completableDeferred.complete(currentExtensionType)
                }

            withContext(Dispatchers.Main) {
                cameraExtensionsInfo.currentExtensionMode!!.observeForever(observer)
            }

            try {
                assertThat(completableDeferred.awaitUntil(3000)).isTrue()
                assertThat(AVAILABLE_EXTENSION_MODES.toList())
                    .contains(completableDeferred.getCompleted())
            } finally {
                withContext(Dispatchers.Main) {
                    cameraExtensionsInfo.currentExtensionMode!!.removeObserver(observer)
                }
            }
        } else {
            assertThat(cameraExtensionsInfo.currentExtensionMode).isNull()
        }
    }

    private fun isCamera2CurrentExtensionModeSupported(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            (context.getSystemService(Context.CAMERA_SERVICE) as CameraManager).let {
                val characteristics = it.getCameraExtensionCharacteristics(cameraId)
                val camera2ExtensionMode =
                    Camera2ExtensionsUtil.convertCameraXModeToCamera2Mode(extensionMode)

                return characteristics
                    .getAvailableCaptureResultKeys(camera2ExtensionMode)
                    .contains(CaptureResult.EXTENSION_CURRENT_TYPE)
            }
        }
        return false
    }

    private fun bindAndRetrieveExtensionsInfo() {
        instrumentation.runOnMainSync {
            fakeLifecycleOwner = FakeLifecycleOwner().apply { startAndResume() }
            preview = Preview.Builder().build()
            preview.surfaceProvider = SurfaceTextureProvider.createSurfaceTextureProvider()
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
    }
}
