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

package androidx.xr.scenecore

import androidx.xr.scenecore.runtime.SpatializerConstants as RtSpatializerConstants

/** Constants for spatialized audio. */
public interface SpatializerConstants {

    /** Specifies the Ambisonics order of a [SoundFieldAttributes] */
    public class AmbisonicsOrder private constructor(private val name: String) {
        public companion object {
            /** Specifies spatial rendering using First Order Ambisonics */
            @JvmField public val FIRST_ORDER: AmbisonicsOrder = AmbisonicsOrder("FIRST")

            /** Specifies spatial rendering using Second Order Ambisonics */
            @JvmField public val SECOND_ORDER: AmbisonicsOrder = AmbisonicsOrder("SECOND")

            /** Specifies spatial rendering using Third Order Ambisonics */
            @JvmField public val THIRD_ORDER: AmbisonicsOrder = AmbisonicsOrder("THIRD")
        }

        override fun toString(): String = name
    }

    /** Represents the type of spatialization for an audio source. */
    public class SourceType private constructor(private val name: String) {

        public companion object {

            /** The sound source has not been spatialized with SceneCore APIs. */
            @JvmField public val DEFAULT: SourceType = SourceType("DEFAULT")

            /** The sound source has been spatialized as a 3D point source. */
            @JvmField public val POINT_SOURCE: SourceType = SourceType("POINT_SOURCE")

            /** The sound source is an ambisonics sound field. */
            public const val SOURCE_TYPE_SOUND_FIELD: Int = 2
            @JvmField public val SOUND_FIELD: SourceType = SourceType("SOUND_FIELD")
        }

        override fun toString(): String = name
    }
}

/** Converts the runtime SourceType IntDef to the SceneCore API. */
internal fun @receiver:RtSpatializerConstants.SourceType Int.sourceTypeToJxr():
    SpatializerConstants.SourceType {
    return when (this) {
        RtSpatializerConstants.SOURCE_TYPE_BYPASS -> SpatializerConstants.SourceType.DEFAULT
        RtSpatializerConstants.SOURCE_TYPE_POINT_SOURCE ->
            SpatializerConstants.SourceType.POINT_SOURCE
        RtSpatializerConstants.SOURCE_TYPE_SOUND_FIELD ->
            SpatializerConstants.SourceType.SOUND_FIELD
        else -> {
            // Unknown source type, returning bypass.
            SpatializerConstants.SourceType.DEFAULT
        }
    }
}

/** Converts the SourceType to the runtime IntDef . */
@RtSpatializerConstants.SourceType
internal fun SpatializerConstants.SourceType.sourceTypeToRt(): Int {
    return when (this) {
        SpatializerConstants.SourceType.DEFAULT -> RtSpatializerConstants.SOURCE_TYPE_BYPASS
        SpatializerConstants.SourceType.POINT_SOURCE ->
            RtSpatializerConstants.SOURCE_TYPE_POINT_SOURCE
        SpatializerConstants.SourceType.SOUND_FIELD ->
            RtSpatializerConstants.SOURCE_TYPE_SOUND_FIELD
        else -> RtSpatializerConstants.SOURCE_TYPE_BYPASS
    }
}

/** Converts the runtime AmbisonicsOrder IntDef to the SceneCore API. */
internal fun @receiver:RtSpatializerConstants.AmbisonicsOrder Int.ambisonicsOrderToJxr():
    SpatializerConstants.AmbisonicsOrder {
    return when (this) {
        RtSpatializerConstants.AMBISONICS_ORDER_FIRST_ORDER ->
            SpatializerConstants.AmbisonicsOrder.FIRST_ORDER
        RtSpatializerConstants.AMBISONICS_ORDER_SECOND_ORDER ->
            SpatializerConstants.AmbisonicsOrder.SECOND_ORDER
        RtSpatializerConstants.AMBISONICS_ORDER_THIRD_ORDER ->
            SpatializerConstants.AmbisonicsOrder.THIRD_ORDER
        else -> {
            // Unknown order, returning first order
            SpatializerConstants.AmbisonicsOrder.FIRST_ORDER
        }
    }
}

/** Converts the SceneCore AmbisonicsOrder API to the SceneCore API. */
@RtSpatializerConstants.AmbisonicsOrder
internal fun SpatializerConstants.AmbisonicsOrder.sourceTypeToRt(): Int {

    return when (this) {
        SpatializerConstants.AmbisonicsOrder.FIRST_ORDER ->
            RtSpatializerConstants.AMBISONICS_ORDER_FIRST_ORDER
        SpatializerConstants.AmbisonicsOrder.SECOND_ORDER ->
            RtSpatializerConstants.AMBISONICS_ORDER_SECOND_ORDER
        SpatializerConstants.AmbisonicsOrder.THIRD_ORDER ->
            RtSpatializerConstants.AMBISONICS_ORDER_THIRD_ORDER
        else -> {
            // Unknown order, returning first order
            RtSpatializerConstants.AMBISONICS_ORDER_FIRST_ORDER
        }
    }
}
