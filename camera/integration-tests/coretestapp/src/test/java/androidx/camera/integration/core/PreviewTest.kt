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

import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.rules.FakeCameraTestRule
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class PreviewTest(
    @CameraSelector.LensFacing private val lensFacing: Int,
) {
    @get:Rule val fakeCameraRule = FakeCameraTestRule(ApplicationProvider.getApplicationContext())

    private lateinit var preview: Preview

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "LensFacing = {0}")
        fun data() =
            listOf(
                arrayOf(CameraSelector.LENS_FACING_BACK),
                arrayOf(CameraSelector.LENS_FACING_FRONT),
            )
    }

    @Test
    fun bindPreview_surfaceRequested() = runTest {
        val countDownLatch = CountDownLatch(1)

        preview = bindPreview { countDownLatch.countDown() }

        assertThat(countDownLatch.await(3, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun bindPreview_surfaceUpdatedWithCaptureFrames_afterCaptureSessionConfigured() = runTest {
        val countDownLatch = CountDownLatch(5)

        preview = bindPreview { request ->
            val surfaceTexture = SurfaceTexture(0)
            surfaceTexture.setDefaultBufferSize(request.resolution.width, request.resolution.height)
            surfaceTexture.detachFromGLContext()
            val frameUpdateThread = HandlerThread("frameUpdateThread").apply { start() }

            surfaceTexture.setOnFrameAvailableListener(
                { countDownLatch.countDown() },
                Handler(frameUpdateThread.getLooper())
            )

            val surface = Surface(surfaceTexture)
            request.provideSurface(surface, CameraXExecutors.directExecutor()) {
                surface.release()
                surfaceTexture.release()
                frameUpdateThread.quitSafely()
            }
        }

        repeat(5) {
            fakeCameraRule
                .getFakeCamera(lensFacing)
                .simulateCaptureFrameAsync()
                .get(3, TimeUnit.SECONDS)
        }

        assertThat(countDownLatch.await(3, TimeUnit.SECONDS)).isTrue()
    }

    private fun bindPreview(surfaceProvider: Preview.SurfaceProvider): Preview {
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(CameraXExecutors.directExecutor(), surfaceProvider)
        fakeCameraRule.bindUseCases(lensFacing = lensFacing, useCases = listOf(preview))
        return preview
    }
}
