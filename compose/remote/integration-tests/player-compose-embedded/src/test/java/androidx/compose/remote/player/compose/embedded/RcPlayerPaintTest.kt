/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.player.compose.embedded

import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ShaderBrush
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerPaintTest {

    @Test
    fun updatePaintFromBundle_resolvesGradientBoundColorIdStop() {
        val remoteContext = AndroidRemoteContext()
        val colorId = 42
        val initialCyan = 0xFF7DE2FF.toInt()
        remoteContext.mRemoteComposeState.overrideColor(colorId, initialCyan)

        // Linear gradient with 3 stops:
        // stop 0: literal 0xFF101820 (register bit 0 = 0)
        // stop 1: color-id 42 (register bit 1 = 1 -> register = 2)
        // stop 2: literal 0xFFFFB86C (register bit 2 = 0)
        val bundle = PaintBundle()
        bundle.setLinearGradient(
            intArrayOf(0xFF101820.toInt(), colorId, 0xFFFFB86C.toInt()),
            1 shl 1, // bit 1 indicates stop 1 is a color-id reference
            floatArrayOf(0f, 0.5f, 1f),
            0f,
            0f,
            100f,
            100f,
            0, // Clamp
        )

        val paintState1 = ComposeLocalPaint()
        updatePaintFromBundle(bundle, paintState1, remoteContext, read = remoteContext)

        val brush1 = paintState1.brush
        assertNotNull("Brush should not be null", brush1)
        assertTrue("Brush should be a ShaderBrush", brush1 is ShaderBrush)

        val shader1 = (brush1 as ShaderBrush).createShader(Size(100f, 100f))
        assertNotNull("Framework shader should be created", shader1)

        // Live recolor: update colorId 42 from Cyan to Magenta
        val updatedMagenta = 0xFFFF00FF.toInt()
        remoteContext.mRemoteComposeState.overrideColor(colorId, updatedMagenta)

        val paintState2 = ComposeLocalPaint()
        updatePaintFromBundle(bundle, paintState2, remoteContext, read = remoteContext)

        val brush2 = paintState2.brush
        assertNotNull("Brush after recolor should not be null", brush2)
        assertTrue("Brush after recolor should be a ShaderBrush", brush2 is ShaderBrush)
        assertNotEquals("Brushes before and after recolor should differ", brush1, brush2)
    }

    @Test
    fun updatePaintFromBundle_allLiteralGradientStops() {
        val remoteContext = AndroidRemoteContext()
        val bundle = PaintBundle()
        // Gradient with all literal stops (register bitmask = 0)
        bundle.setLinearGradient(
            intArrayOf(0xFF101820.toInt(), 0xFF7DE2FF.toInt(), 0xFFFFB86C.toInt()),
            0, // all literal stops
            floatArrayOf(0f, 0.5f, 1f),
            0f,
            0f,
            100f,
            100f,
            0,
        )

        val paintState = ComposeLocalPaint()
        updatePaintFromBundle(bundle, paintState, remoteContext, read = remoteContext)

        assertNotNull(paintState.brush)
        assertTrue(paintState.brush is ShaderBrush)
    }

    @Test
    fun updatePaintFromBundle_radialGradientWithColorIdStop() {
        val remoteContext = AndroidRemoteContext()
        val colorId = 99
        remoteContext.mRemoteComposeState.overrideColor(colorId, 0xFF00FF00.toInt()) // Green

        val bundle = PaintBundle()
        bundle.setRadialGradient(
            intArrayOf(0xFF0000FF.toInt(), colorId),
            1 shl 1, // bit 1 is colorId
            floatArrayOf(0f, 1f),
            50f,
            50f,
            50f,
            0,
        )

        val paintState = ComposeLocalPaint()
        updatePaintFromBundle(bundle, paintState, remoteContext, read = remoteContext)

        assertNotNull(paintState.brush)
        assertTrue(paintState.brush is ShaderBrush)
    }
}
