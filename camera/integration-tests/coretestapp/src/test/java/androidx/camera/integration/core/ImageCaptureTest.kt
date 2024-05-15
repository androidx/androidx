/*
 * Copyright 2024 The Android Open Source Project
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
import android.os.Build
import android.os.Looper
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.integration.core.util.getFakeConfigCameraProvider
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.fakes.FakeAppConfig
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraControl
import androidx.camera.testing.impl.fakes.FakeImageCaptureCallback
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.testutils.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class ImageCaptureTest(
    @CameraSelector.LensFacing private val lensFacing: Int,
) {
    private val testScope = TestScope()
    private val testDispatcher = StandardTestDispatcher(testScope.testScheduler)

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: FakeCamera
    private lateinit var cameraControl: FakeCameraControl
    private lateinit var imageCapture: ImageCapture

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "LensFacing = {0}")
        fun data() = listOf(
            arrayOf(CameraSelector.LENS_FACING_BACK),
            arrayOf(CameraSelector.LENS_FACING_FRONT),
        )
    }

    @Before
    fun setup() {
        imageCapture = bindImageCapture()
        assertThat(imageCapture).isNotNull()
    }

    @After
    fun tearDown() {
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS]
        }
    }

    @Test
    fun canSubmitTakePictureRequest(): Unit = runTest {
        val countDownLatch = CountDownLatch(1)
        cameraControl.setOnNewCaptureRequestListener { countDownLatch.countDown() }

        imageCapture.takePicture(CameraXExecutors.directExecutor(), FakeImageCaptureCallback())

        assertThat(countDownLatch.await(3, TimeUnit.SECONDS)).isTrue()
    }

    @Ignore("TODO: b/318314454")
    @Test
    fun canTakeImage(): Unit = runTest {
        val callback = FakeImageCaptureCallback()
        imageCapture.takePicture(CameraXExecutors.directExecutor(), callback)
        shadowOf(Looper.getMainLooper()).idle()
        callback.awaitCaptures()
    }

    private fun bindImageCapture(): ImageCapture {
        cameraProvider = getFakeConfigCameraProvider(context)

        val imageCapture = ImageCapture.Builder().build()

        cameraProvider.bindToLifecycle(
            FakeLifecycleOwner().apply { startAndResume() },
            CameraSelector.Builder().requireLensFacing(lensFacing).build(),
            imageCapture
        )
        camera = when (lensFacing) {
            CameraSelector.LENS_FACING_BACK -> FakeAppConfig.getBackCamera()
            CameraSelector.LENS_FACING_FRONT -> FakeAppConfig.getFrontCamera()
            else -> throw AssertionError("Unsupported lens facing: $lensFacing")
        }
        cameraControl = camera.cameraControl as FakeCameraControl

        return imageCapture
    }
}
