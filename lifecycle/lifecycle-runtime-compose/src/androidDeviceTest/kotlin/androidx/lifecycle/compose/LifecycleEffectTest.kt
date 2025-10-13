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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.kruth.assertThat
import androidx.kruth.assertWithMessage
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class LifecycleEffectTest {

    private lateinit var lifecycleOwner: TestLifecycleOwner

    @BeforeTest
    fun setup() {
        lifecycleOwner = TestLifecycleOwner()
    }

    @Test
    fun lifecycleEventEffectTest_noEvent() = runComposeUiTest {
        var stopCount = 0

        waitForIdle()
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                LifecycleEventEffect(Lifecycle.Event.ON_STOP) { stopCount++ }
            }
        }

        runOnIdle {
            assertWithMessage("Lifecycle should not have been stopped").that(stopCount).isEqualTo(0)
        }
    }

    @Test
    fun lifecycleEventEffectTest_localLifecycleOwner() = runComposeUiTest {
        val expectedEvent = Lifecycle.Event.ON_STOP
        var stopCount = 0

        waitForIdle()
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                LifecycleEventEffect(expectedEvent) { stopCount++ }
            }
        }

        runOnIdle {
            lifecycleOwner.handleLifecycleEvent(expectedEvent)
            assertWithMessage("Lifecycle should have been stopped").that(stopCount).isEqualTo(1)
        }
    }

    @Test
    fun lifecycleEventEffectTest_customLifecycleOwner() = runComposeUiTest {
        val expectedEvent = Lifecycle.Event.ON_STOP
        var stopCount = 0

        waitForIdle()
        setContent { LifecycleEventEffect(expectedEvent, lifecycleOwner) { stopCount++ } }

        runOnIdle {
            lifecycleOwner.handleLifecycleEvent(expectedEvent)
            assertWithMessage("Lifecycle should have been stopped").that(stopCount).isEqualTo(1)
        }
    }

    @Test
    fun lifecycleEventEffectTest_onPause_beforeOnDispose_isIdempotent() = runComposeUiTest {
        lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)

        var visible by mutableStateOf(true)
        var stopCount = 0

        waitForIdle()
        setContent {
            if (visible) {
                LifecycleEventEffect(Lifecycle.Event.ON_PAUSE, lifecycleOwner) { stopCount++ }
            }
        }

        runOnIdle {
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            visible = false
        }

        runOnIdle { assertThat(stopCount).isEqualTo(1) }
    }

    @Test
    fun lifecycleEventEffectTest_onDestroy_beforeOnDispose_isIdempotent() = runComposeUiTest {
        lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)

        var visible by mutableStateOf(true)
        var stopCount = 0

        waitForIdle()
        setContent {
            if (visible) {
                LifecycleEventEffect(Lifecycle.Event.ON_PAUSE, lifecycleOwner) { stopCount++ }
            }
        }

        runOnIdle {
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            visible = false
        }

        runOnIdle { assertThat(stopCount).isEqualTo(1) }
    }

    @Test
    fun lifecycleEventEffectTest_onDispose_beforeOnDestroy_isIdempotent() = runComposeUiTest {
        lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)

        var visible by mutableStateOf(true)
        var stopCount = 0

        waitForIdle()
        setContent {
            if (visible) {
                LifecycleEventEffect(Lifecycle.Event.ON_PAUSE, lifecycleOwner) { stopCount++ }
            }
        }

        runOnIdle {
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            visible = false
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }

        runOnIdle { assertThat(stopCount).isEqualTo(1) }
    }

    @Test
    fun lifecycleStartEffectTest() = runComposeUiTest {
        lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)
        var startCount = 0
        var stopCount = 0

        waitForIdle()
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                LifecycleStartEffect(key1 = null) {
                    startCount++

                    onStopOrDispose { stopCount++ }
                }
            }
        }

        runOnIdle {
            assertWithMessage("Lifecycle should not be started (or stopped)")
                .that(startCount)
                .isEqualTo(0)

            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            assertWithMessage("Lifecycle should have been started").that(startCount).isEqualTo(1)

            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            assertWithMessage("Lifecycle should have been stopped").that(stopCount).isEqualTo(1)
        }
    }

    @Test
    fun lifecycleStartEffectTest_disposal() = runComposeUiTest {
        lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)
        var startCount = 0
        var stopCount = 0
        var disposalCount = 0
        lateinit var state: MutableState<Boolean>

        waitForIdle()
        setContent {
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

        runOnIdle {
            assertWithMessage("Lifecycle should not yet be started (or stopped)")
                .that(startCount)
                .isEqualTo(0)

            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            assertWithMessage("Lifecycle should have been started").that(startCount).isEqualTo(1)
        }

        runOnUiThread {
            // Change state, kicking off cleanup
            state.value = false
        }

        runOnIdle {
            assertWithMessage("ON_START effect work should have been cleaned up")
                .that(disposalCount)
                .isEqualTo(1)

            assertWithMessage("Lifecycle should not have been stopped").that(stopCount).isEqualTo(0)

            assertWithMessage("Lifecycle should not have been re-started")
                .that(startCount)
                .isEqualTo(1)
        }
    }

    @Test
    fun lifecycleStartEffectTest_disposal_onKeyChange() = runComposeUiTest {
        lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)
        val key = mutableStateOf(true)
        var startCount = 0
        var stopCount = 0
        var disposalCount = 0

        waitForIdle()
        setContent {
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

        runOnIdle {
            assertWithMessage("Lifecycle should not yet be started (or stopped)")
                .that(startCount)
                .isEqualTo(0)

            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            assertWithMessage("Lifecycle should have been started").that(startCount).isEqualTo(1)
        }

        runOnUiThread {
            // Change key, kicking off cleanup
            key.value = false
        }

        runOnIdle {
            assertWithMessage("ON_START effect work should have been cleaned up")
                .that(disposalCount)
                .isEqualTo(1)

            assertWithMessage("Lifecycle should have been re-started").that(startCount).isEqualTo(2)

            assertWithMessage("Lifecycle should never have been stopped (only disposed)")
                .that(stopCount)
                .isEqualTo(0)
        }
    }

    @Test
    fun lifecycleStartEffectTest_disposal_onLifecycleOwner() = runComposeUiTest {
        val secondLifecycleOwner = TestLifecycleOwner()
        val ownerToUse = mutableStateOf("first")
        val startedLifecycles = mutableListOf<Lifecycle>()
        val stoppedLifecycles = mutableListOf<Lifecycle>()

        waitForIdle()
        setContent {
            CompositionLocalProvider(
                LocalLifecycleOwner provides
                    if (ownerToUse.value == "first") lifecycleOwner else secondLifecycleOwner
            ) {
                LifecycleStartEffect(key1 = null) {
                    startedLifecycles += lifecycle

                    onStopOrDispose { stoppedLifecycles += lifecycle }
                }
            }
        }

        runOnIdle {
            assertWithMessage("Lifecycle should be started")
                .that(startedLifecycles)
                .containsExactly(lifecycleOwner.lifecycle)

            ownerToUse.value = "second"
        }

        runOnIdle {
            assertWithMessage("Swapped out LifecycleOwner should be stopped")
                .that(stoppedLifecycles)
                .containsExactly(lifecycleOwner.lifecycle)

            assertWithMessage("Swapped in LifecycleOwner should be started")
                .that(startedLifecycles)
                .containsExactly(lifecycleOwner.lifecycle, secondLifecycleOwner.lifecycle)
                .inOrder()
        }
    }

    @Test
    fun lifecycleStartEffectTest_effectsLambdaUpdate() = runComposeUiTest {
        lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)
        val state = mutableStateOf("default")

        waitForIdle()
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                LifecycleStartEffect(key1 = null) {
                    state.value += " started"

                    onStopOrDispose { state.value += " disposed" }
                }
            }
        }

        runOnUiThread { state.value = "updated" }

        runOnIdle {
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)

            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            assertThat(state.value).isEqualTo("updated started disposed")
        }
    }

    @Test
    fun lifecycleStartEffectTest_effectsLambdaAndKeyChange() = runComposeUiTest {
        lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)
        var state = mutableStateOf("default")

        waitForIdle()
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                LifecycleStartEffect(key1 = state) {
                    state.value += " started"

                    onStopOrDispose { state.value += " disposed" }
                }
            }
        }

        runOnUiThread { state = mutableStateOf("changed") }

        runOnIdle {
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)

            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            assertThat(state.value).isEqualTo("changed started disposed")
        }
    }

    @Test
    fun lifecycleStartEffect_onStop_beforeOnDispose_isIdempotent() = runComposeUiTest {
        lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)

        var visible by mutableStateOf(true)
        var stopCount = 0

        waitForIdle()
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                if (visible) {
                    LifecycleStartEffect(key1 = "key1") { onStopOrDispose { stopCount++ } }
                }
            }
        }

        runOnIdle {
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            visible = false
        }

        runOnIdle { assertThat(stopCount).isEqualTo(1) }
    }

    @Test
    fun lifecycleStartEffect_onDestroy_beforeOnDispose_isIdempotent() = runComposeUiTest {
        lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)

        var visible by mutableStateOf(true)
        var stopCount = 0

        waitForIdle()
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                if (visible) {
                    LifecycleStartEffect(key1 = "key1") { onStopOrDispose { stopCount++ } }
                }
            }
        }

        runOnIdle {
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            visible = false
        }

        runOnIdle { assertThat(stopCount).isEqualTo(1) }
    }

    @Test
    fun lifecycleStartEffect_onDispose_beforeOnDestroy_isIdempotent() = runComposeUiTest {
        lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)

        var visible by mutableStateOf(true)
        var stopCount = 0

        waitForIdle()
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                if (visible) {
                    LifecycleStartEffect(key1 = "key1") { onStopOrDispose { stopCount++ } }
                }
            }
        }

        runOnIdle {
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            visible = false
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }

        runOnIdle { assertThat(stopCount).isEqualTo(1) }
    }

    @Test
    fun lifecycleResumeEffectTest() = runComposeUiTest {
        var resumeCount = 0
        var pauseCount = 0

        waitForIdle()
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                LifecycleResumeEffect(key1 = null) {
                    resumeCount++

                    onPauseOrDispose { pauseCount++ }
                }
            }
        }

        runOnIdle {
            assertWithMessage("Lifecycle should not be resumed (or paused)")
                .that(resumeCount)
                .isEqualTo(0)

            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            assertWithMessage("Lifecycle should have been resumed").that(resumeCount).isEqualTo(1)

            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            assertWithMessage("Lifecycle should have been paused").that(pauseCount).isEqualTo(1)
        }
    }

    @Test
    fun lifecycleResumeEffectTest_disposal() = runComposeUiTest {
        lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)
        var resumeCount = 0
        var pauseCount = 0
        var disposalCount = 0
        lateinit var state: MutableState<Boolean>

        waitForIdle()
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                state = remember { mutableStateOf(true) }
                if (state.value) {
                    LifecycleResumeEffect(key1 = null) {
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

        runOnIdle {
            assertWithMessage("Lifecycle should not be resumed (or paused)")
                .that(resumeCount)
                .isEqualTo(0)

            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            assertWithMessage("Lifecycle should have been resumed").that(resumeCount).isEqualTo(1)
        }

        runOnUiThread {
            // Change state, kicking off cleanup
            state.value = false
        }

        runOnIdle {
            assertWithMessage("ON_RESUME effect work should have been cleaned up")
                .that(disposalCount)
                .isEqualTo(1)

            assertWithMessage("Lifecycle should not have been paused").that(pauseCount).isEqualTo(0)

            assertWithMessage("Lifecycle should not have been resumed")
                .that(resumeCount)
                .isEqualTo(1)
        }
    }

    @Test
    fun lifecycleResumeEffectTest_disposal_onKeyChange() = runComposeUiTest {
        lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)
        val key = mutableStateOf(true)
        var resumeCount = 0
        var pauseCount = 0
        var disposalCount = 0

        waitForIdle()
        setContent {
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

        runOnIdle {
            assertWithMessage("Lifecycle should not yet be resumed (or paused)")
                .that(resumeCount)
                .isEqualTo(0)

            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            assertWithMessage("Lifecycle should have been resumed").that(resumeCount).isEqualTo(1)
        }

        runOnUiThread {
            // Change key, kicking off cleanup
            key.value = false
        }

        runOnIdle {
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
    fun lifecycleResumeEffectTest_disposal_onLifecycleOwnerChange() = runComposeUiTest {
        lifecycleOwner.currentState = Lifecycle.State.RESUMED
        val secondLifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)
        val ownerToUse = mutableStateOf("first")
        val resumedLifecycles = mutableListOf<Lifecycle>()
        val pausedLifecycles = mutableListOf<Lifecycle>()

        waitForIdle()
        setContent {
            CompositionLocalProvider(
                LocalLifecycleOwner provides
                    if (ownerToUse.value == "first") lifecycleOwner else secondLifecycleOwner
            ) {
                LifecycleResumeEffect(key1 = null) {
                    resumedLifecycles += lifecycle

                    onPauseOrDispose { pausedLifecycles += lifecycle }
                }
            }
        }

        runOnIdle {
            assertWithMessage("Lifecycle should be resumed")
                .that(resumedLifecycles)
                .containsExactly(lifecycleOwner.lifecycle)

            ownerToUse.value = "second"
        }

        runOnIdle {
            assertWithMessage("Swapped out LifecycleOwner should be paused")
                .that(pausedLifecycles)
                .containsExactly(lifecycleOwner.lifecycle)

            assertWithMessage("Swapped in LifecycleOwner should be resumed")
                .that(resumedLifecycles)
                .containsExactly(lifecycleOwner.lifecycle, secondLifecycleOwner.lifecycle)
                .inOrder()
        }
    }

    @Test
    fun lifecycleResumeEffectTest_effectsLambdaUpdate() = runComposeUiTest {
        lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)
        val state = mutableStateOf("default")

        waitForIdle()
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                LifecycleResumeEffect(key1 = null) {
                    state.value += " resumed"

                    onPauseOrDispose { state.value += " disposed" }
                }
            }
        }

        runOnUiThread { state.value = "updated" }

        runOnIdle {
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            assertThat(state.value).isEqualTo("updated resumed disposed")
        }
    }

    @Test
    fun lifecycleResumeEffectTest_effectsLambdaAndKeyChange() = runComposeUiTest {
        lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)
        var state = mutableStateOf("default")

        waitForIdle()
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                LifecycleResumeEffect(key1 = state) {
                    state.value += " resumed"

                    onPauseOrDispose { state.value += " disposed" }
                }
            }
        }

        runOnUiThread { state = mutableStateOf("changed") }

        runOnIdle {
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            assertThat(state.value).isEqualTo("changed resumed disposed")
        }
    }

    @Test
    fun lifecycleResumeEffect_onPause_beforeOnDispose_isIdempotent() = runComposeUiTest {
        lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)

        var visible by mutableStateOf(true)
        var stopCount = 0

        waitForIdle()
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                if (visible) {
                    LifecycleResumeEffect(key1 = "key1") { onPauseOrDispose { stopCount++ } }
                }
            }
        }

        runOnIdle {
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            visible = false
        }

        runOnIdle { assertThat(stopCount).isEqualTo(1) }
    }

    @Test
    fun lifecycleResumeEffect_onDispose_beforeOnPause_isIdempotent() = runComposeUiTest {
        lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)

        var visible by mutableStateOf(true)
        var stopCount = 0

        waitForIdle()
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                if (visible) {
                    LifecycleResumeEffect(key1 = "key1") { onPauseOrDispose { stopCount++ } }
                }
            }
        }

        runOnIdle {
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            visible = false
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }

        runOnIdle { assertThat(stopCount).isEqualTo(1) }
    }

    @Test
    fun lifecycleResumeEffect_onDestroy_beforeOnDispose_isIdempotent() = runComposeUiTest {
        lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)

        var visible by mutableStateOf(true)
        var stopCount = 0

        waitForIdle()
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                if (visible) {
                    LifecycleResumeEffect(key1 = "key1") { onPauseOrDispose { stopCount++ } }
                }
            }
        }

        runOnIdle {
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            visible = false
        }

        runOnIdle { assertThat(stopCount).isEqualTo(1) }
    }

    @Test
    fun lifecycleResumeEffect_onDispose_beforeOnDestroy_isIdempotent() = runComposeUiTest {
        lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)

        var visible by mutableStateOf(true)
        var stopCount = 0

        waitForIdle()
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                if (visible) {
                    LifecycleResumeEffect(key1 = "key1") { onPauseOrDispose { stopCount++ } }
                }
            }
        }

        runOnIdle {
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            visible = false
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }

        runOnIdle { assertThat(stopCount).isEqualTo(1) }
    }
}
