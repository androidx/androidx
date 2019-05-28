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
import androidx.viewpager2.widget.SetItemWhileScrollInProgressTest.Event.OnPageScrollStateChangedEvent
import androidx.viewpager2.widget.SetItemWhileScrollInProgressTest.Event.OnPageScrolledEvent
import androidx.viewpager2.widget.SetItemWhileScrollInProgressTest.Event.OnPageSelectedEvent
import androidx.viewpager2.widget.SetItemWhileScrollInProgressTest.TestConfig
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_VERTICAL
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_DRAGGING
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

private const val RANDOM_TESTS_PER_CONFIG = 0 // increase to have random tests generated

/**
 * Tests if rapidly setting the current item to different pages is handled correctly by ViewPager2.
 * Tests with a wide combination of smooth scrolls and instant scrolls that are fired at a rate of
 * 10 per second (100ms in between calls).
 *
 * Enable randomTesting to add random tests to the test suite.
 *
 * Here are some example traces from ViewPager, which should look similar for ViewPager2. Note the
 * patterns in the non-scroll events, and those in the scroll events.
 *
 * 0 -> 3 (smooth) -> 0 (smooth)
 * >> setCurrentItem(3, true);
 * onPageScrollStateChanged(2)
 * onPageSelected(3)
 * onPageScrolled(0, 0.253704, 274)
 * onPageScrolled(0, 0.798148, 862)
 * onPageScrolled(1, 0.228704, 247)
 * onPageScrolled(1, 0.629630, 680)
 * onPageScrolled(1, 0.921296, 994)
 * onPageScrolled(2, 0.175000, 188)
 * onPageScrolled(2, 0.377778, 408)
 * onPageScrolled(2, 0.538889, 582)
 * onPageScrolled(2, 0.658333, 710)
 * onPageScrolled(2, 0.756481, 816)
 * onPageScrolled(2, 0.831481, 898)
 * onPageScrolled(2, 0.886111, 956)
 * >> setCurrentItem(0, true);
 * onPageSelected(0)
 * onPageScrolled(2, 0.704630, 761)
 * onPageScrolled(2, 0.185185, 200)
 * onPageScrolled(1, 0.724074, 782)
 * onPageScrolled(1, 0.343518, 370)
 * onPageScrolled(1, 0.034259, 36)
 * onPageScrolled(0, 0.784259, 847)
 * onPageScrolled(0, 0.595370, 643)
 * onPageScrolled(0, 0.437037, 472)
 * onPageScrolled(0, 0.313889, 339)
 * onPageScrolled(0, 0.220370, 238)
 * onPageScrolled(0, 0.154630, 167)
 * onPageScrolled(0, 0.102778, 111)
 * onPageScrolled(0, 0.065741, 71)
 * onPageScrolled(0, 0.039815, 43)
 * onPageScrolled(0, 0.023148, 25)
 * onPageScrolled(0, 0.012963, 13)
 * onPageScrolled(0, 0.006481, 6)
 * onPageScrolled(0, 0.002778, 3)
 * onPageScrolled(0, 0.000926, 1)
 * onPageScrolled(0, 0.000000, 0)
 * onPageScrollStateChanged(0)
 *
 * 0 -> 3 (smooth) -> 3 (smooth)
 * >> setCurrentItem(3, true);
 * onPageScrollStateChanged(2)
 * onPageSelected(3)
 * onPageScrolled(0, 0.489815, 529)
 * onPageScrolled(0, 0.995370, 1075)
 * onPageScrolled(1, 0.416667, 449)
 * onPageScrolled(1, 0.763889, 824)
 * onPageScrolled(2, 0.047222, 50)
 * onPageScrolled(2, 0.263889, 284)
 * onPageScrolled(2, 0.449074, 484)
 * onPageScrolled(2, 0.595370, 642)
 * onPageScrolled(2, 0.708333, 764)
 * onPageScrolled(2, 0.794445, 858)
 * onPageScrolled(2, 0.855556, 924)
 * onPageScrolled(2, 0.903704, 976)
 * >> setCurrentItem(3, true);
 * onPageScrolled(2, 0.937963, 1013)
 * onPageScrolled(2, 0.962037, 1039)
 * onPageScrolled(2, 0.976852, 1055)
 * onPageScrolled(2, 0.987037, 1065)
 * onPageScrolled(2, 0.993519, 1073)
 * onPageScrolled(2, 0.997222, 1077)
 * onPageScrolled(2, 0.999074, 1078)
 * onPageScrolled(3, 0.000000, 0)
 * onPageScrollStateChanged(0)
 */
