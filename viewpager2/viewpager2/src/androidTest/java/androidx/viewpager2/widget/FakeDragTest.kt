/*
 * Copyright 2019 The Android Open Source Project
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

import android.graphics.Path
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD
import androidx.core.view.animation.PathInterpolatorCompat
import androidx.test.filters.LargeTest
import androidx.testutils.LocaleTestUtils
import androidx.viewpager2.widget.BaseTest.Context.SwipeMethod
import androidx.viewpager2.widget.FakeDragTest.Event.OnPageScrollStateChangedEvent
import androidx.viewpager2.widget.FakeDragTest.Event.OnPageScrolledEvent
import androidx.viewpager2.widget.FakeDragTest.Event.OnPageSelectedEvent
import androidx.viewpager2.widget.FakeDragTest.TestConfig
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_VERTICAL
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_DRAGGING as DRAGGING
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE as IDLE
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_SETTLING as SETTLING
import androidx.viewpager2.widget.swipe.PageSwiperFakeDrag
import androidx.viewpager2.widget.swipe.PageSwiperManual
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors.newSingleThreadExecutor
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.math.min
import kotlin.math.roundToInt
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.greaterThan
import org.junit.Assume.assumeThat
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@LargeTest
class FakeDragTest(private val config: TestConfig) : BaseTest() {
    data class TestConfig(
        @ViewPager2.Orientation val orientation: Int,
        val rtl: Boolean,
        val enableUserInput: Boolean,
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun spec(): List<TestConfig> = createTestSet()

        val mScrollEventAdapterField: Field
        val getRelativeScrollPositionMethod: Method

        init {
            mScrollEventAdapterField =
                ViewPager2::class.java.getDeclaredField("mScrollEventAdapter")
            mScrollEventAdapterField.isAccessible = true
            getRelativeScrollPositionMethod =
                ScrollEventAdapter::class.java.getDeclaredMethod("getRelativeScrollPosition")
            getRelativeScrollPositionMethod.isAccessible = true
        }
    }

    private val pageCount = 10
    private lateinit var test: Context
    private lateinit var adapterProvider: AdapterProvider
    private lateinit var fakeDragger: PageSwiperFakeDrag

    // Used to overcome touch slop and gently slide forward.
    // Similar to but better than DecelerateInterpolator in this case.
    private val fastDecelerateInterpolator =
        PathInterpolatorCompat.create(Path().also { it.cubicTo(0f, .7f, 0f, 1f, 1f, 1f) })

    override fun setUp() {
        super.setUp()
        if (config.rtl) {
            localeUtil.resetLocale()
            localeUtil.setLocale(LocaleTestUtils.RTL_LANGUAGE)
        }
        adapterProvider = viewAdapterProvider.provider(stringSequence(pageCount))
        test =
            setUpTest(config.orientation).also {
                fakeDragger = PageSwiperFakeDrag(it.viewPager) { it.viewPager.pageSize }
                it.viewPager.isUserInputEnabled = config.enableUserInput
                it.setAdapterSync(adapterProvider)
                it.assertBasicState(0)
            }
    }

    @Test
    fun test_flingToNextPage() {
        basicFakeDragTest(.6f, 100, 1, suppressFling = false)
    }

    @Test
    fun test_peekNextPage() {
        basicFakeDragTest(.1f, 200, 0, DecelerateInterpolator(), true)
    }

    @Test
    fun test_flingCompletelyToNextPage() {
        basicFakeDragTest(1f, 100, 1, suppressFling = false)
    }

    @Test
    @Ignore("b/280670752")
    fun test_peekNextAndMoveBack() {
        // Roughly interpolates like this:
        //   |
        // 3 |   .-.
        //   |  /   ',
        // 1 | /      '-.___
        //   |/
        // 0 +--------------
        //   0             1
        basicFakeDragTest(
            .2f,
            300,
            0,
            PathInterpolatorCompat.create(
                Path().also {
                    it.moveTo(0f, 0f)
                    it.cubicTo(.4f, 6f, .5f, 1f, .8f, 1f)
                    it.lineTo(1f, 1f)
                }
            ),
            true,
        )
    }

    @Test
    fun test_dragAlmostToNextPageAndFlingBack() {
        // Roughly interpolates like this:
        //   |
        //   |   .-.
        // 1 |  /   '
        //   | /
        //   |/
        // 0 +-------
        //   0      1
        basicFakeDragTest(
            .4f,
            200,
            0,
            PathInterpolatorCompat.create(
                Path().also {
                    it.moveTo(0f, 0f)
                    it.cubicTo(.4f, 1.7f, .7f, 1.7f, 1f, 1f)
                }
            ),
            false,
        )
    }

    @Test
    fun test_startFakeDragDuringManualDrag() {
        // Skip tests where manual dragging is disabled
        assumeThat(config.enableUserInput, equalTo(true))

        val pageSwiper = PageSwiperManual(test.viewPager)
        try {
            // start manual drag
            val latch = test.viewPager.addWaitForStateLatch(DRAGGING)
            // Perform manual swipe in separate thread, because PageSwiperManual
            // blocks while injecting events, and we need to interrupt it
            newSingleThreadExecutor().execute { pageSwiper.swipeNext() }
            assertThat(latch.await(1, SECONDS), equalTo(true))

            // start fake drag
            assertThat(test.viewPager.beginFakeDrag(), equalTo(false))
        } finally {
            pageSwiper.cancel()
        }
    }

    @Test
    fun test_startFakeDragToTargetPageWhileSettling() {
        // Run the test two times to verify that state doesn't linger
        repeat(2) {
            val targetPage = test.viewPager.currentItem + 1
            startFakeDragWhileSettling(targetPage, .5f, targetPage)
        }
    }

    @Test
    fun test_startFakeDragExactlyToTargetPageWhileSettling() {
        // Run the test two times to verify that state doesn't linger
        repeat(2) {
            val tracker = PositionTracker().also { test.viewPager.registerOnPageChangeCallback(it) }
            val targetPage = test.viewPager.currentItem + 1
            startFakeDragWhileSettling(
                targetPage,
                { (targetPage - tracker.lastPosition).toFloat() },
                targetPage,
                true,
            )
            test.viewPager.unregisterOnPageChangeCallback(tracker)
        }
    }

    @Test
    fun test_startFakeDragToNextPageWhileSettling() {
        // Run the test two times to verify that state doesn't linger
        repeat(2) {
            val targetPage = test.viewPager.currentItem + 1
            startFakeDragWhileSettling(targetPage, 1.5f, targetPage + 1)
        }
    }

    @Test
    fun test_startFakeDragExactlyToNextPageWhileSettling() {
        // Run the test two times to verify that state doesn't linger
        repeat(2) {
            val tracker = PositionTracker().also { test.viewPager.registerOnPageChangeCallback(it) }
            val targetPage = test.viewPager.currentItem + 1
            val nextPage = targetPage + 1
            startFakeDragWhileSettling(
                targetPage,
                { (nextPage - tracker.lastPosition).toFloat() },
                nextPage,
                true,
            )
            test.viewPager.unregisterOnPageChangeCallback(tracker)
        }
    }

    @Test
    fun test_setCurrentItemDuringFakeDrag() {
        setCurrentItemDuringFakeDrag(false)
    }

    @Test
    fun test_smoothScrollDuringFakeDrag() {
        setCurrentItemDuringFakeDrag(true)
    }

    /*
     * Fake drag, interrupted by manual drag after 0.1 page has been fake-dragged
     *
     * > Starting fake drag
     * onPageScrollStateChanged(1)
     * onPageScrolled(0, 0.016, 24)
     * onPageScrolled(0, 0.032, 48)
     * ...
     * onPageScrolled(0, 0.096, 145)
     * onPageScrolled(0, 0.112, 169)
     * > Starting manual drag
     * onPageScrolled(0, 0.129, 194)
     * onPageScrolled(0, 0.187, 281)
     * ...
     * onPageScrolled(0, 0.616, 925)
     * onPageScrolled(0, 0.669, 1004)
     * onPageScrollStateChanged(2)
     * onPageSelected(1)
     * onPageScrolled(0, 0.706, 1059)
     * onPageScrolled(0, 0.740, 1111)
     * ...
     * onPageScrolled(0, 0.999, 1499)
     * onPageScrolled(1, 0.0, 0)
     * onPageScrollStateChanged(0)
     */
    @Test
    fun test_startManualDragDuringFakeDrag() {
        startManualDragDuringFakeDrag(.5f, 1000, interpolator = fastDecelerateInterpolator) {
            test.swipeForward(SwipeMethod.MANUAL)
        }
    }

    /*
     * Fake drag, interrupted by manual drag after 1.1 page has been fake-dragged
     *
     * > Starting fake drag
     * onPageScrollStateChanged(1)
     * onPageScrolled(0, 0.040, 61)
     * onPageScrolled(0, 0.082, 123)
     * ...
     * onPageScrolled(1, 0.063, 95)
     * onPageScrolled(1, 0.104, 157)
     * > Starting manual drag
     * onPageScrolled(1, 0.145, 218)
     * onPageScrolled(1, 0.173, 260)
     * ...
     * onPageScrolled(1, 0.201, 302)
     * onPageScrolled(1, 0.202, 303)
     * onPageScrollStateChanged(2)
     * onPageSelected(1)
     * onPageScrolled(1, 0.165, 248)
     * onPageScrolled(1, 0.128, 193)
     * ...
     * onPageScrolled(1, 0.001, 1)
     * onPageScrolled(1, 0.0, 0)
     * onPageScrollStateChanged(0)
     */
    @Test
    fun test_startManualPeekAfterFakeDrag1Page() {
        val vc = ViewConfiguration.get(test.viewPager.context)
        val touchSlop = vc.scaledPagingTouchSlop
        val swipeDistance = touchSlop * 5f
        val pageSize = test.viewPager.pageSize
        val dragDistance = 1 + (pageSize / 2f - swipeDistance) / pageSize // ~1.4f

        startManualDragDuringFakeDrag(dragDistance, 1000, 1, fastDecelerateInterpolator) {
            PageSwiperManual(test.viewPager).swipeForward(swipeDistance, fastDecelerateInterpolator)
        }
    }

    /*
     * Fake drag, interrupted by an a11y action after 0.1 page has been fake-dragged
     *
     * > Starting fake drag
     * onPageScrollStateChanged(1)
     * onPageScrolled(0, 0.013, 13)
     * onPageScrolled(0, 0.026, 26)
     * ...
     * onPageScrolled(0, 0.090, 90)
     * onPageScrolled(0, 0.103, 103)
     * > Perform a11y action
     * onPageScrollStateChanged(2)
     * onPageSelected(1)
     * onPageScrolled(0, 0.283, 282)
     * onPageScrolled(0, 0.434, 432)
     * ...
     * onPageScrolled(0, 0.996, 992)
     * onPageScrolled(1, 0.0, 0)
     * onPageScrollStateChanged(0)
     */
    @Test
    fun test_performA11yActionDuringFakeDrag() {
        startManualDragDuringFakeDrag(.9f, 1000, interpolator = fastDecelerateInterpolator) {
            test.runOnUiThreadSync {
                test.viewPager.performAccessibilityAction(getNextPageAction(), null)
            }
        }
    }

    private fun getNextPageAction(): Int {
        val useEnhancedA11y = ViewPager2.sFeatureEnhancedA11yEnabled
        val isHorizontal = test.viewPager.isHorizontal

        return if (useEnhancedA11y && isHorizontal && test.viewPager.isRtl) {
            ACTION_ID_PAGE_LEFT
        } else if (useEnhancedA11y && isHorizontal) {
            ACTION_ID_PAGE_RIGHT
        } else if (useEnhancedA11y) {
            ACTION_ID_PAGE_DOWN
        } else {
            ACTION_SCROLL_FORWARD
        }
    }

    private fun basicFakeDragTest(
        relativeDragDistance: Float,
        duration: Long,
        expectedFinalPage: Int,
        interpolator: Interpolator = LinearInterpolator(),
        suppressFling: Boolean = false,
    ) {
        val startPage = test.viewPager.currentItem
        // Run the test two times to verify that state doesn't linger
        repeat(2) {
            val initialPage = test.viewPager.currentItem
            val expectedFinalPageWithOffset = expectedFinalPage + initialPage - startPage
            val recorder = test.viewPager.addNewRecordingCallback()

            val latch = test.viewPager.addWaitForIdleLatch()
            fakeDragger.postFakeDrag(relativeDragDistance, duration, interpolator, suppressFling)
            latch.await(2000 + duration, MILLISECONDS)

            // test assertions
            test.assertBasicState(expectedFinalPageWithOffset)
            assertThat(test.viewPager.isFakeDragging, equalTo(false))
            assertThat(fakeDragger.isInterrupted, equalTo(false))
            recorder.apply {
                scrollEvents.assertValueCorrectness(
                    initialPage,
                    min(pageCount - 1, expectedFinalPageWithOffset + 1 /* for peeking */),
                    test.viewPager.pageSize,
                )
                assertFirstEvents(DRAGGING)
                assertLastEvents(expectedFinalPageWithOffset)
                assertPageSelectedEvents(initialPage, expectedFinalPageWithOffset)
                assertStateChanges(listOf(DRAGGING, SETTLING, IDLE), listOf(DRAGGING, IDLE))
            }

            test.viewPager.unregisterOnPageChangeCallback(recorder)
        }
    }

    private fun startFakeDragWhileSettling(
        settleTarget: Int,
        dragDistance: Float,
        expectedFinalPage: Int,
    ) {
        startFakeDragWhileSettling(settleTarget, { dragDistance }, expectedFinalPage, false)
    }

    private fun startFakeDragWhileSettling(
        settleTarget: Int,
        dragDistanceCallback: () -> Float,
        expectedFinalPage: Int,
        fakeDragMustEndSnapped: Boolean,
    ) {
        val initialPage = test.viewPager.currentItem

        tryNTimes(7, resetBlock = { test.resetViewPagerTo(initialPage) }) {
            val recorder = test.viewPager.addNewRecordingCallback()

            // start smooth scroll
            val scrollLatch = test.viewPager.addWaitForFirstScrollEventLatch()
            test.runOnUiThreadSync { test.viewPager.setCurrentItem(settleTarget, true) }
            assertThat(scrollLatch.await(2, SECONDS), equalTo(true))

            // start fake drag, but check some preconditions first
            var idleLatch: CountDownLatch? = null
            test.runOnUiThreadSync {
                val dragDistance = dragDistanceCallback()
                val currPosition = test.viewPager.relativeScrollPosition

                // Check 1: must still be in scroll state SETTLING
                if (test.viewPager.scrollState != ViewPager2.SCROLL_STATE_SETTLING) {
                    throw RetryException(
                        "Interruption of SETTLING too late: " + "state already left SETTLING"
                    )
                }
                // Check 2: setCurrentItem should not have finished
                if (settleTarget - currPosition <= 0) {
                    throw RetryException("Interruption of SETTLING too late: already at target")
                }
                // Check 3: fake drag should not overshoot its target
                if ((expectedFinalPage - currPosition).toFloat() < dragDistance) {
                    throw RetryException(
                        "Interruption of SETTLING too late: already closer than " +
                            "$dragDistance from target"
                    )
                }

                idleLatch = test.viewPager.addWaitForIdleLatch()
                fakeDragger.fakeDrag(dragDistance, 100)
            }
            assertThat(idleLatch!!.await(2, SECONDS), equalTo(true))

            // test assertions
            test.assertBasicState(expectedFinalPage)
            assertThat(test.viewPager.isFakeDragging, equalTo(false))
            assertThat(fakeDragger.isInterrupted, equalTo(false))
            recorder.apply {
                scrollEvents.assertValueCorrectness(
                    initialPage,
                    expectedFinalPage,
                    test.viewPager.pageSize,
                )
                assertFirstEvents(SETTLING)
                assertLastEvents(expectedFinalPage)
                assertPageSelectedEvents(initialPage, settleTarget, expectedFinalPage)
                if (fakeDragMustEndSnapped) {
                    assertThat(
                        "When a fake drag should end in a snapped position, we expect the last " +
                            "scroll event after the FAKE_DRAG event to be snapped. " +
                            dumpEvents(),
                        expectSettlingAfterState(DRAGGING),
                        equalTo(false),
                    )
                }
                assertStateChanges(
                    listOf(SETTLING, DRAGGING, SETTLING, IDLE),
                    listOf(SETTLING, DRAGGING, IDLE),
                )
            }

            test.viewPager.unregisterOnPageChangeCallback(recorder)
        }
    }

    private fun setCurrentItemDuringFakeDrag(smoothScroll: Boolean) {
        val initialPage = test.viewPager.currentItem
        // start fake drag
        val latch = test.viewPager.addWaitForStateLatch(DRAGGING)
        fakeDragger.postFakeDrag(.5f, 500)
        assertThat(latch.await(1, SECONDS), equalTo(true))

        // start smooth scroll
        doIllegalAction("Cannot change current item when ViewPager2 is fake dragging") {
            test.viewPager.setCurrentItem(initialPage + 1, smoothScroll)
        }
        assertThat(fakeDragger.isInterrupted, equalTo(false))
    }

    private fun doIllegalAction(errorMessage: String, action: () -> Unit) {
        var exception: IllegalStateException? = null
        test.runOnUiThreadSync {
            try {
                action()
            } catch (e: IllegalStateException) {
                exception = e
            }
        }
        assertThat(exception, notNullValue())
        assertThat(exception!!.message, equalTo(errorMessage))
    }

    private fun startManualDragDuringFakeDrag(
        fakeDragDistance: Float,
        fakeDragDuration: Long,
        referencePageOffset: Int = 0,
        interpolator: Interpolator = LinearInterpolator(),
        manualDragCallback: () -> Unit,
    ) {
        // Skip tests where manual dragging is disabled
        assumeThat(config.enableUserInput, equalTo(true))

        // Run the test two times to verify that state doesn't linger
        repeat(2) {
            val initialPage = test.viewPager.currentItem
            val expectedFinalPage = initialPage + 1

            tryNTimes(3, resetBlock = { test.resetViewPagerTo(initialPage) }) {
                val recorder = test.viewPager.addNewRecordingCallback()

                // start fake drag
                val fakeDragLatch =
                    test.viewPager.addWaitForDistanceToTarget(
                        expectedFinalPage + referencePageOffset,
                        .9f,
                    )
                val idleLatch = test.viewPager.addWaitForIdleLatch()
                fakeDragger.postFakeDrag(fakeDragDistance, fakeDragDuration, interpolator)
                assertThat(fakeDragLatch.await(5, SECONDS), equalTo(true))

                // start manual drag
                manualDragCallback()
                assertThat(idleLatch.await(5, SECONDS), equalTo(true))

                assertThat(test.viewPager.isFakeDragging, equalTo(false))
                if (!fakeDragger.isInterrupted) {
                    throw RetryException("Fake drag was not interrupted")
                }
                fakeDragger.resetIsInterrupted()

                // test assertions
                test.assertBasicState(expectedFinalPage)
                recorder.apply {
                    scrollEvents.assertValueCorrectness(
                        initialPage,
                        expectedFinalPage + referencePageOffset,
                        test.viewPager.pageSize,
                    )
                    assertFirstEvents(DRAGGING)
                    assertLastEvents(expectedFinalPage)
                    assertPageSelectedEvents(initialPage, expectedFinalPage)
                    assertStateChanges(listOf(DRAGGING, SETTLING, IDLE), listOf(DRAGGING, IDLE))
                }

                test.viewPager.unregisterOnPageChangeCallback(recorder)
            }
        }
    }

    private val ViewPager2.relativeScrollPosition: Double
        get() {
            val scrollEventAdapter = mScrollEventAdapterField.get(this)
            return getRelativeScrollPositionMethod.invoke(scrollEventAdapter) as Double
        }

    private fun ViewPager2.addNewRecordingCallback(): RecordingCallback {
        return RecordingCallback().also { registerOnPageChangeCallback(it) }
    }

    private sealed class Event {
        data class OnPageScrolledEvent(
            val position: Int,
            val positionOffset: Float,
            val positionOffsetPixels: Int,
        ) : Event()

        data class OnPageSelectedEvent(val position: Int) : Event()

        data class OnPageScrollStateChangedEvent(val state: Int) : Event()
    }

    private class RecordingCallback : ViewPager2.OnPageChangeCallback() {
        private val events = mutableListOf<Event>()

        val scrollEvents
            get() = eventsCopy.mapNotNull { it as? OnPageScrolledEvent }

        val stateEvents
            get() = eventsCopy.mapNotNull { it as? OnPageScrollStateChangedEvent }

        val selectEvents
            get() = eventsCopy.mapNotNull { it as? OnPageSelectedEvent }

        val eventCount
            get() = eventsCopy.size

        val firstEvent
            get() = eventsCopy.firstOrNull()

        val lastEvent
            get() = eventsCopy.lastOrNull()

        private fun addEvent(e: Event) {
            synchronized(events) { events.add(e) }
        }

        val eventsCopy: List<Event>
            get() =
                synchronized(events) {
                    return mutableListOf<Event>().apply { addAll(events) }
                }

        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int,
        ) {
            addEvent(OnPageScrolledEvent(position, positionOffset, positionOffsetPixels))
        }

        override fun onPageSelected(position: Int) {
            addEvent(OnPageSelectedEvent(position))
        }

        override fun onPageScrollStateChanged(state: Int) {
            addEvent(OnPageScrollStateChangedEvent(state))
        }

        fun expectSettlingAfterState(state: Int): Boolean {
            val changeToStateEvent = OnPageScrollStateChangedEvent(state)
            val lastScrollEvent =
                eventsCopy
                    .dropWhile { it != changeToStateEvent }
                    .dropWhile { it !is OnPageScrolledEvent }
                    .takeWhile { it is OnPageScrolledEvent }
                    .lastOrNull() as? OnPageScrolledEvent
            return lastScrollEvent?.let { it.positionOffsetPixels != 0 } ?: true
        }

        fun dumpEvents(): String {
            return eventsCopy.joinToString("\n- ", "\n(${scrollStateGlossary()})\n- ")
        }
    }

    private fun RecordingCallback.assertFirstEvents(expectedFirstState: Int) {
        assertThat("There should be events", eventCount, greaterThan(0))
        assertThat(
            "First event should be state change to " +
                "${scrollStateToString(expectedFirstState)}: ${dumpEvents()}",
            firstEvent,
            equalTo(OnPageScrollStateChangedEvent(expectedFirstState) as Event),
        )
    }

    private fun RecordingCallback.assertLastEvents(expectedFinalPage: Int) {
        assertThat(
            "Last event should be state change to IDLE: ${dumpEvents()}",
            lastEvent,
            equalTo(OnPageScrollStateChangedEvent(IDLE) as Event),
        )
        assertThat(
            "Scroll events don't end in snapped position: ${dumpEvents()}",
            scrollEvents.last().positionOffsetPixels,
            equalTo(0),
        )
        assertThat(
            "Scroll events don't end at page $expectedFinalPage: ${dumpEvents()}",
            scrollEvents.last().position,
            equalTo(expectedFinalPage),
        )
    }

    private fun RecordingCallback.assertPageSelectedEvents(vararg visitedPages: Int) {
        val expectedPageSelects =
            visitedPages.toList().zipWithNext().mapNotNull { pair ->
                // If visited page is same as previous page, no page selected event should be fired
                if (pair.first == pair.second) null else pair.second
            }
        assertThat(
            "Sequence of selected pages should be $expectedPageSelects: ${dumpEvents()}",
            selectEvents.map { it.position },
            equalTo(expectedPageSelects),
        )

        val settleEvent = OnPageScrollStateChangedEvent(SETTLING)
        val idleEvent = OnPageScrollStateChangedEvent(IDLE)
        val events = eventsCopy
        events.forEachIndexed { i, event ->
            if (event is OnPageSelectedEvent) {
                assertThat(
                    "OnPageSelectedEvents cannot be the first or last event: " + dumpEvents(),
                    i,
                    isBetweenInEx(1, eventCount - 1),
                )
                val isAfterSettleEvent = events[i - 1] == settleEvent
                val isBeforeIdleEvent = events[i + 1] == idleEvent
                assertThat(
                    "OnPageSelectedEvent at index $i must follow a SETTLE event or precede" +
                        " an IDLE event, but not both: ${dumpEvents()}",
                    isAfterSettleEvent.xor(isBeforeIdleEvent),
                    equalTo(true),
                )
            }
        }
    }

    private fun RecordingCallback.assertStateChanges(
        statesWithSettling: List<Int>,
        statesWithoutSettling: List<Int>,
    ) {
        assertThat(
            "Unexpected sequence of state changes:" + dumpEvents(),
            stateEvents.map { it.state },
            equalTo(
                if (expectSettlingAfterState(DRAGGING)) {
                    statesWithSettling
                } else {
                    statesWithoutSettling
                }
            ),
        )
    }

    private fun List<OnPageScrolledEvent>.assertValueCorrectness(
        initialPage: Int,
        otherPage: Int,
        pageSize: Int,
    ) = forEach {
        assertThat(
            it.position + it.positionOffset.toDouble(),
            isBetweenInInMinMax(initialPage.toDouble(), otherPage.toDouble()),
        )
        assertThat(it.positionOffset, isBetweenInEx(0f, 1f))
        assertThat((it.positionOffset * pageSize).roundToInt(), equalTo(it.positionOffsetPixels))
    }

    private class PositionTracker : ViewPager2.OnPageChangeCallback() {
        var lastPosition = 0.0

        override fun onPageScrolled(position: Int, offset: Float, offsetPx: Int) {
            lastPosition = position + offset.toDouble()
        }
    }
}

private fun createTestSet(): List<TestConfig> {
    return listOf(ORIENTATION_HORIZONTAL, ORIENTATION_VERTICAL).flatMap { orientation ->
        listOf(false, true).flatMap { rtl ->
            listOf(true, false).map { enableUserInput ->
                TestConfig(orientation, rtl, enableUserInput)
            }
        }
    }
}
