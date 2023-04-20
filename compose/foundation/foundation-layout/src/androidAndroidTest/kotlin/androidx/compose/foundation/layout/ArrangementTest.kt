/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.layout

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@SmallTest
@RunWith(Parameterized::class)
class ArrangementTest(private val testParam: TestParam) {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testArrangement() = with(testParam) {
        with(actualArrangement) {
            composeTestRule.density.arrange(
                actualTotalSize,
                actualSizes,
                actualLayoutDirection,
                actualOutPositions
            )

            assertThat(actualOutPositions).isEqualTo(expectedOutPositions)
        }
    }

    @Suppress("ArrayInDataClass") // Used only in parameterized tests.
    data class TestParam(
        val actualArrangement: Arrangement.HorizontalOrVertical,
        val actualTotalSize: Int,
        val actualSizes: IntArray,
        val actualLayoutDirection: LayoutDirection,
        val actualOutPositions: IntArray,
        val expectedOutPositions: IntArray,
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf<Any>(
            TestParam(
                actualArrangement = Arrangement.SpaceBetween,
                actualTotalSize = 2376,
                actualSizes = intArrayOf(108, 24),
                actualLayoutDirection = LayoutDirection.Rtl,
                actualOutPositions = intArrayOf(0, 0),
                expectedOutPositions = intArrayOf(2268, 0),
            ),
            TestParam(
                actualArrangement = Arrangement.SpaceBetween,
                actualTotalSize = 2376,
                actualSizes = intArrayOf(108),
                actualLayoutDirection = LayoutDirection.Rtl,
                actualOutPositions = intArrayOf(0),
                expectedOutPositions = intArrayOf(2268),
            ),
            TestParam(
                actualArrangement = Arrangement.SpaceBetween,
                actualTotalSize = 2376,
                actualSizes = intArrayOf(108, 24),
                actualLayoutDirection = LayoutDirection.Ltr,
                actualOutPositions = intArrayOf(0, 0),
                expectedOutPositions = intArrayOf(0, 2352),
            ),
            TestParam(
                actualArrangement = Arrangement.SpaceBetween,
                actualTotalSize = 2376,
                actualSizes = intArrayOf(108),
                actualLayoutDirection = LayoutDirection.Ltr,
                actualOutPositions = intArrayOf(0),
                expectedOutPositions = intArrayOf(0),
            ),
        )
    }
}