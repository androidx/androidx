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

import androidx.annotation.RestrictTo
import androidx.xr.runtime.Config
import androidx.xr.runtime.internal.LifecycleManager
import kotlin.time.ComparableTimeMark
import kotlin.time.TestTimeSource

internal class FakeLifecycleManager(private val owner: FakePerceptionRuntime) : LifecycleManager {
    internal companion object {
        internal fun allowOneMoreCallToUpdate() {
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

        @JvmField internal val TestPermissions: List<String> = FakePerceptionRuntime.TestPermissions
    }

    /** Set of possible states of the runtime. */
    internal enum class State {
        NOT_INITIALIZED,
        INITIALIZED,
        RESUMED,
        PAUSED,
        DESTROYED,
    }

    val state: State
        get() {
            return when (owner.state) {
                FakePerceptionRuntime.State.NOT_INITIALIZED -> State.NOT_INITIALIZED
                FakePerceptionRuntime.State.INITIALIZED -> State.INITIALIZED
                FakePerceptionRuntime.State.RESUMED -> State.RESUMED
                FakePerceptionRuntime.State.PAUSED -> State.PAUSED
                FakePerceptionRuntime.State.DESTROYED -> State.DESTROYED
            }
        }

    val timeSource: TestTimeSource
        get() {
            return owner.timeSource
        }

    @get:JvmName("hasMissingPermission")
    var hasMissingPermission: Boolean
        get() {
            return owner.hasMissingPermission
        }
        set(value) {
            owner.hasMissingPermission = value
        }

    @get:JvmName("shouldSupportPlaneTracking")
    var shouldSupportPlaneTracking: Boolean
        get() {
            return owner.shouldSupportPlaneTracking
        }
        set(value) {
            owner.shouldSupportPlaneTracking = value
        }

    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @get:JvmName("shouldSupportFaceTracking")
    var shouldSupportFaceTracking: Boolean
        get() {
            return owner.shouldSupportFaceTracking
        }
        set(value) {
            owner.shouldSupportFaceTracking = value
        }

    override fun create() {
        owner.initialize()
    }

    override var config: Config
        get() {
            return owner.config
        }
        set(value) {
            owner.config = value
        }

    override fun configure(config: Config) {
        owner.configure(config)
    }

    override fun resume() {
        owner.resume()
    }

    /**
     * Retrieves the latest time mark.
     *
     * The first call to this method will execute immediately. Subsequent calls will be blocked
     * until [allowOneMoreCallToUpdate] is called.
     */
    override suspend fun update(): ComparableTimeMark {
        return owner.update()
    }

    override fun pause() {
        owner.pause()
    }

    override fun stop() {
        owner.destroy()
    }
}
