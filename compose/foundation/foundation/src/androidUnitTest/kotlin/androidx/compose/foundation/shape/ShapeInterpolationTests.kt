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

package androidx.compose.foundation.shape

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Interpolatable
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ShapeInterpolationTests {
    private val density = Density(1f)
    private val size = Size(100f, 100f)

    private fun assertEquals(expected: CornerSize, actual: CornerSize) {
        assertEquals(expected.toPx(size, density), actual.toPx(size, density))
    }

    @Test
    fun testRoundedCornerToRoundedCorner() {
        val a = RoundedCornerShape(10.dp)
        val b = RoundedCornerShape(20.dp)
        val result = Interpolatable.lerp(a, b, 0.5f)
        assertIs<RoundedCornerShape>(result)
        assertEquals(CornerSize(15.dp), result.topStart)
    }

    @Test
    fun testCutCornerToCutCorner() {
        val a = CutCornerShape(10.dp)
        val b = CutCornerShape(20.dp)
        val result = Interpolatable.lerp(a, b, 0.5f)
        assertIs<CutCornerShape>(result)
        assertEquals(CornerSize(15.dp), result.topStart)
    }

    @Test
    fun testAbsoluteRoundedCornerToAbsoluteRoundedCorner() {
        val a = AbsoluteRoundedCornerShape(10.dp)
        val b = AbsoluteRoundedCornerShape(20.dp)
        val result = Interpolatable.lerp(a, b, 0.5f)
        assertIs<AbsoluteRoundedCornerShape>(result)
        assertEquals(CornerSize(15.dp), result.topStart)
    }

    @Test
    fun testAbsoluteCutCornerToAbsoluteCutCorner() {
        val a = AbsoluteCutCornerShape(10.dp)
        val b = AbsoluteCutCornerShape(20.dp)
        val result = Interpolatable.lerp(a, b, 0.5f)
        assertIs<AbsoluteCutCornerShape>(result)
        assertEquals(CornerSize(15.dp), result.topStart)
    }

    @Test
    fun testRoundedToCut() {
        val a = RoundedCornerShape(10.dp)
        val b = CutCornerShape(20.dp)
        val result = Interpolatable.lerp(a, b, 0.4f)
        assertEquals(a, result)

        val result2 = Interpolatable.lerp(a, b, 0.6f)
        assertEquals(b, result2)
    }

    @Test
    fun testRectangleShapeToRounded() {
        val a = RectangleShape
        val b = RoundedCornerShape(10.dp)

        val result = Interpolatable.lerp(a, b, 0.5f)
        assertIs<RoundedCornerShape>(result)
        assertEquals(CornerSize(5.dp), result.topStart)

        val result2 = Interpolatable.lerp(b, a, 0.5f)
        assertIs<RoundedCornerShape>(result2)
        assertEquals(CornerSize(5.dp), result2.topStart)
    }

    @Test
    fun testRectangleShapeToCutCornerShape() {
        val a = RectangleShape
        val b = CutCornerShape(10.dp)

        val result = Interpolatable.lerp(a, b, 0.5f)
        assertIs<CutCornerShape>(result)
        assertEquals(CornerSize(5.dp), result.topStart)

        val result2 = Interpolatable.lerp(b, a, 0.5f)
        assertIs<CutCornerShape>(result2)
        assertEquals(CornerSize(5.dp), result2.topStart)
    }

    @Test
    fun testRectangleShapeToAbsoluteCutCornerShape() {
        val a = RectangleShape
        val b = AbsoluteCutCornerShape(10.dp)

        val result = Interpolatable.lerp(a, b, 0.5f)
        assertIs<AbsoluteCutCornerShape>(result)
        assertEquals(CornerSize(5.dp), result.topStart)

        val result2 = Interpolatable.lerp(b, a, 0.5f)
        assertIs<AbsoluteCutCornerShape>(result2)
        assertEquals(CornerSize(5.dp), result2.topStart)
    }

    @Test
    fun testRectangleShapeToAbsoluteRoundedCornerShape() {
        val a = RectangleShape
        val b = AbsoluteRoundedCornerShape(10.dp)

        val result = Interpolatable.lerp(a, b, 0.5f)
        assertIs<AbsoluteRoundedCornerShape>(result)
        assertEquals(CornerSize(5.dp), result.topStart)

        val result2 = Interpolatable.lerp(b, a, 0.5f)
        assertIs<AbsoluteRoundedCornerShape>(result2)
        assertEquals(CornerSize(5.dp), result2.topStart)
    }

    @Test
    fun testCircleShapeToRounded() {
        val a = CircleShape
        val b = RoundedCornerShape(10.dp)

        val result = Interpolatable.lerp(a, b, 0.5f)
        assertIs<RoundedCornerShape>(result)
        assertEquals(CornerSize(lerp(50.dp.value, 10.dp.value, 0.5f).dp), result.topStart)

        val result2 = Interpolatable.lerp(b, a, 0.5f)
        assertIs<RoundedCornerShape>(result2)
        assertEquals(CornerSize(lerp(10.dp.value, 50.dp.value, 0.5f).dp), result2.topStart)
    }

    @Test
    fun testRoundedCornerToRoundedCornerWithDifferentCornerSizes() {
        val a =
            RoundedCornerShape(
                topStart = 10.dp,
                topEnd = 20.dp,
                bottomEnd = 30.dp,
                bottomStart = 40.dp,
            )
        val b =
            RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 30.dp,
                bottomEnd = 40.dp,
                bottomStart = 50.dp,
            )
        val result = Interpolatable.lerp(a, b, 0.5f)
        assertIs<RoundedCornerShape>(result)
        assertEquals(CornerSize(15.dp), result.topStart)
        assertEquals(CornerSize(25.dp), result.topEnd)
        assertEquals(CornerSize(35.dp), result.bottomEnd)
        assertEquals(CornerSize(45.dp), result.bottomStart)
    }

    @Test
    fun testCutCornerToCutCornerWithDifferentCornerSizes() {
        val a =
            CutCornerShape(topStart = 10.dp, topEnd = 20.dp, bottomEnd = 30.dp, bottomStart = 40.dp)
        val b =
            CutCornerShape(topStart = 20.dp, topEnd = 30.dp, bottomEnd = 40.dp, bottomStart = 50.dp)
        val result = Interpolatable.lerp(a, b, 0.5f)
        assertIs<CutCornerShape>(result)
        assertEquals(CornerSize(15.dp), result.topStart)
        assertEquals(CornerSize(25.dp), result.topEnd)
        assertEquals(CornerSize(35.dp), result.bottomEnd)
        assertEquals(CornerSize(45.dp), result.bottomStart)
    }

    @Test
    fun testAbsoluteRoundedCornerToAbsoluteRoundedCornerWithDifferentCornerSizes() {
        val a =
            AbsoluteRoundedCornerShape(
                topLeft = 10.dp,
                topRight = 20.dp,
                bottomRight = 30.dp,
                bottomLeft = 40.dp,
            )
        val b =
            AbsoluteRoundedCornerShape(
                topLeft = 20.dp,
                topRight = 30.dp,
                bottomRight = 40.dp,
                bottomLeft = 50.dp,
            )
        val result = Interpolatable.lerp(a, b, 0.5f)
        assertIs<AbsoluteRoundedCornerShape>(result)
        assertEquals(CornerSize(15.dp), result.topStart)
        assertEquals(CornerSize(25.dp), result.topEnd)
        assertEquals(CornerSize(35.dp), result.bottomEnd)
        assertEquals(CornerSize(45.dp), result.bottomStart)
    }

    @Test
    fun testAbsoluteCutCornerToAbsoluteCutCornerWithDifferentCornerSizes() {
        val a =
            AbsoluteCutCornerShape(
                topLeft = 10.dp,
                topRight = 20.dp,
                bottomRight = 30.dp,
                bottomLeft = 40.dp,
            )
        val b =
            AbsoluteCutCornerShape(
                topLeft = 20.dp,
                topRight = 30.dp,
                bottomRight = 40.dp,
                bottomLeft = 50.dp,
            )
        val result = Interpolatable.lerp(a, b, 0.5f)
        assertIs<AbsoluteCutCornerShape>(result)
        assertEquals(CornerSize(15.dp), result.topStart)
        assertEquals(CornerSize(25.dp), result.topEnd)
        assertEquals(CornerSize(35.dp), result.bottomEnd)
        assertEquals(CornerSize(45.dp), result.bottomStart)
    }
}
