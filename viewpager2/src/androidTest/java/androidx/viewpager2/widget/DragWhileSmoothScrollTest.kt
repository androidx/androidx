/*
 * Copyright (C) 2018 The Android Open Source Project
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

import androidx.test.filters.LargeTest
import androidx.viewpager2.widget.DragWhileSmoothScrollTest.Event.OnPageScrollStateChangedEvent
import androidx.viewpager2.widget.DragWhileSmoothScrollTest.Event.OnPageScrolledEvent
import androidx.viewpager2.widget.DragWhileSmoothScrollTest.Event.OnPageSelectedEvent
import androidx.viewpager2.widget.DragWhileSmoothScrollTest.TestConfig
import androidx.viewpager2.widget.ViewPager2.Orientation.HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.Orientation.VERTICAL
import androidx.viewpager2.widget.ViewPager2.ScrollState.DRAGGING
import androidx.viewpager2.widget.ViewPager2.ScrollState.IDLE
import androidx.viewpager2.widget.ViewPager2.ScrollState.SETTLING
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.greaterThan
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.math.abs
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
        val distanceToTargetWhenStartDrag: Float
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
                setAdapterSync(viewAdapterProvider(stringSequence(max(startPage, targetPage) + 1)))
                if (viewPager.currentItem != startPage) {
                    val latch = viewPager.addWaitForIdleLatch()
                    runOnUiThread { viewPager.setCurrentItem(startPage, false) }
                    latch.await(2, SECONDS)
                }
                val listener = viewPager.addNewRecordingListener()
                val movingForward = targetPage > startPage

                // when
                val waitTillCloseEnough = viewPager.addWaitForDistanceToTarget(targetPage,
                    distanceToTargetWhenStartDrag)
                runOnUiThread { viewPager.setCurrentItem(targetPage, true) }
                waitTillCloseEnough.await(1, SECONDS)

                if (dragInOppositeDirection == movingForward) {
                    swiper.swipePrevious()
                } else {
                    swiper.swipeNext()
                }
                viewPager.addWaitForIdleLatch().await(2, SECONDS)

                // then
                listener.apply {
                    assertThat(stateEvents.map { it.state },
                        equalTo(listOf(SETTLING, DRAGGING, SETTLING, IDLE)))

                    val currentlyVisible = viewPager.currentCompletelyVisibleItem
                    if (currentlyVisible == targetPage) {
                        // drag coincidentally landed us on the targetPage,
                        // this slightly changes the assertions
                        assertThat(viewPager.currentItem, equalTo(targetPage))
                        assertThat(selectEvents.size, equalTo(1))
                        assertThat(selectEvents.first().position, equalTo(targetPage))
                    } else {
                        assertThat(viewPager.currentItem, not(equalTo(targetPage)))
                        assertThat(selectEvents.size, equalTo(2))
                        assertThat(selectEvents.first().position, equalTo(targetPage))
                        assertThat(selectEvents.last().position, equalTo(currentlyVisible))
                    }
                }
            }
        }
    }

    private fun ViewPager2.addNewRecordingListener(): RecordingListener {
        return RecordingListener().also { addOnPageChangeListener(it) }
    }

    private fun ViewPager2.addWaitForDistanceToTarget(
        target: Int,
        distance: Float
    ): CountDownLatch {
        val latch = CountDownLatch(1)

        addOnPageChangeListener(object : ViewPager2.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                if (abs(target - position - positionOffset) <= distance) {
                    latch.countDown()
                    post { removeOnPageChangeListener(this) }
                }
            }

            override fun onPageSelected(position: Int) {
            }

            override fun onPageScrollStateChanged(state: Int) {
            }
        })

        return latch
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

    private class RecordingListener : ViewPager2.OnPageChangeListener {
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
    }
}

// region Test Suite creation

private fun createTestSet(): List<TestConfig> {
    return listOf(HORIZONTAL, VERTICAL).flatMap { orientation ->
        listOf(true, false).flatMap { dragInOppositeDirection ->
            listOf(0.4f, 1.5f).flatMap { distanceToTarget ->
                createTestSet(orientation, dragInOppositeDirection, distanceToTarget)
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

private fun createTestSet(
    orientation: Int,
    dragInOppositeDirection: Boolean,
    distanceToTarget: Float
): List<TestConfig> {
    return listOf(
        TestConfig(
            title = "forward",
            orientation = orientation,
            startPage = 0,
            targetPage = 4,
            dragInOppositeDirection = dragInOppositeDirection,
            distanceToTargetWhenStartDrag = distanceToTarget
        ),
        TestConfig(
            title = "backward",
            orientation = orientation,
            startPage = 8,
            targetPage = 4,
            dragInOppositeDirection = dragInOppositeDirection,
            distanceToTargetWhenStartDrag = distanceToTarget
        )
    )
}

// endregion
