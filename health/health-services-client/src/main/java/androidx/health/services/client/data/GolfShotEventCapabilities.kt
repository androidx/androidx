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
import androidx.health.services.client.proto.DataProto
import androidx.health.services.client.proto.DataProto.ExerciseEventCapabilities.GolfShotCapabilities
import androidx.health.services.client.proto.DataProto.ExerciseEventType.EXERCISE_EVENT_TYPE_GOLF_SHOT
import java.util.Objects

/** Contains the Golf Shot capabilities specific to the associated [GolfShotEvent]. */
public class GolfShotEventCapabilities(
  /** Whether the device has the capability of supporting [GolfShotEvent]. */
  override val isSupported: Boolean,
  /** Whether the device has the capability of supporting [GolfShotSwingType]. */
  public val isSwingTypeClassificationSupported: Boolean,
) : ExerciseEventCapabilities() {
  internal constructor(
    proto: DataProto.ExerciseEventCapabilities.GolfShotCapabilities
  ) : this(proto.isSupported, proto.isSwingTypeClassificationSupported)

  @RestrictTo(RestrictTo.Scope.LIBRARY)
  override fun toProto(): DataProto.ExerciseEventCapabilities =
    DataProto.ExerciseEventCapabilities.newBuilder()
      .setExerciseEventType(EXERCISE_EVENT_TYPE_GOLF_SHOT)
      .setGolfShotCapabilities(
        GolfShotCapabilities.newBuilder()
          .setIsSupported(this.isSupported)
          .setIsSwingTypeClassificationSupported(this.isSwingTypeClassificationSupported)
          .build()
      )
      .build()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is GolfShotEventCapabilities) return false
    if (isSupported != other.isSupported) return false
    if (isSwingTypeClassificationSupported != other.isSwingTypeClassificationSupported) return false

    return true
  }

  override fun hashCode(): Int {
    return Objects.hash(isSupported, isSwingTypeClassificationSupported)
  }
}
