/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.health.services.client.data

import androidx.health.services.client.data.GolfExerciseTypeConfig.GolfShotTrackingPlaceInfo.Companion.GOLF_SHOT_TRACKING_PLACE_INFO_UNSPECIFIED
import androidx.health.services.client.data.GolfExerciseTypeConfig.GolfShotTrackingPlaceInfo.Companion.toProto
import androidx.health.services.client.proto.DataProto
import java.util.Objects

/**
 * An [ExerciseTypeConfig] that is specifically for an [ExerciseType.GOLF] activity to configure the
 * detection of [DataType.GOLF_SHOT_COUNT], enabling higher accuracy of the detection. Developers
 * are not expected to receive or consume it from health-services.
 *
 * @property golfShotTrackingPlaceInfo location where user takes [DataType.GOLF_SHOT_COUNT] during
 * [ExerciseType.GOLF] activity
 */
class GolfExerciseTypeConfig(
    val golfShotTrackingPlaceInfo: GolfShotTrackingPlaceInfo =
        GOLF_SHOT_TRACKING_PLACE_INFO_UNSPECIFIED
) : ExerciseTypeConfig() {

    internal constructor(
        proto: DataProto.ExerciseTypeConfig
    ) : this (
        GolfShotTrackingPlaceInfo.fromProto(proto.golfShotTrackingPlaceInfo)
    )

    override fun toString(): String =
        "GolfExerciseTypeConfig(golfShotTrackingPlaceInfo=$golfShotTrackingPlaceInfo)"

    /**
     * The tracking information for a golf shot used in [GolfExerciseTypeConfig]. It is the semantic
     * location of a user while golfing to assist golf swing activity recognition algorithms.
     *
     */
    class GolfShotTrackingPlaceInfo private constructor(val placeInfoId: Int) {
        override fun equals(other: Any?): Boolean {
            return other is GolfShotTrackingPlaceInfo && other.placeInfoId == this.placeInfoId
        }

        override fun hashCode(): Int {
            return Objects.hash(placeInfoId)
        }

        override fun toString(): String {
            val name = when (placeInfoId) {
                1 -> "GOLF_SHOT_TRACKING_PLACE_INFO_FAIRWAY"
                2 -> "GOLF_SHOT_TRACKING_PLACE_INFO_PUTTING_GREEN"
                3 -> "GOLF_SHOT_TRACKING_PLACE_INFO_TEE_BOX"
                else -> "GOLF_SHOT_TRACKING_PLACE_INFO_UNSPECIFIED"
            }
            return "GolfShotTrackingPlaceInfo(placeInfoId=$placeInfoId):$name"
        }

        companion object {
            internal fun GolfShotTrackingPlaceInfo.toProto():
                DataProto.GolfShotTrackingPlaceInfoType =
                when (this) {
                    GolfShotTrackingPlaceInfo(1) ->
                        DataProto
                            .GolfShotTrackingPlaceInfoType.GOLF_SHOT_TRACKING_PLACE_INFO_FAIRWAY
                    GolfShotTrackingPlaceInfo(2) ->
                        DataProto
                            .GolfShotTrackingPlaceInfoType
                            .GOLF_SHOT_TRACKING_PLACE_INFO_PUTTING_GREEN
                    GolfShotTrackingPlaceInfo(3) ->
                        DataProto
                            .GolfShotTrackingPlaceInfoType.GOLF_SHOT_TRACKING_PLACE_INFO_TEE_BOX
                    else ->
                        DataProto
                            .GolfShotTrackingPlaceInfoType.GOLF_SHOT_TRACKING_PLACE_INFO_UNSPECIFIED
                }
            internal fun fromProto(
                proto: DataProto.GolfShotTrackingPlaceInfoType
            ): GolfShotTrackingPlaceInfo =
                when (proto) {
                    DataProto
                        .GolfShotTrackingPlaceInfoType.GOLF_SHOT_TRACKING_PLACE_INFO_FAIRWAY
                    -> GOLF_SHOT_TRACKING_PLACE_INFO_FAIRWAY
                    DataProto
                        .GolfShotTrackingPlaceInfoType.GOLF_SHOT_TRACKING_PLACE_INFO_PUTTING_GREEN
                    -> GOLF_SHOT_TRACKING_PLACE_INFO_PUTTING_GREEN
                    DataProto
                        .GolfShotTrackingPlaceInfoType.GOLF_SHOT_TRACKING_PLACE_INFO_TEE_BOX
                    -> GOLF_SHOT_TRACKING_PLACE_INFO_TEE_BOX
                    else -> GOLF_SHOT_TRACKING_PLACE_INFO_UNSPECIFIED
                }

            /** The golf shot is being taken from an unspecified place. */
            @JvmField
            val GOLF_SHOT_TRACKING_PLACE_INFO_UNSPECIFIED = GolfShotTrackingPlaceInfo(0)

            /** The golf shot is being taken from the fairway. */
            @JvmField
            val GOLF_SHOT_TRACKING_PLACE_INFO_FAIRWAY = GolfShotTrackingPlaceInfo(1)

            /** The golf shot is being taken from the putting green. */
            @JvmField
            val GOLF_SHOT_TRACKING_PLACE_INFO_PUTTING_GREEN = GolfShotTrackingPlaceInfo(2)

            /** The golf shot is being taken from the tee box area. */
            @JvmField
            val GOLF_SHOT_TRACKING_PLACE_INFO_TEE_BOX = GolfShotTrackingPlaceInfo(3)
        }
    }

    override fun toProto(): DataProto.ExerciseTypeConfig {
        return DataProto
            .ExerciseTypeConfig
            .newBuilder()
            .setGolfShotTrackingPlaceInfo(golfShotTrackingPlaceInfo.toProto())
            .build()
    }
}
