/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.core

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.Rational
import android.util.Size
import android.view.Surface
import androidx.camera.core.impl.CameraFactory
import androidx.camera.core.impl.CameraThreadConfig
import androidx.camera.testing.fakes.FakeAppConfig
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraDeviceSurfaceManager
import androidx.camera.testing.fakes.FakeCameraFactory
import androidx.camera.testing.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import java.util.concurrent.ExecutionException

/**
 * Unit tests for [Preview].
 */
@SmallTest
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP, shadows = [ShadowCameraX::class])
class PreviewTest {

    @Before
    @Throws(ExecutionException::class, InterruptedException::class)
    fun setUp() {
        val cameraFactoryProvider =
            CameraFactory.Provider { _: Context?, _: CameraThreadConfig? ->
                val cameraFactory = FakeCameraFactory()
                cameraFactory.insertDefaultBackCamera(
                    ShadowCameraX.DEFAULT_CAMERA_ID
                ) { FakeCamera(ShadowCameraX.DEFAULT_CAMERA_ID) }
                cameraFactory
            }
        val cameraXConfig = CameraXConfig.Builder.fromConfig(
            FakeAppConfig.create()
        ).setCameraFactoryProvider(cameraFactoryProvider).build()
        val context = ApplicationProvider.getApplicationContext<Context>()
        CameraX.initialize(context, cameraXConfig).get()
    }

    @After
    @Throws(ExecutionException::class, InterruptedException::class)
    fun tearDown() {
        InstrumentationRegistry.getInstrumentation()
            .runOnMainSync { CameraX.unbindAll() }
        CameraX.shutdown().get()
    }

    @Test
    fun viewPortCropSize() {
        val expectedSurfaceRequest = bindToLifecycleAndGetSurfaceRequest(
            ViewPort.Builder(Rational(1, 1), Surface.ROTATION_0).build()
        )
        // The expected value is based on fitting the 1:1 view port into a rect with the size of
        // FakeCameraDeviceSurfaceManager.MAX_OUTPUT_SIZE.
        val expectedPadding = (FakeCameraDeviceSurfaceManager.MAX_OUTPUT_SIZE.width -
                FakeCameraDeviceSurfaceManager.MAX_OUTPUT_SIZE.height) / 2
        Truth.assertThat(expectedSurfaceRequest.viewPortRect).isEqualTo(
            Rect(
                expectedPadding,
                0,
                FakeCameraDeviceSurfaceManager.MAX_OUTPUT_SIZE.width - expectedPadding,
                FakeCameraDeviceSurfaceManager.MAX_OUTPUT_SIZE.height
            )
        )
    }

    @Test
    fun surfaceRequestSize_isSurfaceSize() {
        Truth.assertThat(bindToLifecycleAndGetSurfaceRequest().resolution).isEqualTo(
            Size(
                FakeCameraDeviceSurfaceManager.MAX_OUTPUT_SIZE.width,
                FakeCameraDeviceSurfaceManager.MAX_OUTPUT_SIZE.height
            )
        )
    }

    private fun bindToLifecycleAndGetSurfaceRequest(): SurfaceRequest {
        return bindToLifecycleAndGetSurfaceRequest(null)
    }

    private fun bindToLifecycleAndGetSurfaceRequest(viewPort: ViewPort?): SurfaceRequest {
        // Arrange.
        val preview = Preview.Builder().setTargetRotation(Surface.ROTATION_0).build()
        var surfaceRequest: SurfaceRequest? = null
        preview.setSurfaceProvider { surfaceRequest = it }

        // Act.
        val lifecycleOwner = FakeLifecycleOwner()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            CameraX.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                viewPort,
                preview
            )
            lifecycleOwner.startAndResume()
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        return surfaceRequest!!
    }
}