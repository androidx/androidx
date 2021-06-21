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

package androidx.health.services.client.impl.request

import android.os.Parcel
import android.os.Parcelable
import androidx.health.services.client.data.ExerciseGoal

/**
 * Request for adding a [ExerciseGoal] to an exercise.
 *
 * @hide
 */
public data class ExerciseGoalRequest(val packageName: String, val exerciseGoal: ExerciseGoal) :
    Parcelable {
    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(packageName)
        dest.writeParcelable(exerciseGoal, flags)
    }

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<ExerciseGoalRequest> =
            object : Parcelable.Creator<ExerciseGoalRequest> {
                override fun createFromParcel(source: Parcel): ExerciseGoalRequest? {
                    return ExerciseGoalRequest(
                        source.readString() ?: return null,
                        source.readParcelable(ExerciseGoal::class.java.classLoader) ?: return null,
                    )
                }

                override fun newArray(size: Int): Array<ExerciseGoalRequest?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
