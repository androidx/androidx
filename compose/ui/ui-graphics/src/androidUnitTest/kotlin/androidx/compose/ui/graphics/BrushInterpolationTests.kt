/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.graphics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.util.lerp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrushInterpolationTests {

    // SolidColor
    @Test
    fun testSolidToSolid() {
        val a = SolidColor(Color.Red)
        val b = SolidColor(Color.Blue)
        val result = Interpolatable.lerp(a, b, 0.5f)
        assertIs<SolidColor>(result)
        assertEquals(lerp(Color.Red, Color.Blue, 0.5f), result.value)
    }

    @Test
    fun testSolidToNull() {
        val a = SolidColor(Color.Red)
        val result = Interpolatable.lerp(a, null, 0.5f)
        assertIs<SolidColor>(result)
        assertEquals(lerp(Color.Red, Color.Transparent, 0.5f), result.value)
    }

    @Test
    fun testNullToSolid() {
        val a = SolidColor(Color.Red)
        val result = Interpolatable.lerp(null, a, 0.5f)
        assertIs<SolidColor>(result)
        assertEquals(lerp(Color.Transparent, Color.Red, 0.5f), result.value)
    }

    // Linear Gradients
    @Test
    fun testLinearToLinear() {
        val a =
            LinearGradient(
                start = Offset.Zero,
                end = Offset(1f, 1f),
                colors = listOf(Color.Red, Color.Blue),
            )
        val b =
            LinearGradient(
                start = Offset(1f, 1f),
                end = Offset.Zero,
                colors = listOf(Color.Green, Color.Yellow),
            )
        val result = Interpolatable.lerp(a, b, 0.5f)
        assertIs<LinearGradient>(result)
        val expected =
            LinearGradient(
                start = lerp(a.start, b.start, 0.5f),
                end = lerp(a.end, b.end, 0.5f),
                colors =
                    listOf(lerp(Color.Red, Color.Green, 0.5f), lerp(Color.Blue, Color.Yellow, 0.5f)),
            )
        assertEquals(expected.start, result.start)
        assertEquals(expected.end, result.end)
        assertEquals(expected.colors, result.colors)
    }

    @Test
    fun testVerticalToHorizontalLinearGradient() {
        val a =
            Brush.verticalGradient(
                colors = listOf(Color.Red, Color.Blue),
                startY = 0.0f,
                endY = 100.0f,
            )
        val b =
            Brush.horizontalGradient(
                colors = listOf(Color.Green, Color.Yellow),
                startX = 0.0f,
                endX = 100.0f,
            )

        val result = Interpolatable.lerp(a, b, 0.5f)
        assertIs<LinearGradient>(result)

        val aAsLinear = a as LinearGradient
        val bAsLinear = b as LinearGradient

        val expected =
            LinearGradient(
                start = lerp(aAsLinear.start, bAsLinear.start, 0.5f),
                end = lerp(aAsLinear.end, bAsLinear.end, 0.5f),
                colors =
                    listOf(lerp(Color.Red, Color.Green, 0.5f), lerp(Color.Blue, Color.Yellow, 0.5f)),
            )
        assertEquals(expected.start, result.start)
        assertEquals(expected.end, result.end)
        assertEquals(expected.colors, result.colors)
    }

    @Test
    fun testVerticalToHorizontalWithDefaultValues() {
        val a = Brush.verticalGradient(colors = listOf(Color.Red, Color.Blue))
        val b = Brush.horizontalGradient(colors = listOf(Color.Green, Color.Yellow))

        val result1 = Interpolatable.lerp(a, b, 0.4f)
        val expected1 =
            LinearGradient(
                colors =
                    listOf(
                        lerp(Color.Red, Color.Green, 0.4f),
                        lerp(Color.Blue, Color.Yellow, 0.4f),
                    ),
                stops = null,
                start = Offset.Zero,
                end = Offset(0f, Float.POSITIVE_INFINITY),
            )
        assertIs<LinearGradient>(result1)
        assertEquals(expected1.colors, result1.colors)
        assertEquals(expected1.start, result1.start)
        assertEquals(expected1.end, result1.end)

        val result2 = Interpolatable.lerp(a, b, 0.6f)
        val expected2 =
            LinearGradient(
                colors =
                    listOf(
                        lerp(Color.Red, Color.Green, 0.6f),
                        lerp(Color.Blue, Color.Yellow, 0.6f),
                    ),
                stops = null,
                start = Offset.Zero,
                end = Offset(Float.POSITIVE_INFINITY, 0f),
            )
        assertIs<LinearGradient>(result2)
        assertEquals(expected2.colors, result2.colors)
        assertEquals(expected2.start, result2.start)
        assertEquals(expected2.end, result2.end)
    }

    @Test
    fun testLinearToNull() {
        val a =
            LinearGradient(
                start = Offset.Zero,
                end = Offset(1f, 1f),
                colors = listOf(Color.Red, Color.Blue),
            )
        val result = Interpolatable.lerp(a, null, 0.5f)
        assertIs<LinearGradient>(result)
        val expected =
            LinearGradient(
                start = Offset.Zero,
                end = Offset(1f, 1f),
                colors =
                    listOf(
                        lerp(Color.Red, Color.Transparent, 0.5f),
                        lerp(Color.Blue, Color.Transparent, 0.5f),
                    ),
            )
        assertEquals(expected.colors, result.colors)
    }

    @Test
    fun testNullToLinear() {
        val a =
            LinearGradient(
                start = Offset.Zero,
                end = Offset(1f, 1f),
                colors = listOf(Color.Red, Color.Blue),
            )
        val result = Interpolatable.lerp(null, a, 0.5f)
        assertIs<LinearGradient>(result)
        val expected =
            LinearGradient(
                start = Offset.Zero,
                end = Offset(1f, 1f),
                colors =
                    listOf(
                        lerp(Color.Transparent, Color.Red, 0.5f),
                        lerp(Color.Transparent, Color.Blue, 0.5f),
                    ),
            )
        assertEquals(expected.colors, result.colors)
    }

    @Test
    fun testLinearToLinearWithStops() {
        val a =
            LinearGradient(
                start = Offset.Zero,
                end = Offset(1f, 1f),
                colors = listOf(Color.Red, Color.Blue),
                stops = listOf(0.0f, 1.0f),
            )
        val b =
            LinearGradient(
                start = Offset(1f, 1f),
                end = Offset.Zero,
                colors = listOf(Color.Green, Color.Yellow),
                stops = listOf(0.2f, 0.8f),
            )
        val result = Interpolatable.lerp(a, b, 0.5f)
        assertIs<LinearGradient>(result)
        val expected =
            LinearGradient(
                start = lerp(a.start, b.start, 0.5f),
                end = lerp(a.end, b.end, 0.5f),
                colors =
                    listOf(
                        lerp(Color.Red, Color.Green, 0.5f),
                        lerp(Color.Blue, Color.Yellow, 0.5f),
                    ),
                stops = listOf(lerp(0.0f, 0.2f, 0.5f), lerp(1.0f, 0.8f, 0.5f)),
            )
        assertEquals(expected.start, result.start)
        assertEquals(expected.end, result.end)
        assertEquals(expected.colors, result.colors)
        assertEquals(expected.stops, result.stops)
    }

    @Test
    fun testLinearToLinearWithMismatchedStops() {
        val a =
            LinearGradient(
                start = Offset.Zero,
                end = Offset(1f, 1f),
                colors = listOf(Color.Red, Color.Blue),
                stops = listOf(0.0f, 1.0f),
            )
        val b =
            LinearGradient(
                start = Offset(1f, 1f),
                end = Offset.Zero,
                colors = listOf(Color.Green, Color.Yellow, Color.Cyan),
                stops = listOf(0.2f, 0.8f, 0.9f),
            )
        val result = Interpolatable.lerp(a, b, 0.5f)
        assertIs<LinearGradient>(result)
        val expected =
            LinearGradient(
                start = lerp(a.start, b.start, 0.5f),
                end = lerp(a.end, b.end, 0.5f),
                colors =
                    listOf(
                        lerp(Color.Red, Color.Green, 0.5f),
                        lerp(Color.Blue, Color.Yellow, 0.5f),
                        lerp(Color.Blue, Color.Cyan, 0.5f),
                    ),
                stops =
                    listOf(lerp(0.0f, 0.2f, 0.5f), lerp(1.0f, 0.8f, 0.5f), lerp(1.0f, 0.9f, 0.5f)),
            )
        assertEquals(expected.start, result.start)
        assertEquals(expected.end, result.end)
        assertEquals(expected.colors, result.colors)
        assertEquals(expected.stops, result.stops)
    }

    @Test
    fun testLinearToLinearWithNullStops() {
        val a =
            LinearGradient(
                start = Offset.Zero,
                end = Offset(1f, 1f),
                colors = listOf(Color.Red, Color.Blue),
                stops = listOf(0.0f, 1.0f),
            )
        val b =
            LinearGradient(
                start = Offset(1f, 1f),
                end = Offset.Zero,
                colors = listOf(Color.Green, Color.Yellow),
            )
        val result = Interpolatable.lerp(a, b, 0.5f)
        assertIs<LinearGradient>(result)
        assertEquals(null, result.stops)
    }

    @Test
    fun testLinearToLinearWithTileMode() {
        val a =
            LinearGradient(
                start = Offset.Zero,
                end = Offset(1f, 1f),
                colors = listOf(Color.Red, Color.Blue),
                tileMode = TileMode.Clamp,
            )
        val b =
            LinearGradient(
                start = Offset(1f, 1f),
                end = Offset.Zero,
                colors = listOf(Color.Green, Color.Yellow),
                tileMode = TileMode.Repeated,
            )
        val result = Interpolatable.lerp(a, b, 0.4f)
        assertIs<LinearGradient>(result)
        assertEquals(a.tileMode, result.tileMode)

        val result2 = Interpolatable.lerp(a, b, 0.6f)
        assertIs<LinearGradient>(result2)
        assertEquals(b.tileMode, result2.tileMode)
    }

    @Test
    fun testSolidToLinear() {
        val a = SolidColor(Color.Red)
        val b =
            LinearGradient(
                start = Offset.Zero,
                end = Offset(1f, 1f),
                colors = listOf(Color.Blue, Color.Green),
            )
        val result = Interpolatable.lerp(a, b, 0.5f)
        assertIs<LinearGradient>(result)
        val expected =
            LinearGradient(
                start = Offset.Zero,
                end = Offset(1f, 1f),
                colors =
                    listOf(lerp(Color.Red, Color.Blue, 0.5f), lerp(Color.Red, Color.Green, 0.5f)),
            )
        assertEquals(expected.colors, result.colors)
    }

    @Test
    fun testLinearToSolid() {
        val a =
            LinearGradient(
                start = Offset.Zero,
                end = Offset(1f, 1f),
                colors = listOf(Color.Blue, Color.Green),
            )
        val b = SolidColor(Color.Red)
        val result = Interpolatable.lerp(a, b, 0.5f)
        assertIs<LinearGradient>(result)
        val expected =
            LinearGradient(
                start = Offset.Zero,
                end = Offset(1f, 1f),
                colors =
                    listOf(lerp(Color.Blue, Color.Red, 0.5f), lerp(Color.Green, Color.Red, 0.5f)),
            )
        assertEquals(expected.colors, result.colors)
    }

    // Radial Gradients
    @Test
    fun testRadialToRadial() {
        val a =
            RadialGradient(
                center = Offset.Zero,
                radius = 1f,
                colors = listOf(Color.Red, Color.Blue),
            )
        val b =
            RadialGradient(
                center = Offset(1f, 1f),
                radius = 2f,
                colors = listOf(Color.Green, Color.Yellow),
            )
        val result = Interpolatable.lerp(a, b, 0.5f)
        assertIs<RadialGradient>(result)
        val expected =
            RadialGradient(
                center = lerp(a.center, b.center, 0.5f),
                radius = lerp(a.radius, b.radius, 0.5f),
                colors =
                    listOf(lerp(Color.Red, Color.Green, 0.5f), lerp(Color.Blue, Color.Yellow, 0.5f)),
            )
        assertEquals(expected.center, result.center)
        assertEquals(expected.radius, result.radius)
        assertEquals(expected.colors, result.colors)
    }

    @Test
    fun testRadialToNull() {
        val a =
            RadialGradient(
                center = Offset.Zero,
                radius = 1f,
                colors = listOf(Color.Red, Color.Blue),
            )
        val result = Interpolatable.lerp(a, null, 0.5f)
        assertIs<RadialGradient>(result)
        val expected =
            RadialGradient(
                center = Offset.Zero,
                radius = 1f,
                colors =
                    listOf(
                        lerp(Color.Red, Color.Transparent, 0.5f),
                        lerp(Color.Blue, Color.Transparent, 0.5f),
                    ),
            )
        assertEquals(expected.colors, result.colors)
    }

    @Test
    fun testNullToRadial() {
        val a =
            RadialGradient(
                center = Offset.Zero,
                radius = 1f,
                colors = listOf(Color.Red, Color.Blue),
            )
        val result = Interpolatable.lerp(null, a, 0.5f)
        assertIs<RadialGradient>(result)
        val expected =
            RadialGradient(
                center = Offset.Zero,
                radius = 1f,
                colors =
                    listOf(
                        lerp(Color.Transparent, Color.Red, 0.5f),
                        lerp(Color.Transparent, Color.Blue, 0.5f),
                    ),
            )
        assertEquals(expected.colors, result.colors)
    }

    @Test
    fun testRadialToRadialWithTileMode() {
        val a =
            RadialGradient(
                center = Offset.Zero,
                radius = 1f,
                colors = listOf(Color.Red, Color.Blue),
                tileMode = TileMode.Mirror,
            )
        val b =
            RadialGradient(
                center = Offset(1f, 1f),
                radius = 2f,
                colors = listOf(Color.Green, Color.Yellow),
                tileMode = TileMode.Decal,
            )
        val result = Interpolatable.lerp(a, b, 0.4f)
        assertIs<RadialGradient>(result)
        assertEquals(a.tileMode, result.tileMode)

        val result2 = Interpolatable.lerp(a, b, 0.6f)
        assertIs<RadialGradient>(result2)
        assertEquals(b.tileMode, result2.tileMode)
    }

    @Test
    fun testRadialToRadialWithStops() {
        val a =
            RadialGradient(
                center = Offset.Zero,
                radius = 1f,
                colors = listOf(Color.Red, Color.Blue),
                stops = listOf(0.0f, 1.0f),
            )
        val b =
            RadialGradient(
                center = Offset(1f, 1f),
                radius = 2f,
                colors = listOf(Color.Green, Color.Yellow),
                stops = listOf(0.2f, 0.8f),
            )
        val result = Interpolatable.lerp(a, b, 0.5f)
        assertIs<RadialGradient>(result)
        val expected =
            RadialGradient(
                center = lerp(a.center, b.center, 0.5f),
                radius = lerp(a.radius, b.radius, 0.5f),
                colors =
                    listOf(
                        lerp(Color.Red, Color.Green, 0.5f),
                        lerp(Color.Blue, Color.Yellow, 0.5f),
                    ),
                stops = listOf(lerp(0.0f, 0.2f, 0.5f), lerp(1.0f, 0.8f, 0.5f)),
            )
        assertEquals(expected.center, result.center)
        assertEquals(expected.radius, result.radius)
        assertEquals(expected.colors, result.colors)
        assertEquals(expected.stops, result.stops)
    }

    @Test
    fun testRadialToRadialWithMismatchedStops() {
        val a =
            RadialGradient(
                center = Offset.Zero,
                radius = 1f,
                colors = listOf(Color.Red, Color.Blue),
                stops = listOf(0.0f, 1.0f),
            )
        val b =
            RadialGradient(
                center = Offset(1f, 1f),
                radius = 2f,
                colors = listOf(Color.Green, Color.Yellow, Color.Cyan),
                stops = listOf(0.2f, 0.8f, 0.9f),
            )
        val result = Interpolatable.lerp(a, b, 0.5f)
        assertIs<RadialGradient>(result)
        val expected =
            RadialGradient(
                center = lerp(a.center, b.center, 0.5f),
                radius = lerp(a.radius, b.radius, 0.5f),
                colors =
                    listOf(
                        lerp(Color.Red, Color.Green, 0.5f),
                        lerp(Color.Blue, Color.Yellow, 0.5f),
                        lerp(Color.Blue, Color.Cyan, 0.5f),
                    ),
                stops =
                    listOf(lerp(0.0f, 0.2f, 0.5f), lerp(1.0f, 0.8f, 0.5f), lerp(1.0f, 0.9f, 0.5f)),
            )
        assertEquals(expected.center, result.center)
        assertEquals(expected.radius, result.radius)
        assertEquals(expected.colors, result.colors)
        assertEquals(expected.stops, result.stops)
    }

    @Test
    fun testRadialToRadialWithNullStops() {
        val a =
            RadialGradient(
                center = Offset.Zero,
                radius = 1f,
                colors = listOf(Color.Red, Color.Blue),
                stops = listOf(0.0f, 1.0f),
            )
        val b =
            RadialGradient(
                center = Offset(1f, 1f),
                radius = 2f,
                colors = listOf(Color.Green, Color.Yellow),
            )
        val result = Interpolatable.lerp(a, b, 0.5f)
        assertIs<RadialGradient>(result)
        assertEquals(null, result.stops)
    }

    @Test
    fun testSolidToRadial() {
        val a = SolidColor(Color.Red)
        val b =
            RadialGradient(
                center = Offset.Zero,
                radius = 1f,
                colors = listOf(Color.Blue, Color.Green),
            )
        val result = Interpolatable.lerp(a, b, 0.5f)
        assertIs<RadialGradient>(result)
        val expected =
            RadialGradient(
                center = Offset.Zero,
                radius = 1f,
                colors =
                    listOf(lerp(Color.Red, Color.Blue, 0.5f), lerp(Color.Red, Color.Green, 0.5f)),
            )
        assertEquals(expected.colors, result.colors)
    }

    @Test
    fun testRadialToSolid() {
        val a =
            RadialGradient(
                center = Offset.Zero,
                radius = 1f,
                colors = listOf(Color.Blue, Color.Green),
            )
        val b = SolidColor(Color.Red)
        val result = Interpolatable.lerp(a, b, 0.5f)
        assertIs<RadialGradient>(result)
        val expected =
            RadialGradient(
                center = Offset.Zero,
                radius = 1f,
                colors =
                    listOf(lerp(Color.Blue, Color.Red, 0.5f), lerp(Color.Green, Color.Red, 0.5f)),
            )
        assertEquals(expected.colors, result.colors)
    }

    // Sweep Gradient
    @Test
    fun testSolidToSweep() {
        val a = SolidColor(Color.Red)
        val b = SweepGradient(center = Offset.Zero, colors = listOf(Color.Blue, Color.Green))
        val result = Interpolatable.lerp(a, b, 0.5f)
        assertIs<SweepGradient>(result)
        val expected =
            SweepGradient(
                center = Offset.Zero,
                colors =
                    listOf(lerp(Color.Red, Color.Blue, 0.5f), lerp(Color.Red, Color.Green, 0.5f)),
            )
        assertEquals(expected.colors, result.colors)
    }

    @Test
    fun testSweepToSolid() {
        val a = SweepGradient(center = Offset.Zero, colors = listOf(Color.Blue, Color.Green))
        val b = SolidColor(Color.Red)
        val result = Interpolatable.lerp(a, b, 0.5f)
        assertIs<SweepGradient>(result)
        val expected =
            SweepGradient(
                center = Offset.Zero,
                colors =
                    listOf(lerp(Color.Blue, Color.Red, 0.5f), lerp(Color.Green, Color.Red, 0.5f)),
            )
        assertEquals(expected.colors, result.colors)
    }

    @Test
    fun testSweepToNull() {
        val a = SweepGradient(center = Offset.Zero, colors = listOf(Color.Red, Color.Blue))
        val result = Interpolatable.lerp(a, null, 0.5f)
        assertIs<SweepGradient>(result)
        val expected =
            SweepGradient(
                center = Offset.Zero,
                colors =
                    listOf(
                        lerp(Color.Red, Color.Transparent, 0.5f),
                        lerp(Color.Blue, Color.Transparent, 0.5f),
                    ),
            )
        assertEquals(expected.colors, result.colors)
    }

    @Test
    fun testNullToSweep() {
        val a = SweepGradient(center = Offset.Zero, colors = listOf(Color.Red, Color.Blue))
        val result = Interpolatable.lerp(null, a, 0.5f)
        assertIs<SweepGradient>(result)
        val expected =
            SweepGradient(
                center = Offset.Zero,
                colors =
                    listOf(
                        lerp(Color.Transparent, Color.Red, 0.5f),
                        lerp(Color.Transparent, Color.Blue, 0.5f),
                    ),
            )
        assertEquals(expected.colors, result.colors)
    }

    @Test
    fun testSweepToSweep() {
        val a = SweepGradient(center = Offset.Zero, colors = listOf(Color.Red, Color.Blue))
        val b = SweepGradient(center = Offset(1f, 1f), colors = listOf(Color.Green, Color.Yellow))
        val result = Interpolatable.lerp(a, b, 0.5f)
        assertIs<SweepGradient>(result)
        val expected =
            SweepGradient(
                center = lerp(a.center, b.center, 0.5f),
                colors =
                    listOf(lerp(Color.Red, Color.Green, 0.5f), lerp(Color.Blue, Color.Yellow, 0.5f)),
            )
        assertEquals(expected.center, result.center)
        assertEquals(expected.colors, result.colors)
    }

    // Between Different Gradient Types
    @Test
    fun testLinearToSweep() {
        val a =
            LinearGradient(
                start = Offset.Zero,
                end = Offset(1f, 1f),
                colors = listOf(Color.Red, Color.Blue),
            )
        val b = SweepGradient(center = Offset.Zero, colors = listOf(Color.Green, Color.Yellow))
        val result = Interpolatable.lerp(a, b, 0.4f)
        assertEquals(a, result)

        val result2 = Interpolatable.lerp(a, b, 0.6f)
        assertEquals(b, result2)
    }

    @Test
    fun testSweepToRadial() {
        val a = SweepGradient(center = Offset.Zero, colors = listOf(Color.Red, Color.Blue))
        val b =
            RadialGradient(
                center = Offset.Zero,
                radius = 1f,
                colors = listOf(Color.Green, Color.Yellow),
            )
        val result = Interpolatable.lerp(a, b, 0.4f)
        assertEquals(a, result)

        val result2 = Interpolatable.lerp(a, b, 0.6f)
        assertEquals(b, result2)
    }

    @Test
    fun testLinearToRadial() {
        val a =
            LinearGradient(
                start = Offset.Zero,
                end = Offset(1f, 1f),
                colors = listOf(Color.Red, Color.Blue),
            )
        val b =
            RadialGradient(
                center = Offset.Zero,
                radius = 1f,
                colors = listOf(Color.Green, Color.Yellow),
            )
        val result = Interpolatable.lerp(a, b, 0.4f)
        assertEquals(a, result)

        val result2 = Interpolatable.lerp(a, b, 0.6f)
        assertEquals(b, result2)
    }
}
