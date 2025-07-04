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

package androidx.wear.compose.foundation.lazy

import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class TransformingLazyColumnStateTest {
    @get:Rule val rule = createComposeRule()

    private val lazyListTag = "LazyList"

    private val itemsCount = 20
    private val itemSizePx = 100
    private var itemSizeDp = Dp.Unspecified

    private lateinit var scope: CoroutineScope

    @Before
    fun setup() {
        with(rule.density) { itemSizeDp = itemSizePx.toDp() }
    }

    @Test
    fun testInitialState() {
        lateinit var state: TransformingLazyColumnState
        rule.setContent { state = rememberTransformingLazyColumnState() }
        assertThat(state.anchorItemIndex).isEqualTo(0)
        assertThat(state.anchorItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun testInitialScrollPosition() {
        lateinit var state: TransformingLazyColumnState
        rule.setContent {
            state =
                rememberTransformingLazyColumnState(
                    initialAnchorItemIndex = 10,
                    initialAnchorItemScrollOffset = 20,
                )
        }
        assertThat(state.anchorItemIndex).isEqualTo(10)
        assertThat(state.anchorItemScrollOffset).isEqualTo(20)
    }

    @Test
    fun testAwaitFirstLayoutScrollPosition() {
        lateinit var state: TransformingLazyColumnState
        val shouldLayout = mutableStateOf(false)

        rule.setContent {
            state = rememberTransformingLazyColumnState()
            LaunchedEffect(Unit) { state.animateScrollToItem(10) }

            if (shouldLayout.value) {
                TransformingLazyColumn(state = state) {
                    items(itemsCount) { Spacer(modifier = Modifier.height(itemSizeDp)) }
                }
            }
        }

        rule.waitForIdle()

        // Scroll doesn't happen until the first layout.
        assertThat(state.anchorItemIndex).isEqualTo(0)

        // Force the positioning of the TLC.
        shouldLayout.value = true
        rule.waitForIdle()

        // Scroll happens.
        assertThat(state.anchorItemIndex).isEqualTo(10)
    }

    @Test
    fun testCheckLastScrollDirection() {
        lateinit var state: TransformingLazyColumnState
        rule.setContent {
            state =
                rememberTransformingLazyColumnState(
                    initialAnchorItemIndex = 10,
                    initialAnchorItemScrollOffset = 20,
                )
            scope = rememberCoroutineScope()

            TransformingLazyColumn(Modifier.height(itemSizeDp * 3f).testTag(lazyListTag), state) {
                items(itemsCount) { Spacer(modifier = Modifier.height(itemSizeDp)) }
            }
        }

        // Assert both isLastScrollForward and isLastScrollBackward are false before any scroll
        assertThat(state.lastScrolledBackward).isEqualTo(false)
        assertThat(state.lastScrolledBackward).isEqualTo(false)

        rule.runOnIdle { scope.launch { state.animateScrollBy(100f, tween(1000)) } }
        // Assert isLastScrollForward is true during forward-scroll and isLastScrollBackward is
        // false
        rule.runOnIdle {
            assertThat(state.lastScrolledForward).isTrue()
            assertThat(state.lastScrolledBackward).isFalse()
        }

        rule.mainClock.advanceTimeBy(500)

        // Assert isLastScrollForward is true after forward-scroll and isLastScrollBackward is false
        rule.runOnIdle {
            assertThat(state.lastScrolledForward).isTrue()
            assertThat(state.lastScrolledBackward).isFalse()
        }

        rule.runOnIdle { scope.launch { state.animateScrollBy(-100f, tween(1000)) } }

        rule.mainClock.advanceTimeBy(500)

        // Assert isLastScrollForward is false during backward-scroll and isLastScrollBackward is
        // true
        rule.runOnIdle {
            assertThat(state.lastScrolledForward).isFalse()
            assertThat(state.lastScrolledBackward).isTrue()
        }

        // Stop halfway through the animation
        state.animator.releaseAnimations()

        // Assert isLastScrollForward is false after backward-scroll and isLastScrollBackward is
        // true
        rule.runOnIdle {
            assertThat(state.lastScrolledForward).isFalse()
            assertThat(state.lastScrolledBackward).isTrue()
        }
    }
}
