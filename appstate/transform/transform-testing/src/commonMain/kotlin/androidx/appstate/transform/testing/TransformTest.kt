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

package androidx.appstate.transform.testing

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestResult

/**
 * Runs [block] as a test of [androidx.appstate.transform.transform].
 *
 * @param context context for the test coroutine
 * @param block the test code, run with a [TransformTestScope] receiver
 * @return the [TestResult] of the test
 */
public expect fun transformTest(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend TransformTestScope.() -> Unit,
): TestResult

/** A test scope used in tests that allows controlling and testing transforms. */
public sealed interface TransformTestScope : CoroutineScope {

    /** A scheduler used by [CoroutineScope] */
    public val testCoroutineScheduler: TestCoroutineScheduler

    /** The dispatcher to run transforms on, so that [advance] executes their transforms. */
    public val testDispatcher: TestDispatcher

    /**
     * Scope for work that must not block test completion, such as running transforms. Cancelled
     * when the test completes, which disposes the transforms running in it.
     */
    public val backgroundScope: CoroutineScope

    /** Advance the state which executes any pending transforms, if any. */
    public fun advance()

    /** Advance the clock by [duration] */
    public fun advanceTimeBy(duration: Duration)

    /** Advance the clock until no work is scheduled */
    public fun advanceUntilIdle()
}
