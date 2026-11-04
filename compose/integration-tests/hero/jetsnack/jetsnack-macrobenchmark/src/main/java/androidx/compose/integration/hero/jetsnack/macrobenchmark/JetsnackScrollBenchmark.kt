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

package androidx.compose.integration.hero.jetsnack.macrobenchmark

import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingGfxInfoMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.compose.integration.hero.common.macrobenchmark.HeroMacrobenchmarkDefaults
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import androidx.testutils.createCompilationParams
import androidx.testutils.defaultComposeScrollingMetrics
import androidx.testutils.defaultMemoryMetrics
import androidx.testutils.findObjectOrThrow
import androidx.testutils.waitForComposeIdleOrThrow
import androidx.testutils.waitOrThrow
import androidx.tracing.trace
import java.util.regex.Pattern
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class JetsnackScrollBenchmark(val compilationMode: CompilationMode) {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun scrollHome() =
        benchmarkScroll(
            action = "$JETSNACK_TARGET_PACKAGE_NAME.JETSNACK_ACTIVITY",
            setupBlock = {
                val searchCondition = Until.hasObject(By.res("snack_collection"))
                // Wait until a snack collection item within the list is rendered
                device.waitOrThrow(searchCondition, 3_000)
                device.waitForComposeIdleOrThrow()
            },
            measureBlock = {
                scrollActions(
                    contentListSelector = By.res("snack_list"),
                    idleMethod = { device.waitForComposeIdleOrThrow() },
                )
            },
        )

    @Test
    fun scrollViewsHome() =
        benchmarkScroll(
            action = "$JETSNACK_TARGET_PACKAGE_NAME.JETSNACK_VIEWS_ACTIVITY",
            setupBlock = {
                val resPkg = JETSNACK_TARGET_PACKAGE_NAME
                val searchCondition = Until.hasObject(By.res(resPkg, "snackImageView"))
                // Wait until a snack collection item within the list is rendered
                device.waitOrThrow(searchCondition, 3_000)
                device.waitForIdle()
            },
            measureBlock = {
                scrollActions(
                    contentListSelector =
                        By.res(JETSNACK_TARGET_PACKAGE_NAME, "snackFeedRecyclerView"),
                    idleMethod = { device.waitForIdle() },
                )
            },
        )

    @OptIn(ExperimentalMetricApi::class)
    private fun benchmarkScroll(
        action: String,
        setupBlock: MacrobenchmarkScope.() -> Unit,
        measureBlock: MacrobenchmarkScope.() -> Unit,
    ) =
        benchmarkRule.measureRepeated(
            packageName = JETSNACK_TARGET_PACKAGE_NAME,
            metrics =
                defaultComposeScrollingMetrics() +
                    FrameTimingGfxInfoMetric() +
                    defaultMemoryMetrics(),
            compilationMode = compilationMode,
            iterations = HeroMacrobenchmarkDefaults.ITERATIONS,
            setupBlock = {
                killProcess()
                val intent = Intent()
                intent.action = action
                startActivityAndWait(intent)
                setupBlock()
            },
            measureBlock = measureBlock,
        )

    private fun MacrobenchmarkScope.scrollActions(
        contentListSelector: BySelector,
        idleMethod: () -> Unit,
    ) {
        val horizontalMargin = device.displayWidth / 5
        val verticalMargin = device.displayHeight / 5
        var previousIndex = firstVisibleCollectionIndex(contentListSelector)

        fun flingAndWaitForIdle(direction: Direction) {
            trace("JetsnackScrollBenchmark#fling($direction)") {
                // Re-query the list before each fling because scrolling can refresh the
                // accessibility tree and make a cached UiObject2 stale.
                val contentList = device.findObjectOrThrow(contentListSelector)
                // Set gesture margins to avoid triggering gesture navigation and so upward
                // flings start below the overlaid top app bar (DestinationBar).
                contentList.setGestureMargins(
                    horizontalMargin,
                    verticalMargin,
                    horizontalMargin,
                    verticalMargin,
                )
                contentList.fling(direction)
                idleMethod()
                val currentIndex = firstVisibleCollectionIndex(contentListSelector)
                val scrolled =
                    if (direction == Direction.DOWN) {
                        currentIndex > previousIndex
                    } else {
                        currentIndex < previousIndex
                    }
                check(scrolled) {
                    "Fling $direction did not scroll the list: first visible collection was " +
                        "$previousIndex before and $currentIndex after."
                }
                previousIndex = currentIndex
            }
        }

        flingAndWaitForIdle(Direction.DOWN)
        flingAndWaitForIdle(Direction.UP)
        flingAndWaitForIdle(Direction.DOWN)
    }

    private fun MacrobenchmarkScope.firstVisibleCollectionIndex(
        contentListSelector: BySelector
    ): Int {
        val collections =
            device
                .findObjectOrThrow(contentListSelector)
                .findObjects(By.text(COLLECTION_NAME_PATTERN))
        check(collections.isNotEmpty()) {
            "No snack collections visible in '$contentListSelector'."
        }
        return collections.minOf { COLLECTION_NAMES.indexOf(it.text) }
    }

    companion object {
        private val COLLECTION_NAMES =
            listOf(
                "Android's picks",
                "Popular on Jetsnack",
                "WFH favourites",
                "Newly Added",
                "Only on Jetsnack",
                "Customers also bought",
                "Inspired by your cart",
                "Latest offers",
                "Best selling",
                "Available for pre-order",
            )

        private val COLLECTION_NAME_PATTERN = Pattern.compile(COLLECTION_NAMES.joinToString("|"))

        @Parameterized.Parameters(name = "compilation={0}")
        @JvmStatic
        fun parameters() = createCompilationParams()
    }
}
