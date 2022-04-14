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

package androidx.camera.extensions

import android.content.Context
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.extensions.util.ExtensionsTestUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.GLUtil
import androidx.camera.testing.SurfaceTextureProvider
import androidx.camera.testing.fakes.FakeLifecycleOwner
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
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

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class PreviewTest(
    @field:ExtensionMode.Mode @param:ExtensionMode.Mode private val extensionMode: Int,
    @field:CameraSelector.LensFacing @param:CameraSelector.LensFacing private val lensFacing: Int
) {

    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private lateinit var cameraProvider: ProcessCameraProvider

    private lateinit var extensionsManager: ExtensionsManager

    private lateinit var baseCameraSelector: CameraSelector

    private val surfaceTextureLatch = CountDownLatch(1)
    private val frameReceivedLatch = CountDownLatch(1)
    private var isSurfaceTextureReleased = false
    private val isSurfaceTextureReleasedLock = Any()

    private val onFrameAvailableListener = object : SurfaceTexture.OnFrameAvailableListener {
        private var complete = false
        private var counter = 0

        override fun onFrameAvailable(surfaceTexture: SurfaceTexture): Unit = runBlocking {
            if (complete) {
                return@runBlocking
            }

            withContext(Dispatchers.Main) {
                synchronized(isSurfaceTextureReleasedLock) {
                    if (!isSurfaceTextureReleased) {
                        surfaceTexture.updateTexImage()
                    }
                }
            }

            if (counter++ >= 10) {
                frameReceivedLatch.countDown()
                complete = true
            }
        }
    }

    private val handler: Handler
    private val handlerThread = HandlerThread("FrameAvailableListener").also {
        it.start()
        handler = Handler(it.looper)
    }

    @Before
    @Throws(Exception::class)
    fun setUp() {
        assumeTrue(
            ExtensionsTestUtil.isTargetDeviceAvailableForExtensions(
                lensFacing,
                extensionMode
            )
        )

        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        baseCameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        extensionsManager = ExtensionsManager.getInstanceAsync(
            context,
            cameraProvider
        )[10000, TimeUnit.MILLISECONDS]

        assumeTrue(extensionsManager.isExtensionAvailable(baseCameraSelector, extensionMode))
    }

    @After
    fun teardown(): Unit = runBlocking {
        if (::cameraProvider.isInitialized) {
            withContext(Dispatchers.Main) {
                cameraProvider.unbindAll()
            }
            cameraProvider.shutdown()[10000, TimeUnit.MILLISECONDS]
        }

        if (::extensionsManager.isInitialized) {
            extensionsManager.shutdown()[10000, TimeUnit.MILLISECONDS]
        }
    }

    companion object {
        @JvmStatic
        @get:Parameterized.Parameters(name = "extension = {0}, facing = {1}")
        val parameters: Collection<Array<Any>>
            get() = ExtensionsTestUtil.getAllExtensionsLensFacingCombinations()
    }

    @UiThreadTest
    @Test
    fun canBindToLifeCycleAndDisplayPreview(): Unit = runBlocking {
        withContext(Dispatchers.Main) {
            val preview = Preview.Builder().build()

            preview.setSurfaceProvider(
                SurfaceTextureProvider.createSurfaceTextureProvider(createSurfaceTextureCallback())
            )

            val fakeLifecycleOwner = FakeLifecycleOwner().apply { startAndResume() }

            val extensionEnabledCameraSelector =
                extensionsManager.getExtensionEnabledCameraSelector(
                    baseCameraSelector,
                    extensionMode
                )

            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                extensionEnabledCameraSelector,
                preview
            )
        }

        // Waits for the surface texture being ready
        assertThat(surfaceTextureLatch.await(10000, TimeUnit.MILLISECONDS)).isTrue()

        // Waits for 10 frames are collected
        assertThat(frameReceivedLatch.await(10000, TimeUnit.MILLISECONDS)).isTrue()
    }

    private fun createSurfaceTextureCallback(): SurfaceTextureProvider.SurfaceTextureCallback =
        object : SurfaceTextureProvider.SurfaceTextureCallback {
            override fun onSurfaceTextureReady(
                surfaceTexture: SurfaceTexture,
                resolution: Size
            ) {
                surfaceTexture.attachToGLContext(GLUtil.getTexIdFromGLContext())
                surfaceTexture.setOnFrameAvailableListener(
                    onFrameAvailableListener, handler
                )
                surfaceTextureLatch.countDown()
            }

            override fun onSafeToRelease(surfaceTexture: SurfaceTexture) {
                synchronized(isSurfaceTextureReleasedLock) {
                    isSurfaceTextureReleased = true
                    surfaceTexture.release()
                }
            }
        }
}