@RunWith(Parameterized::class)
@LargeTest
class SetItemWhileScrollInProgressTest(private val config: TestConfig) : BaseTest() {
    data class TestConfig(
        val title: String,
        @ViewPager2.Orientation val orientation: Int,
        val totalPages: Int,
        val pageSequence: List<Int>,
        val instantScrolls: Set<Int> = emptySet()
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun spec(): List<TestConfig> = createTestSet()
    }

    override fun setUp() {
        super.setUp()
        assumeApiBeforeQ()
    }

    @Test
    fun test() {
        config.apply {
            // given
            setUpTest(orientation).apply {
                setAdapterSync(viewAdapterProvider(stringSequence(totalPages)))
                val callback = viewPager.addNewRecordingCallback()
                var currentPage = viewPager.currentItem

                // when
                pageSequence.forEachIndexed { i, targetPage ->
                    runOnUiThread {
                        viewPager.setCurrentItem(targetPage, i !in instantScrolls)
                        viewPager.assertCurrentItemSet(targetPage)
                        if (currentPage != targetPage) {
                            callback.assertPageSelectedEventFired(targetPage)
                        } else {
                            callback.assertNoNewPageSelectedEventFired(targetPage)
                        }
                        currentPage = targetPage
                    }
                    SystemClock.sleep(100)
                }
                PollingCheck.waitFor(2000) {
                    callback.isTestFinished
                }

                // then
                callback.apply {
                    assertThat(draggingIx, equalTo(-1))
                    assertThat(lastState?.state, equalTo(0))
                    assertThat(lastScroll?.position, equalTo(lastSelect?.position))
                    assertThat(lastScroll?.positionOffsetPixels, equalTo(0))
                    assertStateEventsDoNotRepeat()
                    assertScrollTowardsSelectedPage()
                }

                viewPager.unregisterOnPageChangeCallback(callback)
            }
        }
    }

