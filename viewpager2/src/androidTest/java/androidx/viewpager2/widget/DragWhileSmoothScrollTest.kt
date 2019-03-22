/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.viewpager2.widget

import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.filters.LargeTest
import androidx.testutils.SwipeToLocation.flingToCenter
import androidx.viewpager2.widget.BaseTest.Context.SwipeMethod
import androidx.viewpager2.widget.DragWhileSmoothScrollTest.Event.OnPageScrollStateChangedEvent
import androidx.viewpager2.widget.DragWhileSmoothScrollTest.Event.OnPageScrolledEvent
import androidx.viewpager2.widget.DragWhileSmoothScrollTest.Event.OnPageSelectedEvent
import androidx.viewpager2.widget.DragWhileSmoothScrollTest.TestConfig
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_VERTICAL
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_DRAGGING
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_SETTLING
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.greaterThan
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/**
 * Tests what happens when a smooth scroll is interrupted by a drag
 */
@RunWith(Parameterized::class)
@LargeTest
class DragWhileSmoothScrollTest(private val config: TestConfig) : BaseTest() {
    data class TestConfig(
        val title: String,
        @ViewPager2.Orientation val orientation: Int,
        val startPage: Int = 0,
        val targetPage: Int,
        val dragInOppositeDirection: Boolean,
        val distanceToTargetWhenStartDrag: Float,
        val endInSnappedPosition: Boolean = false
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun spec(): List<TestConfig> = createTestSet()
    }

    @Test
    fun test() {
        config.apply {
            // given
            assertThat(distanceToTargetWhenStartDrag, greaterThan(0f))
            setUpTest(orientation).apply {
                val pageCount = max(startPage, targetPage) + 1
                setAdapterSync(viewAdapterProvider(stringSequence(pageCount)))
                viewPager.setCurrentItemSync(startPage, false, 2, SECONDS)

                val callback = viewPager.addNewRecordingCallback()
                val movingForward = targetPage > startPage

                // when we are close enough
                val waitTillCloseEnough = viewPager.addWaitForDistanceToTarget(targetPage,
                    distanceToTargetWhenStartDrag)
                runOnUiThread { viewPager.setCurrentItem(targetPage, true) }
                waitTillCloseEnough.await(1, SECONDS)

                // then perform a swipe
                if (endInSnappedPosition) {
                    onPage(withText("${pageToSnapTo(movingForward)}")).perform(flingToCenter())
                } else if (dragInOppositeDirection == movingForward) {
                    swipeBackward(SwipeMethod.MANUAL)
                } else {
                    swipeForward(SwipeMethod.MANUAL)
                }
                viewPager.addWaitForIdleLatch().await(2, SECONDS)

                // and check the result
                callback.apply {
                    assertThat(
                        "Unexpected sequence of state changes:" + dumpEvents(),
                        stateEvents.map { it.state },
                        equalTo(
                            if (expectIdleAfterDrag()) {
                                listOf(
                                    SCROLL_STATE_SETTLING,
                                    SCROLL_STATE_DRAGGING,
                                    SCROLL_STATE_IDLE
                                )
                            } else {
                                listOf(
                                    SCROLL_STATE_SETTLING,
                                    SCROLL_STATE_DRAGGING,
                                    SCROLL_STATE_SETTLING,
                                    SCROLL_STATE_IDLE
                                )
                            }
                        )
                    )

                    val currentlyVisible = viewPager.currentCompletelyVisibleItem
                    if (currentlyVisible == targetPage) {
                        // drag coincidentally landed us on the targetPage,
                        // this slightly changes the assertions
                        assertThat("viewPager.getCurrentItem() should be $targetPage",
                            viewPager.currentItem, equalTo(targetPage))
                        assertThat("Exactly 1 onPageSelected event should be fired",
                            selectEvents.size, equalTo(1))
                        assertThat("onPageSelected event should have reported $targetPage",
                            selectEvents.first().position, equalTo(targetPage))
                    } else {
                        assertThat("viewPager.getCurrentItem() should not be $targetPage",
                            viewPager.currentItem, not(equalTo(targetPage)))
                        assertThat("Exactly 2 onPageSelected events should be fired",
                            selectEvents.size, equalTo(2))
                        assertThat("First onPageSelected event should have reported $targetPage",
                            selectEvents.first().position, equalTo(targetPage))
                        assertThat("Second onPageSelected event should have reported " +
                                "$currentlyVisible, or visible page should be " +
                                "${selectEvents.last().position}",
                            selectEvents.last().position, equalTo(currentlyVisible))
                    }
                }
            }
        }
    }

