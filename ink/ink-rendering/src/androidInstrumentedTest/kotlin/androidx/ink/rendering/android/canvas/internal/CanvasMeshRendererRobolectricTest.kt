/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.rendering.android.canvas.internal

import android.os.Build
import androidx.ink.brush.Brush
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.InputToolType
import androidx.ink.brush.StockBrushes
import androidx.ink.strokes.ImmutableStrokeInputBatch
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Non-emulator logic test of [CanvasMeshRenderer].
 *
 * Code in this test cannot create an [android.graphics.MeshSpecification] or
 * [android.graphics.Mesh], but it allows a limited subset of tests to run much more quickly.
 *
 * Note that in AndroidX, this test runs on the emulator rather than Robolectric, so it doesn't have
 * a speed benefit.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@RunWith(AndroidJUnit4::class)
@MediumTest
class CanvasMeshRendererRobolectricTest {
    private val brush = Brush(family = StockBrushes.markerLatest, size = 10f, epsilon = 0.1f)

    private val stroke =
        Stroke(
            brush = brush,
            inputs =
                MutableStrokeInputBatch()
                    .addOrThrow(InputToolType.UNKNOWN, x = 10F, y = 10F, elapsedTimeMillis = 100)
                    .asImmutable(),
        )

    private val meshRenderer = @OptIn(ExperimentalInkCustomBrushApi::class) CanvasMeshRenderer()

    @Test
    fun canDraw_withRenderableMesh_returnsTrue() {
        assertThat(meshRenderer.canDraw(stroke)).isTrue()
    }

    @Test
    fun canDraw_withEmptyStroke_returnsTrue() {
        val emptyStroke = Stroke(brush, ImmutableStrokeInputBatch.EMPTY)

        assertThat(meshRenderer.canDraw(emptyStroke)).isTrue()
    }
}
