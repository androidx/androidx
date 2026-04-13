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

package androidx.xr.runtime

import androidx.xr.runtime.internal.JxrRuntime
import kotlin.time.ComparableTimeMark
import kotlin.time.TestTimeSource
import kotlinx.coroutines.sync.Semaphore

/** Stub implementation of [JxrRuntime] for testing purposes. */
@Suppress("NotCloseable")
internal class StubPerceptionRuntime(internal var hasCreatePermission: Boolean = true) :
    JxrRuntime {

    internal companion object {
        @JvmField
        internal val TestPermissions: List<String> =
            listOf("android.permission.SCENE_UNDERSTANDING_COARSE")
    }

    internal var xrDevicePreferredDisplayBlendMode: DisplayBlendMode = DisplayBlendMode.NO_DISPLAY

    /** Set of possible states of the runtime. */
    internal enum class State {
        NOT_INITIALIZED,
        INITIALIZED,
        RESUMED,
        PAUSED,
        DESTROYED,
    }

    internal var state: State = State.NOT_INITIALIZED
        private set

    internal val timeSource: TestTimeSource = TestTimeSource()

    private val semaphore = Semaphore(1)

    @get:JvmName("hasMissingPermission") internal var hasMissingPermission: Boolean = false

    @get:JvmName("shouldSupportPlaneTracking")
    internal var shouldSupportPlaneTracking: Boolean = true

    internal var shouldSupportFaceTracking: Boolean = true

    override fun initialize() {
        check(state == State.NOT_INITIALIZED)
        if (!hasCreatePermission) throw SecurityException()
        if (StubPerceptionRuntimeFactory.lifecycleCreateException != null) {
            val exceptionToThrow = StubPerceptionRuntimeFactory.lifecycleCreateException!!
            StubPerceptionRuntimeFactory.lifecycleCreateException = null
            throw exceptionToThrow
        }
        state = State.INITIALIZED
    }

    internal var config: Config =
        Config(
            PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
            HandTrackingMode.BOTH,
            DeviceTrackingMode.SPATIAL_LAST_KNOWN,
            DepthEstimationMode.SMOOTH_AND_RAW,
            AnchorPersistenceMode.LOCAL,
            // Needs to contain at least one AugmentedObjectCategory to enable
            augmentedObjectCategories = setOf(AugmentedObjectCategory.MOUSE),
        )
        private set

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

    override fun getPreferredDisplayBlendMode(): DisplayBlendMode {
        return xrDevicePreferredDisplayBlendMode
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

    internal fun allowOneMoreCallToUpdate() {
        semaphore.release()
    }

    override fun pause() {
        check(state == State.RESUMED)
        state = State.PAUSED
    }

    override fun destroy() {
        check(state == State.PAUSED || state == State.INITIALIZED)
        state = State.DESTROYED
    }
}
