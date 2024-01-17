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
import androidx.health.services.client.proto.DataProto.ExerciseEventType.EXERCISE_EVENT_TYPE_GOLF_SHOT
import androidx.health.services.client.proto.DataProto.ExerciseEventType.EXERCISE_EVENT_TYPE_UNKNOWN

/**
 * Type of exercise event which specifies the representations of [ExerciseEventCapabilities] for the
 * event.
 *
 * Note: the exercise event type defines only the representation and data format of event types. It
 * does not act as a form of delivery for the event data.
 */
public class ExerciseEventType<C : ExerciseEventCapabilities> @RestrictTo(RestrictTo.Scope.LIBRARY)
public constructor(
  /** Unique identifier for the [ExerciseEventType], as an `int`. */
  private val id: Int
) {
  internal fun toProto(): DataProto.ExerciseEventType =
    when (this) {
      GOLF_SHOT_EVENT -> EXERCISE_EVENT_TYPE_GOLF_SHOT
      else -> EXERCISE_EVENT_TYPE_UNKNOWN
    }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ExerciseEventType<*>) return false

    if (id != other.id) return false

    return true
  }

  override fun hashCode(): Int = id

  override fun toString(): String {
    val name = when (id) {
      2 -> "GolfShotEvent"
      else -> "UNKNOWN"
    }
    return "ExerciseEventType{ $name }"
  }

  public companion object {
    /**
     * An exercise event that can be used to notify the user a golf shot is detected by the device.
     */
    @JvmField
    public val GOLF_SHOT_EVENT: ExerciseEventType<GolfShotEventCapabilities> =
      ExerciseEventType<GolfShotEventCapabilities>(2)

    /** An unknown event type. This should not be received. */
    @JvmField
    public val UNKNOWN: ExerciseEventType<ExerciseEventCapabilities> =
      ExerciseEventType<ExerciseEventCapabilities>(0)

    internal fun fromProto(proto: DataProto.ExerciseEventType): ExerciseEventType<*> =
      when (proto) {
        EXERCISE_EVENT_TYPE_GOLF_SHOT -> GOLF_SHOT_EVENT
        EXERCISE_EVENT_TYPE_UNKNOWN -> UNKNOWN
      }
  }
}
