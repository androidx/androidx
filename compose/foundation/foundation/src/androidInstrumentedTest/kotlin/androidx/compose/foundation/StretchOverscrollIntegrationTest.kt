/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation

import android.os.Build
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.WithTouchSlop
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.AnimationDurationScaleRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Integration test for stretch overscroll with [scrollable] and [nestedScroll]. */
@OptIn(ExperimentalFoundationApi::class)
@MediumTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
@RunWith(AndroidJUnit4::class)
class StretchOverscrollIntegrationTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule
    val animationScaleRule: AnimationDurationScaleRule =
        AnimationDurationScaleRule.createForAllTests(1f)

    /**
     * Test case to make sure that stretch overscroll correctly consumes delta before the scroll
     * cycle when relaxing, and after the scroll cycle when stretching, when pulled. Pull left =
     * showing overscroll from the right edge because of reverse scrolling
     */
    @Test
    fun stretchOverscroll_consumesDelta_pullLeft() {
        val state = setStretchOverscrollContent(Orientation.Horizontal)
        // Move to the end, since pulling left requires us to be at the right edge
        state.dispatchRawDelta(1000f)
        rule.runOnIdle { assertThat(state.scrollPosition).isEqualTo(1000f) }

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            down(center)
            // Stretch by 200
            moveBy(Offset(-200f, 0f))
            state.overscroll.invalidationEnabled = false
        }

        rule.runOnIdle {
            // When stretching, overscroll will consume after the scroll cycle
            assertThat(state.onPreScrollAvailable.x).isEqualTo(-200f)
            assertThat(state.scrollPosition).isEqualTo(1000f)
            assertThat(state.onPostScrollAvailable.x).isEqualTo(-200f)
        }

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            // Stretch by another 200
            moveBy(Offset(-200f, 0f))
        }

        rule.runOnIdle {
            assertThat(state.onPreScrollAvailable.x).isEqualTo(-400f)
            assertThat(state.scrollPosition).isEqualTo(1000f)
            assertThat(state.onPostScrollAvailable.x).isEqualTo(-400f)
        }

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            // Pull 200 in the opposite direction - because we had 200 pixels of stretch before,
            // this should only relax the existing overscroll, and not dispatch anything to the
            // state
            moveBy(Offset(200f, 0f))
        }

        rule.runOnIdle {
            // Overscroll consumes when relaxing before the scroll cycle, so these will not see the
            // new delta - all 200 should have been consumed relaxing the existing stretch
            assertThat(state.onPreScrollAvailable.x).isEqualTo(-400f)
            assertThat(state.scrollPosition).isEqualTo(1000f)
            assertThat(state.onPostScrollAvailable.x).isEqualTo(-400f)
        }

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            // Relax all the way, and continue into a scroll in the other direction
            moveBy(Offset(400f, 0f))
        }

        rule.runOnIdle {
            // Since overscroll fully relaxed, some delta should be available again
            assertThat(state.onPreScrollAvailable.x).isGreaterThan(-400f)
            // The scroll will consume the new delta
            assertThat(state.scrollPosition).isLessThan(1000f)
            // So post scroll will be unchanged at this point
            assertThat(state.onPostScrollAvailable.x).isEqualTo(-400f)
        }
    }

    /**
     * Test case to make sure that stretch overscroll correctly consumes delta after the scroll
     * cycle when stretching with a fling. Fling left = showing overscroll from the right edge
     * because of reverse scrolling
     */
    @Test
    fun stretchOverscroll_consumesDelta_flingLeft_stretch() {
        val state = setStretchOverscrollContent(Orientation.Horizontal)
        // Move to the end, since flinging left requires us to be at the right edge
        state.dispatchRawDelta(1000f)
        rule.runOnIdle { assertThat(state.scrollPosition).isEqualTo(1000f) }

        val velocity = 1000f

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            swipeWithVelocity(
                start = center,
                end = center - Offset(100f, 0f),
                endVelocity = velocity
            )
            state.overscroll.invalidationEnabled = false
        }

        rule.runOnIdle {
            // When stretching, overscroll will consume after the scroll cycle
            assertThat(state.onPreFlingAvailable.x).isWithin(1f).of(-1000f)
            assertThat(state.scrollPosition).isEqualTo(1000f)
            // Some velocity will be consumed as part of scrollable performing a fling animation,
            // even if we are at the end bound, so this value will be a bit lower as some velocity
            // is lost during the decay animation, before we return.
            assertThat(state.onPostFlingAvailable.x).isWithin(100f).of(-1000f)
        }
    }

    /**
     * Test case to make sure that stretch overscroll correctly consumes delta before the scroll
     * cycle when relaxing with a fling. Fling left = showing overscroll from the right edge because
     * of reverse scrolling
     */
    @Test
    fun stretchOverscroll_consumesDelta_flingLeft_relax() {
        val state = setStretchOverscrollContent(Orientation.Horizontal)
        // Move to the end, since flinging left requires us to be at the right edge
        state.dispatchRawDelta(1000f)
        rule.runOnIdle { assertThat(state.scrollPosition).isEqualTo(1000f) }

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            down(center)
            // Stretch by 200
            moveBy(Offset(-200f, 0f))
            state.overscroll.invalidationEnabled = false
        }

        rule.runOnIdle {
            assertThat(state.onPreScrollAvailable.x).isEqualTo(-200f)
            // No fling yet
            assertThat(state.onPreFlingAvailable.x).isEqualTo(0f)
            assertThat(state.scrollPosition).isEqualTo(1000f)
            assertThat(state.onPostFlingAvailable.x).isEqualTo(0f)
        }

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            // Relax by 100 (so there will still be velocity left over to relax the overscroll)
            moveBy(Offset(100f, 0f))
            up()
        }

        rule.runOnIdle {
            // When relaxing, overscroll will consume before the scroll cycle
            assertThat(state.onPreFlingAvailable.x).isEqualTo(0f)
            assertThat(state.scrollPosition).isEqualTo(1000f)
            assertThat(state.onPostFlingAvailable.x).isEqualTo(0f)
        }
    }

    /**
     * Test case to make sure that stretch overscroll correctly consumes delta before the scroll
     * cycle when relaxing, and after the scroll cycle when stretching, when pulled. Pull top (up) =
     * showing overscroll from the bottom edge because of reverse scrolling
     */
    @Test
    fun stretchOverscroll_consumesDelta_pullTop() {
        val state = setStretchOverscrollContent(Orientation.Vertical)
        // Move to the end, since pulling up requires us to be at the bottom edge
        state.dispatchRawDelta(1000f)
        rule.runOnIdle { assertThat(state.scrollPosition).isEqualTo(1000f) }

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            down(center)
            // Stretch by 200
            moveBy(Offset(0f, -200f))
            state.overscroll.invalidationEnabled = false
        }

        rule.runOnIdle {
            // When stretching, overscroll will consume after the scroll cycle
            assertThat(state.onPreScrollAvailable.y).isEqualTo(-200f)
            assertThat(state.scrollPosition).isEqualTo(1000f)
            assertThat(state.onPostScrollAvailable.y).isEqualTo(-200f)
        }

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            // Stretch by another 200
            moveBy(Offset(0f, -200f))
        }

        rule.runOnIdle {
            assertThat(state.onPreScrollAvailable.y).isEqualTo(-400f)
            assertThat(state.scrollPosition).isEqualTo(1000f)
            assertThat(state.onPostScrollAvailable.y).isEqualTo(-400f)
        }

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            // Pull 200 in the opposite direction - because we had 200 pixels of stretch before,
            // this should only relax the existing overscroll, and not dispatch anything to the
            // state
            moveBy(Offset(0f, 200f))
        }

        rule.runOnIdle {
            // Overscroll consumes when relaxing before the scroll cycle, so these will not see the
            // new delta - all 200 should have been consumed relaxing the existing stretch
            assertThat(state.onPreScrollAvailable.y).isEqualTo(-400f)
            assertThat(state.scrollPosition).isEqualTo(1000f)
            assertThat(state.onPostScrollAvailable.y).isEqualTo(-400f)
        }

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            // Relax all the way, and continue into a scroll in the other direction
            moveBy(Offset(0f, 400f))
        }

        rule.runOnIdle {
            // Since overscroll fully relaxed, some delta should be available again
            assertThat(state.onPreScrollAvailable.y).isGreaterThan(-400f)
            // The scroll will consume the new delta
            assertThat(state.scrollPosition).isLessThan(1000f)
            // So post scroll will be unchanged at this point
            assertThat(state.onPostScrollAvailable.y).isEqualTo(-400f)
        }
    }

    /**
     * Test case to make sure that stretch overscroll correctly consumes delta after the scroll
     * cycle when stretching with a fling. Fling top (up) = showing overscroll from the bottom edge
     * because of reverse scrolling
     */
    @Test
    fun stretchOverscroll_consumesDelta_flingTop_stretch() {
        val state = setStretchOverscrollContent(Orientation.Vertical)
        // Move to the end, since flinging up requires us to be at the bottom edge
        state.dispatchRawDelta(1000f)
        rule.runOnIdle { assertThat(state.scrollPosition).isEqualTo(1000f) }

        val velocity = 1000f

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            swipeWithVelocity(
                start = center,
                end = center - Offset(0f, 100f),
                endVelocity = velocity
            )
            state.overscroll.invalidationEnabled = false
        }

        rule.runOnIdle {
            // When stretching, overscroll will consume after the scroll cycle
            assertThat(state.onPreFlingAvailable.y).isWithin(1f).of(-1000f)
            assertThat(state.scrollPosition).isEqualTo(1000f)
            // Some velocity will be consumed as part of scrollable performing a fling animation,
            // even if we are at the end bound, so this value will be a bit lower as some velocity
            // is lost during the decay animation, before we return.
            assertThat(state.onPostFlingAvailable.y).isWithin(100f).of(-1000f)
        }
    }

    /**
     * Test case to make sure that stretch overscroll correctly consumes delta before the scroll
     * cycle when relaxing with a fling. Fling top (up) = showing overscroll from the bottom edge
     * because of reverse scrolling
     */
    @Test
    fun stretchOverscroll_consumesDelta_flingTop_relax() {
        val state = setStretchOverscrollContent(Orientation.Vertical)
        // Move to the end, since flinging up requires us to be at the bottom edge
        state.dispatchRawDelta(1000f)
        rule.runOnIdle { assertThat(state.scrollPosition).isEqualTo(1000f) }

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            down(center)
            // Stretch by 200
            moveBy(Offset(0f, -200f))
            state.overscroll.invalidationEnabled = false
        }

        rule.runOnIdle {
            assertThat(state.onPreScrollAvailable.y).isEqualTo(-200f)
            // No fling yet
            assertThat(state.onPreFlingAvailable.y).isEqualTo(0f)
            assertThat(state.scrollPosition).isEqualTo(1000f)
            assertThat(state.onPostFlingAvailable.y).isEqualTo(0f)
        }

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            // Relax by 100 (so there will still be velocity left over to relax the overscroll)
            moveBy(Offset(0f, 100f))
            up()
        }

        rule.runOnIdle {
            // When relaxing, overscroll will consume before the scroll cycle
            assertThat(state.onPreFlingAvailable.y).isEqualTo(0f)
            assertThat(state.scrollPosition).isEqualTo(1000f)
            assertThat(state.onPostFlingAvailable.y).isEqualTo(0f)
        }
    }

    /**
     * Test case to make sure that stretch overscroll correctly consumes delta before the scroll
     * cycle when relaxing, and after the scroll cycle when stretching, when pulled. Pull right =
     * showing overscroll from the left edge because of reverse scrolling
     */
    @Test
    fun stretchOverscroll_consumesDelta_pullRight() {
        val state = setStretchOverscrollContent(Orientation.Horizontal)

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            down(center)
            // Stretch by 200
            moveBy(Offset(200f, 0f))
            state.overscroll.invalidationEnabled = false
        }

        rule.runOnIdle {
            // When stretching, overscroll will consume after the scroll cycle
            assertThat(state.onPreScrollAvailable.x).isEqualTo(200f)
            assertThat(state.scrollPosition).isEqualTo(0f)
            assertThat(state.onPostScrollAvailable.x).isEqualTo(200f)
        }

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            // Stretch by another 200
            moveBy(Offset(200f, 0f))
        }

        rule.runOnIdle {
            assertThat(state.onPreScrollAvailable.x).isEqualTo(400f)
            assertThat(state.scrollPosition).isEqualTo(0f)
            assertThat(state.onPostScrollAvailable.x).isEqualTo(400f)
        }

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            // Pull 200 in the opposite direction - because we had 200 pixels of stretch before,
            // this should only relax the existing overscroll, and not dispatch anything to the
            // state
            moveBy(Offset(-200f, 0f))
        }

        rule.runOnIdle {
            // Overscroll consumes when relaxing before the scroll cycle, so these will not see the
            // new delta - all 200 should have been consumed relaxing the existing stretch
            assertThat(state.onPreScrollAvailable.x).isEqualTo(400f)
            assertThat(state.scrollPosition).isEqualTo(0f)
            assertThat(state.onPostScrollAvailable.x).isEqualTo(400f)
        }

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            // Relax all the way, and continue into a scroll in the other direction
            moveBy(Offset(-400f, 0f))
        }

        rule.runOnIdle {
            // Since overscroll fully relaxed, some delta should be available again
            assertThat(state.onPreScrollAvailable.x).isLessThan(400f)
            // The scroll will consume the new delta
            assertThat(state.scrollPosition).isGreaterThan(0f)
            // So post scroll will be unchanged at this point
            assertThat(state.onPostScrollAvailable.x).isEqualTo(400f)
        }
    }

    /**
     * Test case to make sure that stretch overscroll correctly consumes delta after the scroll
     * cycle when stretching with a fling. Fling right = showing overscroll from the left edge
     * because of reverse scrolling
     */
    @Test
    fun stretchOverscroll_consumesDelta_flingRight_stretch() {
        val state = setStretchOverscrollContent(Orientation.Horizontal)

        val velocity = 1000f

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            swipeWithVelocity(
                start = center,
                end = center + Offset(100f, 0f),
                endVelocity = velocity
            )
            state.overscroll.invalidationEnabled = false
        }

        rule.runOnIdle {
            // When stretching, overscroll will consume after the scroll cycle
            assertThat(state.onPreFlingAvailable.x).isWithin(1f).of(1000f)
            assertThat(state.scrollPosition).isEqualTo(0f)
            // Some velocity will be consumed as part of scrollable performing a fling animation,
            // even if we are at the end bound, so this value will be a bit lower as some velocity
            // is lost during the decay animation, before we return.
            assertThat(state.onPostFlingAvailable.x).isWithin(100f).of(1000f)
        }
    }

    /**
     * Test case to make sure that stretch overscroll correctly consumes delta before the scroll
     * cycle when relaxing with a fling. Fling right = showing overscroll from the left edge because
     * of reverse scrolling
     */
    @Test
    fun stretchOverscroll_consumesDelta_flingRight_relax() {
        val state = setStretchOverscrollContent(Orientation.Horizontal)

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            down(center)
            // Stretch by 200
            moveBy(Offset(200f, 0f))
            state.overscroll.invalidationEnabled = false
        }

        rule.runOnIdle {
            assertThat(state.onPreScrollAvailable.x).isEqualTo(200f)
            // No fling yet
            assertThat(state.onPreFlingAvailable.x).isEqualTo(0f)
            assertThat(state.scrollPosition).isEqualTo(0f)
            assertThat(state.onPostFlingAvailable.x).isEqualTo(0f)
        }

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            // Relax by 100 (so there will still be velocity left over to relax the overscroll)
            moveBy(Offset(-100f, 0f))
            up()
        }

        rule.runOnIdle {
            // When relaxing, overscroll will consume before the scroll cycle
            assertThat(state.onPreFlingAvailable.x).isEqualTo(0f)
            assertThat(state.scrollPosition).isEqualTo(0f)
            assertThat(state.onPostFlingAvailable.x).isEqualTo(0f)
        }
    }

    /**
     * Test case to make sure that stretch overscroll correctly consumes delta before the scroll
     * cycle when relaxing, and after the scroll cycle when stretching, when pulled. Pull bottom
     * (down) = showing overscroll from the top edge because of reverse scrolling
     */
    @Test
    fun stretchOverscroll_consumesDelta_pullBottom() {
        val state = setStretchOverscrollContent(Orientation.Vertical)

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            down(center)
            // Stretch by 200
            moveBy(Offset(0f, 200f))
            state.overscroll.invalidationEnabled = false
        }

        rule.runOnIdle {
            // When stretching, overscroll will consume after the scroll cycle
            assertThat(state.onPreScrollAvailable.y).isEqualTo(200f)
            assertThat(state.scrollPosition).isEqualTo(0f)
            assertThat(state.onPostScrollAvailable.y).isEqualTo(200f)
        }

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            // Stretch by another 200
            moveBy(Offset(0f, 200f))
        }

        rule.runOnIdle {
            assertThat(state.onPreScrollAvailable.y).isEqualTo(400f)
            assertThat(state.scrollPosition).isEqualTo(0f)
            assertThat(state.onPostScrollAvailable.y).isEqualTo(400f)
        }

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            // Pull 200 in the opposite direction - because we had 200 pixels of stretch before,
            // this should only relax the existing overscroll, and not dispatch anything to the
            // state
            moveBy(Offset(0f, -200f))
        }

        rule.runOnIdle {
            // Overscroll consumes when relaxing before the scroll cycle, so these will not see the
            // new delta - all 200 should have been consumed relaxing the existing stretch
            assertThat(state.onPreScrollAvailable.y).isEqualTo(400f)
            assertThat(state.scrollPosition).isEqualTo(0f)
            assertThat(state.onPostScrollAvailable.y).isEqualTo(400f)
        }

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            // Relax all the way, and continue into a scroll in the other direction
            moveBy(Offset(0f, -400f))
        }

        rule.runOnIdle {
            // Since overscroll fully relaxed, some delta should be available again
            assertThat(state.onPreScrollAvailable.y).isLessThan(400f)
            // The scroll will consume the new delta
            assertThat(state.scrollPosition).isGreaterThan(0f)
            // So post scroll will be unchanged at this point
            assertThat(state.onPostScrollAvailable.y).isEqualTo(400f)
        }
    }

    /**
     * Test case to make sure that stretch overscroll correctly consumes delta after the scroll
     * cycle when stretching with a fling. Fling bottom (down) = showing overscroll from the top
     * edge because of reverse scrolling
     */
    @Test
    fun stretchOverscroll_consumesDelta_flingBottom_stretch() {
        val state = setStretchOverscrollContent(Orientation.Vertical)

        val velocity = 1000f

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            swipeWithVelocity(
                start = center,
                end = center + Offset(0f, 100f),
                endVelocity = velocity
            )
            state.overscroll.invalidationEnabled = false
        }

        rule.runOnIdle {
            // When stretching, overscroll will consume after the scroll cycle
            assertThat(state.onPreFlingAvailable.y).isWithin(1f).of(1000f)
            assertThat(state.scrollPosition).isEqualTo(0f)
            // Some velocity will be consumed as part of scrollable performing a fling animation,
            // even if we are at the end bound, so this value will be a bit lower as some velocity
            // is lost during the decay animation, before we return.
            assertThat(state.onPostFlingAvailable.y).isWithin(100f).of(1000f)
        }
    }

    /**
     * Test case to make sure that stretch overscroll correctly consumes delta before the scroll
     * cycle when relaxing with a fling. Fling bottom (down) = showing overscroll from the top edge
     * because of reverse scrolling
     */
    @Test
    fun stretchOverscroll_consumesDelta_flingBottom_relax() {
        val state = setStretchOverscrollContent(Orientation.Vertical)

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            down(center)
            // Stretch by 200
            moveBy(Offset(0f, 200f))
            state.overscroll.invalidationEnabled = false
        }

        rule.runOnIdle {
            assertThat(state.onPreScrollAvailable.y).isEqualTo(200f)
            // No fling yet
            assertThat(state.onPreFlingAvailable.y).isEqualTo(0f)
            assertThat(state.scrollPosition).isEqualTo(0f)
            assertThat(state.onPostFlingAvailable.y).isEqualTo(0f)
        }

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            // Relax by 100 (so there will still be velocity left over to relax the overscroll)
            moveBy(Offset(0f, -100f))
            up()
        }

        rule.runOnIdle {
            // When relaxing, overscroll will consume before the scroll cycle
            assertThat(state.onPreFlingAvailable.y).isEqualTo(0f)
            assertThat(state.scrollPosition).isEqualTo(0f)
            assertThat(state.onPostFlingAvailable.y).isEqualTo(0f)
        }
    }

    // Tests for b/265363356

    @Test
    fun stretchOverscroll_whenPulledWithSmallDelta_doesNotConsumesOppositePreScroll_pullLeft() {
        val state = setStretchOverscrollContent(Orientation.Horizontal)
        // Move to the end, since flinging left requires us to be at the right edge
        state.dispatchRawDelta(1000f)
        rule.runOnIdle { assertThat(state.scrollPosition).isEqualTo(1000f) }

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            down(center)
            // Try and stretch by 0.4f
            moveBy(Offset(-0.4f, 0f))
            // Pull 200 in the opposite direction - overscroll should have ignored the 0.4f, and
            // so all 200 should be dispatched to the state with nothing being consumed
            moveBy(Offset(200f, 0f))
        }

        rule.runOnIdle {
            // The -0.4f should still have been dispatched to pre scroll, and then it will see the
            // 200f after
            assertThat(state.onPreScrollAvailable.x).isWithin(0.001f).of(199.6f)
            // All 200f should be dispatched directly to the state
            assertThat(state.scrollPosition).isEqualTo(800f)
            // The -0.4f should still have been dispatched to post scroll, but the 200f is fully
            // consumed by the scroll
            assertThat(state.onPostScrollAvailable.x).isWithin(0.001f).of(-0.4f)
        }
    }

    @Test
    fun stretchOverscroll_whenPulledWithSmallDelta_doesNotConsumesOppositePreScroll_pullTop() {
        val state = setStretchOverscrollContent(Orientation.Vertical)
        // Move to the end, since flinging up requires us to be at the bottom edge
        state.dispatchRawDelta(1000f)
        rule.runOnIdle { assertThat(state.scrollPosition).isEqualTo(1000f) }

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            down(center)
            // Try and stretch by 0.4f
            moveBy(Offset(0f, -0.4f))
            // Pull 200 in the opposite direction - overscroll should have ignored the 0.4f, and
            // so all 200 should be dispatched to the state with nothing being consumed
            moveBy(Offset(0f, 200f))
        }

        rule.runOnIdle {
            // The -0.4f should still have been dispatched to pre scroll, and then it will see the
            // 200f after
            assertThat(state.onPreScrollAvailable.y).isWithin(0.001f).of(199.6f)
            // All 200f should be dispatched directly to the state
            assertThat(state.scrollPosition).isEqualTo(800f)
            // The -0.4f should still have been dispatched to post scroll, but the 200f is fully
            // consumed by the scroll
            assertThat(state.onPostScrollAvailable.y).isWithin(0.001f).of(-0.4f)
        }
    }

    @Test
    fun stretchOverscroll_whenPulledWithSmallDelta_doesNotConsumesOppositePreScroll_pullRight() {
        val state = setStretchOverscrollContent(Orientation.Horizontal)

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            down(center)
            // Try and stretch by 0.4f (the max scroll value is 1000)
            moveBy(Offset(0.4f, 0f))
            // Pull 200 in the opposite direction - overscroll should have ignored the 0.4f, and
            // so all -200 should be dispatched to the state with nothing being consumed
            moveBy(Offset(-200f, 0f))
        }

        rule.runOnIdle {
            // The extra 0.4f should still have been dispatched to pre scroll, and then it will see
            // the -200f after
            assertThat(state.onPreScrollAvailable.x).isWithin(0.001f).of(-199.6f)
            // All -200f should be dispatched directly to the state
            assertThat(state.scrollPosition).isEqualTo(200f)
            // The extra 0.4f should still have been dispatched to post scroll, but the 1000f and
            // -200f are fully consumed by the scroll
            assertThat(state.onPostScrollAvailable.x).isWithin(0.001f).of(0.4f)
        }
    }

    @Test
    fun stretchOverscroll_whenPulledWithSmallDelta_doesNotConsumesOppositePreScroll_pullBottom() {
        val state = setStretchOverscrollContent(Orientation.Vertical)

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            down(center)
            // Try and stretch by 0.4f (the max scroll value is 1000)
            moveBy(Offset(0f, 0.4f))
            // Pull 200 in the opposite direction - overscroll should have ignored the 0.4f, and
            // so all -200 should be dispatched to the state with nothing being consumed
            moveBy(Offset(0f, -200f))
        }

        rule.runOnIdle {
            // The extra 0.4f should still have been dispatched to pre scroll, and then it will see
            // the -200f after
            assertThat(state.onPreScrollAvailable.y).isWithin(0.001f).of(-199.6f)
            // All -200f should be dispatched directly to the state
            assertThat(state.scrollPosition).isEqualTo(200f)
            // The extra 0.4f should still have been dispatched to post scroll, but the 1000f and
            // -200f are fully consumed by the scroll
            assertThat(state.onPostScrollAvailable.y).isWithin(0.001f).of(0.4f)
        }
    }

    private fun setStretchOverscrollContent(orientation: Orientation): TestState {
        animationScaleRule.setAnimationDurationScale(1f)
        val state = TestState()
        rule.setContent {
            WithTouchSlop(touchSlop = 0f) {
                state.overscroll =
                    ScrollableDefaults.overscrollEffect() as AndroidEdgeEffectOverscrollEffect
                Box(
                    Modifier.testTag(OverscrollBox)
                        .size(100.dp)
                        .nestedScroll(state.nestedScrollConnection)
                        .scrollable(
                            state = state,
                            orientation = orientation,
                            // Match typical scroll container behavior where the content is moved,
                            // not the viewport
                            reverseDirection =
                                ScrollableDefaults.reverseDirection(
                                    layoutDirection = LocalLayoutDirection.current,
                                    orientation = orientation,
                                    reverseScrolling = false
                                ),
                            overscrollEffect = state.overscroll
                        )
                        .overscroll(state.overscroll)
                )
            }
        }
        return state
    }
}

