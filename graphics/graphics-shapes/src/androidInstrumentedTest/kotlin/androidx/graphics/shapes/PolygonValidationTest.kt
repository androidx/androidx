/*
 * Copyright 2024 The Android Open Source Project
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@SmallTest
class PolygonValidationTest {
    private val pentagonPoints =
        floatArrayOf(
            0.2f,
            0.0f,
            0.8f,
            0.0f,
            1.0f,
            0.6f,
            0.5f,
            1.0f,
            0.0f,
            0.6f,
        )

    private val reverseOrientedPentagonPoints =
        floatArrayOf(
            0.2f,
            0.0f,
            0.0f,
            0.6f,
            0.5f,
            1.0f,
            1.0f,
            0.6f,
            0.8f,
            0.0f,
        )

    @Test fun doesNotFixValidSharpPolygon() = staysUnchanged(RoundedPolygon(5))

    @Test
    fun doesNotFixValidRoundPolygon() =
        staysUnchanged(RoundedPolygon(5, rounding = CornerRounding(0.5f)))

    @Test
    fun fixesAntiClockwiseOrientedPolygon() {
        val valid = RoundedPolygon(pentagonPoints)

        val broken = RoundedPolygon(reverseOrientedPentagonPoints)

        fixes(broken, valid)
    }

    @Test
    fun fixesAntiClockwiseOrientedRoundedPolygon() {
        val valid = RoundedPolygon(pentagonPoints, rounding = CornerRounding(0.5f))

        val broken = RoundedPolygon(reverseOrientedPentagonPoints, rounding = CornerRounding(0.5f))

        fixes(broken, valid)
    }

    private fun staysUnchanged(polygon: RoundedPolygon) {
        val copy = RoundedPolygon(polygon)
        val fixedPolygon = PolygonValidator.fix(polygon)

        assertTrue(polygon === fixedPolygon)
        assertEquals(copy, polygon)
    }

    private fun fixes(broken: RoundedPolygon, expected: RoundedPolygon) {
        val fixed = PolygonValidator.fix(broken)

        assertFalse(broken == fixed)
        assertPolygonsEqualish(expected, fixed)
    }
}
