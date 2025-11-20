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

package androidx.xr.scenecore.testing

import androidx.annotation.RestrictTo
import androidx.xr.scenecore.runtime.SpatialPointerComponent
import androidx.xr.scenecore.runtime.SpatialPointerIcon
import androidx.xr.scenecore.runtime.SpatialPointerIconType

/** Test-only implementation of [FakeSpatialPointerComponent] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeSpatialPointerComponent : FakeComponent(), SpatialPointerComponent {

    @SpatialPointerIconType private var spatialPointerIcon: Int = SpatialPointerIcon.TYPE_NONE

    /** Sets the [androidx.xr.scenecore.runtime.SpatialPointerIconType]. */
    override fun setSpatialPointerIcon(@SpatialPointerIconType iconType: Int) {
        spatialPointerIcon = iconType
    }

    /**
     * Returns the [androidx.xr.scenecore.runtime.SpatialPointerIconType] set from
     * [setSpatialPointerIcon] on this component.
     */
    @SpatialPointerIconType override fun getSpatialPointerIcon(): Int = spatialPointerIcon
}
