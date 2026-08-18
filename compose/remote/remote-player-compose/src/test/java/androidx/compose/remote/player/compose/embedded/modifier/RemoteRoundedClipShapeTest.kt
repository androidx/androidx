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

package androidx.compose.remote.player.compose.embedded.modifier

import androidx.compose.remote.core.CoreDocument
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RemoteRoundedClipShapeTest {

    private val density = Density(2f)
    private val size = Size(100f, 200f)

    @Test
    fun resolvesToFallbackWhenNaN() {
        val topStart = mutableStateOf(Float.NaN)
        val topEnd = mutableStateOf(10f)
        val bottomEnd = mutableStateOf(10f)
        val bottomStart = mutableStateOf(10f)

        val shape = RemoteRoundedClipShape(topStart, topEnd, bottomEnd, bottomStart)
        val outline = shape.createOutline(size, LayoutDirection.Ltr, density) as Outline.Rounded

        // minDimension = 100f, fallback = minDimension / 2f = 50f
        assertEquals(50f, outline.roundRect.topLeftCornerRadius.x)
        assertEquals(20f, outline.roundRect.topRightCornerRadius.x) // 10f * 2 (density)
    }

    @Test
    fun resolvesToZeroWhenZero() {
        val topStart = mutableStateOf(0f)
        val topEnd = mutableStateOf(10f)
        val bottomEnd = mutableStateOf(10f)
        val bottomStart = mutableStateOf(10f)

        val shape = RemoteRoundedClipShape(topStart, topEnd, bottomEnd, bottomStart)
        val outline = shape.createOutline(size, LayoutDirection.Ltr, density) as Outline.Rounded

        assertEquals(0f, outline.roundRect.topLeftCornerRadius.x)
    }

    @Test
    fun resolvesVariableCornerWithDensityUnderDpBehavior() {
        val topStart = mutableStateOf(25f)
        val topEnd = mutableStateOf(10f)
        val bottomEnd = mutableStateOf(10f)
        val bottomStart = mutableStateOf(10f)

        val shape =
            RemoteRoundedClipShape(
                topStart,
                topEnd,
                bottomEnd,
                bottomStart,
                densityBehavior = CoreDocument.DENSITY_BEHAVIOR_DP,
            )
        val outline = shape.createOutline(size, LayoutDirection.Ltr, density) as Outline.Rounded

        // Variable corner evaluated to 25f is density scaled: 25f * 2.0 = 50f
        assertEquals(50f, outline.roundRect.topLeftCornerRadius.x)
    }

    @Test
    fun resolvedCornerMatchesRemoteCoreDensityBehavior() {
        val topStart = mutableStateOf(25f)
        val topEnd = mutableStateOf(10f)
        val bottomEnd = mutableStateOf(10f)
        val bottomStart = mutableStateOf(10f)

        val shapeDp =
            RemoteRoundedClipShape(
                topStart,
                topEnd,
                bottomEnd,
                bottomStart,
                densityBehavior = CoreDocument.DENSITY_BEHAVIOR_DP,
            )
        val outlineDp = shapeDp.createOutline(size, LayoutDirection.Ltr, density) as Outline.Rounded
        assertEquals(50f, outlineDp.roundRect.topLeftCornerRadius.x)

        val shapePx =
            RemoteRoundedClipShape(
                topStart,
                topEnd,
                bottomEnd,
                bottomStart,
                densityBehavior = CoreDocument.DENSITY_BEHAVIOR_PIXELS,
            )
        val outlinePx = shapePx.createOutline(size, LayoutDirection.Ltr, density) as Outline.Rounded
        assertEquals(25f, outlinePx.roundRect.topLeftCornerRadius.x)

        val shapeScale =
            RemoteRoundedClipShape(
                topStart,
                topEnd,
                bottomEnd,
                bottomStart,
                densityBehavior = CoreDocument.DENSITY_BEHAVIOR_LEGACY,
            )
        val outlineScale =
            shapeScale.createOutline(size, LayoutDirection.Ltr, density) as Outline.Rounded
        assertEquals(25f, outlineScale.roundRect.topLeftCornerRadius.x)
    }

    @Test
    fun shapeAppliesDpBehaviorAfterReactiveResolution() {
        val topStart = mutableStateOf(10f)
        val topEnd = mutableStateOf(10f)
        val bottomEnd = mutableStateOf(10f)
        val bottomStart = mutableStateOf(10f)

        val shape =
            RemoteRoundedClipShape(
                topStart,
                topEnd,
                bottomEnd,
                bottomStart,
                densityBehavior = CoreDocument.DENSITY_BEHAVIOR_DP,
            )
        val outline1 = shape.createOutline(size, LayoutDirection.Ltr, density) as Outline.Rounded
        assertEquals(20f, outline1.roundRect.topLeftCornerRadius.x) // 10 * 2

        topStart.value = 30f
        val outline2 = shape.createOutline(size, LayoutDirection.Ltr, density) as Outline.Rounded
        assertEquals(60f, outline2.roundRect.topLeftCornerRadius.x) // 30 * 2
    }

    @Test
    fun smallLiteralCornerNotConvertedToPercentage() {
        val topStart = mutableStateOf(0.5f)
        val topEnd = mutableStateOf(10f)
        val bottomEnd = mutableStateOf(10f)
        val bottomStart = mutableStateOf(10f)

        val shape =
            RemoteRoundedClipShape(
                topStart,
                topEnd,
                bottomEnd,
                bottomStart,
                densityBehavior = CoreDocument.DENSITY_BEHAVIOR_PIXELS,
            )
        val outline = shape.createOutline(size, LayoutDirection.Ltr, density) as Outline.Rounded

        // Literal 0.5f in pixels should remain 0.5f, NOT 0.5 * minDimension (50f)
        assertEquals(0.5f, outline.roundRect.topLeftCornerRadius.x)
    }

    @Test
    fun scalesLiteralCornersUnderDpDensityBehavior() {
        val topStart = mutableStateOf(26f)
        val topEnd = mutableStateOf(10f)
        val bottomEnd = mutableStateOf(10f)
        val bottomStart = mutableStateOf(10f)

        val shapeDp =
            RemoteRoundedClipShape(
                topStart,
                topEnd,
                bottomEnd,
                bottomStart,
                densityBehavior = CoreDocument.DENSITY_BEHAVIOR_DP,
            )
        val outlineDp = shapeDp.createOutline(size, LayoutDirection.Ltr, density) as Outline.Rounded

        // 26f * 2.0 (density) = 52f
        assertEquals(52f, outlineDp.roundRect.topLeftCornerRadius.x)

        val shapePx =
            RemoteRoundedClipShape(
                topStart,
                topEnd,
                bottomEnd,
                bottomStart,
                densityBehavior = CoreDocument.DENSITY_BEHAVIOR_PIXELS,
            )
        val outlinePx = shapePx.createOutline(size, LayoutDirection.Ltr, density) as Outline.Rounded

        // Non-DP behavior: used as-is (26f)
        assertEquals(26f, outlinePx.roundRect.topLeftCornerRadius.x)
    }

    @Test
    fun updatesReactively() {
        val topStart = mutableStateOf(0f)
        val topEnd = mutableStateOf(10f)
        val bottomEnd = mutableStateOf(10f)
        val bottomStart = mutableStateOf(10f)

        val shape =
            RemoteRoundedClipShape(
                topStart,
                topEnd,
                bottomEnd,
                bottomStart,
                densityBehavior = CoreDocument.DENSITY_BEHAVIOR_PIXELS,
            )

        val outline1 = shape.createOutline(size, LayoutDirection.Ltr, density) as Outline.Rounded
        assertEquals(0f, outline1.roundRect.topLeftCornerRadius.x)

        // Mutate the state value
        topStart.value = 40f

        val outline2 = shape.createOutline(size, LayoutDirection.Ltr, density) as Outline.Rounded
        assertEquals(40f, outline2.roundRect.topLeftCornerRadius.x)
    }

    @Test
    fun scalesRadiiProportionallyWhenRadiiExceedDimensions() {
        val topStart = mutableStateOf(80f)
        val topEnd = mutableStateOf(80f)
        val bottomEnd = mutableStateOf(10f)
        val bottomStart = mutableStateOf(10f)

        val shape =
            RemoteRoundedClipShape(
                topStart,
                topEnd,
                bottomEnd,
                bottomStart,
                densityBehavior = CoreDocument.DENSITY_BEHAVIOR_PIXELS,
            )
        // size.width = 100f, topStart (80) + topEnd (80) = 160 > 100
        // scale = 100 / 160 = 0.625 -> 80 * 0.625 = 50f
        val outline = shape.createOutline(size, LayoutDirection.Ltr, density) as Outline.Rounded

        assertEquals(50f, outline.roundRect.topLeftCornerRadius.x)
        assertEquals(50f, outline.roundRect.topRightCornerRadius.x)
    }

    @Test
    fun doesNotScaleRadiiWhenWithinDimensions() {
        val topStart = mutableStateOf(30f)
        val topEnd = mutableStateOf(40f)
        val bottomEnd = mutableStateOf(20f)
        val bottomStart = mutableStateOf(10f)

        val shape =
            RemoteRoundedClipShape(
                topStart,
                topEnd,
                bottomEnd,
                bottomStart,
                densityBehavior = CoreDocument.DENSITY_BEHAVIOR_PIXELS,
            )
        val outline = shape.createOutline(size, LayoutDirection.Ltr, density) as Outline.Rounded

        assertEquals(30f, outline.roundRect.topLeftCornerRadius.x)
        assertEquals(40f, outline.roundRect.topRightCornerRadius.x)
        assertEquals(20f, outline.roundRect.bottomRightCornerRadius.x)
        assertEquals(10f, outline.roundRect.bottomLeftCornerRadius.x)
    }
}
