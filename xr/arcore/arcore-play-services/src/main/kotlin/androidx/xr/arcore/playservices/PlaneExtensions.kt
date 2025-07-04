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

package androidx.xr.arcore.playservices

import androidx.xr.runtime.internal.Plane
import com.google.ar.core.Plane.Type as ARCorePlaneType

/** Create a [Plane.Type] from an AR Core [ARCorePlaneType]. */
internal fun Plane.Type.Companion.fromArCoreType(type: ARCorePlaneType): Plane.Type =
    when (type) {
        ARCorePlaneType.HORIZONTAL_DOWNWARD_FACING -> Plane.Type.HORIZONTAL_DOWNWARD_FACING
        ARCorePlaneType.HORIZONTAL_UPWARD_FACING -> Plane.Type.HORIZONTAL_UPWARD_FACING
        ARCorePlaneType.VERTICAL -> Plane.Type.VERTICAL
    }
