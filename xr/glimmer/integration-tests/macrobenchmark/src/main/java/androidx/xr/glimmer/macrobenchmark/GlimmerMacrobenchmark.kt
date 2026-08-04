/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.glimmer.macrobenchmark

import android.app.UiAutomation
import android.content.Intent
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import androidx.testutils.createCompilationParams
import androidx.testutils.defaultComposeScrollingMetrics
import androidx.testutils.waitOrThrow
import androidx.tracing.trace
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class GlimmerMacrobenchmark(private val compilationMode: CompilationMode) {

    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun scrollList() =
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = defaultComposeScrollingMetrics(),
            compilationMode = compilationMode,
            iterations = DEFAULT_ITERATIONS,
            setupBlock = { launchTargetScreen(DESTINATION_LIST) },
        ) {
            val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation

            device.waitOrThrow(Until.hasObject(By.text("Item: 0")), TIMEOUT_MS)

            // Scroll through the list.
            trace("J<ScrollList>") {
                simulateIndirectSwipeVertical(uiAutomation, distanceDp = 450f)
                device.waitForIdle()
            }

            // Verify we scrolled down the list: Item: 0 has scrolled off screen, Item: 4 is
            // visible, and we did not overscroll to the end.
            check(!device.hasObject(By.text("Item: 0"))) {
                "List did not scroll past the start bound."
            }
            check(device.hasObject(By.text("Item: 4"))) { "Item: 4 is not visible on screen." }
            check(!device.hasObject(By.text("Item: 19"))) {
                "List scrolled to the end bound (overscroll)."
            }
        }

    @Test
    fun scrollPager() =
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = defaultComposeScrollingMetrics(),
            compilationMode = compilationMode,
            iterations = DEFAULT_ITERATIONS,
            setupBlock = { launchTargetScreen(DESTINATION_PAGER) },
        ) {
            val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
            val distanceDp = device.displayWidth / density

            device.waitOrThrow(Until.hasObject(By.text("Page: 0")), TIMEOUT_MS)

            // Swipe to Page 1.
            trace("J<SwipeToPage1>") {
                simulateIndirectSwipeHorizontal(uiAutomation, distanceDp = distanceDp)
                device.waitForIdle()
            }

            // Verify the state after first swipe: Page 1 is visible.
            device.waitOrThrow(Until.hasObject(By.text("Page: 1")), TIMEOUT_MS)

            // Swipe to Page 2.
            trace("J<SwipeToPage2>") {
                simulateIndirectSwipeHorizontal(uiAutomation, distanceDp = distanceDp)
                device.waitForIdle()
            }

            // Verify the state after second swipe: Page 2 is visible.
            device.waitOrThrow(Until.hasObject(By.text("Page: 2")), TIMEOUT_MS)
        }

    @Test
    fun scrollStack() =
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = defaultComposeScrollingMetrics(),
            compilationMode = compilationMode,
            iterations = DEFAULT_ITERATIONS,
            setupBlock = { launchTargetScreen(DESTINATION_STACK) },
        ) {
            val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
            val distanceDp = device.displayHeight / density

            device.waitOrThrow(Until.hasObject(By.text("Stack: 0")), TIMEOUT_MS)

            // Swipe to Stack 1.
            trace("J<SwipeToStack1>") {
                simulateIndirectSwipeVertical(uiAutomation, distanceDp = distanceDp)
                device.waitForIdle()
            }

            // Verify state after first swipe: Stack: 1 is visible.
            device.waitOrThrow(Until.hasObject(By.text("Stack: 1")), TIMEOUT_MS)

            // Swipe to Stack 2.
            trace("J<SwipeToStack2>") {
                simulateIndirectSwipeVertical(uiAutomation, distanceDp = distanceDp)
                device.waitForIdle()
            }

            // Verify state after second swipe: Stack: 2 is visible.
            device.waitOrThrow(Until.hasObject(By.text("Stack: 2")), TIMEOUT_MS)
        }

    private fun MacrobenchmarkScope.launchTargetScreen(destination: String) {
        pressHome()
        startActivityAndWait { intent ->
            intent.putExtra(EXTRA_DESTINATION, destination)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    }

    // TODO(b/532491869): Replace this with UiAutomator's helper functions.
    private fun simulateIndirectSwipe(
        uiAutomation: UiAutomation,
        fromX: Float,
        toX: Float,
        fromY: Float,
        toY: Float,
        durationMs: Long,
    ) {
        val steps = 20
        val delayPerStep = durationMs / steps
        val downTime = SystemClock.uptimeMillis()

        val downEvent =
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, fromX, fromY, 0).apply {
                source = InputDevice.SOURCE_TOUCH_NAVIGATION
            }
        uiAutomation.injectInputEvent(downEvent, true)
        downEvent.recycle()

        for (i in 1..steps) {
            Thread.sleep(delayPerStep)
            val currentTime = SystemClock.uptimeMillis()
            val currentX = fromX + (toX - fromX) * (i.toFloat() / steps)
            val currentY = fromY + (toY - fromY) * (i.toFloat() / steps)
            val moveEvent =
                MotionEvent.obtain(
                        downTime,
                        currentTime,
                        MotionEvent.ACTION_MOVE,
                        currentX,
                        currentY,
                        0,
                    )
                    .apply { source = InputDevice.SOURCE_TOUCH_NAVIGATION }
            uiAutomation.injectInputEvent(moveEvent, true)
            moveEvent.recycle()
        }

        Thread.sleep(delayPerStep)
        val currentTime = SystemClock.uptimeMillis()
        val upEvent =
            MotionEvent.obtain(downTime, currentTime, MotionEvent.ACTION_UP, toX, toY, 0).apply {
                source = InputDevice.SOURCE_TOUCH_NAVIGATION
            }
        uiAutomation.injectInputEvent(upEvent, true)
        upEvent.recycle()
    }

    private fun simulateIndirectSwipeVertical(
        uiAutomation: UiAutomation,
        distanceDp: Float,
        durationMs: Long = 150L,
    ) {
        val distancePx = distanceDp * density
        val fromY = if (distanceDp < 0) distancePx else 0f
        val toY = if (distanceDp < 0) 0f else distancePx
        simulateIndirectSwipe(
            uiAutomation,
            fromX = 0f,
            toX = 0f,
            fromY = fromY,
            toY = toY,
            durationMs = durationMs,
        )
    }

    private fun simulateIndirectSwipeHorizontal(
        uiAutomation: UiAutomation,
        distanceDp: Float,
        durationMs: Long = 150L,
    ) {
        val distancePx = distanceDp * density
        val fromX = if (distanceDp < 0) distancePx else 0f
        val toX = if (distanceDp < 0) 0f else distancePx
        simulateIndirectSwipe(
            uiAutomation,
            fromX = fromX,
            toX = toX,
            fromY = 0f,
            toY = 0f,
            durationMs = durationMs,
        )
    }

    companion object {
        private const val PACKAGE_NAME = "androidx.xr.glimmer.macrobenchmark.target"
        private const val EXTRA_DESTINATION = "destination"
        private const val DESTINATION_LIST = "list"
        private const val DESTINATION_PAGER = "pager"
        private const val DESTINATION_STACK = "stack"
        private const val TIMEOUT_MS = 5000L
        private const val DEFAULT_ITERATIONS = 5

        @Parameterized.Parameters(name = "compilation={0}")
        @JvmStatic
        fun parameters(): List<Array<Any>> = createCompilationParams()
    }
}

private val density: Float
    get() =
        InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
