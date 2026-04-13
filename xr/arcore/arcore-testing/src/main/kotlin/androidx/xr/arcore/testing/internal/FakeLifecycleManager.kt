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

import androidx.xr.runtime.AnchorPersistenceMode
import androidx.xr.runtime.AugmentedObjectCategory
import androidx.xr.runtime.Config
import androidx.xr.runtime.DepthEstimationMode
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.HandTrackingMode
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.internal.LifecycleManager
import kotlin.time.ComparableTimeMark
import kotlin.time.TestTimeSource
import kotlinx.coroutines.sync.Semaphore

internal class FakeLifecycleManager : LifecycleManager {

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

    var state: State = State.NOT_INITIALIZED
        private set

    val timeSource: TestTimeSource = TestTimeSource()

    override fun create() {
        check(state == State.NOT_INITIALIZED)
        state = State.INITIALIZED
        allowOneMoreCallToUpdate()
    }

    override var config: Config =
        Config(
            PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
            HandTrackingMode.BOTH,
            DeviceTrackingMode.SPATIAL_LAST_KNOWN,
            DepthEstimationMode.SMOOTH_AND_RAW,
            AnchorPersistenceMode.LOCAL,
            // Needs to contain at least one AugmentedObjectCategory to enable
            augmentedObjectCategories = setOf(AugmentedObjectCategory.MOUSE),
        )

    override fun configure(config: Config) {
        check(
            state == State.NOT_INITIALIZED ||
                state == State.INITIALIZED ||
                state == State.RESUMED ||
                state == State.PAUSED
        )

        this.config = config
        allowOneMoreCallToUpdate()
    }

    override fun resume() {
        check(state == State.INITIALIZED || state == State.PAUSED)
        state = State.RESUMED
    }

    override suspend fun update(): ComparableTimeMark {
        check(state == State.RESUMED)
        semaphore.acquire()
        return timeSource.markNow()
    }

    override fun pause() {
        check(state == State.RESUMED)
        state = State.PAUSED
    }

    override fun stop() {
        check(state == State.PAUSED || state == State.INITIALIZED)
        state = State.DESTROYED
    }
}
