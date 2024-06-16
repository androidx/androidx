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

import androidx.health.services.client.proto.DataProto

/**
 * Configuration attributes for a specific exercise type that may be modified after the exercise has
 * started. This should not be instantiated outside the health.services.client.data library.
 * Developers should create instances of [ExerciseTypeConfig] using the constructor of available
 * subclasses, depending on their needs. Currently available types are: [GolfExerciseTypeConfig].
 */
abstract class ExerciseTypeConfig internal constructor() {
    internal abstract fun toProto(): DataProto.ExerciseTypeConfig

    companion object {
        internal fun fromProto(proto: DataProto.ExerciseTypeConfig): ExerciseTypeConfig {
            if (proto.hasGolfShotTrackingPlaceInfo()) {
                return GolfExerciseTypeConfig(
                    GolfExerciseTypeConfig.GolfShotTrackingPlaceInfo.fromProto(
                        proto.golfShotTrackingPlaceInfo
                    )
                )
            } else {
                val emptyExerciseTypeConfig =
                    object : ExerciseTypeConfig() {
                        override fun toProto(): DataProto.ExerciseTypeConfig {
                            return DataProto.ExerciseTypeConfig.getDefaultInstance()
                        }
                    }
                return emptyExerciseTypeConfig
            }
        }
    }
}
