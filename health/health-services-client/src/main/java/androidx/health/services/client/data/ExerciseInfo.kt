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

/** High-level info about the exercise. */
public data class ExerciseInfo(
    /** Returns the [ExerciseTrackedStatus]. */
    val exerciseTrackedStatus: ExerciseTrackedStatus,

    /**
     * Returns the [ExerciseType] of the active exercise, or [ExerciseType.UNKNOWN] if there is no
     * active exercise.
     */
    val exerciseType: ExerciseType,
) : Parcelable {
    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(exerciseTrackedStatus.id)
        dest.writeInt(exerciseType.id)
    }

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<ExerciseInfo> =
            object : Parcelable.Creator<ExerciseInfo> {
                override fun createFromParcel(source: Parcel): ExerciseInfo? {
                    return ExerciseInfo(
                        ExerciseTrackedStatus.fromId(source.readInt()) ?: return null,
                        ExerciseType.fromId(source.readInt())
                    )
                }

                override fun newArray(size: Int): Array<ExerciseInfo?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
