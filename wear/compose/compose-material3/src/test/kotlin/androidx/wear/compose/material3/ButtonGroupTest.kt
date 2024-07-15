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

package androidx.wear.compose.material3

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ButtonGroupTest {

    @Test
    fun NoButtonsCase() {
        checkResult(listOf(), computeWidths(listOf(), 0, 200))
    }

    @Test
    fun OneButtonCase() {
        checkResult(listOf(200f), computeWidths(listOf(40f to 1f), 0, 200))
    }

    @Test
    fun SimpleCase() {
        checkResult(
            listOf(100f, 50f, 50f),
            computeWidths(listOf(40f to 2f, 40f to 1f, 40f to 1f), 0, 200)
        )
    }

    @Test
    fun UsesSpacing() {
        checkResult(
            listOf(80f, 40f, 40f),
            computeWidths(listOf(40f to 2f, 40f to 1f, 40f to 1f), 20, 200)
        )
    }

    @Test
    fun MinWidthRequirementDonatedEqually() {
        checkResult(
            // The weights alone will give 64 64 32, but that will put the last one under the
            // minimum
            listOf(60f, 60f, 40f),
            computeWidths(listOf(40f to 2f, 40f to 2f, 40f to 1f), 20, 200)
        )
    }

    @Test
    fun MinWidthRequirementDonatedProportionally() {
        checkResult(
            // The weights alone will give 90 60 30, but that will put the last one under the
            // minimum, they give according to their weight
            // (they need to give 10, so they give 6 & 4)
            listOf(84f, 56f, 40f),
            computeWidths(listOf(40f to 3f, 40f to 2f, 40f to 1f), 0, 180)
        )
    }

    @Test
    fun CascadingBorrow() {
        checkResult(
            // The weights alone will give: 36.956 40.652 44.347 48.043
            // Setting the first to 40 and redistributing gives: 40 39.722 43.333 46.444
            // Doing that again gives the final result.
            listOf(40f, 40f, 43.2f, 46.8f),
            computeWidths(
                listOf(
                    40f to 1f,
                    40f to 1.1f,
                    40f to 1.2f,
                    40f to 1.3f,
                ),
                10,
                200
            )
        ) // Actually available  = 200 - 3 * 10 = 170
    }

    @Test
    fun ZeroWeight() {
        checkResult(
            // Only items with weight > 0 grow
            listOf(80f, 80f, 40f),
            computeWidths(listOf(40f to 1f, 40f to 1f, 40f to 0f), 0, 200)
        )
    }

    @Test
    fun ZeroWeightPlusVariedWeights() {
        checkResult(
            // Only items with weight > 0 grow, first item should be twice as big as second one.
            listOf(100f, 50f, 50f),
            computeWidths(listOf(40f to 2f, 40f to 1f, 50f to 0f), 0, 200)
        )
    }

    private fun checkResult(expected: List<Float>, actual: FloatArray) {
        assertEquals(expected.size, actual.size)
        for (i in expected.indices) {
            assertEquals("Index $i", expected[i], actual[i], 1e-4f)
        }
    }
}
