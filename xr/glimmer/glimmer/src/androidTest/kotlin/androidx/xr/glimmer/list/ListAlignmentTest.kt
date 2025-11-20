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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.ui.Alignment
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
class ListAlignmentTest(orientation: Orientation) : BaseListTestWithOrientation(orientation) {

    @Test
    fun alignment_center() {
        rule.setGlimmerThemeContent {
            TestList(
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.size(100.dp).background(Color.Black),
                itemsCount = 3,
                itemContent = { index ->
                    Box(Modifier.size(20.dp).background(Color.Red).testTag(itemTag(index)))
                },
            )
        }

        val expectedPositions = List(3) { 40.dp to 60.dp }

        checkAlignment(expectedPositions)
    }

    @Test
    fun alignment_start() {
        rule.setGlimmerThemeContent {
            TestList(
                verticalAlignment = Alignment.Top,
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.size(100.dp).background(Color.Black),
                itemsCount = 3,
                itemContent = { index ->
                    Box(Modifier.size(20.dp).background(Color.Red).testTag(itemTag(index)))
                },
            )
        }

        val expectedPositions = List(3) { 0.dp to 20.dp }

        checkAlignment(expectedPositions)
    }

    @Test
    fun alignment_end() {
        rule.setGlimmerThemeContent {
            TestList(
                verticalAlignment = Alignment.Bottom,
                horizontalAlignment = Alignment.End,
                modifier = Modifier.size(100.dp).background(Color.Black),
                itemsCount = 3,
                itemContent = { index ->
                    Box(Modifier.size(20.dp).background(Color.Red).testTag(itemTag(index)))
                },
            )
        }

        val expectedPositions = List(3) { 80.dp to 100.dp }

        checkAlignment(expectedPositions)
    }

    private fun checkAlignment(expectedPositions: List<Pair<Dp, Dp>>) {
        for ((index, expectedRect) in expectedPositions.withIndex()) {
            val childRect = rule.onNodeWithTag(itemTag(index)).getBoundsInRoot()
            if (orientation == Orientation.Vertical) {
                childRect.left.assertIsEqualTo(expectedRect.first)
                childRect.right.assertIsEqualTo(expectedRect.second)
            } else {
                childRect.top.assertIsEqualTo(expectedRect.first)
                childRect.bottom.assertIsEqualTo(expectedRect.second)
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
