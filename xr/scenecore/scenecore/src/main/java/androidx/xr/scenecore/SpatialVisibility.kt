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

package androidx.xr.scenecore

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FIELD,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.TYPE,
)
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    SpatialVisibility.SPATIAL_VISIBILITY_UNKNOWN,
    SpatialVisibility.SPATIAL_VISIBILITY_OUTSIDE_FIELD_OF_VIEW,
    SpatialVisibility.SPATIAL_VISIBILITY_PARTIALLY_WITHIN_FIELD_OF_VIEW,
    SpatialVisibility.SPATIAL_VISIBILITY_WITHIN_FIELD_OF_VIEW,
)
internal annotation class SpatialVisibilityValue

/** Spatial Visibility states of content within the user's field of view. */
public object SpatialVisibility {
    /** Unknown spatial visibility state. */
    public const val SPATIAL_VISIBILITY_UNKNOWN: Int = 0
    /** The content is fully outside the user's field of view. */
    public const val SPATIAL_VISIBILITY_OUTSIDE_FIELD_OF_VIEW: Int = 1
    /** The content is partially within the user's field of view, but not fully inside of it. */
    public const val SPATIAL_VISIBILITY_PARTIALLY_WITHIN_FIELD_OF_VIEW: Int = 2
    /** The content is fully within the user's field of view. */
    public const val SPATIAL_VISIBILITY_WITHIN_FIELD_OF_VIEW: Int = 3
}
