/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.animation

import androidx.animation.AnimationVector1D
import androidx.animation.AnimationVector2D
import androidx.animation.AnimationVector4D
import androidx.ui.geometry.Rect
import androidx.ui.graphics.Color
import androidx.ui.graphics.colorspace.ColorSpaces
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.random.Random

@RunWith(JUnit4::class)
class ConverterTest {
    @Test
    fun testColorConverter() {
        val converter = ColorToVectorConverter(ColorSpaces.Srgb)
        assertEquals(converter.convertFromVector(AnimationVector4D(1f, 1f, 0f, 0f)), Color.Red)
        assertEquals(converter.convertToVector(Color.Green), AnimationVector4D(1f, 0f, 1f, 0f))
        assertEquals(converter.convertFromVector(AnimationVector4D(0f, 0f, 0f, 1f)),
            Color(alpha = 0f, red = 0f, green = 0f, blue = 1f))
    }

    @Test
    fun testRectConverter() {
        assertEquals(RectToVectorConverter.convertToVector(Rect(1f, 2f, 3f, 4f)),
            AnimationVector4D(1f, 2f, 3f, 4f))
        assertEquals(RectToVectorConverter.convertFromVector(
            AnimationVector4D(-400f, -300f, -200f, -100f)),
            Rect(-400f, -300f, -200f, -100f))
    }

    @Test
    fun testPxConverter() {
        val value = Random.nextFloat()
        assertEquals(PxToVectorConverter.convertFromVector(AnimationVector1D(value)), value)

        val value2 = Random.nextFloat()
        assertEquals(PxToVectorConverter.convertToVector(value2), AnimationVector1D(value2))
    }

    @Test
    fun testDpConverter() {
        val value = Random.nextFloat()
        assertEquals(DpToVectorConverter.convertFromVector(AnimationVector1D(value)), value.dp)

        val value2 = Random.nextFloat()
        assertEquals(DpToVectorConverter.convertToVector(value2.dp), AnimationVector1D(value2))
    }

    @Test
    fun testPxPositionConverter() {
        val x = Random.nextFloat()
        val y = Random.nextFloat()
        assertEquals(PxPosition(x, y),
            PxPositionToVectorConverter.convertFromVector(AnimationVector2D(x, y)))
        assertEquals(AnimationVector2D(x, y),
            PxPositionToVectorConverter.convertToVector(PxPosition(x, y)))
    }
}