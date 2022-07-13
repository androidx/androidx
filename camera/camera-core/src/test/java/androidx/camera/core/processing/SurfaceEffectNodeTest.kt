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

import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Looper.getMainLooper
import android.util.Size
import android.view.Surface
import androidx.camera.core.SurfaceEffect.PREVIEW
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.testing.fakes.FakeCamera
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
 * Unit tests for [SurfaceEffectNode].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class SurfaceEffectNodeTest {

    companion object {
        private const val TARGET = PREVIEW
        private const val FORMAT = PixelFormat.RGBA_8888
        private const val ROTATION_DEGREES = 90
        private val SIZE = Size(640, 480)
        private val CROP_RECT = Rect(0, 0, 600, 400)
    }

    private lateinit var surfaceEffect: SurfaceEffectInternal
    private var isReleased = false
    private var surfaceOutputCloseRequested = false
    private var surfaceOutputReceived: SurfaceOutput? = null
    private lateinit var appSurface: Surface
    private lateinit var appSurfaceTexture: SurfaceTexture
    private lateinit var effectSurface: Surface
    private lateinit var effectSurfaceTexture: SurfaceTexture

    @Before
    fun setup() {
        appSurfaceTexture = SurfaceTexture(0)
        appSurface = Surface(appSurfaceTexture)
        effectSurfaceTexture = SurfaceTexture(0)
        effectSurface = Surface(effectSurfaceTexture)

        surfaceEffect = object : SurfaceEffectInternal {
            override fun onInputSurface(request: SurfaceRequest) {
                request.provideSurface(effectSurface, mainThreadExecutor()) {
                    effectSurfaceTexture.release()
                    effectSurface.release()
                }
            }

            override fun onOutputSurface(surfaceOutput: SurfaceOutput) {
                surfaceOutputReceived = surfaceOutput
                surfaceOutput.getSurface(mainThreadExecutor()) {
                    surfaceOutputCloseRequested = true
                }
            }

            override fun release() {
                isReleased = true
            }
        }
    }

    @After
    fun tearDown() {
        appSurfaceTexture.release()
        appSurface.release()
        effectSurfaceTexture.release()
        effectSurface.release()
    }

    @Test
    fun transformInput_outputHasTheSameProperty() {
        // Arrange.
        val node = SurfaceEffectNode(FakeCamera(), surfaceEffect)
        val inputEdge = createInputEdge()
        val inputSurface = inputEdge.surfaces[0]

        // Act.
        val outputEdge = node.transform(inputEdge)

        // Asset: without transformation, the output has the same property as the input.
        assertThat(outputEdge.surfaces).hasSize(1)
        val outputSurface = outputEdge.surfaces[0]
        assertThat(outputSurface.size).isEqualTo(inputSurface.size)
        assertThat(outputSurface.format).isEqualTo(inputSurface.format)
        assertThat(outputSurface.targets).isEqualTo(inputSurface.targets)
        assertThat(outputSurface.cropRect).isEqualTo(inputSurface.cropRect)
        assertThat(outputSurface.mirroring).isEqualTo(inputSurface.mirroring)
        assertThat(outputSurface.hasEmbeddedTransform()).isFalse()
    }

    @Test
    fun provideSurfaceToOutput_surfaceIsPropagatedE2E() {
        // Arrange.
        val node = SurfaceEffectNode(FakeCamera(), surfaceEffect)
        val inputEdge = createInputEdge()
        val inputSurface = inputEdge.surfaces[0]
        val outputEdge = node.transform(inputEdge)
        val outputSurface = outputEdge.surfaces[0]

        // Act.
        outputSurface.setProvider(Futures.immediateFuture(appSurface))
        shadowOf(getMainLooper()).idle()

        // Assert: effect receives app Surface. CameraX receives effect Surface.
        val surfaceReceivedByEffect = surfaceOutputReceived!!.getSurface(mainThreadExecutor()) {}
        assertThat(surfaceReceivedByEffect).isEqualTo(appSurface)
        assertThat(inputSurface.surface.get()).isEqualTo(effectSurface)
    }

    @Test
    fun releaseNode_effectIsReleased() {
        // Arrange.
        val node = SurfaceEffectNode(FakeCamera(), surfaceEffect)
        val outputSurface = node.transform(createInputEdge()).surfaces[0]
        outputSurface.setProvider(Futures.immediateFuture(appSurface))
        shadowOf(getMainLooper()).idle()

        // Act: release the node.
        node.release()
        shadowOf(getMainLooper()).idle()

        // Assert: effect is released and has requested effect to close the SurfaceOutput
        assertThat(isReleased).isTrue()
        assertThat(surfaceOutputCloseRequested).isTrue()
    }

    private fun createInputEdge(): SurfaceEdge {
        val surface = SettableSurface(
            TARGET,
            SIZE,
            FORMAT,
            android.graphics.Matrix(),
            true,
            CROP_RECT,
            ROTATION_DEGREES,
            false
        )
        return SurfaceEdge.create(listOf(surface))
    }
}