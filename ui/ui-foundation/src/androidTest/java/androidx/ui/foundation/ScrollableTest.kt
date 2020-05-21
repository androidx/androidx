/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.foundation

import android.os.Handler
import android.os.Looper
import androidx.animation.ExponentialDecay
import androidx.animation.ManualAnimationClock
import androidx.compose.Composable
import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.setValue
import androidx.test.filters.SmallTest
import androidx.ui.core.Modifier
import androidx.ui.core.testTag
import androidx.ui.foundation.animation.FlingConfig
import androidx.ui.foundation.gestures.DragDirection
import androidx.ui.foundation.gestures.ScrollableState
import androidx.ui.foundation.gestures.scrollable
import androidx.ui.layout.Stack
import androidx.ui.layout.preferredSize
import androidx.ui.test.center
import androidx.ui.test.createComposeRule
import androidx.ui.test.doGesture
import androidx.ui.test.findByTag
import androidx.ui.test.runOnIdleCompose
import androidx.ui.test.runOnUiThread
import androidx.ui.test.sendSwipe
import androidx.ui.test.sendSwipeWithVelocity
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.milliseconds
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class ScrollableTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val scrollableBoxTag = "scrollableBox"

    @Test
    fun scrollable_horizontalScroll() {
        val clocks = ManualAnimationClock(0L)
        var total = 0f
        val state = ScrollableState(
            onScrollDeltaConsumptionRequested = {
                total += it
                it
            },
            flingConfig = FlingConfig(decayAnimation = ExponentialDecay()),
            animationClock = clocks
        )
        setScrollableContent {
            Modifier.scrollable(
                scrollableState = state,
                dragDirection = DragDirection.Horizontal
            )
        }
        findByTag(scrollableBoxTag).doGesture {
            this.sendSwipe(
                start = this.center,
                end = PxPosition(this.center.x + 100f, this.center.y),
                duration = 100.milliseconds
            )
        }
        advanceClockAndAwaitAnimation(state, clocks)

        val lastTotal = runOnIdleCompose {
            assertThat(total).isGreaterThan(0)
            total
        }
        findByTag(scrollableBoxTag).doGesture {
            this.sendSwipe(
                start = this.center,
                end = PxPosition(this.center.x, this.center.y + 100f),
                duration = 100.milliseconds
            )
        }
        advanceClockAndAwaitAnimation(state, clocks)

        runOnIdleCompose {
            assertThat(total).isEqualTo(lastTotal)
        }
        findByTag(scrollableBoxTag).doGesture {
            this.sendSwipe(
                start = this.center,
                end = PxPosition(this.center.x - 100f, this.center.y),
                duration = 100.milliseconds
            )
        }
        advanceClockAndAwaitAnimation(state, clocks)
        runOnIdleCompose {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @Test
    fun scrollable_verticalScroll() {
        val clocks = ManualAnimationClock(0L)
        var total = 0f
        val state = ScrollableState(
            onScrollDeltaConsumptionRequested = {
                total += it
                it
            },
            flingConfig = FlingConfig(decayAnimation = ExponentialDecay()),
            animationClock = clocks
        )
        setScrollableContent {
            Modifier.scrollable(
                scrollableState = state,
                dragDirection = DragDirection.Vertical
            )
        }
        findByTag(scrollableBoxTag).doGesture {
            this.sendSwipe(
                start = this.center,
                end = PxPosition(this.center.x, this.center.y + 100f),
                duration = 100.milliseconds
            )
        }
        advanceClockAndAwaitAnimation(state, clocks)

        val lastTotal = runOnIdleCompose {
            assertThat(total).isGreaterThan(0)
            total
        }
        findByTag(scrollableBoxTag).doGesture {
            this.sendSwipe(
                start = this.center,
                end = PxPosition(this.center.x + 100f, this.center.y),
                duration = 100.milliseconds
            )
        }
        advanceClockAndAwaitAnimation(state, clocks)

        runOnIdleCompose {
            assertThat(total).isEqualTo(lastTotal)
        }
        findByTag(scrollableBoxTag).doGesture {
            this.sendSwipe(
                start = this.center,
                end = PxPosition(this.center.x, this.center.y - 100f),
                duration = 100.milliseconds
            )
        }
        advanceClockAndAwaitAnimation(state, clocks)
        runOnIdleCompose {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @Test
    fun scrollable_startStop_notify() {
        var startTrigger = 0f
        var stopTrigger = 0f
        val clocks = ManualAnimationClock(0L)
        var total = 0f
        val state = ScrollableState(
            onScrollDeltaConsumptionRequested = {
                total += it
                it
            },
            flingConfig = FlingConfig(decayAnimation = ExponentialDecay()),
            animationClock = clocks
        )
        setScrollableContent {
            Modifier.scrollable(
                scrollableState = state,
                dragDirection = DragDirection.Horizontal,
                onScrollStarted = { startTrigger++ },
                onScrollStopped = { stopTrigger++ }
            )
        }
        runOnIdleCompose {
            assertThat(startTrigger).isEqualTo(0)
            assertThat(stopTrigger).isEqualTo(0)
        }
        findByTag(scrollableBoxTag).doGesture {
            this.sendSwipe(
                start = this.center,
                end = PxPosition(this.center.x + 100f, this.center.y),
                duration = 100.milliseconds
            )
        }
        // don't wait for animation so stop is 0, as we flinging still
        runOnIdleCompose {
            assertThat(startTrigger).isEqualTo(1)
            assertThat(stopTrigger).isEqualTo(0)
        }
        advanceClockAndAwaitAnimation(state, clocks)
        // after wait we expect stop to trigger
        runOnIdleCompose {
            assertThat(startTrigger).isEqualTo(1)
            assertThat(stopTrigger).isEqualTo(1)
        }
    }

    @Test
    fun scrollable_disabledWontCallLambda() {
        var enabled = mutableStateOf(true)
        val clocks = ManualAnimationClock(0L)
        var total = 0f
        val state = ScrollableState(
            onScrollDeltaConsumptionRequested = {
                total += it
                it
            },
            flingConfig = FlingConfig(decayAnimation = ExponentialDecay()),
            animationClock = clocks
        )
        setScrollableContent {
            Modifier.scrollable(
                scrollableState = state,
                dragDirection = DragDirection.Horizontal,
                enabled = enabled.value
            )
        }
        findByTag(scrollableBoxTag).doGesture {
            this.sendSwipe(
                start = this.center,
                end = PxPosition(this.center.x + 100f, this.center.y),
                duration = 100.milliseconds
            )
        }
        advanceClockAndAwaitAnimation(state, clocks)
        val prevTotal = runOnIdleCompose {
            assertThat(total).isGreaterThan(0f)
            enabled.value = false
            total
        }
        findByTag(scrollableBoxTag).doGesture {
            this.sendSwipe(
                start = this.center,
                end = PxPosition(this.center.x + 100f, this.center.y),
                duration = 100.milliseconds
            )
        }
        advanceClockAndAwaitAnimation(state, clocks)
        runOnIdleCompose {
            assertThat(total).isEqualTo(prevTotal)
        }
    }

    @Test
    fun scrollable_velocityProxy() {
        var velocityTriggered = 0f
        val clocks = ManualAnimationClock(0L)
        var total = 0f
        val state = ScrollableState(
            onScrollDeltaConsumptionRequested = {
                total += it
                it
            },
            flingConfig = FlingConfig(decayAnimation = ExponentialDecay()),
            animationClock = clocks
        )
        setScrollableContent {
            Modifier.scrollable(
                scrollableState = state,
                dragDirection = DragDirection.Horizontal,
                onScrollStopped = { velocity ->
                    velocityTriggered = velocity
                }
            )
        }
        findByTag(scrollableBoxTag).doGesture {
            this.sendSwipeWithVelocity(
                start = this.center,
                end = PxPosition(this.center.x + 100f, this.center.y),
                endVelocity = 112f,
                duration = 100.milliseconds

            )
        }
        // don't advance clocks, so animation won't trigger yet
        // and interrupt
        findByTag(scrollableBoxTag).doGesture {
            this.sendSwipeWithVelocity(
                start = this.center,
                end = PxPosition(this.center.x - 100f, this.center.y),
                endVelocity = 312f,
                duration = 100.milliseconds

            )
        }
        runOnIdleCompose {
            // should be first velocity, as fling was disrupted
            assertThat(velocityTriggered - 112f).isLessThan(0.1f)
        }
    }

    @Test
    fun scrollable_startWithoutSlop_ifFlinging() {
        val clocks = ManualAnimationClock(0L)
        var total = 0f
        val state = ScrollableState(
            onScrollDeltaConsumptionRequested = {
                total += it
                it
            },
            flingConfig = FlingConfig(decayAnimation = ExponentialDecay()),
            animationClock = clocks
        )
        setScrollableContent {
            Modifier.scrollable(
                scrollableState = state,
                dragDirection = DragDirection.Horizontal
            )
        }
        findByTag(scrollableBoxTag).doGesture {
            this.sendSwipe(
                start = this.center,
                end = PxPosition(this.center.x + 100f, this.center.y),
                duration = 100.milliseconds
            )
        }
        // don't advance clocks
        val prevTotal = runOnUiThread {
            Truth.assertThat(total).isGreaterThan(0f)
            total
        }
        findByTag(scrollableBoxTag).doGesture {
            this.sendSwipe(
                start = this.center,
                end = PxPosition(this.center.x + 114f, this.center.y),
                duration = 100.milliseconds
            )
        }
        runOnIdleCompose {
            // last swipe should add exactly 114 as we don't advance clocks and already flinging
            val expected = prevTotal + 114
            assertThat(total - expected).isLessThan(0.1f)
        }
    }

    @Test
    fun scrollable_cancel_callsDragStop() {
        var total by mutableStateOf(0f)
        var dragStopped = 0f
        val clocks = ManualAnimationClock(0L)
        val state = ScrollableState(
            onScrollDeltaConsumptionRequested = {
                total += it
                it
            },
            flingConfig = FlingConfig(decayAnimation = ExponentialDecay()),
            animationClock = clocks
        )
        setScrollableContent {
            if (total < 20) {
                Modifier.scrollable(
                    scrollableState = state,
                    dragDirection = DragDirection.Horizontal,
                    onScrollStopped = {
                        dragStopped++
                    }
                )
            } else {
                Modifier
            }
        }
        findByTag(scrollableBoxTag).doGesture {
            this.sendSwipe(
                start = this.center,
                end = PxPosition(this.center.x + 100, this.center.y),
                duration = 100.milliseconds
            )
        }
        runOnIdleCompose {
            assertThat(total).isGreaterThan(0f)
            assertThat(dragStopped).isEqualTo(1f)
        }
    }

    @Test
    fun scrollable_snappingScrolling() {
        var total = 0f
        val clocks = ManualAnimationClock(0L)
        val state = ScrollableState(
            onScrollDeltaConsumptionRequested = {
                total += it
                it
            },
            flingConfig = FlingConfig(decayAnimation = ExponentialDecay()),
            animationClock = clocks
        )
        setScrollableContent {
            Modifier.scrollable(dragDirection = DragDirection.Vertical, scrollableState = state)
        }
        runOnIdleCompose {
            assertThat(total).isEqualTo(0f)
        }
        runOnIdleCompose {
            state.smoothScrollBy(1000f)
        }
        advanceClockAndAwaitAnimation(state, clocks)
        runOnIdleCompose {
            assertThat(total).isEqualTo(1000f)
        }
        runOnIdleCompose {
            state.smoothScrollBy(-200f)
        }
        advanceClockAndAwaitAnimation(state, clocks)
        runOnIdleCompose {
            assertThat(total).isEqualTo(800f)
        }
    }

    @Test
    fun scrollable_immediateDisposal() {
        val disposed = mutableStateOf(false)
        var total = 0f
        val clocks = ManualAnimationClock(0L)
        val state = ScrollableState(
            onScrollDeltaConsumptionRequested = {
                Truth.assertWithMessage("Animating after dispose!").that(disposed.value).isFalse()
                total += it
                it
            },
            flingConfig = FlingConfig(decayAnimation = ExponentialDecay()),
            animationClock = clocks
        )
        setScrollableContent {
            if (!disposed.value) {
                Modifier.scrollable(dragDirection = DragDirection.Vertical, scrollableState = state)
            } else {
                Modifier
            }
        }
        runOnUiThread {
            state.smoothScrollBy(1000f)
            disposed.value = true
        }
        advanceClockAndAwaitAnimation(state, clocks)
        runOnIdleCompose {
            assertThat(total).isEqualTo(0f)
        }
    }

    @Test
    fun scrollable_explicitDisposal() {
        val disposed = mutableStateOf(false)
        var total = 0f
        val clocks = ManualAnimationClock(0L)
        val state = ScrollableState(
            onScrollDeltaConsumptionRequested = {
                Truth.assertWithMessage("Animating after dispose!").that(disposed.value).isFalse()
                total += it
                it
            },
            flingConfig = FlingConfig(decayAnimation = ExponentialDecay()),
            animationClock = clocks
        )
        setScrollableContent {
            if (!disposed.value) {
                Modifier.scrollable(dragDirection = DragDirection.Vertical, scrollableState = state)
            } else {
                Modifier
            }
        }
        runOnIdleCompose {
            state.smoothScrollBy(300f)
        }
        advanceClockAndAwaitAnimation(state, clocks)
        runOnIdleCompose {
            assertThat(total).isEqualTo(300f)
        }
        runOnIdleCompose {
            state.smoothScrollBy(200f)
        }
        // don't advance clocks yet, toggle disposed value
        runOnUiThread {
            disposed.value = true
        }
        advanceClockAndAwaitAnimation(state, clocks)
        // still 300 and didn't fail in onScrollConsumptionRequested.. lambda
        runOnIdleCompose {
            assertThat(total).isEqualTo(300f)
        }
    }

    @Test
    fun scrollable_nestedDrag() {
        var innerDrag = 0f
        var outerDrag = 0f
        val clocks = ManualAnimationClock(0L)
        val outerState = ScrollableState(
            onScrollDeltaConsumptionRequested = {
                outerDrag += it
                it
            },
            flingConfig = FlingConfig(decayAnimation = ExponentialDecay()),
            animationClock = clocks
        )
        val innerState = ScrollableState(
            onScrollDeltaConsumptionRequested = {
                innerDrag += it / 2
                it / 2
            },
            flingConfig = FlingConfig(decayAnimation = ExponentialDecay()),
            animationClock = clocks
        )

        composeTestRule.setContent {
            Stack {
                Box(
                    gravity = ContentGravity.Center,
                    modifier = Modifier
                        .testTag(scrollableBoxTag)
                        .preferredSize(300.dp)
                        .scrollable(
                            scrollableState = outerState,
                            dragDirection = DragDirection.Horizontal
                        )
                ) {
                    Box(
                        modifier = Modifier.preferredSize(300.dp).scrollable(
                            scrollableState = innerState,
                            dragDirection = DragDirection.Horizontal
                        )
                    )
                }
            }
        }
        findByTag(scrollableBoxTag).doGesture {
            this.sendSwipe(
                start = this.center,
                end = PxPosition(this.center.x + 200f, this.center.y),
                duration = 300.milliseconds
            )
        }
        val lastEqualDrag = runOnIdleCompose {
            assertThat(innerDrag).isGreaterThan(0f)
            assertThat(outerDrag).isGreaterThan(0f)
            // we consumed half delta in child, so exactly half should go to the parent
            assertThat(outerDrag).isEqualTo(innerDrag)
            innerDrag
        }
        advanceClockAndAwaitAnimation(innerState, clocks)
        advanceClockAndAwaitAnimation(outerState, clocks)
        // and nothing should change as we don't do nested fling
        runOnIdleCompose {
            assertThat(outerDrag).isEqualTo(lastEqualDrag)
        }
    }

    private fun setScrollableContent(scrollableModifierFactory: @Composable () -> Modifier) {
        composeTestRule.setContent {
            Stack {
                val scrollable = scrollableModifierFactory()
                Box(modifier = Modifier
                    .testTag(scrollableBoxTag)
                    .preferredSize(100.dp) +
                        scrollable
                )
            }
        }
    }

    // TODO(b/147291885): This should not be needed in the future.
    private fun awaitScrollAnimation(state: ScrollableState) {
        val latch = CountDownLatch(1)
        val handler = Handler(Looper.getMainLooper())
        handler.post(object : Runnable {
            override fun run() {
                if (state.isAnimating) {
                    handler.post(this)
                } else {
                    latch.countDown()
                }
            }
        })
        Truth.assertWithMessage("Scroll didn't finish after 20 seconds")
            .that(latch.await(20, TimeUnit.SECONDS)).isTrue()
    }

    private fun advanceClockAndAwaitAnimation(state: ScrollableState, clock: ManualAnimationClock) {
        runOnIdleCompose {
            clock.clockTimeMillis += 5000
        }
        awaitScrollAnimation(state)
    }
}