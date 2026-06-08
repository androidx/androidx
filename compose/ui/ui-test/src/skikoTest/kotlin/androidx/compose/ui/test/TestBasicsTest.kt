/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.test

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertTrue
import kotlin.concurrent.Volatile
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalTestApi::class)
class TestBasicsTest : AbstractTestBasicsTest(
    runUiTest = { runComposeUiTest(block = it) }
)

@OptIn(ExperimentalTestApi::class)
class TestBasicsTestV2 : AbstractTestBasicsTest(
    runUiTest = { androidx.compose.ui.test.v2.runComposeUiTest(block = it) }
)

/**
 * Basic tests of the testing framework itself.
 */
@OptIn(ExperimentalTestApi::class)
abstract class AbstractTestBasicsTest(
    private val runUiTest: (ComposeUiTest.() -> Unit) -> TestResult
) {

    // See https://github.com/JetBains/compose-multiplatform/issues/3117
    @Test
    fun recompositionCompletesBeforeSetContentReturns() = repeat(100) {
        runUiTest {
            val globalValue = object {
                var value by atomic(0)
            }

            setContent {
                var localValue by remember { mutableStateOf(0) }

                remember(localValue) {
                    globalValue.value = localValue
                }

                Layout(
                    {},
                    Modifier,
                    measurePolicy = { _, constraints ->
                        localValue = 100
                        layout(constraints.maxWidth, constraints.maxHeight) {}
                    }
                )
            }

            assertEquals(100, globalValue.value)
        }
    }

    @Test
    fun inputEventAdvancesClock() = runUiTest {
        setContent {
            Box(Modifier.testTag("box").size(100.dp))
        }

        val clockBefore = mainClock.currentTime
        onNodeWithTag("box").performClick()
        val clockAfter = mainClock.currentTime
        assertTrue(clockAfter > clockBefore, "performClick did not advance the test clock")
    }

    @Test
    fun obtainingSemanticsNodeInteractionWaitsUntilIdle() = runUiTest {
        var text by mutableStateOf("1")

        setContent {
            Text(text, modifier = Modifier.testTag("text"))
        }

        onNodeWithTag("text").assertTextEquals("1")
        text = "2"
        onNodeWithTag("text").assertTextEquals("2")
    }

    @Test
    fun testCaptureToImage() = runUiTest {
        val color = Color.Green
        setContent {
            Box(Modifier.testTag("box").size(20.dp).background(color))
        }

        val screenshot = onNodeWithTag("box").captureToImage()

        assertEquals(20, screenshot.width)
        assertEquals(20, screenshot.height)

        IntArray(20 * 20).let { buffer ->
            screenshot.readPixels(buffer)
            val expectedPixel = color.toArgb()
            for (pixel in buffer) {
                assertEquals(expectedPixel, pixel)
            }
        }
    }

    @Test
    fun infiniteDelayLoopInLaunchedEffectDoesNotHang() = runUiTest {
        runTest(timeout = 500.milliseconds) {
            var runDelayLoop by mutableStateOf(false)
            var effectLaunched = false
            setContent {
                if (runDelayLoop) {
                    LaunchedEffect(Unit) {
                        effectLaunched = true
                        while (true) {
                            delay(1000)
                        }
                    }
                }
            }

            // We can't just run the delay loop immediately because `setContent` calls `waitForIdle`
            // before returning, and because it's a blocking call, the `runTest` timeout will have
            // no effect on it. Meaning if the test fails (the delay-loop causes hanging), the test
            // itself will hang.
            runDelayLoop = true
            awaitIdle()
            assertTrue(effectLaunched)
        }
    }

    @Test
    fun delayInLaunchedEffectIsExecutedAfterAdvancingClock() = runUiTest {
        var value = 0
        mainClock.autoAdvance = false
        setContent {
            LaunchedEffect(Unit) {
                repeat(5) {
                    delay(1000)
                    value = it+1
                }
            }
        }

        assertEquals(0, value)
        mainClock.advanceTimeBy(999, ignoreFrameDuration = true)
        assertEquals(0, value)
        mainClock.advanceTimeBy(2, ignoreFrameDuration = true)
        assertEquals(1, value)
        mainClock.advanceTimeBy(2000, ignoreFrameDuration = true)
        assertEquals(3, value)
    }

    @Test
    fun advancingClockCausesRecompositions() = runUiTest {
        var value by mutableStateOf(0)
        val compositionValues = mutableListOf<Int>()
        setContent {
            compositionValues.add(value)
            val capturedValue = value
            LaunchedEffect(capturedValue) {
                delay(1000)
                value = capturedValue + 1
            }
        }

        assertEquals(0, value)
        mainClock.advanceTimeBy(10000)
        assertContentEquals(0..9, compositionValues)
    }

    @Test
    fun advancingClockByLessThanFrameDoesNotRecompose() = runUiTest {
        mainClock.autoAdvance = false
        var value by mutableIntStateOf(1)
        var compositionValue = 0
        setContent {
            compositionValue = value
        }
        assertEquals(1, compositionValue)
        value = 2
        mainClock.advanceTimeBy(1, ignoreFrameDuration = true)
        assertEquals(1, compositionValue)
        mainClock.advanceTimeByFrame()
        assertEquals(2, compositionValue)
    }

    @Test
    fun launchedEffectsRunAfterComposition() = runUiTest {
        val actions = mutableListOf<String>()
        setContent {
            LaunchedEffect(Unit) {
                actions.add("LaunchedEffect")
            }
            actions.add("Composition")
        }

        assertContentEquals(listOf("Composition", "LaunchedEffect"), actions)
    }

    @Test
    fun waitForIdleDoesNotAdvanceClockIfAlreadyIdle() = runUiTest {
        setContent { }

        val initialTime = mainClock.currentTime
        waitForIdle()
        assertEquals(initialTime, mainClock.currentTime)
    }

    @Test
    @IgnoreWebTest
    fun runOnIdleExecutesOnUiThread() = runUiTest {
        setContent { }
        runOnIdle {
            assertTrue(isOnUiThread())
        }
    }

    @Test
    fun runOnIdleExecutesWhenIdle() = runUiTest {
        var sourceValue by mutableIntStateOf(0)
        var targetValue = 0
        setContent {
            LaunchedEffect(sourceValue) {
                targetValue = sourceValue
            }
        }
        sourceValue = 1
        runOnIdle {
            assertEquals(targetValue, 1)
        }
    }

    @Test
    fun runOnIdleDoesNotWaitForIdleAfterward() = runUiTest {
        var sourceValue by mutableIntStateOf(0)
        var targetValue = 0
        setContent {
            LaunchedEffect(sourceValue) {
                targetValue = sourceValue
            }
        }
        runOnIdle {
            sourceValue = 1
        }
        assertEquals(targetValue, 0)
    }

    @Test
    fun emptyTestPerformance() = runTest(timeout = 6.seconds) {
        repeat(100) {
            runUiTest {
                setContent { }
            }
        }
    }

    // The two tests below work together to make sure that the initial input is reset between tests
    @Test
    fun inputModeResetBetweenTests1() = runUiTest {
        var initialInputMode: InputMode? = null
        var inputModeAfterwards: InputMode? = null
        setContent {
            val inputModeManager = LocalInputModeManager.current
            initialInputMode = inputModeManager.inputMode
            LaunchedEffect(Unit) {
                inputModeManager.requestInputMode(
                    if (initialInputMode == InputMode.Touch) InputMode.Keyboard else InputMode.Touch
                )
                inputModeAfterwards = inputModeManager.inputMode
            }
        }

        assertNotNull(initialInputMode)
        assertNotNull(inputModeAfterwards)
        assertNotSame(initialInputMode, inputModeAfterwards)
        if (initialInputModeFromOtherTest != null) {
            assertEquals(initialInputModeFromOtherTest, initialInputMode)
        } else {
            initialInputModeFromOtherTest = initialInputMode
        }
    }

    @Test
    fun inputModeResetBetweenTests2() = runUiTest {
        var initialInputMode: InputMode? = null
        var inputModeAfterwards: InputMode? = null
        setContent {
            val inputModeManager = LocalInputModeManager.current
            initialInputMode = inputModeManager.inputMode
            LaunchedEffect(Unit) {
                inputModeManager.requestInputMode(
                    if (initialInputMode == InputMode.Touch) InputMode.Keyboard else InputMode.Touch
                )
                inputModeAfterwards = inputModeManager.inputMode
            }
        }

        assertNotNull(initialInputMode)
        assertNotNull(inputModeAfterwards)
        assertNotSame(initialInputMode, inputModeAfterwards)
        if (initialInputModeFromOtherTest != null) {
            assertEquals(initialInputModeFromOtherTest, initialInputMode)
        } else {
            initialInputModeFromOtherTest = initialInputMode
        }
    }
}

private var initialInputModeFromOtherTest: InputMode? = null
