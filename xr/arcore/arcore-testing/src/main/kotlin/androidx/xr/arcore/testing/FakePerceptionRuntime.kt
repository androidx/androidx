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

package androidx.xr.arcore.testing

import android.graphics.Bitmap
import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.PerceptionRuntime
import androidx.xr.runtime.AnchorPersistenceMode
import androidx.xr.runtime.AugmentedImageDatabase
import androidx.xr.runtime.AugmentedImageDatabaseEntryMode
import androidx.xr.runtime.AugmentedObjectCategory
import androidx.xr.runtime.Config
import androidx.xr.runtime.DepthEstimationMode
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.DisplayBlendMode
import androidx.xr.runtime.FaceTrackingMode
import androidx.xr.runtime.HandTrackingMode
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.QrCodeTrackingMode
import kotlin.time.ComparableTimeMark
import kotlin.time.TestTimeSource
import kotlinx.coroutines.sync.Semaphore

// TODO b/500091606 Remove when no longer used in G3
/**
 * Fake implementation of [PerceptionRuntime] for testing purposes.
 *
 * @property perceptionManager the [FakePerceptionManager] for this fake runtime
 * @property xrDevicePreferredDisplayBlendMode the value that will be returned by
 *   [androidx.xr.runtime.XrDevice.getPreferredDisplayBlendMode]
 * @deprecated This will be removed in a future release. In order to test androidx.xr.arcore APIs,
 *   use an [ArCoreTestRule] in your tests.
 */
@Suppress("DataClassDefinition")
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Deprecated(
    "arcore-testing fakes have been moved internal and should no longer be used by unit tests."
)
public data class FakePerceptionRuntime(
    @Suppress("DEPRECATION") override val perceptionManager: FakePerceptionManager,
    /** If false, [initialize] will throw an exception during testing. */
    @get:JvmName("hasCreatePermission") public var hasCreatePermission: Boolean = true,
) : PerceptionRuntime {
    public var xrDevicePreferredDisplayBlendMode: DisplayBlendMode = DisplayBlendMode.NO_DISPLAY

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

    /** The current state of the runtime. */
    public var state: State = State.NOT_INITIALIZED
        private set

    /** The time source used for this runtime. */
    public val timeSource: TestTimeSource = TestTimeSource()

    private val semaphore = Semaphore(1)

    /** If true, [configure] will emulate the failure case for missing permissions. */
    @get:JvmName("hasMissingPermission") public var hasMissingPermission: Boolean = false

    /** If false, [configure] will throw an Exception if the config enables PlaneTracking. */
    @get:JvmName("shouldSupportPlaneTracking") public var shouldSupportPlaneTracking: Boolean = true

    /** If false, [configure] will throw an exception if the config enables FaceTracking */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @get:JvmName("shouldSupportFaceTracking")
    public var shouldSupportFaceTracking: Boolean = true

    /** If false, [configure] will throw an Exception if the config enables ImageTracking. */
    @get:JvmName("shouldSupportImageTracking") public var shouldSupportImageTracking: Boolean = true

    public var augmentedImageDatabase: AugmentedImageDatabase = AugmentedImageDatabase()

    /** If false, [configure] will throw an Exception if the config enables QrCodeTracking. */
    @get:JvmName("shouldSupportQrCodeTracking")
    public var shouldSupportQrCodeTracking: Boolean = true

    public override var config: Config =
        Config(
            PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
            HandTrackingMode.BOTH,
            DeviceTrackingMode.SPATIAL,
            DepthEstimationMode.SMOOTH_AND_RAW,
            AnchorPersistenceMode.LOCAL,
            augmentedObjectCategories = setOf(AugmentedObjectCategory.MOUSE),
            augmentedImageDatabase = augmentedImageDatabase,
            qrCodeTracking = QrCodeTrackingMode.DYNAMIC,
        )

    override fun initialize() {
        check(state == State.NOT_INITIALIZED)
        augmentedImageDatabase.addAugmentedImageDatabaseEntry(
            mode = AugmentedImageDatabaseEntryMode.DYNAMIC,
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
        )
        state = State.INITIALIZED
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

        if (
            !shouldSupportImageTracking &&
                config.augmentedImageDatabase?.entries?.isNotEmpty() == true
        ) {
            throw UnsupportedOperationException()
        }

        if (!shouldSupportQrCodeTracking && config.qrCodeTracking != QrCodeTrackingMode.DISABLED) {
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
     * Retrieves the latest time mark. The first call to this method will execute immediately.
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

    override fun destroy() {
        check(state == State.PAUSED || state == State.INITIALIZED)
        state = State.DESTROYED
    }
}
