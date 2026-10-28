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

package androidx.benchmark.integration.macrobenchmark

import android.content.Intent
import android.graphics.Point
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import androidx.tracing.trace
import kotlin.math.abs
import kotlin.math.max
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@LargeTest
class VectorsListScrollBenchmark {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    private lateinit var device: UiDevice

    /** First visible item after launch in the first iteration, used to validate later ones. */
    private var expectedStartIndex = -1

    /** Items scrolled past in the first iteration, used to validate subsequent iterations. */
    private var expectedItemsScrolled = -1

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        device = UiDevice.getInstance(instrumentation)
    }

    @Test
    fun start() {
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Full(),
            // Each iteration takes ~35s on the CI devices. 8 iterations put the total runtime
            // right at the 5 minute per-test timeout, so this benchmark timed out in most CI
            // runs (b/574464818). Matches the iteration count of the Compose counterpart.
            iterations = 5,
            setupBlock = {
                trace("$TRACE_PREFIX setup") {
                    // Restart the app so every iteration starts at the top of the list. Without
                    // this, the scroll position carries over and iterations are not comparable.
                    killProcess()
                    val intent = Intent()
                    intent.action = ACTION
                    startActivityAndWait(intent)
                }
            },
        ) {
            val margin = device.displayWidth / 5
            // Which item is reported as first visible after a cold launch depends on the device
            // (shiba on API 35 reports item 1, not 0), so only require it to be the same in
            // every iteration.
            val startIndices = trace("$TRACE_PREFIX verify setup") { visibleItemIndices() }
            val startIndex = startIndices.first()
            if (expectedStartIndex < 0) {
                expectedStartIndex = startIndex
            } else {
                check(startIndex == expectedStartIndex) {
                    "Expected list to start at item $expectedStartIndex like the first " +
                        "iteration, but visible items were $startIndices"
                }
            }

            var previousIndex = startIndex
            repeat(SCROLL_COUNT) { scroll ->
                trace("$TRACE_PREFIX drag $scroll") {
                    val recycler = requireRecycler()
                    // Setting a gesture margin is important otherwise gesture nav is triggered.
                    recycler.setGestureMargin(margin)
                    // From the center we drag up by 2/3 of the center, which scrolls the list by
                    // 1/3 of the screen. UiObject2.drag does not clip to the gesture margins
                    // (only swipe/scroll/fling do), so clamp the end point to them explicitly.
                    val bounds = recycler.visibleBounds
                    val center = Point(bounds.centerX(), bounds.centerY())
                    val dragTarget = Point(center.x, max(center.y / 3, bounds.top + margin))
                    recycler.drag(dragTarget)
                }
                trace("$TRACE_PREFIX waitForIdle $scroll") { device.waitForIdle() }
                trace("$TRACE_PREFIX verify $scroll") {
                    val indices = visibleItemIndices()
                    val index = indices.first()
                    check(index > previousIndex) {
                        "Drag $scroll did not scroll the list: first visible item was " +
                            "$previousIndex before, visible items after: $indices"
                    }
                    previousIndex = index
                }
            }

            // Device size and density determine how many items a drag scrolls past, so compare
            // iterations against each other rather than against a constant.
            val itemsScrolled = previousIndex - startIndex
            if (expectedItemsScrolled < 0) {
                expectedItemsScrolled = itemsScrolled
            } else {
                val tolerance = max(ITEMS_SCROLLED_MIN_TOLERANCE, expectedItemsScrolled / 4)
                check(abs(itemsScrolled - expectedItemsScrolled) <= tolerance) {
                    "Iteration scrolled past $itemsScrolled items, but the first iteration " +
                        "scrolled past $expectedItemsScrolled (tolerance $tolerance)"
                }
            }
        }
    }

    /** Finds the list, failing with a diagnostic message if it is not on screen. */
    private fun requireRecycler(): UiObject2 {
        val selector = By.res(PACKAGE_NAME, RESOURCE_ID)
        return checkNotNull(device.wait(Until.findObject(selector), FIND_OBJECT_TIMEOUT_MS)) {
            "RecyclerView $RESOURCE_ID not found within ${FIND_OBJECT_TIMEOUT_MS}ms. " +
                "Foreground package: ${device.currentPackageName}, " +
                "screen on: ${device.isScreenOn}"
        }
    }

    /** Returns the indices of the (partially) visible list items, in ascending order. */
    private fun visibleItemIndices(): List<Int> {
        val items = requireRecycler().findObjects(By.res(PACKAGE_NAME, ITEM_RESOURCE_ID))
        check(items.isNotEmpty()) { "No list items visible in $RESOURCE_ID" }
        return items
            .map { item ->
                val text = item.text.orEmpty()
                checkNotNull(ITEM_TEXT_REGEX.matchEntire(text)?.groupValues?.get(1)?.toInt()) {
                    "Unexpected list item text: '$text'"
                }
            }
            .sorted()
    }

    companion object {
        private const val PACKAGE_NAME = "androidx.benchmark.integration.macrobenchmark.target"
        private const val ACTION =
            "androidx.benchmark.integration.macrobenchmark.target.VECTORS_LIST_ACTIVITY"
        private const val RESOURCE_ID = "recycler"
        private const val ITEM_RESOURCE_ID = "content"
        private const val FIND_OBJECT_TIMEOUT_MS = 5_000L
        private const val SCROLL_COUNT = 8
        private const val ITEMS_SCROLLED_MIN_TOLERANCE = 3
        private const val TRACE_PREFIX = "VectorsListScrollBenchmark"

        /** Matches the text set in VectorsListActivity.entries(). */
        private val ITEM_TEXT_REGEX = Regex("""ViewXYZ(\d+)""")
    }
}
