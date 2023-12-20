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

package androidx.compose.material3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3Api::class)
class SwipeToDismissTest {

    @get:Rule
    val rule = createComposeRule()

    private val backgroundTag = "background"
    private val dismissContentTag = "dismissContent"
    private val swipeDismissTag = "swipeDismiss"

    private fun advanceClock() {
        rule.mainClock.advanceTimeBy(100_000L)
    }

    @Test
    fun swipeDismiss_testOffset_whenDefault() {
        rule.setContent {
            SwipeToDismissBox(
                state = rememberDismissState(DismissValue.Default),
                backgroundContent = { }
            ) { Box(Modifier.fillMaxSize().testTag(dismissContentTag)) }
        }

        rule.onNodeWithTag(dismissContentTag)
            .assertLeftPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun swipeDismiss_testOffset_whenDismissedToEnd() {
        rule.setContent {
            SwipeToDismissBox(
                state = rememberDismissState(DismissValue.DismissedToEnd),
                backgroundContent = { }
            ) { Box(Modifier.fillMaxSize().testTag(dismissContentTag)) }
        }

        val width = rule.rootWidth()
        rule.onNodeWithTag(dismissContentTag)
            .assertLeftPositionInRootIsEqualTo(width)
    }

    @Test
    fun swipeDismiss_testOffset_whenDismissedToStart() {
        rule.setContent {
            SwipeToDismissBox(
                state = rememberDismissState(DismissValue.DismissedToStart),
                backgroundContent = { }
            ) { Box(Modifier.fillMaxSize().testTag(dismissContentTag)) }
        }

        val width = rule.rootWidth()
        rule.onNodeWithTag(dismissContentTag)
            .assertLeftPositionInRootIsEqualTo(-width)
    }

    @Test
    fun swipeDismiss_testBackgroundMatchesContentSize() {
        rule.setContent {
            SwipeToDismissBox(
                state = rememberDismissState(DismissValue.Default),
                backgroundContent = { Box(Modifier.fillMaxSize().testTag(backgroundTag)) }
            ) { Box(Modifier.size(100.dp)) }
        }

        rule.onNodeWithTag(backgroundTag)
            .assertIsSquareWithSize(100.dp)
    }

    @Test
    fun swipeDismiss_dismissBySwipe_toEnd() {
        lateinit var dismissState: DismissState
        rule.setContent {
            dismissState = rememberDismissState(DismissValue.Default)
            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = { },
                modifier = Modifier.testTag(swipeDismissTag),
                directions = setOf(DismissDirection.StartToEnd)
            ) { Box(Modifier.fillMaxSize()) }
        }

        rule.onNodeWithTag(swipeDismissTag).performTouchInput { swipeRight() }

        advanceClock()

        rule.runOnIdle {
            assertThat(dismissState.currentValue).isEqualTo(DismissValue.DismissedToEnd)
        }
    }

    @Test
    fun swipeDismiss_dismissBySwipe_toStart() {
        lateinit var dismissState: DismissState
        rule.setContent {
            dismissState = rememberDismissState(DismissValue.Default)
            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = { },
                modifier = Modifier.testTag(swipeDismissTag),
                directions = setOf(DismissDirection.EndToStart)
            ) { Box(Modifier.fillMaxSize()) }
        }

        rule.onNodeWithTag(swipeDismissTag).performTouchInput { swipeLeft() }

        advanceClock()

        rule.runOnIdle {
            assertThat(dismissState.currentValue).isEqualTo(DismissValue.DismissedToStart)
        }
    }

    @Test
    fun swipeDismiss_dismissBySwipe_toEnd_rtl() {
        lateinit var dismissState: DismissState
        rule.setContent {
            dismissState = rememberDismissState(DismissValue.Default)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = { },
                    modifier = Modifier.testTag(swipeDismissTag),
                    directions = setOf(DismissDirection.StartToEnd)
                ) { Box(Modifier.fillMaxSize()) }
            }
        }

        rule.onNodeWithTag(swipeDismissTag).performTouchInput { swipeLeft() }

        advanceClock()

        rule.runOnIdle {
            assertThat(dismissState.currentValue).isEqualTo(DismissValue.DismissedToEnd)
        }
    }

    @Test
    fun swipeDismiss_dismissBySwipe_toStart_rtl() {
        lateinit var dismissState: DismissState
        rule.setContent {
            dismissState = rememberDismissState(DismissValue.Default)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = { },
                    modifier = Modifier.testTag(swipeDismissTag),
                    directions = setOf(DismissDirection.EndToStart)
                ) { Box(Modifier.fillMaxSize()) }
            }
        }

        rule.onNodeWithTag(swipeDismissTag).performTouchInput { swipeRight() }

        advanceClock()

        rule.runOnIdle {
            assertThat(dismissState.currentValue).isEqualTo(DismissValue.DismissedToStart)
        }
    }

    @Test
    fun swipeDismiss_dismissBySwipe_disabled() {
        lateinit var dismissState: DismissState
        rule.setContent {
            dismissState = rememberDismissState(DismissValue.Default)
            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = { },
                modifier = Modifier.testTag(swipeDismissTag),
                directions = setOf()
            ) { Box(Modifier.fillMaxSize()) }
        }

        rule.onNodeWithTag(swipeDismissTag).performTouchInput { swipeRight() }

        advanceClock()

        rule.runOnIdle {
            assertThat(dismissState.currentValue).isEqualTo(DismissValue.Default)
        }

        rule.onNodeWithTag(swipeDismissTag).performTouchInput { swipeLeft() }

        advanceClock()

        rule.runOnIdle {
            assertThat(dismissState.currentValue).isEqualTo(DismissValue.Default)
        }
    }
}
