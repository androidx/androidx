/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.core.processing

import android.graphics.SurfaceTexture
import android.opengl.Matrix
import android.os.Build
import android.os.Looper
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector.LENS_FACING_FRONT
import androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
import androidx.camera.core.impl.utils.TransformUtils.sizeToRect
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [SurfaceOutputImpl].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class SurfaceOutputImplTest {

    companion object {
        private const val TARGET = CameraEffect.PREVIEW
        private val OUTPUT_SIZE = Size(640, 480)
        private val INPUT_SIZE = Size(640, 480)
    }

    private lateinit var fakeSurface: Surface
    private lateinit var fakeSurfaceTexture: SurfaceTexture
    private val surfacesToCleanup = mutableListOf<SurfaceEdge>()
    private val surfaceOutputsToCleanup = mutableListOf<SurfaceOutputImpl>()

    @Before
    fun setUp() {
        fakeSurfaceTexture = SurfaceTexture(0)
        fakeSurface = Surface(fakeSurfaceTexture)
    }

    @After
    fun tearDown() {
        fakeSurfaceTexture.release()
        fakeSurface.release()
        surfacesToCleanup.forEach {
            it.close()
        }
        surfaceOutputsToCleanup.forEach {
            it.close()
        }
    }

    @Test
    fun closeSurface_closeFutureIsDone() {
        // Arrange.
        val surfaceOutImpl = createFakeSurfaceOutputImpl()
        // Act: close the SurfaceOutput.
        surfaceOutImpl.close()
        shadowOf(Looper.getMainLooper()).idle()
        // Assert:
        assertThat(surfaceOutImpl.closeFuture.isDone).isTrue()
    }

    @Test
    fun requestClose_receivesOnCloseRequested() {
        // Arrange.
        val surfaceOutImpl = createFakeSurfaceOutputImpl()
        var hasRequestedClose = false
        surfaceOutImpl.getSurface(mainThreadExecutor()) {
            hasRequestedClose = true
        }
        // Act.
        surfaceOutImpl.requestClose()
        shadowOf(Looper.getMainLooper()).idle()
        // Assert.
        assertThat(hasRequestedClose).isTrue()
    }

    @Test
    fun updateMatrixWithFrontCamera_mirrored() {
        // Arrange.
        val cameraInfo = FakeCameraInfoInternal(180, LENS_FACING_FRONT)
        val camera = FakeCamera(null, cameraInfo)
        val surfaceOut = createFakeSurfaceOutputImpl(camera)
        val input = FloatArray(16).also {
            Matrix.setIdentityM(it, 0)
        }

        // Act.
        val result = FloatArray(16)
        surfaceOut.updateTransformMatrix(result, input)

        // Assert: the result contains the flipping for OpenGL.
        val expected =
            floatArrayOf(-1F, 0F, 0F, 0F, 0F, 1F, 0F, 0F, 0F, 0F, 1F, 0F, 1F, 0F, 0F, 1F)
        assertThat(result).usingTolerance(1E-4).containsExactly(expected)
    }

    @Test
    fun updateMatrixWithoutCameraTransform_noCameraTransform() {
        // Arrange.
        val surfaceOut = createFakeSurfaceOutputImpl(null)
        val input = FloatArray(16).also {
            Matrix.setIdentityM(it, 0)
        }

        // Act.
        val result = FloatArray(16)
        surfaceOut.updateTransformMatrix(result, input)

        // Assert: the result contains the flipping for OpenGL.
        val expected =
            floatArrayOf(-1F, 0F, 0F, 0F, 0F, -1F, 0F, 0F, 0F, 0F, 1F, 0F, 1F, 1F, 0F, 1F)
        assertThat(result).usingTolerance(1E-4).containsExactly(expected)
    }

    @Test
    fun updateMatrixOnNonIdentityTransform_transformAppliedOnTopOfInput() {
        // Arrange.
        val surfaceOut = createFakeSurfaceOutputImpl()
        val input = FloatArray(16).also {
            Matrix.setIdentityM(it, 0)
            Matrix.rotateM(it, 0, 180F, 0F, 0F, 1F)
        }

        // Act.
        val result = FloatArray(16)
        surfaceOut.updateTransformMatrix(result, input)

        // Assert: the result contains the flipping for OpenGL.
        val expected =
            floatArrayOf(1F, 0F, 0F, 0F, 0F, 1F, 0F, 0F, 0F, 0F, 1F, 0F, -1F, -1F, 0F, 1F)
        assertThat(result).usingTolerance(1E-4).containsExactly(expected)
    }

    @Test
    fun closedSurface_noLongerReceivesCloseRequest() {
        // Arrange.
        val surfaceOutImpl = createFakeSurfaceOutputImpl()
        var hasRequestedClose = false
        surfaceOutImpl.getSurface(mainThreadExecutor()) {
            hasRequestedClose = true
        }

        // Act.
        surfaceOutImpl.close()
        surfaceOutImpl.requestClose()
        shadowOf(Looper.getMainLooper()).idle()

        // Assert.
        assertThat(hasRequestedClose).isFalse()
    }

    private fun createFakeSurfaceOutputImpl(camera: FakeCamera? = FakeCamera()) = SurfaceOutputImpl(
        fakeSurface,
        TARGET,
        INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
        OUTPUT_SIZE,
        INPUT_SIZE,
        sizeToRect(INPUT_SIZE),
        /*rotationDegrees=*/180,
        /*mirroring=*/false,
        camera,
        android.graphics.Matrix()
    ).apply {
        surfaceOutputsToCleanup.add(this)
    }
}
