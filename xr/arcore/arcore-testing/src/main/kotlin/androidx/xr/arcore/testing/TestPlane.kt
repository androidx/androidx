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
// TODO b/482675376 remove Suppress when no longer needed
@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION")

package androidx.xr.arcore.testing

import androidx.xr.arcore.PlaneLabel
import androidx.xr.arcore.PlaneType
import androidx.xr.arcore.runtime.Plane as RuntimePlane
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.arcore.testing.internal.FakePerceptionRuntime
import androidx.xr.arcore.testing.internal.FakeRuntimePlane
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector2

/**
 * Represents a flat surface in the test environment, which can be described by a [PlaneType].
 *
 * @property type the [PlaneType] of the surface
 * @property label the [PlaneLabel] that describes the surface
 * @property centerPose the [Pose] at the center of the plane
 * @property extents a pair of extents describing the size of the plane
 * @property vertices the [Vector2] vertices of a convex polygon approximating the detected plane
 * @property subsumedBy a possible other plane that this plane has been merged into
 */
public class TestPlane(planeType: PlaneType, planeLabel: PlaneLabel) : TestTrackable() {

    internal val fakeRuntimeTrackable =
        FakeRuntimePlane(planeType.toRuntimeType(), planeLabel.toRuntimeType())

    override var isVisible: Boolean = true
        set(value) {
            field = value
            if (isConfigured()) {
                fakeRuntimeTrackable.trackingState =
                    if (value) {
                        TrackingState.TRACKING
                    } else {
                        TrackingState.PAUSED
                    }
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    // TODO b/482675376 remove Suppress when no longer needed
    @get:SuppressWarnings("ReferencesDeprecated")
    @set:SuppressWarnings("ReferencesDeprecated")
    public var type: PlaneType = planeType
        set(value) {
            field = value
            if (isConfigured()) {
                fakeRuntimeTrackable.type = value.toRuntimeType()
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    // TODO b/482675376 remove Suppress when no longer needed
    @get:SuppressWarnings("ReferencesDeprecated")
    @set:SuppressWarnings("ReferencesDeprecated")
    public var label: PlaneLabel = planeLabel
        set(value) {
            field = value
            if (isConfigured()) {
                fakeRuntimeTrackable.label = value.toRuntimeType()
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    public var centerPose: Pose = Pose()
        set(value) {
            field = value
            if (isConfigured()) {
                fakeRuntimeTrackable.centerPose = value
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    public var extents: FloatSize2d = FloatSize2d()
        set(value) {
            field = value
            if (isConfigured()) {
                fakeRuntimeTrackable.extents = value
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    public var vertices: List<Vector2> = emptyList()
        set(value) {
            field = value
            if (isConfigured()) {
                fakeRuntimeTrackable.vertices = value
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    public var subsumedBy: TestPlane? = null
        set(value) {
            field = value
            if (isConfigured()) {
                fakeRuntimeTrackable.subsumedBy = value?.fakeRuntimeTrackable
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    internal fun isConfigured(): Boolean =
        if (isAddedToTestRule)
            arCoreTestRule.runtime.config.planeTracking != PlaneTrackingMode.DISABLED
        else false
}

// TODO b/482675376 remove Suppress when no longer needed
@Suppress("DEPRECATION")
internal fun PlaneType.toRuntimeType() =
    when (this) {
        PlaneType.HORIZONTAL_UPWARD_FACING -> RuntimePlane.Type.HORIZONTAL_UPWARD_FACING
        PlaneType.HORIZONTAL_DOWNWARD_FACING -> RuntimePlane.Type.HORIZONTAL_DOWNWARD_FACING
        else -> RuntimePlane.Type.VERTICAL
    }

// TODO b/482675376 remove Suppress when no longer needed
@Suppress("DEPRECATION")
internal fun PlaneLabel.toRuntimeType() =
    when (this) {
        PlaneLabel.WALL -> RuntimePlane.Label.WALL
        PlaneLabel.TABLE -> RuntimePlane.Label.TABLE
        PlaneLabel.FLOOR -> RuntimePlane.Label.FLOOR
        PlaneLabel.CEILING -> RuntimePlane.Label.CEILING
        else -> RuntimePlane.Label.UNKNOWN
    }
