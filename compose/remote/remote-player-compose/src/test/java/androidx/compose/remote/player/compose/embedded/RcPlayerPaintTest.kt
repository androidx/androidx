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

import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerPaintTest {

    @Test
    fun defaultColorIsOpaqueBlackAndIsColorSetIsFalse() {
        val paint = ComposeLocalPaint()
        assertThat(paint.color).isEqualTo(0xFF000000.toInt())
        assertThat(paint.isColorSet).isFalse()
        assertThat(paint.effectiveColor()).isEqualTo(Color(0xFF000000.toInt()))
    }

    @Test
    fun effectiveColorAppliesAlphaToDefaultBlack() {
        val paint = ComposeLocalPaint().apply { alpha = 0.5f }
        val effective = paint.effectiveColor()
        assertThat(effective.alpha).isWithin(0.01f).of(0.5f)
        assertThat(effective.red).isEqualTo(0f)
        assertThat(effective.green).isEqualTo(0f)
        assertThat(effective.blue).isEqualTo(0f)
    }

    @Test
    fun updatePaintWithExplicitColorSetsIsColorSetTrue() {
        val paint = ComposeLocalPaint()
        val context = AndroidRemoteContext(RemoteClock.SYSTEM)
        val bundle = PaintBundle().apply { setColor(0xFFFF0000.toInt()) }

        updatePaintFromBundle(bundle, paint, context)

        assertThat(paint.color).isEqualTo(0xFFFF0000.toInt())
        assertThat(paint.isColorSet).isTrue()
    }

    @Test
    fun updatePaintResolvesNanBoxedStrokeWidth() {
        val paint = ComposeLocalPaint()
        val context = AndroidRemoteContext(RemoteClock.SYSTEM)
        val strokeWidthId = 42
        context.loadFloat(strokeWidthId, 5.5f)
        val bundle = PaintBundle().apply { setStrokeWidth(Utils.asNan(strokeWidthId)) }

        updatePaintFromBundle(bundle, paint, context)

        assertThat(paint.strokeWidth).isEqualTo(5.5f)
        assertThat(paint.isStrokeWidthSet).isTrue()
    }

    @Test
    fun updatePaintResolvesNanBoxedTextSize() {
        val paint = ComposeLocalPaint()
        val context = AndroidRemoteContext(RemoteClock.SYSTEM)
        val textSizeId = 43
        context.loadFloat(textSizeId, 24f)
        val bundle = PaintBundle().apply { setTextSize(Utils.asNan(textSizeId)) }

        updatePaintFromBundle(bundle, paint, context)

        assertThat(paint.textSize).isEqualTo(24f)
        assertThat(paint.isTextSizeSet).isTrue()
    }

    @Test
    fun updatePaintResolvesNanBoxedAlpha() {
        val paint = ComposeLocalPaint()
        val context = AndroidRemoteContext(RemoteClock.SYSTEM)
        val alphaId = 44
        context.loadFloat(alphaId, 0.75f)
        val bundle = PaintBundle().apply { setAlpha(Utils.asNan(alphaId)) }

        updatePaintFromBundle(bundle, paint, context)

        assertThat(paint.alpha).isWithin(0.001f).of(0.75f)
    }
}
