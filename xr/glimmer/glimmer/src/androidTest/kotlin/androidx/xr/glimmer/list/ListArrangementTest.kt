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

package androidx.xr.glimmer.list

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.xr.glimmer.setGlimmerThemeContent
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class ListArrangementTest(orientation: Orientation) : BaseListTestWithOrientation(orientation) {

    @Test
    fun arrangement_spaceEvenly() {
        rule.setGlimmerThemeContent {
            TestList(
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.size(100.dp).background(Color.Black),
                itemsCount = 2,
                itemContent = { index ->
                    Box(Modifier.size(20.dp).background(Color.Red).testTag(itemTag(index)))
                },
            )
        }

        val expectedPositions = listOf(20.dp to 40.dp, 60.dp to 80.dp)

        checkArrangement(expectedPositions)
    }

    @Test
    fun arrangement_spaceBetween() {
        rule.setGlimmerThemeContent {
            TestList(
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.size(100.dp).background(Color.Black),
                itemsCount = 2,
                itemContent = { index ->
                    Box(Modifier.size(20.dp).background(Color.Red).testTag(itemTag(index)))
                },
            )
        }

        val expectedPositions = listOf(0.dp to 20.dp, 80.dp to 100.dp)

        checkArrangement(expectedPositions)
    }

    @Test
    fun arrangement_spaceAround() {
        rule.setGlimmerThemeContent {
            TestList(
                verticalArrangement = Arrangement.SpaceAround,
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier.size(100.dp).background(Color.Black),
                itemsCount = 2,
                itemContent = { index ->
                    Box(Modifier.size(20.dp).background(Color.Red).testTag(itemTag(index)))
                },
            )
        }

        val expectedPositions = listOf(15.dp to 35.dp, 65.dp to 85.dp)

        checkArrangement(expectedPositions)
    }

    @Test
    fun arrangement_center() {
        rule.setGlimmerThemeContent {
            TestList(
                verticalArrangement = Arrangement.Center,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.size(100.dp).background(Color.Black),
                itemsCount = 2,
                itemContent = { index ->
                    Box(Modifier.size(20.dp).background(Color.Red).testTag(itemTag(index)))
                },
            )
        }

        val expectedPositions = listOf(30.dp to 50.dp, 50.dp to 70.dp)

        checkArrangement(expectedPositions)
    }

    @Test
    fun arrangement_top() {
        rule.setGlimmerThemeContent {
            TestList(
                verticalArrangement = Arrangement.Top,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.size(100.dp).background(Color.Black),
                itemsCount = 2,
                itemContent = { index ->
                    Box(Modifier.size(20.dp).background(Color.Red).testTag(itemTag(index)))
                },
            )
        }

        val expectedPositions = listOf(0.dp to 20.dp, 20.dp to 40.dp)

        checkArrangement(expectedPositions)
    }

    @Test
    fun arrangement_bottom() {
        rule.setGlimmerThemeContent {
            TestList(
                verticalArrangement = Arrangement.Bottom,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.size(100.dp).background(Color.Black),
                itemsCount = 2,
                itemContent = { index ->
                    Box(Modifier.size(20.dp).background(Color.Red).testTag(itemTag(index)))
                },
            )
        }

        val expectedPositions = listOf(60.dp to 80.dp, 80.dp to 100.dp)

        checkArrangement(expectedPositions)
    }

    @Test
    fun arrangement_spacedBy() {
        rule.setGlimmerThemeContent {
            TestList(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.size(100.dp).background(Color.Black),
                itemsCount = 3,
                itemContent = { index ->
                    Box(Modifier.size(20.dp).background(Color.Red).testTag(itemTag(index)))
                },
            )
        }

        val expectedPositions = listOf(0.dp to 20.dp, 22.dp to 42.dp, 44.dp to 64.dp)

        checkArrangement(expectedPositions)
    }

    private fun checkArrangement(expectedPositions: List<Pair<Dp, Dp>>) {
        for ((index, expectedRect) in expectedPositions.withIndex()) {
            val actualRect = rule.onNodeWithTag(itemTag(index)).getBoundsInRoot()
            if (orientation == Orientation.Vertical) {
                actualRect.top.assertIsEqualTo(expectedRect.first)
                actualRect.bottom.assertIsEqualTo(expectedRect.second)
            } else {
                actualRect.left.assertIsEqualTo(expectedRect.first)
                actualRect.right.assertIsEqualTo(expectedRect.second)
            }
        }
    }

    companion object {
        private fun itemTag(index: Int): String = "item-tag-$index"

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    }
}
