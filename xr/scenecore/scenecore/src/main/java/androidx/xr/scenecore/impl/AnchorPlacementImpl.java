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

package androidx.xr.scenecore.impl;

import androidx.xr.runtime.internal.AnchorPlacement;
import androidx.xr.runtime.internal.PlaneSemantic;
import androidx.xr.runtime.internal.PlaneType;

import java.util.HashSet;
import java.util.Set;

/**
 * Constructor for an AnchorPlacement.
 *
 * <p>Setting a [PlaneType] or [PlaneSemantic] anchor placement means that the [Entity] with a
 * [MovableComponent] will be anchored to a plane of that [PlaneType] or [PlaneSemantic] if it is
 * released while nearby after being moved. If no [PlaneType] or [PlaneSemantic] is set the [Entity]
 * will not be anchored.
 */
class AnchorPlacementImpl implements AnchorPlacement {
    protected Set<PlaneType> mPlaneTypeFilter = new HashSet<>();
    protected Set<PlaneSemantic> mPlaneSemanticFilter = new HashSet<>();

    AnchorPlacementImpl() {}
}
