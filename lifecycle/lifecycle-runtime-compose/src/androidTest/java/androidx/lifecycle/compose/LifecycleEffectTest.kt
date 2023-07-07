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

package androidx.lifecycle.compose

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class LifecycleEffectTest {

    private lateinit var lifecycleOwner: TestLifecycleOwner

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        lifecycleOwner = TestLifecycleOwner()
    }

    @Test
    fun lifecycleEventEffectTest_noEvent() {
        var stopCount = 0

        composeTestRule.waitForIdle()
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
                    stopCount++
                }
            }
        }

        composeTestRule.runOnIdle {
            assertWithMessage("Lifecycle should not have been stopped")
                .that(stopCount)
                .isEqualTo(0)
        }
    }

    @Test
    fun lifecycleEventEffectTest_localLifecycleOwner() {
        val expectedEvent = Lifecycle.Event.ON_STOP
        var stopCount = 0

        composeTestRule.waitForIdle()
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                LifecycleEventEffect(expectedEvent) {
                    stopCount++
                }
            }
        }

        composeTestRule.runOnIdle {
            lifecycleOwner.handleLifecycleEvent(expectedEvent)
            assertWithMessage("Lifecycle should have been stopped")
                .that(stopCount)
                .isEqualTo(1)
        }
    }

    @Test
    fun lifecycleEventEffectTest_customLifecycleOwner() {
        val expectedEvent = Lifecycle.Event.ON_STOP
        var stopCount = 0

        composeTestRule.waitForIdle()
        composeTestRule.setContent {
            LifecycleEventEffect(expectedEvent, lifecycleOwner) {
                stopCount++
            }
        }

        composeTestRule.runOnIdle {
            lifecycleOwner.handleLifecycleEvent(expectedEvent)
            assertWithMessage("Lifecycle should have been stopped")
                .that(stopCount)
                .isEqualTo(1)
        }
    }

    @Test
    fun lifecycleStartEffectTest() {
        lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)
        var startCount = 0
        var stopCount = 0

        composeTestRule.waitForIdle()
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                LifecycleStartEffect(key1 = null) {
                    startCount++

                    onStopOrDispose {
                        stopCount++
                    }
                }
            }
        }

        composeTestRule.runOnIdle {
            assertWithMessage("Lifecycle should not be started (or stopped)")
                .that(startCount)
                .isEqualTo(0)

            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            assertWithMessage("Lifecycle should have been started")
                .that(startCount)
                .isEqualTo(1)

            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            assertWithMessage("Lifecycle should have been stopped")
                .that(stopCount)
                .isEqualTo(1)
        }
    }

    @Test
    fun lifecycleStartEffectTest_disposal() {
        lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)
        var startCount = 0
        var stopCount = 0
        var disposalCount = 0
        lateinit var state: MutableState<Boolean>

        composeTestRule.waitForIdle()
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                state = remember { mutableStateOf(true) }
                if (state.value) {
                    LifecycleStartEffect(null) {
                        startCount++

                        onStopOrDispose {
                            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                                disposalCount++
                            } else {
                                stopCount++
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.runOnIdle {
            assertWithMessage("Lifecycle should not yet be started (or stopped)")
                .that(startCount)
                .isEqualTo(0)

            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            assertWithMessage("Lifecycle should have been started")
                .that(startCount)
                .isEqualTo(1)
        }

        runOnUiThread {
            // Change state, kicking off cleanup
            state.value = false
        }

        composeTestRule.runOnIdle {
            assertWithMessage("ON_START effect work should have been cleaned up")
                .that(disposalCount)
                .isEqualTo(1)

            assertWithMessage("Lifecycle should not have been stopped")
                .that(stopCount)
                .isEqualTo(0)

            assertWithMessage("Lifecycle should not have been re-started")
                .that(startCount)
                .isEqualTo(1)
        }
    }

    @Test
    fun lifecycleStartEffectTest_disposal_onKeyChange() {
        lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)
        val key = mutableStateOf(true)
        var startCount = 0
        var stopCount = 0
        var disposalCount = 0

        composeTestRule.waitForIdle()
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                LifecycleStartEffect(key.value) {
                    startCount++

                    onStopOrDispose {
                        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                            disposalCount++
                        } else {
                            stopCount++
                        }
                    }
                }
            }
        }

        composeTestRule.runOnIdle {
            assertWithMessage("Lifecycle should not yet be started (or stopped)")
                .that(startCount)
                .isEqualTo(0)

            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            assertWithMessage("Lifecycle should have been started")
                .that(startCount)
                .isEqualTo(1)
        }

        runOnUiThread {
            // Change key, kicking off cleanup
            key.value = false
        }

        composeTestRule.runOnIdle {
            assertWithMessage("ON_START effect work should have been cleaned up")
                .that(disposalCount)
                .isEqualTo(1)

            assertWithMessage("Lifecycle should have been re-started")
                .that(startCount)
                .isEqualTo(2)

            assertWithMessage("Lifecycle should never have been stopped (only disposed)")
                .that(stopCount)
                .isEqualTo(0)
        }
    }

    @Test
    fun lifecycleStartEffectTest_effectsLambdaUpdate() {
        lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)
        val state = mutableStateOf("default")

        composeTestRule.waitForIdle()
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                LifecycleStartEffect(key1 = null) {
                    state.value += " started"

                    onStopOrDispose {
                        state.value += " disposed"
                    }
                }
            }
        }

        runOnUiThread {
            state.value = "updated"
        }

        composeTestRule.runOnIdle {
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)

            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            assertThat(state.value)
                .isEqualTo("updated started disposed")
        }
    }

    @Test
    fun lifecycleStartEffectTest_effectsLambdaAndKeyChange() {
        lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)
        var state = mutableStateOf("default")

        composeTestRule.waitForIdle()
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                LifecycleStartEffect(key1 = state) {
                    state.value += " started"

                    onStopOrDispose {
                        state.value += " disposed"
                    }
                }
            }
        }

        runOnUiThread {
            state = mutableStateOf("changed")
        }

        composeTestRule.runOnIdle {
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)

            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            assertThat(state.value)
                .isEqualTo("changed started disposed")
        }
    }

    @Test
    fun lifecycleResumeEffectTest() {
        var resumeCount = 0
        var pauseCount = 0

        composeTestRule.waitForIdle()
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                LifecycleResumeEffect {
                    resumeCount++

                    onPauseOrDispose {
                        pauseCount++
                    }
                }
            }
        }

        composeTestRule.runOnIdle {
            assertWithMessage("Lifecycle should not be resumed (or paused)")
                .that(resumeCount)
                .isEqualTo(0)

            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            assertWithMessage("Lifecycle should have been resumed")
                .that(resumeCount)
                .isEqualTo(1)

            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            assertWithMessage("Lifecycle should have been paused")
                .that(pauseCount)
                .isEqualTo(1)
        }
    }

    @Test
    fun lifecycleResumeEffectTest_disposal() {
        lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)
        var resumeCount = 0
        var pauseCount = 0
        var disposalCount = 0
        lateinit var state: MutableState<Boolean>

        composeTestRule.waitForIdle()
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                state = remember { mutableStateOf(true) }
                if (state.value) {
                    LifecycleResumeEffect {
                        resumeCount++

                        onPauseOrDispose {
                            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                                disposalCount++
                            } else {
                                pauseCount++
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.runOnIdle {
            assertWithMessage("Lifecycle should not be resumed (or paused)")
                .that(resumeCount)
                .isEqualTo(0)

            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            assertWithMessage("Lifecycle should have been resumed")
                .that(resumeCount)
                .isEqualTo(1)
        }

        runOnUiThread {
            // Change state, kicking off cleanup
            state.value = false
        }

        composeTestRule.runOnIdle {
            assertWithMessage("ON_RESUME effect work should have been cleaned up")
                .that(disposalCount)
                .isEqualTo(1)

            assertWithMessage("Lifecycle should not have been paused")
                .that(pauseCount)
                .isEqualTo(0)

            assertWithMessage("Lifecycle should not have been resumed")
                .that(resumeCount)
                .isEqualTo(1)
        }
    }

    @Test
    fun lifecycleResumeEffectTest_disposal_onKeyChange() {
        lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)
        val key = mutableStateOf(true)
        var resumeCount = 0
        var pauseCount = 0
        var disposalCount = 0

        composeTestRule.waitForIdle()
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                LifecycleResumeEffect(key.value) {
                    resumeCount++

                    onPauseOrDispose {
                        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            disposalCount++
                        } else {
                            pauseCount++
                        }
                    }
                }
            }
        }

        composeTestRule.runOnIdle {
            assertWithMessage("Lifecycle should not yet be resumed (or paused)")
                .that(resumeCount)
                .isEqualTo(0)

            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            assertWithMessage("Lifecycle should have been resumed")
                .that(resumeCount)
                .isEqualTo(1)
        }

        runOnUiThread {
            // Change key, kicking off cleanup
            key.value = false
        }

        composeTestRule.runOnIdle {
            assertWithMessage("ON_RESUME effect work should have been cleaned up")
                .that(disposalCount)
                .isEqualTo(1)

            assertWithMessage("Lifecycle should have been resumed again")
                .that(resumeCount)
                .isEqualTo(2)

            assertWithMessage("Lifecycle should never have been paused (only disposed)")
                .that(pauseCount)
                .isEqualTo(0)
        }
    }

    @Test
    fun lifecycleResumeEffectTest_effectsLambdaUpdate() {
        lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)
        val state = mutableStateOf("default")

        composeTestRule.waitForIdle()
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                LifecycleResumeEffect(key1 = null) {
                    state.value += " resumed"

                    onPauseOrDispose {
                        state.value += " disposed"
                    }
                }
            }
        }

        runOnUiThread {
            state.value = "updated"
        }

        composeTestRule.runOnIdle {
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            assertThat(state.value)
                .isEqualTo("updated resumed disposed")
        }
    }

    @Test
    fun lifecycleResumeEffectTest_effectsLambdaAndKeyChange() {
        lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)
        var state = mutableStateOf("default")

        composeTestRule.waitForIdle()
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                LifecycleResumeEffect(key1 = state) {
                    state.value += " resumed"

                    onPauseOrDispose {
                        state.value += " disposed"
                    }
                }
            }
        }

        runOnUiThread {
            state = mutableStateOf("changed")
        }

        composeTestRule.runOnIdle {
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            assertThat(state.value)
                .isEqualTo("changed resumed disposed")
        }
    }
}
