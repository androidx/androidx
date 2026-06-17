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

package androidx.compose.ui.test.v2

import androidx.compose.runtime.Immutable
import androidx.compose.ui.input.InputMode
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher

/**
 * Defines the configuration requirements for a Compose test environment.
 *
 * This configuration allows for fine-grained control over the test execution environment, including
 * the coroutine contexts used for composition and test execution, the overall test timeout, and the
 * initial input mode.
 *
 * @property effectContext The [CoroutineContext] used to run the composition. The context for
 *   `LaunchedEffect`s and `rememberCoroutineScope` will be derived from this context. If this
 *   context contains a [TestDispatcher] or [TestCoroutineScheduler] (in that order), it will be
 *   used for composition and the [androidx.compose.ui.test.MainTestClock]. Defaults to
 *   [EmptyCoroutineContext].
 * @property runTestContext The [CoroutineContext] used to create the context to run the test block.
 *   By default, test block will run using [kotlinx.coroutines.test.StandardTestDispatcher].
 *   [runTestContext] and [effectContext] must not share [TestCoroutineScheduler]. Defaults to
 *   [EmptyCoroutineContext].
 * @property testTimeout The [Duration] within which the test is expected to complete, otherwise a
 *   platform specific timeout exception will be thrown. Defaults to `60 seconds`.
 * @property inputMode The [InputMode] to be used for the test. This determines how input events
 *   (such as touch or keyboard) are injected and handled during the test execution. Defaults to
 *   [InputMode.Touch].
 */
@Immutable
actual class ComposeTestConfig
actual constructor(
    actual val effectContext: CoroutineContext,
    actual val runTestContext: CoroutineContext,
    actual val testTimeout: Duration,
    actual val inputMode: InputMode,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ComposeTestConfig) return false

        if (effectContext != other.effectContext) return false
        if (runTestContext != other.runTestContext) return false
        if (testTimeout != other.testTimeout) return false
        if (inputMode != other.inputMode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = effectContext.hashCode()
        result = 31 * result + runTestContext.hashCode()
        result = 31 * result + testTimeout.hashCode()
        result = 31 * result + inputMode.hashCode()
        return result
    }
}
