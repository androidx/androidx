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

package androidx.compose.material3.pulltorefresh

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.PositionalThreshold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsProperties.Text
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class PullToRefreshBoxTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun box_startRefreshing_updatesFraction() {
        val state = PullToRefreshState()
        rule.setContent { PullToRefreshBox(isRefreshing = true, state = state, onRefresh = {}) {} }

        assertThat(state.distanceFraction).isEqualTo(1f)
    }

    @Test
    fun box_startNotRefreshing_updatesFraction() {
        val state = PullToRefreshState()
        rule.setContent { PullToRefreshBox(isRefreshing = false, state = state, onRefresh = {}) {} }

        assertThat(state.distanceFraction).isEqualTo(0f)
    }

    @Test
    fun boxNotVisible_notVisibleToSemantics() {
        val state = PullToRefreshState()
        rule.setContent {
            PullToRefreshBox(
                modifier = Modifier.testTag("PullToRefresh"),
                isRefreshing = false,
                state = state,
                onRefresh = {},
            ) {}
        }

        assertThat(state.distanceFraction).isEqualTo(0f)
        rule
            .onNodeWithTag("PullToRefresh")
            .onChild()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ProgressBarRangeInfo))
    }

    @Test
    fun boxVisible_VisibleToSemantics() {
        val state = PullToRefreshState()
        rule.setContent { PullToRefreshBox(isRefreshing = true, state = state, onRefresh = {}) {} }

        assertThat(state.distanceFraction).isEqualTo(1f)
        rule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assertExists()
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun loadingIndicator_atRest_isNotComposed() {
        val state = PullToRefreshState()
        rule.setContent {
            PullToRefreshBox(
                isRefreshing = false,
                state = state,
                onRefresh = {},
                indicator = {
                    PullToRefreshDefaults.LoadingIndicator(state = state, isRefreshing = false)
                },
            ) {}
        }

        assertThat(state.distanceFraction).isEqualTo(0f)
        rule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assertDoesNotExist()
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun loadingIndicator_refreshing_isComposed() {
        val state = PullToRefreshState()
        rule.setContent {
            PullToRefreshBox(
                isRefreshing = true,
                state = state,
                onRefresh = {},
                indicator = {
                    PullToRefreshDefaults.LoadingIndicator(state = state, isRefreshing = true)
                },
            ) {}
        }

        assertThat(state.distanceFraction).isEqualTo(1f)
        rule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assertExists()
    }

    @Test
    fun startRefreshing_pull_isNoop() {
        val state = PullToRefreshState()
        rule.setContent {
            PullToRefreshBox(isRefreshing = true, state = state, onRefresh = {}) {
                LazyColumn(Modifier.testTag("lazy")) { items(50) { Text("Item") } }
            }
        }

        rule.onNodeWithTag("lazy").performTouchInput { swipeDownToThreshold() }
        rule.runOnIdle { assertThat(state.distanceFraction).isEqualTo(1f) }
    }

    @Test
    fun startIdle_pull_triggersRefresh() {
        val state = PullToRefreshState()
        val isRefreshing = mutableStateOf(false)

        rule.setContent {
            PullToRefreshBox(
                isRefreshing = isRefreshing.value,
                state = state,
                onRefresh = { isRefreshing.value = true },
            ) {
                LazyColumn(Modifier.testTag("lazy")) { items(50) { Text("Item") } }
            }
        }

        rule.runOnIdle { assertThat(state.distanceFraction).isEqualTo(0f) }

        rule.onNodeWithTag("lazy").performTouchInput { swipeDownToThreshold() }

        rule.runOnIdle { assertThat(state.distanceFraction).isEqualTo(1f) }
    }

    @Test
    fun startIdle_pull_isRefreshingStaysFalse_resetsFraction() {
        val state = PullToRefreshState()
        var refreshCount = 0

        rule.setContent {
            PullToRefreshBox(
                isRefreshing = false,
                state = state,
                onRefresh = { refreshCount++ },
            ) {
                LazyColumn(Modifier.testTag("lazy")) { items(50) { Text("Item") } }
            }
        }

        rule.runOnIdle { assertThat(state.distanceFraction).isEqualTo(0f) }

        rule.onNodeWithTag("lazy").performTouchInput { swipeDownToThreshold() }

        rule.runOnIdle {
            assertThat(refreshCount).isEqualTo(1)
            assertThat(state.distanceFraction).isEqualTo(0f)
        }
    }

    @Test
    fun startIdle_pull_customState_refreshStarts_staysAtThreshold() {
        val state = NonInterruptingPullToRefreshState()
        val isRefreshing = mutableStateOf(false)

        rule.setContent {
            PullToRefreshBox(
                isRefreshing = isRefreshing.value,
                state = state,
                onRefresh = { isRefreshing.value = true },
            ) {
                LazyColumn(Modifier.testTag("lazy")) { items(50) { Text("Item") } }
            }
        }

        rule.onNodeWithTag("lazy").performTouchInput { swipeDownToThreshold() }

        rule.runOnIdle {
            assertThat(isRefreshing.value).isTrue()
            assertThat(state.distanceFraction).isEqualTo(1f)
        }
    }

    @Test
    fun box_fling_isConsumed() {
        val state = PullToRefreshState()
        val isRefreshing = mutableStateOf(false)
        var remainingVelocity = Float.NaN

        rule.setContent {
            PullToRefreshBox(
                isRefreshing = isRefreshing.value,
                state = state,
                onRefresh = { isRefreshing.value = true },
            ) {
                Column(
                    Modifier.fillMaxWidth()
                        .testTag("lazy")
                        .verticalScroll(
                            state = rememberScrollState(),
                            flingBehavior =
                                object : FlingBehavior {
                                    override suspend fun ScrollScope.performFling(
                                        initialVelocity: Float
                                    ): Float {
                                        remainingVelocity = initialVelocity
                                        return initialVelocity
                                    }
                                },
                        )
                ) {
                    repeat(50) { Text("Lorem ipsum") }
                }
            }
        }

        rule.onNodeWithTag("lazy").performTouchInput {
            // do a swipe that doesn't trigger a refresh
            swipeDown(startY = 0f, endY = PositionalThreshold.toPx() / 2, 100L)
        }

        rule.runOnIdle {
            assertThat(state.distanceFraction).isEqualTo(0f)
            assertThat(abs(remainingVelocity)).isEqualTo(0f)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun TouchInjectionScope.swipeDownToThreshold() {
    val touchSlop = 18.dp
    swipeDown(startY = 0f, endY = PositionalThreshold.toPx() * 2 + touchSlop.toPx())
}

/**
 * A [PullToRefreshState] whose animations run independently: [animateToThreshold] does not
 * interrupt an in-flight [animateToHidden], unlike the default `Animatable`-backed implementation.
 * The hide animation outlasts the threshold one so that running both concurrently ends hidden.
 */
private class NonInterruptingPullToRefreshState : PullToRefreshState {
    private var fraction by mutableFloatStateOf(0f)
    private var animating by mutableStateOf(false)

    override val distanceFraction: Float
        get() = fraction

    override val isAnimating: Boolean
        get() = animating

    override suspend fun animateToThreshold() = animateTo(1f, durationMillis = 100)

    override suspend fun animateToHidden() = animateTo(0f, durationMillis = 300)

    override suspend fun snapTo(targetValue: Float) {
        fraction = targetValue
    }

    private suspend fun animateTo(target: Float, durationMillis: Int) {
        animating = true
        try {
            animate(fraction, target, animationSpec = tween(durationMillis)) { value, _ ->
                fraction = value
            }
        } finally {
            animating = false
        }
    }
}
