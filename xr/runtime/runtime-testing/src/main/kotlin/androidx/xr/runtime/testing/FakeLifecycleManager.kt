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

package androidx.xr.runtime.testing

import androidx.xr.runtime.internal.LifecycleManager
import kotlin.time.ComparableTimeMark
import kotlin.time.TestTimeSource
import kotlinx.coroutines.sync.Semaphore

/** Test-only implementation of [LifecycleManager] used to validate state transitions. */
@Suppress("NotCloseable")
public class FakeLifecycleManager : LifecycleManager {

    /** Set of possible states of the runtime. */
    public enum class State {
        NOT_INITIALIZED,
        INITIALIZED,
        RESUMED,
        PAUSED,
        STOPPED,
    }

    /** The current state of the runtime. */
    public var state: FakeLifecycleManager.State = State.NOT_INITIALIZED
        private set

    /** The time source used for this runtime. */
    public val timeSource: TestTimeSource = TestTimeSource()

    private val semaphore = Semaphore(1)

    override fun create() {
        check(state == State.NOT_INITIALIZED)
        state = State.INITIALIZED
    }

    override fun configure() {
        check(state == State.INITIALIZED || state == State.RESUMED || state == State.PAUSED)
    }

    override fun resume() {
        check(state == State.INITIALIZED || state == State.PAUSED)
        state = State.RESUMED
    }

    /**
     * Retrieves the latest timemark. The first call to this method will execute immediately.
     * Subsequent calls will be blocked until [allowOneMoreCallToUpdate] is called.
     */
    override suspend fun update(): ComparableTimeMark {
        check(state == State.RESUMED)
        semaphore.acquire()
        return timeSource.markNow()
    }

    /**
     * Allows an additional call to [update] to not be blocked. Requires that [update] has been
     * called exactly once before each call to this method. Failure to do so will result in an
     * [IllegalStateException].
     */
    public fun allowOneMoreCallToUpdate() {
        semaphore.release()
    }

    override fun pause() {
        check(state == State.RESUMED)
        state = State.PAUSED
    }

    override fun stop() {
        check(state == State.PAUSED || state == State.INITIALIZED)
        state = State.STOPPED
    }
}
