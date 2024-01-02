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

package androidx.wear.compose.foundation

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties.HorizontalScrollAxisRange
import androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue
import org.junit.Rule
import org.junit.Test

internal const val CHILD_TEST_TAG = "childTestTag"

// TODO(b/201009199) Some of these tests may need specific values adjusted when swipeableV2
// supports property nested scrolling, but the tests should all still be valid.
@OptIn(ExperimentalWearFoundationApi::class)
class SwipeableV2Test {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun hasHorizontalScrollSemantics_atMaxValue_whenUnswiped() {
        val state = SwipeableV2State(false)
        rule.setContent {
            SimpleSwipeableV2Box { size ->
                Modifier
                    .swipeableV2(
                        state = state,
                        orientation = Orientation.Horizontal,
                    )
                    .swipeAnchors(
                        state = state,
                        possibleValues = setOf(false, true),
                    ) { value, _ ->
                        when (value) {
                            false -> 0f
                            true -> size.width
                        }
                    }
            }
        }

        rule.onNodeWithTag(TEST_TAG)
            .assert(hasScrollRangeCloseTo(Orientation.Horizontal, value = 1f, maxValue = 1f))
            .assert(SemanticsMatcher.keyNotDefined(VerticalScrollAxisRange))
    }

    @Test
    fun hasVerticalScrollSemantics_atMaxValue_whenUnswiped() {
        val state = SwipeableV2State(false)
        rule.setContent {
            SimpleSwipeableV2Box { size ->
                Modifier
                    .swipeableV2(
                        state = state,
                        orientation = Orientation.Vertical,
                    )
                    .swipeAnchors(
                        state = state,
                        possibleValues = setOf(false, true),
                    ) { value, _ ->
                        when (value) {
                            false -> 0f
                            true -> size.height
                        }
                    }
            }
        }

        rule.onNodeWithTag(TEST_TAG)
            .assert(hasScrollRangeCloseTo(Orientation.Vertical, value = 1f, maxValue = 1f))
            .assert(SemanticsMatcher.keyNotDefined(HorizontalScrollAxisRange))
    }

    @Test
    fun hasScrollSemantics_whenSwiped() {
        val state = SwipeableV2State(false)
        rule.setContent {
            SimpleSwipeableV2Box { size ->
                Modifier
                    .swipeableV2(
                        state = state,
                        orientation = Orientation.Horizontal,
                    )
                    .swipeAnchors(
                        state = state,
                        possibleValues = setOf(false, true),
                    ) { value, _ ->
                        when (value) {
                            false -> 0f
                            true -> size.width
                        }
                    }
            }
        }

        rule.onNodeWithTag(TEST_TAG)
            .performTouchInput {
                down(centerLeft)
                moveTo(centerLeft + percentOffset(.25f, 0f))
            }
        rule.onNodeWithTag(TEST_TAG)
            .assert(hasScrollRangeCloseTo(Orientation.Horizontal, value = 0.75f, maxValue = 1f))

        rule.onNodeWithTag(TEST_TAG)
            .performTouchInput {
                moveTo(center)
            }
        rule.onNodeWithTag(TEST_TAG)
            .assert(hasScrollRangeCloseTo(Orientation.Horizontal, value = 0.5f, maxValue = 1f))
    }

    @Test
    fun hasScrollSemantics_whenSwipedWithReverseDirection() {
        val state = SwipeableV2State(false)
        rule.setContent {
            SimpleSwipeableV2Box { size ->
                Modifier
                    .swipeableV2(
                        state = state,
                        orientation = Orientation.Horizontal,
                        reverseDirection = true
                    )
                    .swipeAnchors(
                        state = state,
                        possibleValues = setOf(false, true),
                    ) { value, _ ->
                        when (value) {
                            false -> 0f
                            true -> size.width
                        }
                    }
            }
        }

        rule.onNodeWithTag(TEST_TAG)
            .performTouchInput {
                down(centerRight)
                moveTo(centerRight - percentOffset(.25f, 0f))
            }
        rule.onNodeWithTag(TEST_TAG)
            .assert(
                hasScrollRangeCloseTo(
                    orientation = Orientation.Horizontal,
                    value = 0.75f,
                    maxValue = 1f,
                    reverseScrolling = true
                )
            )

        rule.onNodeWithTag(TEST_TAG)
            .performTouchInput {
                moveTo(center)
            }
        rule.onNodeWithTag(TEST_TAG)
            .assert(
                hasScrollRangeCloseTo(
                    orientation = Orientation.Horizontal,
                    value = 0.5f,
                    maxValue = 1f,
                    reverseScrolling = true
                )
            )
    }