/**
 * Returns a default [ScrollableState] with a [scrollPosition] clamped between 0f and 1000f, and
 * exposes properties for seeing nested scroll available delta and velocities.
 */
private class TestState : ScrollableState {
    var scrollPosition by mutableStateOf(0f)
        private set

    var onPreScrollAvailable: Offset = Offset.Zero
        private set

    var onPostScrollAvailable: Offset = Offset.Zero
        private set

    var onPreFlingAvailable: Velocity = Velocity.Zero
        private set

    var onPostFlingAvailable: Velocity = Velocity.Zero
        private set

    lateinit var overscroll: AndroidEdgeEffectOverscrollEffect

    // Using ScrollableState here instead of ScrollState as ScrollState will automatically round to
    // an int, and we need to assert floating point values
    private val scrollableState = ScrollableState {
        val newPosition = (scrollPosition + it).coerceIn(0f, 1000f)
        val consumed = newPosition - scrollPosition
        scrollPosition = newPosition
        consumed
    }

    val nestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                onPreScrollAvailable += available
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                onPostScrollAvailable += available
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                onPreFlingAvailable += available
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                onPostFlingAvailable += available
                return Velocity.Zero
            }
        }

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit
    ) = scrollableState.scroll(scrollPriority, block)

    override fun dispatchRawDelta(delta: Float) = scrollableState.dispatchRawDelta(delta)

    override val isScrollInProgress: Boolean
        get() = scrollableState.isScrollInProgress
}

private const val OverscrollBox = "box"
