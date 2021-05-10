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

import android.os.Parcel
import android.os.Parcelable

/** Defines an achieved [ExerciseGoal]. */
public data class AchievedExerciseGoal(
    /** [ExerciseGoal] that has been achieved. */
    // TODO(b/181235444): do we need to deliver the DataPoint to the user again here, given
    // that they will have already gotten it in the ExerciseState? And, what other data do we need
    // to
    // tag along an achieved ExerciseGoal?
    val goal: ExerciseGoal,
) : Parcelable {
    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(goal, flags)
    }

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<AchievedExerciseGoal> =
            object : Parcelable.Creator<AchievedExerciseGoal> {
                override fun createFromParcel(source: Parcel): AchievedExerciseGoal? {
                    val goal =
                        source.readParcelable<ExerciseGoal>(ExerciseGoal::class.java.classLoader)
                            ?: return null
                    return AchievedExerciseGoal(goal)
                }

                override fun newArray(size: Int): Array<AchievedExerciseGoal?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
