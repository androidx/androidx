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

import androidx.health.services.client.proto.DataProto
import java.time.Duration
import java.util.Objects

/** An [ExerciseEvent] that contains information about Golf Shot events for the current exercise. */
public class GolfShotEvent(
  /** [Duration] since device boot when the golf shot was detected. */
  val durationSinceBoot: Duration,
  /** The type of golf swing that was detected. */
  val swingType: GolfShotSwingType
) : ExerciseEvent() {

  internal constructor(
    proto: DataProto.GolfShotData
  ) : this(
    Duration.ofMillis(proto.durationFromBootMs),
    GolfShotSwingType.fromProto(proto.golfShotSwingType),
  )

  /** Golf Shot Swing Types. */
  public class GolfShotSwingType private constructor(private val id: Int) {
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is GolfShotSwingType) return false
      if (id != other.id) return false

      return true
    }
    override fun hashCode(): Int = id

    override fun toString(): String {
      val name = when (id) {
        1 -> "PUTT"
        2 -> "PARTIAL"
        3 -> "FULL"
        else -> "UNKNOWN"
      }
      return "GolfShotEvent{ $name }"
    }

    internal fun toProto(): DataProto.GolfShotSwingType =
      DataProto.GolfShotSwingType.forNumber(id)
        ?: DataProto.GolfShotSwingType.GOLF_SHOT_SWING_TYPE_UNKNOWN

    companion object {
      /** The swing type of the received golf shot is unknown. */
      @JvmField val UNKNOWN: GolfShotSwingType = GolfShotSwingType(0)

      /** The swing type of the received golf shot is putt. */
      @JvmField val PUTT: GolfShotSwingType = GolfShotSwingType(1)

      /** The swing type of the received golf shot is partial. */
      @JvmField val PARTIAL: GolfShotSwingType = GolfShotSwingType(2)

      /** The swing type of the received golf shot is full. */
      @JvmField val FULL: GolfShotSwingType = GolfShotSwingType(3)

      internal fun fromProto(proto: DataProto.GolfShotSwingType): GolfShotSwingType =
        when (proto) {
          DataProto.GolfShotSwingType.GOLF_SHOT_SWING_TYPE_PUTT -> PUTT
          DataProto.GolfShotSwingType.GOLF_SHOT_SWING_TYPE_PARTIAL -> PARTIAL
          DataProto.GolfShotSwingType.GOLF_SHOT_SWING_TYPE_FULL -> FULL
          else -> UNKNOWN
        }
    }
  }

  internal override fun toProto(): DataProto.ExerciseEvent =
    DataProto.ExerciseEvent.newBuilder()
      .setExerciseEventData(
        DataProto.ExerciseEvent.ExerciseEventData.newBuilder()
          .setGolfShotData(
            DataProto.GolfShotData.newBuilder()
              .setDurationFromBootMs(durationSinceBoot.toMillis())
              .setGolfShotSwingType(swingType.toProto()))
      )
      .build()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is GolfShotEvent) return false

    if (durationSinceBoot != other.durationSinceBoot) return false
    if (swingType != other.swingType) return false

    return true
  }

  override fun hashCode(): Int {
    return Objects.hash(durationSinceBoot, swingType)
  }

  override fun toString(): String {
    return "${this::class.simpleName}" +
      "(durationSinceBoot=$durationSinceBoot, swingType=$swingType)"
  }
}
