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
import androidx.xr.runtime.AnchorPersistenceMode
import androidx.xr.runtime.AugmentedObjectCategory
import androidx.xr.runtime.Config
import androidx.xr.runtime.DepthEstimationMode
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.FaceTrackingMode
import androidx.xr.runtime.HandTrackingMode
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.internal.LifecycleManager
import kotlin.time.ComparableTimeMark
import kotlin.time.TestTimeSource
import kotlinx.coroutines.sync.Semaphore

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
public class FakeLifecycleManager(
    @get:JvmName("hasCreatePermission") public var hasCreatePermission: Boolean = true
) : LifecycleManager {

    public companion object {
        @JvmField
        public val TestPermissions: List<String> =
            listOf("android.permission.SCENE_UNDERSTANDING_COARSE")
    }

    /** Set of possible states of the runtime. */
    public enum class State {
        NOT_INITIALIZED,
        INITIALIZED,
        RESUMED,
        PAUSED,
        DESTROYED,
    }

    public var state: State = State.NOT_INITIALIZED
        private set

    public val timeSource: TestTimeSource = TestTimeSource()

    private val semaphore = Semaphore(1)

    @get:JvmName("hasMissingPermission") public var hasMissingPermission: Boolean = false

    @get:JvmName("shouldSupportPlaneTracking") public var shouldSupportPlaneTracking: Boolean = true

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @get:JvmName("shouldSupportFaceTracking")
    public var shouldSupportFaceTracking: Boolean = true

    override fun create() {
        check(state == State.NOT_INITIALIZED)
        if (!hasCreatePermission) throw SecurityException()
        if (FakePerceptionRuntimeFactory.lifecycleCreateException != null) {
            // FakeRuntimeFactory will continue to throw exception on subsequent tests unless
            // cleared.
            val exceptionToThrow = FakePerceptionRuntimeFactory.lifecycleCreateException!!
            FakePerceptionRuntimeFactory.lifecycleCreateException = null
            throw exceptionToThrow
        }
        state = State.INITIALIZED
    }

    override var config: Config =
        Config(
            PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
            HandTrackingMode.BOTH,
            DeviceTrackingMode.SPATIAL_LAST_KNOWN,
            DepthEstimationMode.SMOOTH_AND_RAW,
            AnchorPersistenceMode.LOCAL,
            augmentedObjectCategories = AugmentedObjectCategory.allSupported(),
        )

    override fun configure(config: Config) {
        check(
            state == State.NOT_INITIALIZED ||
                state == State.INITIALIZED ||
                state == State.RESUMED ||
                state == State.PAUSED
        )
        if (!shouldSupportPlaneTracking && config.planeTracking != PlaneTrackingMode.DISABLED) {
            throw UnsupportedOperationException()
        }

        if (!shouldSupportFaceTracking && config.faceTracking == FaceTrackingMode.BLEND_SHAPES) {
            throw UnsupportedOperationException()
        }

        if (hasMissingPermission) throw SecurityException()
        this.config = config
    }

    override fun resume() {
        check(state == State.INITIALIZED || state == State.PAUSED)
        state = State.RESUMED
    }

    /**
     * Retrieves the latest time mark.
     *
     * The first call to this method will execute immediately. Subsequent calls will be blocked
     * until [allowOneMoreCallToUpdate] is called.
     */
    override suspend fun update(): ComparableTimeMark {
        check(state == State.RESUMED)
        semaphore.acquire()
        return timeSource.markNow()
    }

    /**
     * Allows an additional call to [update] to not be blocked.
     *
     * Requires that [update] has been called exactly once before each call to this method. Failure
     * to do so will result in an [IllegalStateException].
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
        state = State.DESTROYED
    }
}
