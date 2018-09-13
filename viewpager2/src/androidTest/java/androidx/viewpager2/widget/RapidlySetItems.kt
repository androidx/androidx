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

import android.os.SystemClock
import androidx.test.filters.LargeTest
import androidx.testutils.PollingCheck
import androidx.viewpager2.widget.RapidlySetItems.Event.OnPageScrollStateChangedEvent
import androidx.viewpager2.widget.RapidlySetItems.Event.OnPageScrolledEvent
import androidx.viewpager2.widget.RapidlySetItems.Event.OnPageSelectedEvent
import androidx.viewpager2.widget.ViewPager2.Orientation.HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.Orientation.VERTICAL
import androidx.viewpager2.widget.ViewPager2.ScrollState.DRAGGING
import org.hamcrest.CoreMatchers.anyOf
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Random

private const val randomTesting = false // change to true to enable random tests
private const val randomTestsPerConfig = 1 // increase to have more random tests generated

/**
 * Tests if rapidly setting the current item to different pages is handled correctly by ViewPager2.
 * Tests with a wide combination of smooth scrolls and instant scrolls that are fired at a rate of
 * 10 per second (100ms in between calls).
 *
 * Enable randomTesting to add random tests to the test suite.
 */
@RunWith(Parameterized::class)
@LargeTest
class RapidlySetItems(private val config: RapidlySetItemsConfig) : BaseTest() {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun spec(): List<RapidlySetItemsConfig> = createTestSet()
    }

    @Test
    fun test() {
        config.apply {
            // given
            setUpTest(totalPages, orientation).apply {
                viewPager.clearOnPageChangeListeners()
                val listener = viewPager.addNewRecordingListener()
                var currentPage = viewPager.currentItem

                // when
                pageSequence.forEachIndexed { i, targetPage ->
                    runOnUiThread {
                        viewPager.setCurrentItem(targetPage, i !in noSmoothScrolls)
                        viewPager.assertCurrentItemSet(targetPage)
                        if (currentPage != targetPage) {
                            listener.assertTargetPageSelected(targetPage)
                        } else {
                            listener.assertTargetPagePreviouslySelected(targetPage)
                        }
                        currentPage = targetPage
                    }
                    SystemClock.sleep(100)
                }
                PollingCheck.waitFor(2000) {
                    listener.testFinished
                }

                // then
                listener.apply {
                    assertThat(draggingIx, equalTo(-1))
                    assertThat(lastState?.state, equalTo(0))
                    assertThat(lastScroll?.position, equalTo(lastSelect?.position))
                    assertThat(lastScroll?.positionOffsetPixels, equalTo(0))
                    assertThatStateEventsDoNotRepeat()
                    assertScrollTowardsSelectedPage()
                }
            }
        }
    }

    private fun ViewPager2.addNewRecordingListener(): RecordingListener {
        val listener = RecordingListener()
        addOnPageChangeListener(listener)
        return listener
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
        val events = mutableListOf<Event>()

        val stateEvents get() = events.mapNotNull { it as? OnPageScrollStateChangedEvent }
        val lastEvent get() = events.last()
        val lastState get() = events.findLast { it is OnPageScrollStateChangedEvent }
                as? OnPageScrollStateChangedEvent
        val lastScroll get() = events.findLast { it is OnPageScrolledEvent } as? OnPageScrolledEvent
        val lastSelect get() = events.findLast { it is OnPageSelectedEvent } as? OnPageSelectedEvent
        val draggingIx get() = events.indexOf(OnPageScrollStateChangedEvent(DRAGGING))

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

    private val RecordingListener.testFinished get() = synchronized(events) {
        lastState?.state == 0 &&
                lastSelect?.position == lastScroll?.position &&
                lastScroll?.positionOffsetPixels == 0
    }

    private fun RecordingListener.assertTargetPageSelected(targetPage: Int) {
        assertThat(lastEvent, instanceOf(OnPageSelectedEvent::class.java))
        val selectedEvent = lastEvent as Event.OnPageSelectedEvent
        assertThat(selectedEvent.position, equalTo(targetPage))
    }

    private fun RecordingListener.assertTargetPagePreviouslySelected(page: Int) {
        assertThat(lastSelect?.position, anyOf(nullValue(), equalTo(page)))
    }

    private fun RecordingListener.assertThatStateEventsDoNotRepeat() {
        stateEvents.zipWithNext { a, b ->
            assertThat("State transition to same state found", a.state, not(equalTo(b.state)))
        }
    }

    private fun RecordingListener.assertScrollTowardsSelectedPage() {
        var target = 0
        var prevPosition = 0f
        events.forEach {
            when (it) {
                is OnPageSelectedEvent -> target = it.position
                is Event.OnPageScrolledEvent -> {
                    val currentPosition = it.position + it.positionOffset
                    assertThat(
                        "Scroll event fired before page selected event",
                        target, not(equalTo(-1))
                    )
                    assertThat(
                        "Scroll event not between start and destination",
                        currentPosition, isBetweenInInMinMax(prevPosition, target.toFloat())
                    )
                    prevPosition = currentPosition
                }
            }
        }
    }

    private fun ViewPager2.assertCurrentItemSet(targetPage: Int) {
        assertThat(currentItem, equalTo(targetPage))
    }
}