    private fun ViewPager2.addNewRecordingCallback(): RecordingCallback {
        return RecordingCallback().also { registerOnPageChangeCallback(it) }
    }

    private fun TestConfig.pageToSnapTo(movingForward: Boolean): Int {
        val positionToStartDragging = if (movingForward) {
            targetPage - distanceToTargetWhenStartDrag
        } else {
            targetPage + distanceToTargetWhenStartDrag
        }
        return if (movingForward == dragInOppositeDirection) {
            floor(positionToStartDragging).toInt()
        } else {
            ceil(positionToStartDragging).toInt()
        }
    }

    private sealed class Event {
        data class OnPageScrolledEvent(
            val position: Int,
            val positionOffset: Float,
            val positionOffsetPixels: Int
        ) : Event()
        data class OnPageSelectedEvent(val position: Int) : Event()
        data class OnPageScrollStateChangedEvent(val state: Int) : Event()
    }

    private class RecordingCallback : ViewPager2.OnPageChangeCallback() {
        private val events = mutableListOf<Event>()

        val stateEvents get() = events.mapNotNull { it as? OnPageScrollStateChangedEvent }
        val selectEvents get() = events.mapNotNull { it as? OnPageSelectedEvent }

        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            synchronized(events) {
                events.add(OnPageScrolledEvent(position, positionOffset, positionOffsetPixels))
            }
        }

        override fun onPageSelected(position: Int) {
            synchronized(events) {
                events.add(OnPageSelectedEvent(position))
            }
        }

        override fun onPageScrollStateChanged(state: Int) {
            synchronized(events) {
                events.add(OnPageScrollStateChangedEvent(state))
            }
        }

        fun expectIdleAfterDrag(): Boolean {
            val lastScrollEvent = events
                .dropWhile { it != OnPageScrollStateChangedEvent(SCROLL_STATE_DRAGGING) }.drop(1)
                .takeWhile { it is OnPageScrolledEvent }
                .lastOrNull() as? OnPageScrolledEvent
            return lastScrollEvent?.let { it.positionOffsetPixels == 0 } ?: false
        }

        fun dumpEvents(): String {
            return events.joinToString("\n- ", "\n(${scrollStateGlossary()})\n- ")
        }
    }
}

// region Test Suite creation

private fun createTestSet(): List<TestConfig> {
    return listOf(ORIENTATION_HORIZONTAL, ORIENTATION_VERTICAL).flatMap { orientation ->
        listOf(true, false).flatMap { dragInOppositeDirection ->
            listOf(0.4f, 1.5f).flatMap { distanceToTarget ->
                listOf(true, false).flatMap { endInSnappedPosition ->
                    listOf(
                        TestConfig(
                            title = "forward",
                            orientation = orientation,
                            startPage = 0,
                            targetPage = 4,
                            dragInOppositeDirection = dragInOppositeDirection,
                            distanceToTargetWhenStartDrag = distanceToTarget,
                            endInSnappedPosition = endInSnappedPosition
                        ),
                        TestConfig(
                            title = "backward",
                            orientation = orientation,
                            startPage = 8,
                            targetPage = 4,
                            dragInOppositeDirection = dragInOppositeDirection,
                            distanceToTargetWhenStartDrag = distanceToTarget,
                            endInSnappedPosition = endInSnappedPosition
                        )
                    )
                }
            }
        }.plus(listOf(
            TestConfig(
                title = "drag back to start",
                orientation = orientation,
                startPage = 0,
                targetPage = 1,
                dragInOppositeDirection = true,
                distanceToTargetWhenStartDrag = .7f
            )
        ))
    }
}

// endregion
