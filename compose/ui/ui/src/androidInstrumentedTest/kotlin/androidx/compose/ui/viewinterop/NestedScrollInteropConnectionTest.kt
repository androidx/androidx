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

package androidx.compose.ui.viewinterop

import androidx.activity.ComponentActivity
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.testutils.WithTouchSlop
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.tests.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import kotlin.math.absoluteValue
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class NestedScrollInteropConnectionTest {

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val deltaCollectorNestedScrollConnection = InspectableNestedScrollConnection()

    private val nestedScrollParentView by lazy {
        rule.activity.findViewById<TestNestedScrollParentView>(R.id.main_layout)
    }

    private val items = (1..300).map { it.toString() }
    private val topItemTag = items.first()
    private val appBarExpandedSize = 240.dp
    private val appBarCollapsedSize = 54.dp
    private val appBarScrollDelta = appBarExpandedSize - appBarCollapsedSize
    private val completelyCollapsedScroll = 600.dp

    @Before
    fun setUp() {
        deltaCollectorNestedScrollConnection.reset()
    }

    @Test
    fun swipeComposeScrollable_insideNestedScrollingParentView_shouldScrollViewToo() {
        // arrange
        createViewComposeActivity { TestListWithNestedScroll(items) }

        // act: scroll compose side
        rule.onNodeWithTag(MainListTestTag).performTouchInput { swipeUp() }

        // assert: compose list is scrolled
        rule.onNodeWithTag(topItemTag).assertDoesNotExist()
        // assert: toolbar is collapsed
        onView(withId(R.id.fab)).check(matches(not(isDisplayed())))
    }

    @Test
    fun swipeComposeScrollable_insideNestedScrollingParentView_shouldPropagateConsumedDelta() {
        // arrange
        createViewComposeActivity {
            TestListWithNestedScroll(
                items,
                Modifier.nestedScroll(deltaCollectorNestedScrollConnection),
            )
        }

        // act: small scroll, everything will be consumed by view side
        rule.onNodeWithTag(MainListTestTag).performTouchInput {
            swipeWithVelocity(
                start = center,
                end = Offset(center.x, center.y + 100),
                endVelocity = 0f,
            )
        }

        // assert: check delta on view side
        rule.runOnIdle {
            assertThat(
                    abs(
                        nestedScrollParentView.offeredToParentOffset.y -
                            deltaCollectorNestedScrollConnection.offeredFromChild.y
                    )
                )
                .isAtMost(ScrollRoundingErrorTolerance)

            assertThat(deltaCollectorNestedScrollConnection.consumedDownChain)
                .isEqualTo(Offset.Zero)
        }
    }

    @Test
    fun swipeNoOpComposeScrollable_insideNestedScrollingParentView_shouldNotScrollView() {
        // arrange

        createViewComposeActivity { TestListWithNestedScroll(items, Modifier) }

        // act: scroll compose side
        rule.onNodeWithTag(MainListTestTag).performTouchInput { swipeUp() }

        // assert: compose list is scrolled
        rule.onNodeWithTag(topItemTag).assertDoesNotExist()
    }

    @Test
    fun swipeTurnOffNestedInterop_insideNestedScrollingParentView_shouldNotScrollView() {
        // arrange
        createViewComposeActivity(enableInterop = false) { TestListWithNestedScroll(items) }

        // act: scroll compose side
        rule.onNodeWithTag(MainListTestTag).performTouchInput { swipeUp() }

        // assert: compose list is scrolled
        rule.onNodeWithTag(topItemTag).assertDoesNotExist()
        // assert: toolbar  not collapsed
        onView(withId(R.id.fab)).check(matches(isDisplayed()))
    }

    @Test
    fun swipeComposeUpAndDown_insideNestedScrollingParentView_shouldPutViewToStartingPosition() {
        // arrange
        createViewComposeActivity { TestListWithNestedScroll(items) }

        // act: scroll compose side
        rule.onNodeWithTag(MainListTestTag).performTouchInput {
            swipeUp()
            swipeDown()
        }

        // assert: compose list is scrolled
        rule.onNodeWithTag(topItemTag).assertIsDisplayed()
        // assert: toolbar is collapsed
        onView(withId(R.id.fab)).check(matches(isDisplayed()))
    }

    @Test
    fun swipeComposeScrollable_byAppBarSize_shouldCollapseToolbar() {
        // arrange
        createViewComposeActivity { TestListWithNestedScroll(items) }

        // act: scroll compose side by the height of the toolbar
        rule.onNodeWithTag(MainListTestTag).performTouchInput {
            swipeWithVelocity(
                start = center,
                end = Offset(center.x, center.y - appBarExpandedSize.roundToPx()),
                endVelocity = 0f,
            )
        }

        // assert: compose list is scrolled just enough to collapse toolbar
        rule.onNodeWithTag(topItemTag).assertIsDisplayed()
        // assert: toolbar is collapsed
        onView(withId(R.id.fab)).check(matches(not(isDisplayed())))
    }

    @Test
    fun swipeNestedScrollingParentView_hasComposeScrollable_shouldNotScrollElement() {
        // arrange
        createViewComposeActivity { TestListWithNestedScroll(items) }

        // act
        onView(withId(R.id.app_bar)).perform(click(), swipeUp())

        // assert: toolbar is collapsed
        onView(withId(R.id.fab)).check(matches(not(isDisplayed())))
        // assert: compose list is not scrolled
        rule.onNodeWithTag(topItemTag).assertIsDisplayed()
    }

    @Test
    fun swipeComposeScrollable_insideNestedScrollingParentView_shouldPropagateCorrectPreDelta() {
        // arrange
        createViewComposeActivity {
            TestListWithNestedScroll(
                items,
                Modifier.nestedScroll(deltaCollectorNestedScrollConnection),
            )
        }

        // act: split scroll, some will be consumed by view and the rest by compose
        rule.onNodeWithTag(MainListTestTag).performTouchInput {
            swipeWithVelocity(
                start = center,
                end = Offset(center.x, center.y - completelyCollapsedScroll.roundToPx()),
                endVelocity = 0f,
            )
        }

        // assert: check that compose presented the correct delta values to view
        rule.runOnIdle {
            val appBarScrollDeltaPixels = appBarScrollDelta.value * rule.density.density * -1
            val offeredToParent = nestedScrollParentView.offeredToParentOffset.y
            val availableToParent =
                deltaCollectorNestedScrollConnection.offeredFromChild.y + appBarScrollDeltaPixels
            assertThat(offeredToParent - availableToParent).isAtMost(ScrollRoundingErrorTolerance)
        }
    }

    @Test
    fun swipingComposeScrollable_insideNestedScrollingParentView_shouldPropagateCorrectPostDelta() {
        // arrange
        createViewComposeActivity {
            TestListWithNestedScroll(
                items,
                Modifier.nestedScroll(deltaCollectorNestedScrollConnection),
            )
        }

        // act: split scroll, some will be consumed by view rest by compose
        rule.onNodeWithTag(MainListTestTag).performTouchInput {
            swipeWithVelocity(
                start = center,
                end = Offset(center.x, center.y - completelyCollapsedScroll.roundToPx()),
                endVelocity = 0f,
            )
        }

        // assert: check that whatever that is unconsumed by view was consumed by children
        val unconsumedInView = nestedScrollParentView.unconsumedOffset.round().y
        val consumedByChildren = deltaCollectorNestedScrollConnection.consumedDownChain.round().y
        rule.runOnIdle {
            assertThat(abs(unconsumedInView - consumedByChildren))
                .isAtMost(ScrollRoundingErrorTolerance)
        }
    }

    @Test
    fun swipeComposeScrollable_insideNestedScrollParentView_shouldPropagateCorrectPreVelocity() {
        // arrange
        createViewComposeActivity {
            TestListWithNestedScroll(
                items,
                Modifier.nestedScroll(deltaCollectorNestedScrollConnection),
            )
        }

        // act: split scroll, some will be consumed by view rest by compose
        rule.onNodeWithTag(MainListTestTag).performTouchInput {
            swipeUp(
                startY = center.y,
                endY = center.y - completelyCollapsedScroll.roundToPx(),
                durationMillis = 200,
            )
        }

        // assert: check that whatever that is unconsumed by view was consumed by children
        val velocityOfferedInView = abs(nestedScrollParentView.velocityDuringPreFlingPassOffset.y)
        val velocityAvailableInCompose =
            abs(deltaCollectorNestedScrollConnection.velocityOfferedFromChild.y)
        rule.runOnIdle {
            assertThat(velocityOfferedInView - velocityAvailableInCompose)
                .isEqualTo(VelocityRoundingErrorTolerance)
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun swipeComposeScrollable_insideNestedScrollParentView_shouldPropagateCorrectPostVelocity() {
        // arrange
        createViewComposeActivity {
            TestListWithNestedScroll(
                (1..20).map { it.toString() },
                Modifier.nestedScroll(deltaCollectorNestedScrollConnection),
            )
        }

        // act: split scroll, some will be consumed by view rest by compose
        rule.onNodeWithTag(MainListTestTag).performTouchInput {
            swipeUp(startY = bottomCenter.y, endY = topCenter.y, durationMillis = 200)
        }

        rule.runOnIdle {
            // assert: check that whatever that is unconsumed by children was released to the
            // view
            val velocityUnconsumedOffset =
                abs(nestedScrollParentView.velocityDuringFlingPassOffset.y)
            val velocityConsumedByChildren =
                abs(deltaCollectorNestedScrollConnection.velocityConsumedDownChain.y) +
                    abs(deltaCollectorNestedScrollConnection.velocityNotConsumedByChild.y)

            assertThat(abs(velocityUnconsumedOffset - velocityConsumedByChildren))
                .isAtMost(VelocityRoundingErrorTolerance)

            assertThat(nestedScrollParentView.nestedPreFlingCalled).isTrue()
            assertThat(nestedScrollParentView.nestedFlingCalled).isTrue()
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun swipeComposeScrollable_insideNestedScrollParentView_shouldNotPropagateCorrectPostVelocity() {
        // arrange
        val state = LazyListState()
        createViewComposeActivity {
            TestListWithNestedScroll(
                (1..20).map { it.toString() },
                Modifier.nestedScroll(deltaCollectorNestedScrollConnection),
                state = state,
            )
        }

        nestedScrollParentView.reportConsumedOnPreFling = true

        // act: split scroll, some will be consumed by view rest by compose
        rule.onNodeWithTag(MainListTestTag).performTouchInput {
            swipeUp(startY = bottomCenter.y, endY = topCenter.y, durationMillis = 200)
        }
        val topItem = state.firstVisibleItemIndex

        rule.runOnIdle {
            assertThat(nestedScrollParentView.nestedPreFlingCalled).isTrue()
            assertThat(nestedScrollParentView.nestedFlingCalled).isFalse()

            // item didn't move because we consumed it all during pre fling
            assertThat(state.firstVisibleItemIndex).isEqualTo(topItem)
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun swipeComposeScrollable_insideNestedScrollParentView_shouldPropagateCorrectConsumptionInfo() {
        val state = LazyListState()
        // arrange
        createViewComposeActivity {
            TestListWithNestedScroll(
                (1..20).map { it.toString() },
                Modifier.nestedScroll(deltaCollectorNestedScrollConnection),
                state = state,
            )
        }

        nestedScrollParentView.reportConsumedOnFling = true

        // act: split scroll, some will be consumed by view rest by compose
        rule.onNodeWithTag(MainListTestTag).performTouchInput {
            swipeUp(startY = bottomCenter.y, endY = topCenter.y, durationMillis = 200)
        }
        val topItem = state.firstVisibleItemIndex

        // item didn't move because we consumed it all during fling
        rule.runOnIdle { assertThat(state.firstVisibleItemIndex).isEqualTo(topItem) }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun swipeComposeScrollable_insideNestedScrollParentView_shouldNotPropagateCorrectConsumptionInfo() {
        val state = LazyListState()
        // arrange
        createViewComposeActivity {
            TestListWithNestedScroll(
                (1..20).map { it.toString() },
                Modifier.nestedScroll(deltaCollectorNestedScrollConnection),
                state = state,
            )
        }
        val topItem = state.firstVisibleItemIndex

        // act: split scroll, some will be consumed by view rest by compose
        rule.onNodeWithTag(MainListTestTag).performTouchInput {
            swipeUp(startY = bottomCenter.y, endY = topCenter.y, durationMillis = 200)
        }

        // item moved because we didn't consume in any fling pass
        rule.runOnIdle { assertThat(state.firstVisibleItemIndex).isNotEqualTo(topItem) }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun swipeComposeScrollable_shouldNotReceiveNonTouchCallbackIfFlingDidNotPropagateDeltas() {
        val fling =
            object : FlingBehavior {
                override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                    return initialVelocity
                }
            }

        // arrange
        createViewComposeActivity {
            TestListWithNestedScroll(
                (1..200).map { it.toString() },
                modifier = Modifier.nestedScroll(deltaCollectorNestedScrollConnection),
                flingBehavior = fling,
            )
        }

        rule.onNodeWithTag(MainListTestTag).performTouchInput {
            swipeUp(startY = bottomCenter.y, endY = topCenter.y, durationMillis = 200)
        }

        // We didn't receive any fling type nested scroll start call from Compose because
        // the fling behavior did not propagate any delta.
        rule.runOnIdle {
            assertThat(nestedScrollParentView.onNestedScrollNonTouchStartedCount).isEqualTo(0)
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun swipeComposeScrollable_shouldReceiveNonTouchCallbackForEveryFlingDelta() {
        val fling =
            object : FlingBehavior {
                override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                    scrollBy(10f)
                    return initialVelocity
                }
            }

        // arrange
        createViewComposeActivity {
            TestListWithNestedScroll(
                (1..200).map { it.toString() },
                modifier = Modifier.nestedScroll(deltaCollectorNestedScrollConnection),
                flingBehavior = fling,
            )
        }

        rule.onNodeWithTag(MainListTestTag).performTouchInput {
            swipeUp(startY = bottomCenter.y, endY = topCenter.y, durationMillis = 200)
        }

        // We only receive one call for nested scroll start from compose, due to only one
        // call to scrollBy in the child's fling behavior.
        rule.runOnIdle {
            assertThat(nestedScrollParentView.onNestedScrollNonTouchStartedCount).isEqualTo(1)
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun swipeComposeScrollable_shouldReceiveZeroDeltasIfTooSmall_vertical() {
        val fling =
            object : FlingBehavior {
                override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                    return initialVelocity
                }
            }

        // arrange
        createViewComposeActivity {
            WithTouchSlop(0.0f) {
                TestListWithNestedScroll(
                    (1..200).map { it.toString() },
                    modifier = Modifier.nestedScroll(deltaCollectorNestedScrollConnection),
                    flingBehavior = fling,
                )
            }
        }

        rule.onNodeWithTag(MainListTestTag).performTouchInput {
            down(center)
            moveBy(Offset(0.0f, 0.2f))
            up()
        }

        // We only receive one call for nested scroll start from compose, due to only one
        // call to scrollBy in the child's fling behavior.
        rule.runOnIdle {
            assertThat(nestedScrollParentView.offeredToParentOffset.y.absoluteValue).isEqualTo(0f)
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun swipeComposeScrollable_shouldReceiveZeroDeltasIfTooSmall_horizontal() {
        val fling =
            object : FlingBehavior {
                override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                    return initialVelocity
                }
            }

        // arrange
        createViewComposeActivity {
            WithTouchSlop(0.0f) {
                TestListWithNestedScroll(
                    (1..200).map { it.toString() },
                    modifier = Modifier.nestedScroll(deltaCollectorNestedScrollConnection),
                    flingBehavior = fling,
                    orientation = Orientation.Horizontal,
                )
            }
        }

        rule.onNodeWithTag(MainListTestTag).performTouchInput {
            down(center)
            moveBy(Offset(-0.2f, 0.0f))
            up()
        }

        // We only receive one call for nested scroll start from compose, due to only one
        // call to scrollBy in the child's fling behavior.
        rule.runOnIdle {
            assertThat(nestedScrollParentView.offeredToParentOffset.x.absoluteValue).isEqualTo(0f)
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun swipeComposeScrollable_shouldReceiveNonZeroDeltasIfBigEnough_vertical() {
        val fling =
            object : FlingBehavior {
                override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                    return initialVelocity
                }
            }

        // arrange
        createViewComposeActivity {
            WithTouchSlop(0.0f) {
                TestListWithNestedScroll(
                    (1..200).map { it.toString() },
                    modifier = Modifier.nestedScroll(deltaCollectorNestedScrollConnection),
                    flingBehavior = fling,
                )
            }
        }

        rule.onNodeWithTag(MainListTestTag).performTouchInput {
            down(center)
            moveBy(Offset(0.0f, 0.6f))
            up()
        }

        // We only receive one call for nested scroll start from compose, due to only one
        // call to scrollBy in the child's fling behavior.
        rule.runOnIdle {
            assertThat(nestedScrollParentView.offeredToParentOffset.y.absoluteValue).isEqualTo(1f)
        }
    }

    private fun createViewComposeActivity(
        enableInterop: Boolean = true,
        content: @Composable () -> Unit,
    ) {
        rule.activityRule.scenario.createActivityWithComposeContent(
            layout = R.layout.test_nested_scroll_coordinator_layout,
            enableInterop = enableInterop,
            content = content,
        )
    }
}

private const val ScrollRoundingErrorTolerance = 10
private const val VelocityRoundingErrorTolerance = 0
private const val MainListTestTag = "MainListTestTag"

@Composable
private fun TestListWithNestedScroll(
    items: List<String>,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    orientation: Orientation = Orientation.Vertical,
) {
    Box(modifier) {
        if (orientation == Orientation.Vertical) {
            LazyColumn(
                Modifier.testTag(MainListTestTag),
                userScrollEnabled = userScrollEnabled,
                flingBehavior = flingBehavior,
                state = state,
            ) {
                items(items) { TestItem(it) }
            }
        } else {
            LazyRow(
                Modifier.testTag(MainListTestTag),
                userScrollEnabled = userScrollEnabled,
                flingBehavior = flingBehavior,
                state = state,
            ) {
                items(items) { TestItem(it) }
            }
        }
    }
}

@Composable
private fun TestItem(item: String) {
    Box(
        modifier = Modifier.padding(16.dp).height(56.dp).fillMaxWidth().testTag(item),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(item)
    }
}