    @Test
    fun hasNoScrollSemantics_whenDisabled() {
        val state = SwipeableV2State(false)
        rule.setContent {
            SimpleSwipeableV2Box { size ->
                Modifier
                    .swipeableV2(
                        state = state,
                        orientation = Orientation.Horizontal,
                        enabled = false
                    )
                    .swipeAnchors(
                        state = state,
                        possibleValues = setOf(false, true),
                    ) { value, _ ->
                        when (value) {
                            false -> 0f
                            true -> size.width
                        }
                    }
            }
        }

        rule.onNodeWithTag(TEST_TAG)
            .assert(SemanticsMatcher.keyNotDefined(HorizontalScrollAxisRange))
            .assert(SemanticsMatcher.keyNotDefined(VerticalScrollAxisRange))
    }

    @Test
    fun onSwipeLeft_sendsPreScrollEventToParent() {
        var delta = 0f
        rule.testSwipe(
            touchInput = { swipeLeft() },
            consumePreScrollDelta = { offset ->
                delta = offset.x
            }
        )

        assert(delta < 0) {
            "Expected delta to be negative, was $delta"
        }
    }

    @Test
    fun onSwipeRight_sendsPreScrollEventToParent() {
        var delta = 0f
        rule.testSwipe(
            touchInput = { swipeRight() },
            consumePreScrollDelta = { offset ->
                delta = offset.x
            }
        )

        assert(delta > 0) {
            "Expected delta to be positive, was $delta"
        }
    }

    @Test
    fun onSwipeUp_sendsPreScrollEventToParent() {
        var delta = 0f
        rule.testSwipe(
            touchInput = { swipeUp() },
            consumePreScrollDelta = { offset ->
                delta = offset.y
            },
            orientation = Orientation.Vertical
        )

        assert(delta < 0) {
            "Expected delta to be negative, was $delta"
        }
    }

    @Test
    fun onSwipeDown_sendsPreScrollEventToParent() {
        var delta = 0f
        rule.testSwipe(
            touchInput = { swipeDown() },
            consumePreScrollDelta = { offset ->
                delta = offset.y
            },
            orientation = Orientation.Vertical
        )

        assert(delta > 0) {
            "Expected delta to be positive, was $delta"
        }
    }

    @Test
    fun onSwipeLeft_sendsPostScrollEventToParent() {
        var delta = 0f
        rule.testSwipe(
            touchInput = { swipeLeft() },
            consumePostScrollDelta = { offset ->
                delta = offset.x
            }
        )

        assert(delta < 0) {
            "Expected delta to be negative, was $delta"
        }
    }

    @Test
    fun onSwipeRight_sendsPostScrollEventToParent() {
        var delta = 0f
        rule.testSwipe(
            touchInput = { swipeRight() },
            consumePostScrollDelta = { offset ->
                delta = offset.x
            },
            reverseAnchors = true //  reverse anchors or else swipeable consumes whole delta
        )

        assert(delta > 0) {
            "Expected delta to be positive, was $delta"
        }
    }

    @Test
    fun onSwipeUp_sendsPostScrollEventToParent() {
        var delta = 0f
        rule.testSwipe(
            touchInput = { swipeUp() },
            consumePostScrollDelta = { offset ->
                delta = offset.y
            },
            orientation = Orientation.Vertical
        )

        assert(delta < 0) {
            "Expected delta to be negative, was $delta"
        }
    }

    @Test
    fun onSwipeDown_sendsPostScrollEventToParent() {
        var delta = 0f
        rule.testSwipe(
            touchInput = { swipeDown() },
            consumePostScrollDelta = { offset ->
                delta = offset.y
            },
            orientation = Orientation.Vertical,
            reverseAnchors = true //  reverse anchors or else swipeable consumes whole delta
        )

        assert(delta > 0) {
            "Expected delta to be positive, was $delta"
        }
    }

    @Test
    fun onSwipeLeftToChild_sendsPreScrollEventToParent() {
        var delta = 0f
        rule.testSwipe(
            touchInput = { swipeLeft() },
            consumePreScrollDelta = { offset ->
                delta = offset.x
            },
            testTag = CHILD_TEST_TAG
        )

        assert(delta < 0) {
            "Expected delta to be negative, was $delta"
        }
    }

    @Test
    fun onSwipeRightToChild_sendsPreScrollEventToParent() {
        var delta = 0f
        rule.testSwipe(
            touchInput = { swipeRight() },
            consumePreScrollDelta = { offset ->
                delta = offset.x
            },
            testTag = CHILD_TEST_TAG
        )

        assert(delta > 0) {
            "Expected delta to be positive, was $delta"
        }
    }

