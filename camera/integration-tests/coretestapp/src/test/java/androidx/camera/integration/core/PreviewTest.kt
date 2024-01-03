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
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.integration.core.util.getFakeConfigCameraProvider
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.fakes.FakeAppConfig
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
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
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: FakeCamera
    private lateinit var preview: Preview

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "LensFacing = {0}")
        fun data() = listOf(
            arrayOf(CameraSelector.LENS_FACING_BACK),
            arrayOf(CameraSelector.LENS_FACING_FRONT),
        )
    }

    @After
    fun tearDown() {
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS]
        }
    }

    @Test
    fun bindPreview_surfaceRequested() {
        val countDownLatch = CountDownLatch(1)

        preview = bindPreview { countDownLatch.countDown() }

        assertThat(countDownLatch.await(3, TimeUnit.SECONDS)).isTrue()
    }

    // TODO(b/318364991): Add tests for Preview receiving frames after binding

    private fun bindPreview(surfaceProvider: Preview.SurfaceProvider): Preview {
        cameraProvider = getFakeConfigCameraProvider(context)

        val preview = Preview.Builder().build()

        preview.setSurfaceProvider(CameraXExecutors.directExecutor(), surfaceProvider)

        cameraProvider.bindToLifecycle(
            FakeLifecycleOwner().apply { startAndResume() },
            CameraSelector.Builder().requireLensFacing(lensFacing).build(),
            preview
        )
        camera = when (lensFacing) {
            CameraSelector.LENS_FACING_BACK -> FakeAppConfig.getBackCamera()
            CameraSelector.LENS_FACING_FRONT -> FakeAppConfig.getFrontCamera()
            else -> throw AssertionError("Unsupported lens facing: $lensFacing")
        }

        return preview
    }
}
