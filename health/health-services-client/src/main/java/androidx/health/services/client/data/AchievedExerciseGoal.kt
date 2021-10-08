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

/** Defines an achieved [ExerciseGoal]. */
@Suppress("ParcelCreator")
public class AchievedExerciseGoal(
    /** [ExerciseGoal] that has been achieved. */
    // TODO(b/181235444): do we need to deliver the DataPoint to the user again here, given
    // that they will have already gotten it in the ExerciseState? And, what other data do we need
    // to
    // tag along an achieved ExerciseGoal?
    public val goal: ExerciseGoal,
) : ProtoParcelable<DataProto.AchievedExerciseGoal>() {

    internal constructor(
        proto: DataProto.AchievedExerciseGoal
    ) : this(ExerciseGoal(proto.exerciseGoal))

    /** @hide */
    override val proto: DataProto.AchievedExerciseGoal by lazy {
        DataProto.AchievedExerciseGoal.newBuilder().setExerciseGoal(goal.proto).build()
    }

    override fun toString(): String = "AchievedExerciseGoal(goal=$goal)"

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<AchievedExerciseGoal> = newCreator {
            val proto = DataProto.AchievedExerciseGoal.parseFrom(it)
            AchievedExerciseGoal(proto)
        }
    }
}
