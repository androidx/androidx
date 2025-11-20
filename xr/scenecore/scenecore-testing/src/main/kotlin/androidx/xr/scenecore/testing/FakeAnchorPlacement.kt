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

package androidx.xr.scenecore.testing

import androidx.annotation.RestrictTo
import androidx.xr.scenecore.runtime.AnchorPlacement
import androidx.xr.scenecore.runtime.PlaneSemantic
import androidx.xr.scenecore.runtime.PlaneType

/**
 * A test implementation of the [androidx.xr.scenecore.runtime.AnchorPlacement] interface, used to
 * define and inspect anchor placement rules in tests.
 *
 * This class specifies the conditions under which an entity can be anchored to a real-world plane.
 * An entity is eligible for anchoring if it is released near a plane that matches the predefined
 * criteria for plane type and semantic labels.
 *
 * For plane-based anchoring to function, the [androidx.xr.runtime.Session] must be configured with
 * [androidx.xr.runtime.Config.PlaneTrackingMode.Companion.HORIZONTAL_AND_VERTICAL] to enable plane
 * detection.
 *
 * When an entity is successfully anchored, its pose is adjusted so that its local Z-axis aligns
 * with the plane's normal vector (i.e., it sits flat against the surface).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeAnchorPlacement
internal constructor(
    internal val planeTypeFilter: Set<@JvmSuppressWildcards PlaneType> = setOf(PlaneType.ANY),
    internal val planeSemanticFilter: Set<@JvmSuppressWildcards PlaneSemantic> =
        setOf(PlaneSemantic.ANY),
) : AnchorPlacement
