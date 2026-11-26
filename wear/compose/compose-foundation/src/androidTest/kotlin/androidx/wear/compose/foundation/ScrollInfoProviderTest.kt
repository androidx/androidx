/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.wear.compose.foundation

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ScrollInfoProviderTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun lazyColumn_overflowingContent_isScrollAwayValid_isTrue() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberLazyListState()
            provider = ScrollInfoProvider(state)
            LazyColumn(
                state = state,
                reverseLayout = false,
                modifier = Modifier.requiredSize(100.dp),
            ) {
                items(10) { Box(Modifier.requiredSize(30.dp)) }
            }
        }

        rule.runOnIdle { assertThat(provider.isScrollAwayValid).isTrue() }
    }

    @Test
    fun lazyColumn_overflowingContent_isScrollable_isTrue() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberLazyListState()
            provider = ScrollInfoProvider(state)
            LazyColumn(
                state = state,
                reverseLayout = false,
                modifier = Modifier.requiredSize(100.dp),
            ) {
                items(10) { Box(Modifier.requiredSize(30.dp)) }
            }
        }

        rule.runOnIdle { assertThat(provider.isScrollable).isTrue() }
    }

    @Test
    fun lazyColumn_overflowingContent_isScrollInProgress_isFalse() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberLazyListState()
            provider = ScrollInfoProvider(state)
            LazyColumn(
                state = state,
                reverseLayout = false,
                modifier = Modifier.requiredSize(100.dp),
            ) {
                items(10) { Box(Modifier.requiredSize(30.dp)) }
            }
        }

        rule.runOnIdle { assertThat(provider.isScrollInProgress).isFalse() }
    }

    @Test
    fun lazyColumn_overflowingContent_anchorItemOffset_isZero() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberLazyListState()
            provider = ScrollInfoProvider(state)
            LazyColumn(
                state = state,
                reverseLayout = false,
                modifier = Modifier.requiredSize(100.dp),
            ) {
                items(10) { Box(Modifier.requiredSize(30.dp)) }
            }
        }

        rule.runOnIdle { assertThat(provider.anchorItemOffset).isWithin(0.0001f).of(0f) }
    }

    @Test
    fun lazyColumn_overflowingContent_lastItemOffset_isZero() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberLazyListState()
            provider = ScrollInfoProvider(state)
            LazyColumn(
                state = state,
                reverseLayout = false,
                modifier = Modifier.requiredSize(100.dp),
            ) {
                items(10) { Box(Modifier.requiredSize(30.dp)) }
            }
        }

        rule.runOnIdle { assertThat(provider.lastItemOffset).isWithin(0.0001f).of(0f) }
    }

    @Test
    fun lazyColumn_empty_isScrollAwayValid_isFalse() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberLazyListState()
            provider = ScrollInfoProvider(state)
            LazyColumn(state = state, modifier = Modifier.requiredSize(100.dp)) {}
        }

        rule.runOnIdle { assertThat(provider.isScrollAwayValid).isFalse() }
    }

    @Test
    fun lazyColumn_empty_isScrollable_isFalse() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberLazyListState()
            provider = ScrollInfoProvider(state)
            LazyColumn(state = state, modifier = Modifier.requiredSize(100.dp)) {}
        }

        rule.runOnIdle { assertThat(provider.isScrollable).isFalse() }
    }

    @Test
    fun lazyColumn_empty_anchorItemOffset_isNaN() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberLazyListState()
            provider = ScrollInfoProvider(state)
            LazyColumn(state = state, modifier = Modifier.requiredSize(100.dp)) {}
        }

        rule.runOnIdle { assertThat(provider.anchorItemOffset).isNaN() }
    }

    @Test
    fun lazyColumn_empty_lastItemOffset_isZero() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberLazyListState()
            provider = ScrollInfoProvider(state)
            LazyColumn(state = state, modifier = Modifier.requiredSize(100.dp)) {}
        }

        rule.runOnIdle { assertThat(provider.lastItemOffset).isWithin(0.0001f).of(0f) }
    }

    @Test
    fun lazyColumn_itemFitsViewport_isScrollable_isTrue() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberLazyListState()
            provider = ScrollInfoProvider(state)
            LazyColumn(state = state, modifier = Modifier.requiredSize(100.dp)) {
                items(1) { Box(Modifier.requiredSize(20.dp)) }
            }
        }

        rule.runOnIdle {
            // Known Limitation: A single item that fits the viewport physically cannot scroll.
            // However, standard LazyListState natively evaluates `isScrollable` naively using
            // `totalItemsCount > 0` (unlike Wear-specific lists which accurately evaluate physical
            // scroll bounds). We assert true to explicitly document this bugged behavior.

            assertThat(provider.isScrollable).isTrue()
        }
    }

    @Test
    fun lazyColumn_scrollByDelta_anchorItemOffset_isDelta() {
        lateinit var provider: ScrollInfoProvider
        lateinit var state: LazyListState
        lateinit var scope: CoroutineScope

        rule.setContent {
            state = rememberLazyListState()
            provider = ScrollInfoProvider(state)
            scope = rememberCoroutineScope()
            LazyColumn(
                state = state,
                reverseLayout = false,
                modifier = Modifier.requiredSize(100.dp),
            ) {
                items(10) { Box(Modifier.requiredSize(30.dp)) }
            }
        }

        rule.runOnIdle { assertThat(provider.anchorItemOffset).isWithin(0.0001f).of(0f) }

        val scrollDelta = with(rule.density) { 20.dp.roundToPx().toFloat() }
        rule.runOnIdle { scope.launch { state.scrollBy(scrollDelta) } }
        rule.waitForIdle()

        rule.runOnIdle { assertThat(provider.anchorItemOffset).isWithin(0.0001f).of(scrollDelta) }
    }

    @Test
    fun lazyColumn_scrolledPastAnchor_anchorItemOffset_isNaN() {
        lateinit var provider: ScrollInfoProvider
        lateinit var state: LazyListState
        lateinit var scope: CoroutineScope

        rule.setContent {
            state = rememberLazyListState()
            provider = ScrollInfoProvider(state)
            scope = rememberCoroutineScope()
            LazyColumn(
                state = state,
                reverseLayout = false,
                modifier = Modifier.requiredSize(100.dp),
            ) {
                items(10) { Box(Modifier.requiredSize(30.dp)) }
            }
        }

        rule.runOnIdle { assertThat(provider.anchorItemOffset).isWithin(0.0001f).of(0f) }

        rule.runOnIdle { scope.launch { state.scrollToItem(index = 5, scrollOffset = 0) } }
        rule.waitForIdle()

        rule.runOnIdle { assertThat(provider.anchorItemOffset).isNaN() }
    }

    @Test
    fun lazyColumn_reverseLayout_lastItemOffset_isZero() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberLazyListState()
            provider = ScrollInfoProvider(state)
            LazyColumn(
                state = state,
                reverseLayout = true,
                modifier = Modifier.requiredSize(100.dp),
            ) {
                items(10) { Box(Modifier.requiredSize(30.dp)) }
            }
        }

        val expectedPx = with(rule.density) { 0.dp.toPx() }

        rule.runOnIdle { assertThat(provider.lastItemOffset).isWithin(0.0001f).of(expectedPx) }
    }

    @Test
    fun lazyColumn_viewportOverflow_lastItemOffset_isZero() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberLazyListState()
            provider = ScrollInfoProvider(state)
            LazyColumn(state = state, modifier = Modifier.requiredSize(60.dp)) {
                items(2) { Box(Modifier.requiredSize(40.dp)) }
            }
        }

        rule.runOnIdle { assertThat(provider.lastItemOffset).isWithin(0.0001f).of(0f) }
    }

    @Test
    fun scalingLazyColumn_overflowingContent_isScrollAwayValid_isTrue() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberScalingLazyListState()
            provider = ScrollInfoProvider(state)
            ScalingLazyColumn(
                state = state,
                reverseLayout = false,
                modifier = Modifier.requiredSize(100.dp),
            ) {
                items(10) { Box(Modifier.requiredSize(30.dp)) }
            }
        }

        rule.runOnIdle { assertThat(provider.isScrollAwayValid).isTrue() }
    }

    @Test
    fun scalingLazyColumn_overflowingContent_isScrollable_isTrue() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberScalingLazyListState()
            provider = ScrollInfoProvider(state)
            ScalingLazyColumn(
                state = state,
                reverseLayout = false,
                modifier = Modifier.requiredSize(100.dp),
            ) {
                items(10) { Box(Modifier.requiredSize(30.dp)) }
            }
        }

        rule.runOnIdle { assertThat(provider.isScrollable).isTrue() }
    }

    @Test
    fun scalingLazyColumn_overflowingContent_isScrollInProgress_isFalse() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberScalingLazyListState()
            provider = ScrollInfoProvider(state)
            ScalingLazyColumn(
                state = state,
                reverseLayout = false,
                modifier = Modifier.requiredSize(100.dp),
            ) {
                items(10) { Box(Modifier.requiredSize(30.dp)) }
            }
        }

        rule.runOnIdle { assertThat(provider.isScrollInProgress).isFalse() }
    }

    @Test
    fun scalingLazyColumn_overflowingContent_anchorItemOffset_isZero() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberScalingLazyListState()
            provider = ScrollInfoProvider(state)
            ScalingLazyColumn(
                state = state,
                reverseLayout = false,
                modifier = Modifier.requiredSize(100.dp),
            ) {
                items(10) { Box(Modifier.requiredSize(30.dp)) }
            }
        }

        rule.runOnIdle { assertThat(provider.anchorItemOffset).isWithin(0.0001f).of(0f) }
    }

    @Test
    fun scalingLazyColumn_overflowingContent_lastItemOffset_isZero() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberScalingLazyListState()
            provider = ScrollInfoProvider(state)
            ScalingLazyColumn(
                state = state,
                reverseLayout = false,
                modifier = Modifier.requiredSize(100.dp),
            ) {
                items(10) { Box(Modifier.requiredSize(30.dp)) }
            }
        }

        rule.runOnIdle { assertThat(provider.lastItemOffset).isWithin(0.0001f).of(0f) }
    }

    @Test
    fun scalingLazyColumn_empty_isScrollAwayValid_isFalse() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberScalingLazyListState()
            provider = ScrollInfoProvider(state)
            ScalingLazyColumn(state = state, modifier = Modifier.requiredSize(100.dp)) {}
        }

        rule.runOnIdle { assertThat(provider.isScrollAwayValid).isFalse() }
    }

    @Test
    fun scalingLazyColumn_empty_isScrollable_isFalse() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberScalingLazyListState()
            provider = ScrollInfoProvider(state)
            ScalingLazyColumn(state = state, modifier = Modifier.requiredSize(100.dp)) {}
        }

        rule.runOnIdle { assertThat(provider.isScrollable).isFalse() }
    }

    @Test
    fun scalingLazyColumn_empty_anchorItemOffset_isNaN() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberScalingLazyListState()
            provider = ScrollInfoProvider(state)
            ScalingLazyColumn(state = state, modifier = Modifier.requiredSize(100.dp)) {}
        }

        rule.runOnIdle { assertThat(provider.anchorItemOffset).isNaN() }
    }

    @Test
    fun scalingLazyColumn_empty_lastItemOffset_isZero() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberScalingLazyListState()
            provider = ScrollInfoProvider(state)
            ScalingLazyColumn(state = state, modifier = Modifier.requiredSize(100.dp)) {}
        }

        rule.runOnIdle { assertThat(provider.lastItemOffset).isWithin(0.0001f).of(0f) }
    }

    @Test
    fun scalingLazyColumn_itemFitsViewport_isScrollable_isFalse() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberScalingLazyListState()
            provider = ScrollInfoProvider(state)
            ScalingLazyColumn(state = state, modifier = Modifier.requiredSize(100.dp)) {
                items(1) { Box(Modifier.requiredSize(20.dp)) }
            }
        }

        rule.runOnIdle { assertThat(provider.isScrollable).isFalse() }
    }

    @Test
    fun scalingLazyColumn_scrollByDelta_anchorItemOffset_isDelta() {
        lateinit var provider: ScrollInfoProvider
        lateinit var state: ScalingLazyListState
        lateinit var scope: CoroutineScope

        rule.setContent {
            state = rememberScalingLazyListState()
            provider = ScrollInfoProvider(state)
            scope = rememberCoroutineScope()
            ScalingLazyColumn(
                state = state,
                reverseLayout = false,
                modifier = Modifier.requiredSize(100.dp),
            ) {
                items(10) { Box(Modifier.requiredSize(30.dp)) }
            }
        }

        rule.runOnIdle { assertThat(provider.anchorItemOffset).isWithin(0.0001f).of(0f) }

        val scrollDelta = with(rule.density) { 20.dp.roundToPx().toFloat() }
        rule.runOnIdle { scope.launch { state.scrollBy(scrollDelta) } }
        rule.waitForIdle()

        rule.runOnIdle { assertThat(provider.anchorItemOffset).isWithin(0.0001f).of(scrollDelta) }
    }

    @Test
    fun scalingLazyColumn_scrolledPastAnchor_anchorItemOffset_isNaN() {
        lateinit var provider: ScrollInfoProvider
        lateinit var state: ScalingLazyListState
        lateinit var scope: CoroutineScope

        rule.setContent {
            state = rememberScalingLazyListState()
            provider = ScrollInfoProvider(state)
            scope = rememberCoroutineScope()
            ScalingLazyColumn(
                state = state,
                reverseLayout = false,
                modifier = Modifier.requiredSize(100.dp),
            ) {
                items(10) { Box(Modifier.requiredSize(30.dp)) }
            }
        }

        rule.runOnIdle { assertThat(provider.anchorItemOffset).isWithin(0.0001f).of(0f) }

        rule.runOnIdle { scope.launch { state.scrollToItem(index = 5, scrollOffset = 0) } }
        rule.waitForIdle()

        rule.runOnIdle { assertThat(provider.anchorItemOffset).isNaN() }
    }

    @Test
    fun scalingLazyColumn_contentPaddingDecreasesWhileScrolled_anchorItemOffset_isZeroWhenScrolledToTop() {
        lateinit var provider: ScrollInfoProvider
        lateinit var state: ScalingLazyListState
        lateinit var scope: CoroutineScope
        var topPadding by mutableStateOf(24.dp)

        rule.setContent {
            state = rememberScalingLazyListState(initialCenterItemIndex = 0)
            provider = remember(state) { ScrollInfoProvider(state) }
            scope = rememberCoroutineScope()

            // Observe anchorItemOffset across composition and scroll passes, mimicking components
            // like ScreenScaffold or Modifier.scrollAway.
            LaunchedEffect(provider) { snapshotFlow { provider.anchorItemOffset }.collect {} }

            ScalingLazyColumn(
                state = state,
                autoCentering = null,
                contentPadding = PaddingValues(top = topPadding),
                modifier = Modifier.requiredSize(100.dp),
            ) {
                items(10) { Box(Modifier.requiredSize(30.dp)) }
            }
        }

        rule.runOnIdle { assertThat(provider.anchorItemOffset).isWithin(0.0001f).of(0f) }

        val scrollDelta = with(rule.density) { 50.dp.roundToPx().toFloat() }
        rule.runOnIdle { scope.launch { state.scrollBy(scrollDelta) } }
        rule.waitForIdle()

        // Content padding changes (e.g. 24.dp -> 21.dp) while item 0 is scrolled
        rule.runOnIdle { topPadding = 21.dp }
        rule.waitForIdle()

        // Smooth scroll back to the top of the list
        rule.runOnIdle { scope.launch { state.animateScrollBy(-scrollDelta) } }
        rule.waitForIdle()

        rule.runOnIdle { assertThat(provider.anchorItemOffset).isWithin(0.0001f).of(0f) }
    }

    @Test
    fun scalingLazyColumn_viewportOverflow_lastItemOffset_isZero() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberScalingLazyListState()
            provider = ScrollInfoProvider(state)
            ScalingLazyColumn(state = state, modifier = Modifier.requiredSize(60.dp)) {
                // Uses larger items (80.dp) to ensure viewport overflow despite edge scaling
                // effects.

                items(2) { Box(Modifier.requiredSize(80.dp)) }
            }
        }

        rule.runOnIdle { assertThat(provider.lastItemOffset).isWithin(0.0001f).of(0f) }
    }

    @Test
    fun transformingLazyColumn_overflowingContent_isScrollAwayValid_isTrue() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberTransformingLazyColumnState()
            provider = ScrollInfoProvider(state)
            TransformingLazyColumn(
                state = state,
                reverseLayout = false,
                modifier = Modifier.requiredSize(100.dp),
            ) {
                items(10) { Box(Modifier.requiredSize(30.dp)) }
            }
        }

        rule.runOnIdle { assertThat(provider.isScrollAwayValid).isTrue() }
    }

    @Test
    fun transformingLazyColumn_overflowingContent_isScrollable_isTrue() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberTransformingLazyColumnState()
            provider = ScrollInfoProvider(state)
            TransformingLazyColumn(
                state = state,
                reverseLayout = false,
                modifier = Modifier.requiredSize(100.dp),
            ) {
                items(10) { Box(Modifier.requiredSize(30.dp)) }
            }
        }

        rule.runOnIdle { assertThat(provider.isScrollable).isTrue() }
    }

    @Test
    fun transformingLazyColumn_overflowingContent_isScrollInProgress_isFalse() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberTransformingLazyColumnState()
            provider = ScrollInfoProvider(state)
            TransformingLazyColumn(
                state = state,
                reverseLayout = false,
                modifier = Modifier.requiredSize(100.dp),
            ) {
                items(10) { Box(Modifier.requiredSize(30.dp)) }
            }
        }

        rule.runOnIdle { assertThat(provider.isScrollInProgress).isFalse() }
    }

    @Test
    fun transformingLazyColumn_overflowingContent_anchorItemOffset_isZero() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberTransformingLazyColumnState()
            provider = ScrollInfoProvider(state)
            TransformingLazyColumn(
                state = state,
                reverseLayout = false,
                modifier = Modifier.requiredSize(100.dp),
            ) {
                items(10) { Box(Modifier.requiredSize(30.dp)) }
            }
        }

        rule.runOnIdle { assertThat(provider.anchorItemOffset).isWithin(0.0001f).of(0f) }
    }

    @Test
    fun transformingLazyColumn_overflowingContent_lastItemOffset_isZero() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberTransformingLazyColumnState()
            provider = ScrollInfoProvider(state)
            TransformingLazyColumn(
                state = state,
                reverseLayout = false,
                modifier = Modifier.requiredSize(100.dp),
            ) {
                items(10) { Box(Modifier.requiredSize(30.dp)) }
            }
        }

        rule.runOnIdle { assertThat(provider.lastItemOffset).isWithin(0.0001f).of(0f) }
    }

    @Test
    fun transformingLazyColumn_empty_isScrollAwayValid_isFalse() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberTransformingLazyColumnState()
            provider = ScrollInfoProvider(state)
            TransformingLazyColumn(state = state, modifier = Modifier.requiredSize(100.dp)) {}
        }

        rule.runOnIdle { assertThat(provider.isScrollAwayValid).isFalse() }
    }

    @Test
    fun transformingLazyColumn_empty_isScrollable_isFalse() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberTransformingLazyColumnState()
            provider = ScrollInfoProvider(state)
            TransformingLazyColumn(state = state, modifier = Modifier.requiredSize(100.dp)) {}
        }

        rule.runOnIdle { assertThat(provider.isScrollable).isFalse() }
    }

    @Test
    fun transformingLazyColumn_empty_anchorItemOffset_isNaN() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberTransformingLazyColumnState()
            provider = ScrollInfoProvider(state)
            TransformingLazyColumn(state = state, modifier = Modifier.requiredSize(100.dp)) {}
        }

        rule.runOnIdle { assertThat(provider.anchorItemOffset).isNaN() }
    }

    @Test
    fun transformingLazyColumn_empty_lastItemOffset_isZero() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberTransformingLazyColumnState()
            provider = ScrollInfoProvider(state)
            TransformingLazyColumn(state = state, modifier = Modifier.requiredSize(100.dp)) {}
        }

        rule.runOnIdle { assertThat(provider.lastItemOffset).isWithin(0.0001f).of(0f) }
    }

    @Test
    fun transformingLazyColumn_itemFitsViewport_isScrollable_isFalse() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberTransformingLazyColumnState()
            provider = ScrollInfoProvider(state)
            TransformingLazyColumn(state = state, modifier = Modifier.requiredSize(100.dp)) {
                items(1) { Box(Modifier.requiredSize(20.dp)) }
            }
        }

        rule.runOnIdle { assertThat(provider.isScrollable).isFalse() }
    }

    @Test
    fun transformingLazyColumn_scrollByDelta_anchorItemOffset_isDelta() {
        lateinit var provider: ScrollInfoProvider
        lateinit var state: TransformingLazyColumnState
        lateinit var scope: CoroutineScope

        rule.setContent {
            state = rememberTransformingLazyColumnState()
            provider = ScrollInfoProvider(state)
            scope = rememberCoroutineScope()
            TransformingLazyColumn(
                state = state,
                reverseLayout = false,
                modifier = Modifier.requiredSize(100.dp),
            ) {
                items(10) { Box(Modifier.requiredSize(30.dp)) }
            }
        }

        rule.runOnIdle { assertThat(provider.anchorItemOffset).isWithin(0.0001f).of(0f) }

        val scrollDelta = with(rule.density) { 20.dp.roundToPx().toFloat() }
        rule.runOnIdle { scope.launch { state.scrollBy(scrollDelta) } }
        rule.waitForIdle()

        rule.runOnIdle { assertThat(provider.anchorItemOffset).isWithin(0.0001f).of(scrollDelta) }
    }

    @Test
    fun transformingLazyColumn_scrolledPastAnchor_anchorItemOffset_isNaN() {
        lateinit var provider: ScrollInfoProvider
        lateinit var state: TransformingLazyColumnState
        lateinit var scope: CoroutineScope

        rule.setContent {
            state = rememberTransformingLazyColumnState()
            provider = ScrollInfoProvider(state)
            scope = rememberCoroutineScope()
            TransformingLazyColumn(
                state = state,
                reverseLayout = false,
                modifier = Modifier.requiredSize(100.dp),
            ) {
                items(10) { Box(Modifier.requiredSize(30.dp)) }
            }
        }

        rule.runOnIdle { assertThat(provider.anchorItemOffset).isWithin(0.0001f).of(0f) }

        rule.runOnIdle { scope.launch { state.scrollToItem(index = 5, scrollOffset = 0) } }
        rule.waitForIdle()

        rule.runOnIdle { assertThat(provider.anchorItemOffset).isNaN() }
    }

    @Test
    fun transformingLazyColumn_contentPaddingDecreasesWhileScrolled_anchorItemOffset_isZeroWhenScrolledToTop() {
        lateinit var provider: ScrollInfoProvider
        lateinit var state: TransformingLazyColumnState
        lateinit var scope: CoroutineScope
        var topPadding by mutableStateOf(24.dp)

        rule.setContent {
            state = rememberTransformingLazyColumnState()
            provider = remember(state) { ScrollInfoProvider(state) }
            scope = rememberCoroutineScope()

            // Observe anchorItemOffset across composition and scroll passes, mimicking components
            // like ScreenScaffold or Modifier.scrollAway.
            LaunchedEffect(provider) { snapshotFlow { provider.anchorItemOffset }.collect {} }

            TransformingLazyColumn(
                state = state,
                contentPadding = PaddingValues(top = topPadding),
                modifier = Modifier.requiredSize(100.dp),
            ) {
                items(10) { Box(Modifier.requiredSize(30.dp)) }
            }
        }

        rule.runOnIdle { assertThat(provider.anchorItemOffset).isWithin(0.0001f).of(0f) }

        val scrollDelta = with(rule.density) { 50.dp.roundToPx().toFloat() }
        rule.runOnIdle { scope.launch { state.scrollBy(scrollDelta) } }
        rule.waitForIdle()

        // Content padding changes (e.g. 24.dp -> 21.dp) while item 0 is scrolled
        rule.runOnIdle { topPadding = 21.dp }
        rule.waitForIdle()

        // Smooth scroll back to the top of the list
        rule.runOnIdle { scope.launch { state.animateScrollBy(-scrollDelta) } }
        rule.waitForIdle()

        rule.runOnIdle { assertThat(provider.anchorItemOffset).isWithin(0.0001f).of(0f) }
    }

    @Test
    fun transformingLazyColumn_reverseLayout_lastItemOffset_isZero() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberTransformingLazyColumnState()
            provider = ScrollInfoProvider(state)
            TransformingLazyColumn(
                state = state,
                reverseLayout = true,
                modifier = Modifier.requiredSize(100.dp),
            ) {
                items(10) { Box(Modifier.requiredSize(30.dp)) }
            }
        }

        val expectedPx = with(rule.density) { 0.dp.toPx() }

        rule.runOnIdle { assertThat(provider.lastItemOffset).isWithin(0.0001f).of(expectedPx) }
    }

    @Test
    fun transformingLazyColumn_viewportOverflow_lastItemOffset_isZero() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberTransformingLazyColumnState()
            provider = ScrollInfoProvider(state)
            TransformingLazyColumn(state = state, modifier = Modifier.requiredSize(60.dp)) {
                // Uses larger items (80.dp) to ensure viewport overflow despite item
                // transformations.

                items(2) { Box(Modifier.requiredSize(80.dp)) }
            }
        }

        rule.runOnIdle { assertThat(provider.lastItemOffset).isWithin(0.0001f).of(0f) }
    }

    @Test
    fun transformingLazyColumn_placementAnimation_lastItemOffset_tracksMidTransition() {
        lateinit var provider: ScrollInfoProvider
        var list by mutableStateOf(listOf(0, 1, 2))
        val viewportSize = 100.dp
        val itemSize = 30.dp

        rule.setContent {
            val state = rememberTransformingLazyColumnState()
            provider = ScrollInfoProvider(state)
            TransformingLazyColumn(
                state = state,
                verticalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier.requiredSize(viewportSize),
            ) {
                items(list, key = { it }) { item ->
                    Box(Modifier.requiredSize(itemSize).animateItem())
                }
            }
        }

        // Each element is rounded to integer pixels individually during compose layout
        // rendering. A 100.dp viewport with 3 items of 30.dp leaves an initial gap of
        // (viewport - 3 * item) pixels. Removing 1 item leaves 2 items, expanding the
        // gap to (viewport - 2 * item) pixels.

        val initialExpectedPx: Float
        val finalExpectedPx: Float
        with(rule.density) {
            val itemHeight = itemSize.roundToPx()
            val viewportHeight = viewportSize.roundToPx()

            initialExpectedPx = (viewportHeight - 3 * itemHeight).toFloat()
            finalExpectedPx = (viewportHeight - 2 * itemHeight).toFloat()
        }

        rule.runOnIdle {
            assertThat(provider.lastItemOffset).isWithin(0.0001f).of(initialExpectedPx)
        }

        rule.mainClock.autoAdvance = false
        list = listOf(1, 2)
        rule.mainClock.advanceTimeBy(100)

        // Verifying mid-animation bounds

        rule.runOnIdle {
            assertThat(provider.lastItemOffset).isGreaterThan(initialExpectedPx)
            assertThat(provider.lastItemOffset).isLessThan(finalExpectedPx)
        }

        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(provider.lastItemOffset).isWithin(0.0001f).of(finalExpectedPx)
        }
    }

    @Test
    fun transformingLazyColumn_deleteLastItem_withTransformedHeight_lastItemOffsetDoesNotOvershoot() {
        lateinit var provider: ScrollInfoProvider
        lateinit var state: TransformingLazyColumnState
        lateinit var scope: CoroutineScope
        val bottomPadding = 30.dp
        val itemSize = 40.dp
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4))
        var recordReactiveOffsets = false
        val reactiveOffsets = mutableListOf<Float>()

        rule.setContent {
            state = rememberTransformingLazyColumnState()
            scope = rememberCoroutineScope()
            provider = remember(state) { ScrollInfoProvider(state) }
            val derivedLastItemOffset by
                remember(provider) {
                    derivedStateOf { provider.lastItemOffset }
                }
            LaunchedEffect(provider) {
                snapshotFlow { derivedLastItemOffset }
                    .collect {
                        if (recordReactiveOffsets) {
                            reactiveOffsets += it
                        }
                    }
            }
            TransformingLazyColumn(
                state = state,
                contentPadding = PaddingValues(bottom = bottomPadding),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier.requiredSize(100.dp),
            ) {
                items(list, key = { it }) {
                    Box(
                        Modifier.requiredSize(itemSize)
                            // Items shrink as they approach the top edge.
                            .transformedHeight { height, progress ->
                                val scale =
                                    0.5f +
                                        0.5f * (progress.topOffsetFraction / 0.3f).coerceIn(0f, 1f)
                                (height * scale).roundToInt()
                            }
                            .animateItem()
                    )
                }
            }
        }

        // Scroll to the end so that the new last item will slide down from the shrinking top area.
        rule.runOnIdle { scope.launch { state.scrollBy(10_000f) } }
        rule.waitForIdle()

        val expectedOffsetPx = with(rule.density) { bottomPadding.roundToPx() }.toFloat()
        val itemHeightPx = with(rule.density) { itemSize.roundToPx() }

        // Read once so that previousLastItemKey points at the current last item, and verify that
        // key=3 starts shrunk in the top transition area.
        rule.runOnIdle {
            assertThat(provider.lastItemOffset).isWithin(1f).of(expectedOffsetPx)
            val newLastItemBeforeDelete = state.layoutInfo.visibleItems.first { it.key == 3 }
            assertThat(newLastItemBeforeDelete.transformedHeight).isLessThan(itemHeightPx)
        }

        rule.mainClock.autoAdvance = false
        rule.runOnIdle {
            recordReactiveOffsets = true
            list = listOf(0, 1, 2, 3)
        }

        val offsetsDuringAnimation = mutableListOf<Float>()
        repeat(20) {
            rule.mainClock.advanceTimeByFrame()
            offsetsDuringAnimation += rule.runOnIdle { provider.lastItemOffset }
        }

        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        val finalOffset = rule.runOnIdle { provider.lastItemOffset }
        assertThat(finalOffset).isWithin(1f).of(expectedOffsetPx)

        // Before the fix, the measured offset was mixed with the shrunk animated height, which
        // inflated lastItemOffset until the placement animation finished.
        offsetsDuringAnimation.forEach { assertThat(it).isWithin(1f).of(expectedOffsetPx) }
        reactiveOffsets.forEach { assertThat(it).isWithin(1f).of(expectedOffsetPx) }
    }

    @Test
    fun transformingLazyColumn_deleteNonLastItem_withTransformedHeight_lastItemOffsetIsMonotonic() {
        lateinit var provider: ScrollInfoProvider
        var list by mutableStateOf(listOf(0, 1, 2))
        var recordReactiveOffsets = false
        val reactiveOffsets = mutableListOf<Float>()

        rule.setContent {
            val state = rememberTransformingLazyColumnState()
            provider = remember(state) { ScrollInfoProvider(state) }
            val derivedLastItemOffset by
                remember(provider) {
                    derivedStateOf { provider.lastItemOffset }
                }
            LaunchedEffect(provider) {
                snapshotFlow { derivedLastItemOffset }
                    .collect {
                        if (recordReactiveOffsets) {
                            reactiveOffsets += it
                        }
                    }
            }
            TransformingLazyColumn(
                state = state,
                verticalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier.requiredSize(100.dp),
            ) {
                items(list, key = { it }) {
                    Box(
                        Modifier.requiredSize(30.dp)
                            // Items shrink as they approach the bottom edge.
                            .transformedHeight { height, progress ->
                                val scale =
                                    0.5f +
                                        0.5f *
                                            ((1f - progress.bottomOffsetFraction) / 0.3f).coerceIn(
                                                0f,
                                                1f,
                                            )
                                (height * scale).roundToInt()
                            }
                            .animateItem()
                    )
                }
            }
        }

        val initialOffset = rule.runOnIdle { provider.lastItemOffset }

        rule.mainClock.autoAdvance = false
        // The last item keeps its key and moves up out of the shrinking bottom area.
        rule.runOnIdle {
            recordReactiveOffsets = true
            list = listOf(1, 2)
        }

        val offsetsDuringAnimation = mutableListOf<Float>()
        repeat(20) {
            rule.mainClock.advanceTimeByFrame()
            offsetsDuringAnimation += rule.runOnIdle { provider.lastItemOffset }
        }

        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        val finalOffset = rule.runOnIdle { provider.lastItemOffset }
        assertThat(finalOffset).isGreaterThan(initialOffset)

        // Verify the animation branch actually interpolated instead of jumping straight to final.
        assertThat(offsetsDuringAnimation.any { it > initialOffset + 1f && it < finalOffset - 1f })
            .isTrue()
        assertThat(reactiveOffsets.any { it > initialOffset + 1f && it < finalOffset - 1f })
            .isTrue()

        // Mixing the animated offset with the measured (final, larger) height would dip below the
        // initial value at the start of the animation.
        fun assertMonotonic(samples: List<Float>) {
            var previous = initialOffset
            samples.forEach {
                assertThat(it).isAtLeast(initialOffset - 0.5f)
                assertThat(it).isAtMost(finalOffset + 0.5f)
                assertThat(it).isAtLeast(previous - 1f)
                previous = it
            }
        }

        assertMonotonic(offsetsDuringAnimation)
        assertMonotonic(reactiveOffsets)
    }

    @Test
    fun scrollState_overflowingContent_isScrollAwayValid_isTrue() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberScrollState()
            provider = ScrollInfoProvider(state)

            Box(Modifier.requiredSize(100.dp).verticalScroll(state)) { Box(Modifier.size(300.dp)) }
        }

        rule.runOnIdle { assertThat(provider.isScrollAwayValid).isTrue() }
    }

    @Test
    fun scrollState_overflowingContent_isScrollable_isTrue() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberScrollState()
            provider = ScrollInfoProvider(state)

            Box(Modifier.requiredSize(100.dp).verticalScroll(state)) { Box(Modifier.size(300.dp)) }
        }

        rule.runOnIdle { assertThat(provider.isScrollable).isTrue() }
    }

    @Test
    fun scrollState_overflowingContent_isScrollInProgress_isFalse() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberScrollState()
            provider = ScrollInfoProvider(state)

            Box(Modifier.requiredSize(100.dp).verticalScroll(state)) { Box(Modifier.size(300.dp)) }
        }

        rule.runOnIdle { assertThat(provider.isScrollInProgress).isFalse() }
    }

    @Test
    fun scrollState_overflowingContent_anchorItemOffset_isZero() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberScrollState()
            provider = ScrollInfoProvider(state)

            Box(Modifier.requiredSize(100.dp).verticalScroll(state)) { Box(Modifier.size(300.dp)) }
        }

        rule.runOnIdle { assertThat(provider.anchorItemOffset).isWithin(0.0001f).of(0f) }
    }

    @Test
    fun scrollState_overflowingContent_lastItemOffset_isZero() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberScrollState()
            provider = ScrollInfoProvider(state)

            Box(Modifier.requiredSize(100.dp).verticalScroll(state)) { Box(Modifier.size(300.dp)) }
        }

        rule.runOnIdle { assertThat(provider.lastItemOffset).isWithin(0.0001f).of(0f) }
    }

    @Test
    fun scrollState_contentFitsViewport_isScrollable_isFalse() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberScrollState()
            provider = ScrollInfoProvider(state)
            Box(Modifier.requiredSize(100.dp).verticalScroll(state)) { Box(Modifier.size(50.dp)) }
        }

        rule.runOnIdle { assertThat(provider.isScrollable).isFalse() }
    }

    @Test
    fun scrollState_scrollByDelta_anchorItemOffset_isDelta() {
        lateinit var provider: ScrollInfoProvider
        lateinit var state: ScrollState
        lateinit var scope: CoroutineScope

        rule.setContent {
            state = rememberScrollState()
            provider = ScrollInfoProvider(state)
            scope = rememberCoroutineScope()

            Box(Modifier.requiredSize(100.dp).verticalScroll(state)) { Box(Modifier.size(300.dp)) }
        }

        rule.runOnIdle { assertThat(provider.anchorItemOffset).isWithin(0.0001f).of(0f) }

        val scrollDelta = with(rule.density) { 20.dp.roundToPx().toFloat() }
        rule.runOnIdle { scope.launch { state.scrollBy(scrollDelta) } }
        rule.waitForIdle()

        rule.runOnIdle { assertThat(provider.anchorItemOffset).isWithin(0.0001f).of(scrollDelta) }
    }

    @Test
    fun scrollState_activeScrolling_isScrollInProgress_isTrueThenFalse() {
        lateinit var scrollState: ScrollState
        lateinit var provider: ScrollInfoProvider
        lateinit var scope: CoroutineScope

        rule.setContent {
            scrollState = rememberScrollState()
            provider = ScrollInfoProvider(scrollState)
            scope = rememberCoroutineScope()
            Box(Modifier.requiredSize(100.dp).verticalScroll(scrollState)) {
                Box(Modifier.size(300.dp))
            }
        }

        rule.runOnIdle { assertThat(provider.isScrollInProgress).isFalse() }

        rule.mainClock.autoAdvance = false
        scope.launch { scrollState.animateScrollTo(50) }
        rule.mainClock.advanceTimeByFrame()

        // Assert during active scroll animation
        assertThat(provider.isScrollInProgress).isTrue()

        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        rule.runOnIdle {
            // Workaround (Known Issue): As explicitly flagged in the provider's source code,
            // `prevOffset` is cached to handle timing issues when animations end. The first
            // idle read returns true because it catches the final un-synced offset delta from
            // the animation that just finished. The second read confirms the offset has fully
            // settled (false).

            assertThat(provider.isScrollInProgress).isTrue()
            assertThat(provider.isScrollInProgress).isFalse()
        }
    }

    @Test
    fun pagerState_multiplePages_isScrollAwayValid_isFalse() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberPagerState { 5 }
            provider = ScrollInfoProvider(state)
            HorizontalPager(state = state, modifier = Modifier.requiredSize(100.dp)) {
                Box(Modifier.requiredSize(100.dp))
            }
        }

        rule.runOnIdle { assertThat(provider.isScrollAwayValid).isFalse() }
    }

    @Test
    fun pagerState_multiplePages_isScrollable_isTrue() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberPagerState { 5 }
            provider = ScrollInfoProvider(state)
            HorizontalPager(state = state, modifier = Modifier.requiredSize(100.dp)) {
                Box(Modifier.requiredSize(100.dp))
            }
        }

        rule.runOnIdle { assertThat(provider.isScrollable).isTrue() }
    }

    @Test
    fun pagerState_multiplePages_isScrollInProgress_isFalse() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberPagerState { 5 }
            provider = ScrollInfoProvider(state)
            HorizontalPager(state = state, modifier = Modifier.requiredSize(100.dp)) {
                Box(Modifier.requiredSize(100.dp))
            }
        }

        rule.runOnIdle { assertThat(provider.isScrollInProgress).isFalse() }
    }

    @Test
    fun pagerState_multiplePages_anchorItemOffset_isNaN() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberPagerState { 5 }
            provider = ScrollInfoProvider(state)
            HorizontalPager(state = state, modifier = Modifier.requiredSize(100.dp)) {
                Box(Modifier.requiredSize(100.dp))
            }
        }

        rule.runOnIdle { assertThat(provider.anchorItemOffset).isNaN() }
    }

    @Test
    fun pagerState_multiplePages_lastItemOffset_isZero() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberPagerState { 5 }
            provider = ScrollInfoProvider(state)
            HorizontalPager(state = state, modifier = Modifier.requiredSize(100.dp)) {
                Box(Modifier.requiredSize(100.dp))
            }
        }

        rule.runOnIdle { assertThat(provider.lastItemOffset).isWithin(0.0001f).of(0f) }
    }

    @Test
    fun pagerState_singlePage_isScrollable_isTrue() {
        lateinit var provider: ScrollInfoProvider

        rule.setContent {
            val state = rememberPagerState { 1 }
            provider = ScrollInfoProvider(state)
            HorizontalPager(state = state, modifier = Modifier.requiredSize(100.dp)) {
                Box(Modifier.requiredSize(100.dp))
            }
        }

        // Note: A single-page Pager physically cannot scroll to another page.
        // However, the upstream foundation PagerState natively evaluates as scrollable
        // as long as it has content (pageCount > 0) to support overscroll touch physics.
        // We assert true here to intentionally document this upstream behavior.
        rule.runOnIdle { assertThat(provider.isScrollable).isTrue() }
    }
}
