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
import androidx.xr.runtime.FaceTrackingMode
import androidx.xr.runtime.PlaneTrackingMode
import kotlin.time.ComparableTimeMark
import kotlin.time.TestTimeSource
import kotlinx.coroutines.sync.Semaphore

internal class FakePerceptionRuntime(
    override val perceptionManager: FakePerceptionManager,
    /** If false, [initialize] will throw an exception during testing. */
    @get:JvmName("hasCreatePermission") var hasCreatePermission: Boolean = true,
) : PerceptionRuntime {
    override val lifecycleManager: FakeLifecycleManager = FakeLifecycleManager(this)
    var xrDevicePreferredDisplayBlendMode: DisplayBlendMode = DisplayBlendMode.NO_DISPLAY

    companion object {
        private val semaphore = Semaphore(1)

        @JvmStatic
        internal fun allowOneMoreCallToUpdate() {
            if (semaphore.availablePermits == 0) {
                semaphore.release()
            }
        }

        @JvmField
        val TestPermissions: List<String> = listOf("android.permission.SCENE_UNDERSTANDING_COARSE")
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

    /** If true, [configure] will emulate the failure case for missing permissions. */
    @get:JvmName("hasMissingPermission") var hasMissingPermission: Boolean = false

    /** If false, [configure] will throw an Exception if the config enables PlaneTracking. */
    @get:JvmName("shouldSupportPlaneTracking") var shouldSupportPlaneTracking: Boolean = true

    /** If false, [configure] will throw an exception if the config enables FaceTracking */
    @get:JvmName("shouldSupportFaceTracking") var shouldSupportFaceTracking: Boolean = true

    override var config: Config = Config()

    override fun initialize() {
        check(state == State.NOT_INITIALIZED)
        if (!hasCreatePermission) throw SecurityException()
        if (FakePerceptionRuntimeFactory.runtimeInitializeException != null) {
            // FakeRuntimeFactory will continue to throw exception on subsequent tests unless
            // cleared.
            val exceptionToThrow = FakePerceptionRuntimeFactory.runtimeInitializeException!!
            FakePerceptionRuntimeFactory.runtimeInitializeException = null
            throw exceptionToThrow
        }
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

        if (!shouldSupportPlaneTracking && config.planeTracking != PlaneTrackingMode.DISABLED) {
            throw UnsupportedOperationException()
        }

        if (!shouldSupportFaceTracking && config.faceTracking == FaceTrackingMode.BLEND_SHAPES) {
            throw UnsupportedOperationException()
        }

        if (hasMissingPermission) throw SecurityException()

        this.config = config
        allowOneMoreCallToUpdate()
        perceptionManager.updateTrackingStates(config)
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
        return timeSource.markNow()
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
