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

package androidx.camera.camera2.pipe.testing

import kotlin.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle

/**
 * TestThreadScope is an interface intended to be supplied to the CameraPipe simulators to enable
 * automatic task execution within a test environment.
 */
public interface TestThreadScope {
    /**
     * Advance the virtual clock by the specified duration.
     *
     * [advanceTimeBy] is expected advance the virtual clock, start the expectation of pending tasks
     * scheduled to be run within the advanced time and *wait* for the completion of these tasks.
     * This means after the return of this method, it is expected that, barring any tasks that
     * weren't scheduled to be run (e.g., a delayed task that is scheduled past the current virtual
     * clock), all tasks should be run and completed.
     */
    public fun advanceTimeBy(duration: Duration)

    /** Advance until there are no tasks remaining. */
    public fun advanceUntilIdle(timeout: Duration? = null)

    public companion object {
        @JvmStatic
        public fun from(testScope: TestScope): TestThreadScope = TestThreadScopeImpl(testScope)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
public class TestThreadScopeImpl internal constructor(private val testScope: TestScope) :
    TestThreadScope {
    override fun advanceTimeBy(duration: Duration) {
        testScope.advanceTimeBy(duration)
    }

    override fun advanceUntilIdle(timeout: Duration?) {
        testScope.advanceUntilIdle()
    }
}
