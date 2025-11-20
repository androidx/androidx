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

package androidx.xr.scenecore.runtime

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

@IntDef(
    SpatialPointerIcon.TYPE_NONE,
    SpatialPointerIcon.TYPE_DEFAULT,
    SpatialPointerIcon.TYPE_CIRCLE,
)
@Retention(AnnotationRetention.SOURCE)
@Suppress("PublicTypedef")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public annotation class SpatialPointerIconType

/** The type of the pointer icon to render. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public object SpatialPointerIcon {
    /** Hides the pointer icon. */
    public const val TYPE_NONE: Int = 0

    /** Use the default pointer icon, as determined by the system. */
    public const val TYPE_DEFAULT: Int = 1

    /** Render the pointer icon as a circle. */
    public const val TYPE_CIRCLE: Int = 2
}

/** Runtime interface for component that modifies the pointer. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface SpatialPointerComponent : Component {

    public fun setSpatialPointerIcon(@SpatialPointerIconType iconType: Int)

    @SpatialPointerIconType public fun getSpatialPointerIcon(): Int
}
