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

/** Constants for spatialized audio. */
public interface SpatializerConstants {

    public companion object {
        /** Specifies spatial rendering using First Order Ambisonics */
        public const val AMBISONICS_ORDER_FIRST_ORDER: Int = 0
        /** Specifies spatial rendering using Second Order Ambisonics */
        public const val AMBISONICS_ORDER_SECOND_ORDER: Int = 1
        /** Specifies spatial rendering using Third Order Ambisonics */
        public const val AMBISONICS_ORDER_THIRD_ORDER: Int = 2

        /** The sound source has not been spatialized with the Spatial Audio SDK. */
        public const val SOURCE_TYPE_BYPASS: Int = 0
        /** The sound source has been spatialized as a 3D point source. */
        public const val SOURCE_TYPE_POINT_SOURCE: Int = 1
        /** The sound source is an ambisonics sound field. */
        public const val SOURCE_TYPE_SOUND_FIELD: Int = 2
    }

    /** Used to set the Ambisonics order of a [SoundFieldAttributes] */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        value =
            [
                AMBISONICS_ORDER_FIRST_ORDER,
                AMBISONICS_ORDER_SECOND_ORDER,
                AMBISONICS_ORDER_THIRD_ORDER
            ]
    )
    public annotation class AmbisonicsOrder

    /** Represents the type of spatialization for an audio source. */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(value = [SOURCE_TYPE_BYPASS, SOURCE_TYPE_POINT_SOURCE, SOURCE_TYPE_SOUND_FIELD])
    public annotation class SourceType
}

/** Converts the [JxrPlatformAdapter] SourceType IntDef to the SceneCore API. */
@SpatializerConstants.SourceType
internal fun @receiver:JxrPlatformAdapter.SpatializerConstants.SourceType Int.sourceTypeToJxr():
    Int {
    return when (this) {
        JxrPlatformAdapter.SpatializerConstants.SOURCE_TYPE_BYPASS ->
            SpatializerConstants.SOURCE_TYPE_BYPASS
        JxrPlatformAdapter.SpatializerConstants.SOURCE_TYPE_POINT_SOURCE ->
            SpatializerConstants.SOURCE_TYPE_POINT_SOURCE
        JxrPlatformAdapter.SpatializerConstants.SOURCE_TYPE_SOUND_FIELD ->
            SpatializerConstants.SOURCE_TYPE_SOUND_FIELD
        else -> {
            // Unknown source type, returning bypass.
            SpatializerConstants.SOURCE_TYPE_BYPASS
        }
    }
}

/** Converts the [JxrPlatformAdapter] SourceType IntDef to the SceneCore API. */
@SpatializerConstants.AmbisonicsOrder
internal fun @receiver:JxrPlatformAdapter.SpatializerConstants.AmbisonicsOrder
Int.ambisonicsOrderToJxr(): Int {
    return when (this) {
        JxrPlatformAdapter.SpatializerConstants.AMBISONICS_ORDER_FIRST_ORDER ->
            SpatializerConstants.AMBISONICS_ORDER_FIRST_ORDER
        JxrPlatformAdapter.SpatializerConstants.AMBISONICS_ORDER_SECOND_ORDER ->
            SpatializerConstants.AMBISONICS_ORDER_SECOND_ORDER
        JxrPlatformAdapter.SpatializerConstants.AMBISONICS_ORDER_THIRD_ORDER ->
            SpatializerConstants.AMBISONICS_ORDER_THIRD_ORDER
        else -> {
            // Unknown order, returning first order
            SpatializerConstants.AMBISONICS_ORDER_FIRST_ORDER
        }
    }
}
