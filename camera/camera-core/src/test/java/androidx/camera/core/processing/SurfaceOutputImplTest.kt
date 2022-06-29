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

import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.opengl.Matrix
import android.os.Build
import android.os.Looper
import android.util.Size
import android.view.Surface
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
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
        private val IDENTITY_MATRIX = FloatArray(16).apply {
            Matrix.setIdentityM(this, 0)
        }
        private const val FLOAT_TOLERANCE = 1E-4
    }

    lateinit var fakeSurface: Surface
    lateinit var fakeSurfaceTexture: SurfaceTexture
    private val surfacesToCleanup = mutableListOf<SettableSurface>()
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

    @Test(expected = java.lang.IllegalStateException::class)
    fun createWithIncompleteFuture_throwsException() {
        createFakeSurfaceOutputImpl(settableSurface = createIncompleteSettableSurface())
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

    @Test
    fun updateMatrix_multipliesMatrices() {
        // Arrange.
        // 2x scaling on the x axis.
        val scale2x = FloatArray(16).apply {
            Matrix.setIdentityM(this, 0)
            Matrix.scaleM(this, 0, 2F, 1F, 1F)
        }
        val surfaceOut = createFakeSurfaceOutputImpl(transform = scale2x)

        // Act: apply the 2x scaling on top of the 90° rotation.
        // 90° clockwise rotation around (0, 0).
        val rotate90 = FloatArray(16).apply {
            Matrix.setRotateM(this, 0, 90F, 0F, 0F, -1F)
        }
        val result = FloatArray(16)
        surfaceOut.updateTransformMatrix(result, rotate90)

        // Assert.
        // Assert the result is a multiplication of the two matrices.
        val expectedMatrix = FloatArray(16).apply {
            Matrix.multiplyMM(this, 0, scale2x, 0, rotate90, 0)
        }
        assertThat(result).usingTolerance(FLOAT_TOLERANCE).containsExactly(expectedMatrix)

        // Assert coordinates mapping is correct.
        //       90° rotation         2x scaling on the X axis
        // (1,1) -------------> (1,-1) ----------------------> (2,-1)
        val point = floatArrayOf(1F, 1F, 0F, 1F)
        val expectedPoint = FloatArray(4)
        Matrix.multiplyMV(expectedPoint, 0, result, 0, point, 0)
        assertThat(expectedPoint).usingTolerance(FLOAT_TOLERANCE)
            .containsExactly(floatArrayOf(2F, -1F, 0F, 1F))
    }

    private fun createCompleteSettableSurface(): SettableSurface {
        return createFakeSettableSurface(true)
    }

    private fun createIncompleteSettableSurface(): SettableSurface {
        return createFakeSettableSurface(false)
    }

    private fun createFakeSettableSurface(setComplete: Boolean): SettableSurface {
        val settableSurface = SettableSurface(
            SurfaceOutput.PREVIEW, Size(640, 480), ImageFormat.PRIVATE,
            android.graphics.Matrix(), true, Rect(), 0, false
        )
        if (setComplete) {
            settableSurface.mCompleter.set(fakeSurface)
        }
        surfacesToCleanup.add(settableSurface)
        return settableSurface
    }

    private fun createFakeSurfaceOutputImpl(
        settableSurface: SettableSurface = createCompleteSettableSurface(),
        transform: FloatArray = IDENTITY_MATRIX
    ) = SurfaceOutputImpl(settableSurface, transform).apply {
        surfaceOutputsToCleanup.add(this)
    }
}