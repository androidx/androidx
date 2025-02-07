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

package androidx.xr.arcore.apps.whitebox.helloar.rendering

import androidx.xr.arcore.Plane
import androidx.xr.scenecore.Entity
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow

/** Represents a rendered plane model. */
data class PlaneModel(
    val id: Int,
    val type: Plane.Type,
    val stateFlow: StateFlow<Plane.State>,
    internal val entity: Entity,
    internal val renderJob: Job?,
) {}
