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

package androidx.xr.arcore.testing.internal

import androidx.xr.arcore.runtime.PerceptionRuntime
import androidx.xr.runtime.Config
import androidx.xr.runtime.DisplayBlendMode
import kotlin.time.ComparableTimeMark
import kotlin.time.TestTimeSource
import kotlinx.coroutines.sync.Semaphore

internal class FakePerceptionRuntime(override val perceptionManager: FakePerceptionManager) :
    PerceptionRuntime {
    var xrDevicePreferredDisplayBlendMode: DisplayBlendMode = DisplayBlendMode.NO_DISPLAY

    companion object {
        private val semaphore = Semaphore(1)

        @JvmStatic
        internal fun allowOneMoreCallToUpdate() {
            if (semaphore.availablePermits == 0) {
                semaphore.release()
            }
        }
    }

    /** Set of possible states of the runtime. */
    enum class State {
        NOT_INITIALIZED,
        INITIALIZED,
        RESUMED,
        PAUSED,
        DESTROYED,
    }

    /** The current state of the runtime. */
    var state: State = State.NOT_INITIALIZED
        private set

    /** The time source used for this runtime. */
    val timeSource: TestTimeSource = TestTimeSource()

    val pendingTrackableProviders: MutableSet<PendingTrackablesProvider> = mutableSetOf()

    override var config: Config = Config()

    override fun initialize() {
        check(state == State.NOT_INITIALIZED)
        state = State.INITIALIZED
        allowOneMoreCallToUpdate()
    }

    override fun configure(config: Config) {
        check(
            state == State.NOT_INITIALIZED ||
                state == State.INITIALIZED ||
                state == State.RESUMED ||
                state == State.PAUSED
        )

        this.config = config

        perceptionManager.updateTrackingStates(config)
        allowOneMoreCallToUpdate()
    }

    override fun getPreferredDisplayBlendMode(): DisplayBlendMode {
        return xrDevicePreferredDisplayBlendMode
    }

    override fun resume() {
        check(state == State.INITIALIZED || state == State.PAUSED)
        state = State.RESUMED
    }

    /**
     * Retrieves the latest time mark. The first call to this method will execute immediately.
     * Subsequent calls will be blocked until [allowOneMoreCallToUpdate] is called.
     */
    override suspend fun update(): ComparableTimeMark {
        check(state == State.RESUMED)
        semaphore.acquire()

        perceptionManager.updateTrackingStates(config)

        // Move any pending Trackable objects to PerceptionManager
        pendingTrackableProviders.forEach { provider ->
            perceptionManager.trackables.addAll(provider.getPendingTrackables())
        }

        return timeSource.markNow()
    }

    override fun pause() {
        check(state == State.RESUMED)
        state = State.PAUSED
    }

    override fun destroy() {
        check(state == State.PAUSED || state == State.INITIALIZED)
        state = State.DESTROYED
        pendingTrackableProviders.clear()
    }

    internal fun addPendingTrackableProvider(provider: PendingTrackablesProvider) {
        pendingTrackableProviders.add(provider)
    }
}