    private fun ComposeContentTestRule.testSwipe(
        touchInput: TouchInjectionScope.() -> Unit,
        consumePreScrollDelta: (Offset) -> Unit = {},
        consumePostScrollDelta: (Offset) -> Unit = {},
        orientation: Orientation = Orientation.Horizontal,
        reverseAnchors: Boolean = false,
        testTag: String = TEST_TAG
    ) {
        setContent {
            val nestedScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        consumePreScrollDelta(available)
                        return super.onPreScroll(available, source)
                    }

                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        consumePostScrollDelta(available)
                        return super.onPostScroll(consumed, available, source)
                    }
                }
            }
            Box(modifier = Modifier.nestedScroll(nestedScrollConnection)) {
                SwipeableContent(
                    orientation = orientation,
                    reverseAnchors = reverseAnchors,
                    modifier = Modifier.testTag(TEST_TAG)
                ) {
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .testTag(CHILD_TEST_TAG)
                        .nestedScroll(remember { object : NestedScrollConnection {} })
                        .scrollable(
                            state = rememberScrollableState { _ ->
                                0f // Do not consume any delta, just return it
                            },
                            orientation = orientation
                        )
                    )
                }
            }
        }

        onNodeWithTag(testTag).performTouchInput { touchInput() }
    }

    @Composable
    private fun SwipeableContent(
        modifier: Modifier = Modifier,
        orientation: Orientation = Orientation.Horizontal,
        reverseAnchors: Boolean = false,
        content: @Composable BoxScope.() -> Unit = {}
    ) {
        // To participate as a producer of scroll events
        val nestedScrollDispatcher = remember { NestedScrollDispatcher() }
        // To participate as a consumer of scroll events
        val nestedScrollConnection = remember { object : NestedScrollConnection {} }
        val swipeableV2State = remember {
            SwipeableV2State(
                initialValue = false,
                nestedScrollDispatcher = nestedScrollDispatcher
            )
        }
        val factor = if (reverseAnchors) -1 else 1
        Box(
            modifier = modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .swipeableV2(
                    swipeableV2State,
                    orientation
                )
                .swipeAnchors(
                    state = swipeableV2State,
                    possibleValues = setOf(false, true)
                ) { value, layoutSize ->
                    when (value) {
                        false -> 0f
                        true -> factor * (
                            if (orientation == Orientation.Horizontal) layoutSize.width.toFloat()
                            else layoutSize.height.toFloat()
                            )
                    }
                }
                .nestedScroll(nestedScrollConnection, nestedScrollDispatcher),
            content = content
        )
    }

    /**
     * A square [Box] has the [TEST_TAG] test tag. Touch slop is disabled to make swipe calculations
     * more exact.
     */
    @Composable
    private fun SimpleSwipeableV2Box(swipeableV2Modifier: (Size) -> Modifier) {
        val originalViewConfiguration = LocalViewConfiguration.current
        val viewConfiguration = remember(originalViewConfiguration) {
            object : ViewConfiguration by originalViewConfiguration {
                override val touchSlop: Float = 0f
            }
        }

        with(LocalDensity.current) {
            val size = 100.dp
            val sizePx = size.toPx()

            CompositionLocalProvider(LocalViewConfiguration provides viewConfiguration) {
                Box(
                    Modifier
                        .testTag(TEST_TAG)
                        .requiredSize(size)
                        .then(remember { swipeableV2Modifier(Size(sizePx, sizePx)) })
                )
            }
        }
    }

    /**
     * Matches either a [HorizontalScrollAxisRange] or [VerticalScrollAxisRange] that has the given
     * [maxValue] and [reverseScrolling], and a `value` that is within a small threshold of [value].
     */
    private fun hasScrollRangeCloseTo(
        orientation: Orientation,
        value: Float,
        maxValue: Float,
        reverseScrolling: Boolean = false
    ): SemanticsMatcher = SemanticsMatcher(
        "has $orientation scroll range [0,$maxValue] with " +
            "value=$value" + if (reverseScrolling) " (reversed)" else ""
    ) { node ->
        val threshold = .1f
        val property = when (orientation) {
            Orientation.Horizontal -> HorizontalScrollAxisRange
            Orientation.Vertical -> VerticalScrollAxisRange
        }
        node.config.getOrNull(property)?.let { range ->
            (range.value() - value).absoluteValue <= threshold &&
                range.maxValue() == maxValue &&
                range.reverseScrolling == reverseScrolling
        } ?: false
    }
}
