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

package androidx.graphics.shapes

import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Test

@SmallTest
class CornerRoundingTest {

    @Test
    fun cornerRoundingTest() {
        val defaultCorner = CornerRounding()
        assertEquals(0f, defaultCorner.radius)
        assertEquals(0f, defaultCorner.smoothing)

        val unrounded = CornerRounding.Unrounded
        assertEquals(0f, unrounded.radius)
        assertEquals(0f, unrounded.smoothing)

        val rounded = CornerRounding(radius = 5f)
        assertEquals(5f, rounded.radius)
        assertEquals(0f, rounded.smoothing)

        val smoothed = CornerRounding(smoothing = .5f)
        assertEquals(0f, smoothed.radius)
        assertEquals(.5f, smoothed.smoothing)

        val roundedAndSmoothed = CornerRounding(radius = 5f, smoothing = .5f)
        assertEquals(5f, roundedAndSmoothed.radius)
        assertEquals(.5f, roundedAndSmoothed.smoothing)
    }
}
