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

import androidx.compose.runtime.snapshots.Snapshot
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

internal fun runTransformTest(
    context: CoroutineContext,
    block: suspend TransformTestScope.() -> Unit,
): TestResult =
    runTest(context) {
        TransformTestScopeImpl(this).block()
    }

private class TransformTestScopeImpl(private val testScope: TestScope) :
    TransformTestScope, CoroutineScope by testScope {

    override val testCoroutineScheduler: TestCoroutineScheduler
        get() = testScope.testScheduler

    override val testDispatcher: TestDispatcher =
        testScope.coroutineContext[ContinuationInterceptor] as TestDispatcher

    override val backgroundScope: CoroutineScope
        get() = testScope.backgroundScope

    override fun advance() {
        testCoroutineScheduler.runCurrent()
        Snapshot.sendApplyNotifications()
        testCoroutineScheduler.runCurrent()
    }

    override fun advanceTimeBy(duration: Duration) = testCoroutineScheduler.advanceTimeBy(duration)

    override fun advanceUntilIdle() = testCoroutineScheduler.advanceUntilIdle()
}
