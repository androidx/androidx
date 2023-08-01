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
import androidx.health.services.client.proto.DataProto.ExerciseEventCapabilities.ExerciseEventCapabilitiesCase

/** Contains the capabilities specific to the associated [ExerciseEvent]. */
public abstract class ExerciseEventCapabilities @RestrictTo(Scope.LIBRARY_GROUP) constructor() {
  /** Returns true if this [ExerciseEvent] is supported by the device and false otherwise. */
  public abstract val isSupported: Boolean

  internal open fun toProto(): DataProto.ExerciseEventCapabilities =
    DataProto.ExerciseEventCapabilities.getDefaultInstance()

  public companion object {
    @JvmStatic
    internal fun fromProto(proto: DataProto.ExerciseEventCapabilities): ExerciseEventCapabilities? =
      when (proto.exerciseEventCapabilitiesCase) {
        ExerciseEventCapabilitiesCase.GOLF_SHOT_CAPABILITIES ->
          GolfShotEventCapabilities(proto.golfShotCapabilities)
        else -> null
      }
  }
}
