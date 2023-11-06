/*
 * Copyright (C) 2023 The Android Open Source Project
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

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.health.services.client.proto.DataProto
import androidx.health.services.client.proto.DataProto.ExerciseEvent.ExerciseEventData.ExerciseEventDataCase
import java.time.Duration

/** Contains the latest exercise event for the current exercise. */
public abstract class ExerciseEvent @RestrictTo(Scope.LIBRARY_GROUP) constructor() {
  internal open fun toProto():
    DataProto.ExerciseEvent = DataProto.ExerciseEvent.getDefaultInstance()

  public companion object {
    @JvmStatic
    internal fun fromProto(proto: DataProto.ExerciseEvent): ExerciseEvent =
      when (proto.exerciseEventData.exerciseEventDataCase) {
        ExerciseEventDataCase.GOLF_SHOT_DATA ->
          GolfShotEvent(
            Duration.ofMillis(proto.exerciseEventData.golfShotData.durationFromBootMs),
            GolfShotEvent.GolfShotSwingType.fromProto(
              proto.exerciseEventData.golfShotData.golfShotSwingType)
          )
        else -> throw IllegalStateException("Exercise event not set on $proto")
      }
  }
}
