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
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.xr.glimmer.GOLDEN_DIRECTORY
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.assertRootAgainstGolden
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
internal class ListEdgeScrimScreenshotTest(orientation: Orientation) :
    BaseListTestWithOrientation(orientation) {

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_DIRECTORY)

    private val orientationPrefix = orientation.name.lowercase()

    @Test
    fun scrim_atEndEdge() {
        val state = ListState()
        rule.setContent { TestList(state) }
        rule.waitForIdle()

        rule.assertRootAgainstGolden("${orientationPrefix}_scrim_atEndEdge", screenshotRule)
    }

    @Test
    fun scrim_atBothEdges() {
        val state = ListState()
        rule.setContent { TestList(state) }
        rule.waitForIdle()

        // There are 10 items, each with a height of 100 dp. Scroll to the middle of the list.
        state.scrollByAndWaitForIdle(500.dp)

        rule.assertRootAgainstGolden("${orientationPrefix}_scrim_atBothEdges", screenshotRule)
    }

    @Test
    fun scrim_atStartEdge() {
        val state = ListState()
        rule.setContent { TestList(state) }
        rule.waitForIdle()

        // There are 10 items, each with a height of 100 dp. Scroll to the very bottom.
        state.scrollByAndWaitForIdle(1_000.dp)

        rule.assertRootAgainstGolden("${orientationPrefix}_scrim_atStartEdge", screenshotRule)
    }

    @Test
    fun scrim_halfExpanded_atStartEdge() {
        val state = ListState()
        rule.setContent { TestList(state) }
        rule.waitForIdle()

        // The default scrim size is 46dp. Scroll the list until the scrim expands to half its size.
        val totalScroll = with(rule.density) { 23.dp.toPx() }
        runBlocking(Dispatchers.Main) { state.scrollContentBy(totalScroll) }
        rule.waitForIdle()

        rule.assertRootAgainstGolden(
            "${orientationPrefix}_scrim_halfExpanded_atStartEdge",
            screenshotRule,
        )
    }

    @Composable
    private fun TestList(state: ListState) {
        GlimmerTheme {
            Box(Modifier.background(Color.Yellow)) {
                TestList(state = state, itemsCount = 10, modifier = Modifier.size(300.dp)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier.size(100.dp).padding(1.dp).background(Color.Blue).focusable(),
                    ) {
                        Text("$it")
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    }
}
