/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.arcore.testing

import androidx.annotation.RestrictTo
import androidx.xr.runtime.Config
import androidx.xr.runtime.internal.LifecycleManager
import kotlin.time.ComparableTimeMark
import kotlin.time.TestTimeSource

/**
 * Fake implementation of [LifecycleManager] used to validate state transitions.
 *
 * @property hasCreatePermission if false, [create] will throw an exception during testing
 * @property state the current [State] of the runtime
 * @property timeSource the [TestTimeSource] used for this runtime
 * @property hasMissingPermission if true, [configure] will emulate the failure case for missing
 *   permissions
 * @property shouldSupportPlaneTracking if false, [configure] will throw an exception if the config
 *   enables plane tracking
 * @property config the current [Config] of the session
 */
@Suppress("NotCloseable")
@Deprecated(
    "arcore-testing fakes have been moved internal and should no longer be used by unit tests."
)
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class FakeLifecycleManager(
    @Suppress("DEPRECATION") private val owner: FakePerceptionRuntime
) : LifecycleManager {

    public companion object {
        @Suppress("DEPRECATION")
        @JvmField
        public val TestPermissions: List<String> = FakePerceptionRuntime.TestPermissions
    }

    /** Set of possible states of the runtime. */
    public enum class State {
        NOT_INITIALIZED,
        INITIALIZED,
        RESUMED,
        PAUSED,
        DESTROYED,
    }

    @get:Suppress("DEPRECATION")
    public val state: State
        get() {
            return when (owner.state) {
                FakePerceptionRuntime.State.NOT_INITIALIZED -> State.NOT_INITIALIZED
                FakePerceptionRuntime.State.INITIALIZED -> State.INITIALIZED
                FakePerceptionRuntime.State.RESUMED -> State.RESUMED
                FakePerceptionRuntime.State.PAUSED -> State.PAUSED
                FakePerceptionRuntime.State.DESTROYED -> State.DESTROYED
            }
        }

    public val timeSource: TestTimeSource
        get() {
            return owner.timeSource
        }

    @get:JvmName("hasMissingPermission")
    public var hasMissingPermission: Boolean
        get() {
            return owner.hasMissingPermission
        }
        set(value) {
            owner.hasMissingPermission = value
        }

    @get:JvmName("shouldSupportPlaneTracking")
    public var shouldSupportPlaneTracking: Boolean
        get() {
            return owner.shouldSupportPlaneTracking
        }
        set(value) {
            owner.shouldSupportPlaneTracking = value
        }

    @get:JvmName("shouldSupportFaceTracking")
    public var shouldSupportFaceTracking: Boolean
        get() {
            return owner.shouldSupportFaceTracking
        }
        set(value) {
            owner.shouldSupportFaceTracking = value
        }

    @Suppress("DEPRECATION")
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

    /**
     * Allows an additional call to [update] to not be blocked.
     *
     * Requires that [update] has been called exactly once before each call to this method. Failure
     * to do so will result in an [IllegalStateException].
     */
    public fun allowOneMoreCallToUpdate() {
        owner.allowOneMoreCallToUpdate()
    }

    override fun pause() {
        owner.pause()
    }

    override fun stop() {
        owner.destroy()
    }
}
