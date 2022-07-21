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
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeSurfaceEffectInternal
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

    private lateinit var surfaceEffectInternal: FakeSurfaceEffectInternal
    private lateinit var appSurface: Surface
    private lateinit var appSurfaceTexture: SurfaceTexture
    private lateinit var node: SurfaceEffectNode
    private lateinit var inputEdge: SurfaceEdge

    @Before
    fun setup() {
        appSurfaceTexture = SurfaceTexture(0)
        appSurface = Surface(appSurfaceTexture)
        surfaceEffectInternal = FakeSurfaceEffectInternal(mainThreadExecutor())
        node = SurfaceEffectNode(FakeCamera(), surfaceEffectInternal)
        inputEdge = createInputEdge()
    }

    @After
    fun tearDown() {
        appSurfaceTexture.release()
        appSurface.release()
        surfaceEffectInternal.release()
        node.release()
        inputEdge.surfaces[0].close()
        shadowOf(getMainLooper()).idle()
    }

    @Test
    fun transformInput_outputHasTheSameProperty() {
        // Arrange.
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
        val inputSurface = inputEdge.surfaces[0]
        val outputEdge = node.transform(inputEdge)
        val outputSurface = outputEdge.surfaces[0]

        // Act.
        outputSurface.setProvider(Futures.immediateFuture(appSurface))
        shadowOf(getMainLooper()).idle()

        // Assert: effect receives app Surface. CameraX receives effect Surface.
        assertThat(surfaceEffectInternal.outputSurface).isEqualTo(appSurface)
        assertThat(inputSurface.surface.get()).isEqualTo(surfaceEffectInternal.inputSurface)
    }

    @Test
    fun releaseNode_effectIsReleased() {
        // Arrange.
        val outputSurface = node.transform(inputEdge).surfaces[0]
        outputSurface.setProvider(Futures.immediateFuture(appSurface))
        shadowOf(getMainLooper()).idle()

        // Act: release the node.
        node.release()
        shadowOf(getMainLooper()).idle()

        // Assert: effect is released and has requested effect to close the SurfaceOutput
        assertThat(surfaceEffectInternal.isReleased).isTrue()
        assertThat(surfaceEffectInternal.isOutputSurfaceRequestedToClose).isTrue()
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