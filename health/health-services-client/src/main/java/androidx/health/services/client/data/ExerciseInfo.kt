/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.os.Parcelable
import androidx.health.services.client.proto.DataProto
import java.lang.IllegalStateException

/** High-level info about the exercise. */
@Suppress("ParcelCreator")
public class ExerciseInfo(
    /** Returns the [ExerciseTrackedStatus]. */
    public val exerciseTrackedStatus: ExerciseTrackedStatus,

    /**
     * Returns the [ExerciseType] of the active exercise, or [ExerciseType.UNKNOWN] if there is no
     * active exercise.
     */
    public val exerciseType: ExerciseType,
) : ProtoParcelable<DataProto.ExerciseInfo>() {

    internal constructor(
        proto: DataProto.ExerciseInfo
    ) : this(
        ExerciseTrackedStatus.fromProto(proto.exerciseTrackedStatus)
            ?: throw IllegalStateException("Invalid status ${proto.exerciseTrackedStatus}"),
        ExerciseType.fromProto(proto.exerciseType)
    )

    /** @hide */
    override val proto: DataProto.ExerciseInfo by lazy {
        DataProto.ExerciseInfo.newBuilder()
            .setExerciseTrackedStatus(exerciseTrackedStatus.toProto())
            .setExerciseType(exerciseType.toProto())
            .build()
    }

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<ExerciseInfo> = newCreator { bytes ->
            val proto = DataProto.ExerciseInfo.parseFrom(bytes)
            ExerciseInfo(proto)
        }
    }
}
