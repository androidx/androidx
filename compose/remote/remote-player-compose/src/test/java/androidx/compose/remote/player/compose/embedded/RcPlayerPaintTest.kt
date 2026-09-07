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

import androidx.compose.remote.core.PaintOperation
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.operations.ClipPath
import androidx.compose.remote.core.operations.DrawCircle
import androidx.compose.remote.core.operations.DrawOval
import androidx.compose.remote.core.operations.DrawPath
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

    @Test
    fun updatePaintResolvesNanBoxedGradientStops() {
        val paint = ComposeLocalPaint()
        val context = AndroidRemoteContext(RemoteClock.SYSTEM)
        val stop1Id = 51
        val stop2Id = 52
        context.loadFloat(stop1Id, 0.2f)
        context.loadFloat(stop2Id, 0.8f)

        val bundle =
            PaintBundle().apply {
                setLinearGradient(
                    intArrayOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt()),
                    0,
                    floatArrayOf(Utils.asNan(stop1Id), Utils.asNan(stop2Id)),
                    0f,
                    0f,
                    100f,
                    100f,
                    0,
                )
            }

        updatePaintFromBundle(bundle, paint, context)

        assertThat(paint.brush).isNotNull()
    }

    @Test
    fun drawCircleResolvesNanBoxedCoordinates() {
        val context = AndroidRemoteContext(RemoteClock.SYSTEM)
        val cxId = 101
        val cyId = 102
        val rId = 103
        context.loadFloat(cxId, 50f)
        context.loadFloat(cyId, 60f)
        context.loadFloat(rId, 25f)

        val op = DrawCircle(Utils.asNan(cxId), Utils.asNan(cyId), Utils.asNan(rId))
        val data = op.readDataReflection()

        val resolvedV1 = resolveFloat(data.value1, data.v1, context)
        val resolvedV2 = resolveFloat(data.value2, data.v2, context)
        val resolvedV3 = resolveFloat(data.value3, data.v3, context)

        assertThat(resolvedV1).isEqualTo(50f)
        assertThat(resolvedV2).isEqualTo(60f)
        assertThat(resolvedV3).isEqualTo(25f)
    }

    @Test
    fun drawOvalResolvesNanBoxedCoordinates() {
        val context = AndroidRemoteContext(RemoteClock.SYSTEM)
        val leftId = 111
        val topId = 112
        val rightId = 113
        val bottomId = 114
        context.loadFloat(leftId, 10f)
        context.loadFloat(topId, 20f)
        context.loadFloat(rightId, 80f)
        context.loadFloat(bottomId, 90f)

        val op =
            DrawOval(
                Utils.asNan(leftId),
                Utils.asNan(topId),
                Utils.asNan(rightId),
                Utils.asNan(bottomId),
            )
        val data = op.readDataReflection()

        val resolvedX1 = resolveFloat(data.x1Value, data.x1, context)
        val resolvedY1 = resolveFloat(data.y1Value, data.y1, context)
        val resolvedX2 = resolveFloat(data.x2Value, data.x2, context)
        val resolvedY2 = resolveFloat(data.y2Value, data.y2, context)

        assertThat(resolvedX1).isEqualTo(10f)
        assertThat(resolvedY1).isEqualTo(20f)
        assertThat(resolvedX2).isEqualTo(80f)
        assertThat(resolvedY2).isEqualTo(90f)
    }

    @Test
    fun drawPathDereferencesPointerId() {
        val context = AndroidRemoteContext(RemoteClock.SYSTEM)
        val pointerVarId = 201
        val targetPathId = 301
        context.mRemoteComposeState.updateInteger(pointerVarId, targetPathId)
        context.mRemoteComposeState.putPathData(targetPathId, floatArrayOf(0f, 0f, 10f, 10f))

        val dereferencedId =
            (pointerVarId and PaintOperation.VALUE_MASK) or PaintOperation.PTR_DEREFERENCE
        val op = DrawPath(dereferencedId)
        val data = op.readDataReflection()

        val actualId = derefId(data.id, context)
        assertThat(actualId).isEqualTo(targetPathId)
        assertThat(context.mRemoteComposeState.getPathData(actualId)).isNotNull()
    }

    @Test
    fun clipPathDereferencesPointerId() {
        val context = AndroidRemoteContext(RemoteClock.SYSTEM)
        val pointerVarId = 202
        val targetPathId = 302
        context.mRemoteComposeState.updateInteger(pointerVarId, targetPathId)
        context.mRemoteComposeState.putPathData(targetPathId, floatArrayOf(50f, 50f, 20f))

        val dereferencedId =
            (pointerVarId and PaintOperation.VALUE_MASK) or PaintOperation.PTR_DEREFERENCE
        val op = ClipPath(dereferencedId, ClipPath.PATH_CLIP_INTERSECT)
        val data = op.readData()

        val actualId = derefId(data.id, context)
        assertThat(actualId).isEqualTo(targetPathId)
        assertThat(context.mRemoteComposeState.getPathData(actualId)).isNotNull()
    }
}