    private fun ViewPager2.addNewRecordingCallback(): RecordingCallback {
        return RecordingCallback().also { registerOnPageChangeCallback(it) }
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
        val events = mutableListOf<Event>()

        val stateEvents get() = events.mapNotNull { it as? OnPageScrollStateChangedEvent }
        val lastEvent get() = events.last()
        val lastState get() = events.findLast { it is OnPageScrollStateChangedEvent }
                as? OnPageScrollStateChangedEvent
        val lastScroll get() = events.findLast { it is OnPageScrolledEvent } as? OnPageScrolledEvent
        val lastSelect get() = events.findLast { it is OnPageSelectedEvent } as? OnPageSelectedEvent
        val draggingIx get() = events.indexOf(OnPageScrollStateChangedEvent(SCROLL_STATE_DRAGGING))

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

    private val RecordingCallback.isTestFinished get() = synchronized(events) {
        lastState?.state == 0 &&
                lastSelect?.position == lastScroll?.position &&
                lastScroll?.positionOffsetPixels == 0
    }

    private fun RecordingCallback.assertPageSelectedEventFired(targetPage: Int) {
        assertThat(lastEvent, instanceOf(OnPageSelectedEvent::class.java))
        val selectedEvent = lastEvent as Event.OnPageSelectedEvent
        assertThat(selectedEvent.position, equalTo(targetPage))
    }

    private fun RecordingCallback.assertNoNewPageSelectedEventFired(targetPage: Int) {
        assertThat(lastSelect?.position, anyOf(nullValue(), equalTo(targetPage)))
    }

    private fun RecordingCallback.assertStateEventsDoNotRepeat() {
        stateEvents.zipWithNext { a, b ->
            assertThat("State transition to same state found", a.state, not(equalTo(b.state)))
        }
    }

    private fun RecordingCallback.assertScrollTowardsSelectedPage() {
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

// region Test Suite creation

private fun createTestSet(): List<TestConfig> {
    return listOf(
        ORIENTATION_HORIZONTAL,
        ORIENTATION_VERTICAL
    ).flatMap { orientation -> createTestSet(orientation) }
}

private fun createTestSet(orientation: Int): List<TestConfig> {
    return listOf(
        TestConfig(
            title = "cone-increasing-slow",
            orientation = orientation,
            totalPages = 10,
            pageSequence = listOf(1, 0, 2, 1, 3, 1, 4, 2, 5, 2, 6, 3, 7, 3, 8, 4, 9)
        ),
        TestConfig(
            title = "cone-increasing-fast",
            orientation = orientation,
            totalPages = 19,
            pageSequence = listOf(2, 1, 4, 2, 6, 3, 8, 4, 10)
        ),
        TestConfig(
            title = "cone-decreasing-slow",
            orientation = orientation,
            totalPages = 10,
            pageSequence = listOf(9, 8, 9, 7, 8, 6, 8, 5, 7, 4, 7, 3, 6, 2, 6, 1, 5, 0)
        ),
        TestConfig(
            title = "cone-decreasing-fast",
            orientation = orientation,
            totalPages = 11,
            pageSequence = listOf(10, 8, 9, 6, 8, 4, 7, 2, 6, 0)
        ),
        TestConfig(
            title = "regression-hump-positive",
            orientation = orientation,
            totalPages = 10,
            pageSequence = listOf(7, 6, 0, 7, 6, 0, 7, 6)
        ),
        TestConfig(
            title = "regression-hump-negative",
            orientation = orientation,
            totalPages = 10,
            pageSequence = listOf(8, 2, 3, 8, 2, 3, 8, 2, 3)
        ),
        TestConfig(
            title = "regression-do-not-jump-forward",
            orientation = orientation,
            totalPages = 10,
            pageSequence = listOf(3, 6, 9, 5)
        ),
        TestConfig(
            title = "random-starts-with-noSmooth",
            orientation = orientation,
            totalPages = 12,
            pageSequence = listOf(5, 11, 3, 8, 0, 10, 9, 7, 0, 4),
            instantScrolls = setOf(0, 1, 2, 8)
        ),
        TestConfig(
            title = "random-ends-with-noSmooth",
            orientation = orientation,
            totalPages = 12,
            pageSequence = listOf(2, 7, 10, 1, 6, 10, 2, 8, 9, 6),
            instantScrolls = setOf(1, 7, 9)
        ),
        TestConfig(
            title = "random-ends-with-double-noSmooth",
            orientation = orientation,
            totalPages = 12,
            pageSequence = listOf(8, 7, 9, 7, 3, 0, 7, 11, 10, 0),
            instantScrolls = setOf(1, 4, 5, 8, 9)
        )
    )
    .plus(
        List(RANDOM_TESTS_PER_CONFIG) { createRandomTest(orientation) }
    )
    // To rerun a failed random test, lookup the seed and the orientation of the test in the test
    // output, give it a name and add the following code to createTestSet():
    //   .plus(listOf(
    //       recreateRandomTest(<orientation>, <seed>L, "<name>")
    //   ))
}

// endregion

// region Random test creation

@Suppress("unused")
private fun createRandomTest(orientation: Int): TestConfig {
    return recreateRandomTest(orientation, generateSeed())
}

private fun recreateRandomTest(
    orientation: Int,
    seed: Long,
    name: String? = null,
    numScrolls: Int = 10,
    numPages: Int = 12,
    noSmoothScrollPr: Float = .3f
): TestConfig {
    val r = Random(seed)
    return TestConfig(
        title = "${name ?: "random"}_seed_${seed}L",
        orientation = orientation,
        totalPages = numPages,
        pageSequence = generateRandomSequence(r, numScrolls, numPages),
        instantScrolls = pickIndices(r, noSmoothScrollPr, numScrolls)
    )
}

private fun generateSeed(): Long {
    return Random().nextLong()
}

private fun generateRandomSequence(r: Random, len: Int, max: Int): List<Int> {
    return List(len) { r.nextInt(max) }
}

private fun pickIndices(r: Random, probability: Float, seqLen: Int): Set<Int> {
    return (0 until seqLen).filter { r.nextFloat() < probability }.toSet()
}

// endregion