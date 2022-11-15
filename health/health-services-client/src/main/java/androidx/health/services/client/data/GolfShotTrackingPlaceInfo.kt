/*
 * Copyright (C) 2022 The Android Open Source Project
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

import androidx.annotation.IntDef
import androidx.health.services.client.proto.DataProto
import kotlin.annotation.AnnotationRetention.SOURCE

/**
 * The tracking information for a golf shot used in [ExerciseTypeConfig]. It is the semantic
 * location of a user while golfing to assist golf swing activity recognition algorithms.
 *
 * @hide
 */
@Retention(AnnotationRetention.SOURCE)
@IntDef(
  GolfShotTrackingPlaceInfo.UNSPECIFIED,
  GolfShotTrackingPlaceInfo.FAIRWAY,
  GolfShotTrackingPlaceInfo.PUTTING_GREEN,
  GolfShotTrackingPlaceInfo.TEE_BOX
)
annotation class GolfShotTrackingPlaceInfo {
  companion object {
    /** The golf shot is being taken from an unknown place. */
    const val UNSPECIFIED: Int = 0
    /** The golf shot is being taken from the fairway. */
    const val FAIRWAY: Int = 1
    /** The golf shot is being taken from the putting green. */
    const val PUTTING_GREEN: Int = 2
    /** The golf shot is being taken from the tee box area. */
    const val TEE_BOX: Int = 3

    internal fun @receiver:GolfShotTrackingPlaceInfo Int.toProto():
      DataProto.GolfShotTrackingPlaceInfoType =
      when (this) {
        FAIRWAY -> DataProto.GolfShotTrackingPlaceInfoType.GOLF_SHOT_TRACKING_PLACE_INFO_FAIRWAY
        PUTTING_GREEN ->
          DataProto.GolfShotTrackingPlaceInfoType.GOLF_SHOT_TRACKING_PLACE_INFO_PUTTING_GREEN
        TEE_BOX -> DataProto.GolfShotTrackingPlaceInfoType.GOLF_SHOT_TRACKING_PLACE_INFO_TEE_BOX
        else -> DataProto.GolfShotTrackingPlaceInfoType.GOLF_SHOT_TRACKING_PLACE_INFO_UNSPECIFIED
      }

    @GolfShotTrackingPlaceInfo
    internal fun fromProto(proto: DataProto.GolfShotTrackingPlaceInfoType): Int =
      when (proto) {
        DataProto.GolfShotTrackingPlaceInfoType.GOLF_SHOT_TRACKING_PLACE_INFO_PUTTING_GREEN ->
          PUTTING_GREEN
        DataProto.GolfShotTrackingPlaceInfoType.GOLF_SHOT_TRACKING_PLACE_INFO_TEE_BOX -> TEE_BOX
        DataProto.GolfShotTrackingPlaceInfoType.GOLF_SHOT_TRACKING_PLACE_INFO_FAIRWAY -> FAIRWAY
        else -> UNSPECIFIED
      }
  }
}
