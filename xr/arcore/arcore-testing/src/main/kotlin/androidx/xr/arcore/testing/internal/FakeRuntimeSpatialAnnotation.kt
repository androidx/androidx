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

import androidx.xr.arcore.runtime.SpatialAnnotation as RuntimeSpatialAnnotation
import androidx.xr.arcore.runtime.SpatialAnnotationId
import androidx.xr.arcore.runtime.SpatialAnnotationQuadAlignment
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quad
import androidx.xr.runtime.math.Vector2

/**
 * A fake implementation of [RuntimeSpatialAnnotation] for automated unit testing.
 *
 * Allows test cases to programmatically build simulated 3D spatial annotations and inject them into
 * a FakeSession's perception state to test App UI rendering.
 */
internal class FakeRuntimeSpatialAnnotation(
    override var trackingState: TrackingState = TrackingState.TRACKING,
    override val id: SpatialAnnotationId = SpatialAnnotationId.fromString("box-1"),
    override var alignment: SpatialAnnotationQuadAlignment? = null,
    override var centerPose: Pose = Pose(),
    override var quad: Quad? =
        Quad.createFromCorners(
            upperLeft = Vector2.Zero,
            upperRight = Vector2.Right,
            lowerRight = Vector2.One,
            lowerLeft = Vector2.Up,
        ),
) : RuntimeSpatialAnnotation
