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

import androidx.activity.ComponentActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.test.util.TestCounter
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for passing a custom CoroutineContext when [running a ComposeUiTest][runComposeUiTest].
 * Similar tests are available for ComposeTestRule in compose:ui:ui-test-junit4
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class CustomEffectContextTest {

    @Test
    fun effectContextPropagatedToComposition_runComposeUiTest() {
        val testElement = TestCoroutineContextElement()
        runComposeUiTest(effectContext = testElement) {
            lateinit var compositionScope: CoroutineScope
            setContent { compositionScope = rememberCoroutineScope() }

            runOnIdle {
                val elementFromComposition =
                    compositionScope.coroutineContext[TestCoroutineContextElement]
                assertThat(elementFromComposition).isSameInstanceAs(testElement)
            }
        }
    }

    @Test
    fun motionDurationScale_defaultValue() = runComposeUiTest {
        var lastRecordedMotionDurationScale: Float? = null
        setContent {
            val context = rememberCoroutineScope().coroutineContext
            lastRecordedMotionDurationScale = context[MotionDurationScale]?.scaleFactor
        }

        runOnIdle { assertThat(lastRecordedMotionDurationScale).isNull() }
    }

    @Test
    fun motionDurationScale_propagatedToCoroutines() {
        val motionDurationScale =
            object : MotionDurationScale {
                override val scaleFactor: Float
                    get() = 0f
            }
        runComposeUiTest(effectContext = motionDurationScale) {
            var lastRecordedMotionDurationScale: Float? = null
            setContent {
                val context = rememberCoroutineScope().coroutineContext
                lastRecordedMotionDurationScale = context[MotionDurationScale]?.scaleFactor
            }

            runOnIdle { assertThat(lastRecordedMotionDurationScale).isEqualTo(0f) }
        }
    }

    @Test
    fun customDispatcher_ignoredWhenNotSubclassOfTestDispatcher() {
        val notATestDispatcher =
            object : CoroutineDispatcher() {
                override fun isDispatchNeeded(context: CoroutineContext): Boolean {
                    throw AssertionError("This dispatcher should be unused")
                }

                override fun dispatch(context: CoroutineContext, block: Runnable) {
                    throw AssertionError("This dispatcher should be unused")
                }

                @OptIn(ExperimentalCoroutinesApi::class)
                override fun limitedParallelism(parallelism: Int): CoroutineDispatcher {
                    throw AssertionError("This dispatcher should be unused")
                }
            }

        // The custom dispatcher is not a TestDispatcher, so should be completely discarded.
        // The custom dispatcher throws when it is used, so running the below is enough
        runComposeUiTest(effectContext = notATestDispatcher) {
            setContent {
                LaunchedEffect(Unit) {
                    withFrameNanos {}
                    launch {}
                }
            }
        }
    }

    @Test
    fun customDispatcher_StandardTestDispatcher() {
        val counter = TestCounter()

        runAndroidComposeUiTest<ComponentActivity>(effectContext = StandardTestDispatcher()) {
            // b/328299124: sometimes the timing of window focus can change the order or execution
            waitForWindowFocus()

            // With a StandardTestDispatcher, both launched effects and the launched coroutine are
            // dispatched, meaning they are added to the queue of tasks after all pending tasks.
            // Thus, the launched effects run first, and the launched coroutine comes last.

            setContent {
                LaunchedEffect(Unit) {
                    counter.expect(1)
                    launch { counter.expect(3) }
                }
                LaunchedEffect(Unit) { counter.expect(2) }
            }
            runOnIdle { counter.expect(4) }
        }
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun customDispatcher_UnconfinedTestDispatcher() {
        val counter = TestCounter()

        runAndroidComposeUiTest<ComponentActivity>(effectContext = UnconfinedTestDispatcher()) {
            // b/328299124: sometimes the timing of window focus can change the order or execution
            waitForWindowFocus()

            // With a UnconfinedTestDispatcher, both launched effects and the launched coroutine are
            // running unconfined, meaning they are executed immediately, regardless if there are
            // pending tasks.
            // Thus, the launched coroutine comes before the second launched effect.

            setContent {
                LaunchedEffect(Unit) {
                    counter.expect(1)
                    launch { counter.expect(2) }
                }
                LaunchedEffect(Unit) { counter.expect(3) }
            }
            runOnIdle { counter.expect(4) }
        }
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun scheduler_usedWhenPresent() {
        val scheduler = TestCoroutineScheduler()
        val startTime = scheduler.currentTime

        // We don't need any content, we only need to trigger the scheduler
        runComposeUiTest(scheduler) {
            setContent { rememberCoroutineScope().launch { withFrameNanos {} } }
        }

        // Only if it is used will the scheduler's time be changed
        assertThat(scheduler.currentTime).isNotEqualTo(startTime)
    }

    private class TestCoroutineContextElement : CoroutineContext.Element {
        override val key: CoroutineContext.Key<*>
            get() = Key

        companion object Key : CoroutineContext.Key<TestCoroutineContextElement>
    }
}

@OptIn(ExperimentalTestApi::class)
private fun AndroidComposeUiTest<ComponentActivity>.waitForWindowFocus() {
    val activity = activity ?: error("no activity")
    waitUntil("did not get window focus") { activity.hasWindowFocus() }
}
