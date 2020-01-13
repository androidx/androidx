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

package androidx.ui.graphics

import androidx.test.filters.SmallTest
import androidx.ui.unit.IntPx
import androidx.ui.unit.PxSize
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class ScaleFitTest {

    @Test
    fun testScaleNone() {
        val scale = ScaleFit.None.scale(
            srcSize = PxSize(IntPx(100), IntPx(100)),
            dstSize = PxSize(IntPx(200), IntPx(200))
        )
        assertEquals(1.0f, scale)
    }

    @Test
    fun testScaleFit() {
        val scale = ScaleFit.Fit.scale(
            srcSize = PxSize(IntPx(200), IntPx(100)),
            dstSize = PxSize(IntPx(100), IntPx(200))
        )
        assertEquals(.5f, scale)
    }

    @Test
    fun testScaleFillWidth() {
        val scale = ScaleFit.FillWidth.scale(
            srcSize = PxSize(IntPx(400), IntPx(100)),
            dstSize = PxSize(IntPx(100), IntPx(200))
        )
        assertEquals(0.25f, scale)
    }

    @Test
    fun testScaleFillHeight() {
        val scale = ScaleFit.FillHeight.scale(
            srcSize = PxSize(IntPx(400), IntPx(100)),
            dstSize = PxSize(IntPx(100), IntPx(200))
        )
        assertEquals(2.0f, scale)
    }

    @Test
    fun testScaleFillMaxDimension() {
        val scale = ScaleFit.FillMaxDimension.scale(
            srcSize = PxSize(IntPx(400), IntPx(100)),
            dstSize = PxSize(IntPx(100), IntPx(200))
        )
        assertEquals(2.0f, scale)
    }

    @Test
    fun testScaleFillMinDimension() {
        val scale = ScaleFit.FillMinDimension.scale(
            srcSize = PxSize(IntPx(400), IntPx(100)),
            dstSize = PxSize(IntPx(100), IntPx(200))
        )
        assertEquals(0.25f, scale)
    }
}