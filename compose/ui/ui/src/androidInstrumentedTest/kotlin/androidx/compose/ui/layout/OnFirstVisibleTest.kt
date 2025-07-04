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
package androidx.compose.ui.layout

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions.ScrollBy
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class OnFirstVisibleTest {
    @get:Rule val rule = createAndroidComposeRule<TestActivity>()

    @Test
    fun testOneMinFraction() {
        var top by mutableStateOf(-1000)
        var called = 0
        rule.setContent {
            Column {
                Column {
                    Box(
                        Modifier.offset(y = top.dp)
                            .onFirstVisible(minFractionVisible = 1.0f) { called++ }
                            .size(100.dp)
                    )
                }
            }
        }
        rule.runOnIdle {
            // initially it is off-screen and not visible
            assertEquals(0, called)
            top = -20
        }
        rule.runOnIdle {
            // it still isn't completely in the viewport, so still shouldn't be called
            assertEquals(0, called)
            top = 0
        }
        rule.runOnIdle {
            // completely in the viewport now, so got called with true
            assertEquals(1, called)
            top = -20
        }
        rule.runOnIdle {
            // partially in the viewport, but minFraction = 1 means the whole thing needs to be.
            // so now called with false
            assertEquals(1, called)
            top = -1000
        }
        rule.runOnIdle {
            // outside the viewport completely now, but not called any additional amount since it
            // was already marked as out
            assertEquals(1, called)
        }
    }

    @Test
    fun testZeroMinFraction() {
        var top by mutableStateOf(-1000)
        var called = 0
        rule.setContent {
            Column {
                Column {
                    Box(
                        Modifier.offset(y = top.dp)
                            .onFirstVisible(minFractionVisible = 0f) { called++ }
                            .size(100.dp)
                    )
                }
            }
        }
        rule.runOnIdle {
            // initially outside the viewport
            assertEquals(0, called)
            top = -101
        }
        rule.runOnIdle {
            // it should be just bordering the viewport, but still 100% outside of it, so still no
            // call
            assertEquals(0, called)
            top = -99
        }
        rule.runOnIdle {
            // should be 1dp inside the viewport, so callback should trigger
            assertEquals(1, called)
            top = 10
        }
        rule.runOnIdle {
            // completely inside the viewport, but no extra calls made
            assertEquals(1, called)
            top = -99
        }
        rule.runOnIdle {
            // still 1dp inside the viewport
            assertEquals(1, called)
            top = -100
        }
        rule.runOnIdle {
            // completely outside the viewport
            assertEquals(1, called)
        }
    }

    @Test
    fun testHalfMinFraction() {
        var top by mutableStateOf(-1000)
        var called = 0
        rule.setContent {
            Column {
                Column {
                    Box(
                        Modifier.offset(y = top.dp)
                            .onFirstVisible(minFractionVisible = 0.5f) { called++ }
                            .size(100.dp)
                    )
                }
            }
        }
        rule.runOnIdle {
            // initially outside the viewport
            assertEquals(0, called)
            top = -99
        }
        rule.runOnIdle {
            // it should be just barely inside the viewport, but since minFraction is 0.5, it still
            // should not have triggered
            assertEquals(0, called)
            top = -40
        }
        rule.runOnIdle {
            // should be inside inside the viewport, so callback should trigger
            assertEquals(1, called)
            top = 10
        }
        rule.runOnIdle {
            // completely inside the viewport, but no extra calls made
            assertEquals(1, called)
            top = -40
        }
        rule.runOnIdle {
            // still 1dp inside the viewport
            assertEquals(1, called)
            top = -60
        }
        rule.runOnIdle {
            // more than half is outside the viewport, callback should be triggered
            assertEquals(1, called)
            top = -1000
        }
        rule.runOnIdle {
            // completely outside the viewport, no additional calls
            assertEquals(1, called)
        }
    }

    @Test
    fun testVisibleCalledOnFirstLoadZeroFraction() {
        var called = 0
        rule.setContent {
            Column {
                Column {
                    Box(Modifier.onFirstVisible(minFractionVisible = 0f) { called++ }.size(100.dp))
                }
            }
        }
        rule.runOnIdle { assertEquals(1, called) }
    }

    @Test
    fun testVisibleCalledOnFirstLoadOneFraction() {
        var called = 0
        rule.setContent {
            Column {
                Column {
                    Box(Modifier.onFirstVisible(minFractionVisible = 1f) { called++ }.size(100.dp))
                }
            }
        }
        rule.runOnIdle { assertEquals(1, called) }
    }

    @Test
    fun testOneFractionWithNodeMuchLargerThanViewport() {
        var called = 0
        rule.setContent {
            Column {
                Column {
                    Box(Modifier.onFirstVisible(minFractionVisible = 1f) { called++ }.size(5000.dp))
                }
            }
        }
        rule.runOnIdle { assertEquals(1, called) }
    }

    @Test
    fun testLazyColumn() {
        val lazyListState = LazyListState()
        val stats = IntArray(200)
        lateinit var view: View
        rule.setContent {
            view = LocalView.current
            LazyColumn(state = lazyListState, modifier = Modifier.testTag("lazy")) {
                items(200) { index ->
                    Box(
                        Modifier.onFirstVisible(minFractionVisible = 1f) { stats[index]++ }
                            .fillMaxWidth()
                            .height(100.dp)
                    )
                }
            }
        }
        rule.runOnIdle {
            val dpHeight = (view.height.toFloat() / rule.density.density).roundToInt()
            val rowsPerScreen = dpHeight / 100
            for (i in 0 until rowsPerScreen) {
                assertEquals(1, stats[i])
            }
        }
        val scrollBy =
            rule.onNodeWithTag("lazy").fetchSemanticsNode().config[ScrollBy].action
                ?: error("No scrollByAction found")
        val scrollDistance = with(rule.density) { 300.dp.toPx() } // 3 rows
        scrollBy(0f, scrollDistance)
        rule.runOnIdle {
            val dpHeight = (view.height.toFloat() / rule.density.density).roundToInt()
            val rowsPerScreen = dpHeight / 100
            val scrollRows = 3
            for (i in 0 until rowsPerScreen + scrollRows) {
                assertEquals(1, stats[i])
            }
            for (i in rowsPerScreen + scrollRows + 1 until 200) {
                assertEquals(0, stats[i])
            }
        }
    }
}
