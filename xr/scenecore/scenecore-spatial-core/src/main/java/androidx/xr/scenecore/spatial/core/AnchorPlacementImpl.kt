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
package androidx.xr.scenecore.spatial.core

import androidx.xr.scenecore.runtime.AnchorPlacement
import androidx.xr.scenecore.runtime.PlaneSemantic
import androidx.xr.scenecore.runtime.PlaneType

/**
 * Constructor for an AnchorPlacement.
 *
 * Setting a [PlaneType] or [PlaneSemantic] anchor placement means that the
 * [androidx.xr.scenecore.runtime.Entity] with a [androidx.xr.scenecore.runtime.MovableComponent]
 * will be anchored to a plane of that [PlaneType] or [PlaneSemantic] if it is released while nearby
 * after being moved. If no [PlaneType] or [PlaneSemantic] is set the
 * [androidx.xr.scenecore.runtime.Entity] will not be anchored.
 */
internal class AnchorPlacementImpl : AnchorPlacement {
    @JvmField var planeTypeFilter: MutableSet<PlaneType> = mutableSetOf()
    @JvmField var planeSemanticFilter: MutableSet<PlaneSemantic> = mutableSetOf()
}
