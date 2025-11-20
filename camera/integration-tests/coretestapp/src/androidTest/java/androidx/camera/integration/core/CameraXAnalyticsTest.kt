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

package androidx.camera.integration.core

import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.Preview
import androidx.camera.core.impl.TagBundle
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.util.Camera2InteropUtil
import androidx.concurrent.futures.await
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class CameraXAnalyticsTest(private val implName: String, private val cameraXConfig: CameraXConfig) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            listOf(
                arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
                arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig()),
            )
    }

    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(cameraXConfig)
        )

    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName == CameraPipeConfig::class.simpleName)
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraSelector: CameraSelector
    private var fakeLifecycleOwner = FakeLifecycleOwner()

    @Before
    fun setUp() = runBlocking {
        cameraSelector = CameraUtil.assumeFirstAvailableCameraSelector()
        ProcessCameraProvider.configureInstance(cameraXConfig)
        cameraProvider = ProcessCameraProvider.awaitInstance(context)
        fakeLifecycleOwner.startAndResume()
    }

    @After
    fun tearDown(): Unit = runBlocking {
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync().await()
        }
    }

    @Test
    fun captureRequestTagContainsAnalyticsPrefix(): Unit = runBlocking {
        verifyCaptureRequestTagContainsAnalyticsPrefix(cameraSelector)
    }

    suspend fun verifyCaptureRequestTagContainsAnalyticsPrefix(cameraSelector: CameraSelector) {
        val captureRequestTagDeferred = CompletableDeferred<Any?>()
        val preview =
            Preview.Builder()
                .also {
                    Camera2InteropUtil.setCameraCaptureSessionCallback(
                        implName,
                        it,
                        object : CameraCaptureSession.CaptureCallback() {
                            override fun onCaptureCompleted(
                                session: CameraCaptureSession,
                                request: CaptureRequest,
                                result: TotalCaptureResult,
                            ) {
                                if (!captureRequestTagDeferred.isCompleted) {
                                    captureRequestTagDeferred.complete(request.tag)
                                }
                            }
                        },
                    )
                }
                .build()
        withContext(Dispatchers.Main) {
            preview.surfaceProvider =
                SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, preview)
        }

        assertThat(withTimeoutOrNull(3000) { captureRequestTagDeferred.await() }.toString())
            .isEqualTo(TagBundle.CAMERAX_USER_TAG_PREFIX)
    }

    @Test
    fun extensionsCaptureRequestTagContainsAnalyticsPrefix(): Unit = runBlocking {
        val extensionsManager = ExtensionsManager.getInstanceAsync(context, cameraProvider).await()
        assumeTrue(extensionsManager.isExtensionAvailable(cameraSelector, ExtensionMode.NIGHT))

        val extensionCameraSelector =
            extensionsManager.getExtensionEnabledCameraSelector(cameraSelector, ExtensionMode.NIGHT)
        verifyCaptureRequestTagContainsAnalyticsPrefix(extensionCameraSelector)
    }
}
