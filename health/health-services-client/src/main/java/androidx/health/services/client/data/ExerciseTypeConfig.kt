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

import androidx.health.services.client.data.GolfShotTrackingPlaceInfo.Companion.toProto
import androidx.health.services.client.proto.DataProto

/**
 * Configuration attributes for a specific exercise type that may be modified after the exercise has
 * started.
 *
 * @property golfShotTrackingPlaceInfo location where user takes [DataType.GOLF_SHOT_COUNT] during
 * [ExerciseType.GOLF] activity
 */
class ExerciseTypeConfig private constructor(
  @GolfShotTrackingPlaceInfo
  val golfShotTrackingPlaceInfo: Int = GolfShotTrackingPlaceInfo.UNSPECIFIED
) {

  internal constructor(
    proto: DataProto.ExerciseTypeConfig
  ) : this (
    GolfShotTrackingPlaceInfo.fromProto(proto.golfShotTrackingPlaceInfo)
  )

  internal fun toProto(): DataProto.ExerciseTypeConfig {
    return DataProto.ExerciseTypeConfig.newBuilder()
      .setGolfShotTrackingPlaceInfo(golfShotTrackingPlaceInfo.toProto())
      .build()
  }

  override fun toString(): String =
    "ExerciseTypeConfig(golfShotTrackingPlaceInfo=$golfShotTrackingPlaceInfo)"

  companion object {
    /**
     * Creates golf-specific exercise type configuration.
     *
     * @param golfShotTrackingPlaceInfo location where user takes [DataType.GOLF_SHOT_COUNT] during
     * [ExerciseType.GOLF] activity
     *
     * @return an instance of [ExerciseTypeConfig] with specific [GolfShotTrackingPlaceInfo]
     */
    @JvmStatic
    fun createGolfExerciseTypeConfig(
      @GolfShotTrackingPlaceInfo golfShotTrackingPlaceInfo: Int
    ): ExerciseTypeConfig {
      return ExerciseTypeConfig(golfShotTrackingPlaceInfo)
    }
  }
}
