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

import androidx.annotation.RestrictTo
import androidx.xr.runtime.internal.ActivityPose
import androidx.xr.runtime.internal.ActivitySpace
import androidx.xr.runtime.internal.Dimensions
import androidx.xr.runtime.internal.HitTestResult
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.Vector3
import com.google.common.util.concurrent.Futures.immediateFuture
import com.google.common.util.concurrent.ListenableFuture
import java.util.Collections
import java.util.concurrent.atomic.AtomicReference

/** A test double for [ActivitySpace], designed for use in unit or integration tests. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeActivitySpace() : FakeSystemSpaceEntity(), ActivitySpace {
    private val _bounds: AtomicReference<Dimensions> =
        AtomicReference<Dimensions>(Dimensions(100.0f, 100.0f, 100.0f))
    private val _onBoundsChangedListeners: MutableSet<ActivitySpace.OnBoundsChangedListener> =
        Collections.synchronizedSet(
            mutableSetOf(
                ActivitySpace.OnBoundsChangedListener { newBounds -> _bounds.set(newBounds) }
            )
        )

    /** Returns the bounds of this ActivitySpace. */
    override val bounds: Dimensions
        get() = _bounds.get()

    /**
     * For test purposes only.
     *
     * The set of listeners to be called when the bounds of the primary Activity change.
     *
     * @param bounds The new bounds of the primary Activity in Meters
     */
    public val onBoundsChangedListeners: Set<ActivitySpace.OnBoundsChangedListener>
        get() = _onBoundsChangedListeners

    /**
     * For test purposes only.
     *
     * Simulates a bounds change event, invoking all registered listeners with the new bounds. This
     * will also update the internal `bounds` property of this fake.
     *
     * @param bounds The new bounds to propagate to the listeners.
     */
    public fun onBoundsChanged(bounds: Dimensions) {
        val listeners =
            synchronized(_onBoundsChangedListeners) { _onBoundsChangedListeners.toSet() }
        listeners.forEach { it.onBoundsChanged(bounds) }
    }

    /**
     * Adds a listener to be called when the bounds of the primary Activity change. If the same
     * listener is added multiple times, it will only fire each event on time.
     *
     * @param listener The listener to register.
     */
    @Suppress("ExecutorRegistration")
    override fun addOnBoundsChangedListener(listener: ActivitySpace.OnBoundsChangedListener) {
        _onBoundsChangedListeners.add(listener)
    }

    /**
     * Removes a listener to be called when the bounds of the primary Activity change. If the given
     * listener was not added, this call does nothing.
     *
     * @param listener The listener to unregister.
     */
    override fun removeOnBoundsChangedListener(listener: ActivitySpace.OnBoundsChangedListener) {
        _onBoundsChangedListeners.remove(listener)
    }

    /**
     * The [HitTestResult] that will be returned by [hitTestRelativeToActivityPose]. This can be
     * modified in tests to simulate different hit test outcomes.
     */
    public var activitySpaceHitTestResult: HitTestResult =
        HitTestResult(
            hitPosition = null,
            surfaceNormal = null,
            surfaceType = HitTestResult.HitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_UNKNOWN,
            distance = 0f,
        )

    @Suppress("AsyncSuffixFuture")
    override fun hitTestRelativeToActivityPose(
        origin: Vector3,
        direction: Vector3,
        @ActivityPose.HitTestFilterValue hitTestFilter: Int,
        activityPose: ActivityPose,
    ): ListenableFuture<HitTestResult> = immediateFuture(activitySpaceHitTestResult)

    override val recommendedContentBoxInFullSpace: BoundingBox
        get() =
            BoundingBox(
                min = Vector3(-1.73f / 2, -1.61f / 2, -0.5f / 2),
                max = Vector3(1.73f / 2, 1.61f / 2, 0.5f / 2),
            )
}