// region Parameter definition

data class RapidlySetItemsConfig(
    val title: String,
    @ViewPager2.Orientation val orientation: Int,
    val totalPages: Int,
    val pageSequence: List<Int>,
    val noSmoothScrolls: List<Int> = emptyList()
) {
    override fun toString(): String {
        return "$title-" +
                (if (orientation == HORIZONTAL) "hor-" else "ver-") +
                "pages_$totalPages-" +
                "seq_${pageSequence.joinToString("_")}-" +
                "not_smooth_${noSmoothScrolls.joinToString("_")}"
    }
}

// endregion

// region Test Suite creation

private fun createTestSet(): List<RapidlySetItemsConfig> {
    return listOf(HORIZONTAL, VERTICAL).flatMap { orientation -> createTestSet(orientation) }
//        .plus(listOf(
//            recreateRandomTest(VERTICAL, 6303260983100342208L, "example_test")
//        ))
}

private fun createTestSet(orientation: Int): List<RapidlySetItemsConfig> {
    return listOf(
        RapidlySetItemsConfig(
            title = "cone-increasing-slow",
            orientation = orientation,
            totalPages = 10,
            pageSequence = listOf(1, 0, 2, 1, 3, 1, 4, 2, 5, 2, 6, 3, 7, 3, 8, 4, 9)
        ),
        RapidlySetItemsConfig(
            title = "cone-increasing-fast",
            orientation = orientation,
            totalPages = 19,
            pageSequence = listOf(2, 1, 4, 2, 6, 3, 8, 4, 10)
        ),
        RapidlySetItemsConfig(
            title = "cone-decreasing-slow",
            orientation = orientation,
            totalPages = 10,
            pageSequence = listOf(9, 8, 9, 7, 8, 6, 8, 5, 7, 4, 7, 3, 6, 2, 6, 1, 5, 0)
        ),
        RapidlySetItemsConfig(
            title = "cone-decreasing-fast",
            orientation = orientation,
            totalPages = 11,
            pageSequence = listOf(10, 8, 9, 6, 8, 4, 7, 2, 6, 0)
        ),
        RapidlySetItemsConfig(
            title = "regression-hump-positive",
            orientation = orientation,
            totalPages = 10,
            pageSequence = listOf(7, 6, 0, 7, 6, 0, 7, 6)
        ),
        RapidlySetItemsConfig(
            title = "regression-hump-negative",
            orientation = orientation,
            totalPages = 10,
            pageSequence = listOf(8, 2, 3, 8, 2, 3, 8, 2, 3)
        ),
        RapidlySetItemsConfig(
            title = "regression-do-not-jump-forward",
            orientation = orientation,
            totalPages = 10,
            pageSequence = listOf(3, 6, 9, 5)
        ),
        RapidlySetItemsConfig(
            title = "random-starts-with-noSmooth",
            orientation = orientation,
            totalPages = 12,
            pageSequence = listOf(5, 11, 3, 8, 0, 10, 9, 7, 0, 4),
            noSmoothScrolls = listOf(0, 1, 2, 8)
        ),
        RapidlySetItemsConfig(
            title = "random-ends-with-noSmooth",
            orientation = orientation,
            totalPages = 12,
            pageSequence = listOf(2, 7, 10, 1, 6, 10, 2, 8, 9, 6),
            noSmoothScrolls = listOf(1, 7, 9)
        ),
        RapidlySetItemsConfig(
            title = "random-ends-with-double-noSmooth",
            orientation = orientation,
            totalPages = 12,
            pageSequence = listOf(8, 7, 9, 7, 3, 0, 7, 11, 10, 0),
            noSmoothScrolls = listOf(1, 4, 5, 8, 9)
        )
    )
    .plus(
        if (randomTesting) {
            List(randomTestsPerConfig) { createRandomTest(orientation) }
        } else {
            emptyList()
        }
    )
}

// endregion

// region Random test creation

@Suppress("unused")
private fun createRandomTest(orientation: Int): RapidlySetItemsConfig {
    return recreateRandomTest(orientation, generatePositiveSeed())
}

private fun recreateRandomTest(
    orientation: Int,
    seed: Long,
    name: String? = null,
    numScrolls: Int = 10,
    numPages: Int = 12,
    noSmoothScrollPr: Float = .3f
): RapidlySetItemsConfig {
    val seeds = Random(seed)
    return RapidlySetItemsConfig(
        title = "${name ?: "random"}_seed_${seed}L",
        orientation = orientation,
        totalPages = numPages,
        pageSequence = generateRandomSequence(seeds.nextLong().inv(), numScrolls, numPages),
        noSmoothScrolls = pickIndices(seeds.nextLong().inv(), noSmoothScrollPr, numScrolls)
    )
}

private fun generatePositiveSeed(): Long {
    return Random().nextLong() and (1L shl 63).inv()
}

private fun generateRandomSequence(seed: Long, len: Int, max: Int): List<Int> {
    val r = Random(seed)
    return List(len) { r.nextInt(max) }
}

private fun pickIndices(seed: Long, probability: Float, numScrolls: Int): List<Int> {
    val r = Random(seed)
    return (0 until numScrolls).filter { r.nextFloat() < probability }
}

// endregion