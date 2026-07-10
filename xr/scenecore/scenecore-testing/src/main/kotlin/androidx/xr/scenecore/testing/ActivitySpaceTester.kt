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

package androidx.xr.scenecore.testing

import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.scenecore.ActivitySpace
import androidx.xr.scenecore.HitTestResult
import androidx.xr.scenecore.runtime.Dimensions as RtDimensions
import androidx.xr.scenecore.runtime.HitTestResult as RtHitTestResult
import androidx.xr.scenecore.testing.internal.FakeActivitySpace as InternalFakeActivitySpace
import androidx.xr.scenecore.toHitTestResult

/**
 * A test-only data accessor for [ActivitySpace] that enables direct manipulation and inspection of
 * its internal state.
 */
public class ActivitySpaceTester
internal constructor(private val rtActivitySpace: InternalFakeActivitySpace) {

    /**
     * The [HitTestResult] to be returned by subsequent calls to [ActivitySpace.hitTest].
     *
     * This property is typically used for testing or simulation purposes, allowing you to define
     * the outcome of hit tests performed within the [ActivitySpace].
     *
     * Setting a non-null value describes the location and normal of the closest object hit,
     * relative to the origin of the hit test ray. Set to `null` to simulate the hit test not
     * intersecting with any objects.
     */
    public var hitTestResult: HitTestResult?
        get() = rtActivitySpace.hitTestResult.toHitTestResult()
        set(value) {
            rtActivitySpace.hitTestResult =
                value?.toRtHitTestResult()
                    ?: RtHitTestResult(
                        null,
                        null,
                        RtHitTestResult.HitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_UNKNOWN,
                        0f,
                    )
        }

    /**
     * Sets the [ActivitySpace] bounds, invoking all registered listeners with the new bounds.
     *
     * @param bounds The new bounds to propagate to the listeners.
     */
    public fun triggerOnBoundsChanged(bounds: FloatSize3d) {
        rtActivitySpace.onBoundsChanged(RtDimensions(bounds.width, bounds.height, bounds.depth))
    }

    /**
     * The recommended box for content to be placed in when in Full Space Mode.
     *
     * The box is relative to the [ActivitySpace]'s coordinate system. It is not scaled by the
     * [ActivitySpace]'s transform. The dimensions are always in meters. This provides a
     * device-specific default volume that developers can use to size their content appropriately.
     */
    public var recommendedContentBoxInFullSpace: BoundingBox
        get() = rtActivitySpace.recommendedContentBoxInFullSpace
        set(value) {
            rtActivitySpace.recommendedContentBoxInFullSpace = value
        }

    /**
     * Simulates a change to the underlying space's origin.
     *
     * This function manually triggers any listeners registered via
     * [ActivitySpace.addOriginChangedListener], allowing tests to verify that the application
     * correctly responds to spatial updates from the system.
     */
    public fun triggerOnOriginChanged() {
        rtActivitySpace.onOriginChanged()
    }
}
