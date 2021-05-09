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

package androidx.health.services.client.impl.response

import android.os.Parcel
import android.os.Parcelable
import androidx.health.services.client.data.ExerciseUpdate

/**
 * Response containing [ExerciseUpdate] when it's updated.
 *
 * @hide
 */
public data class ExerciseUpdateResponse(val exerciseUpdate: ExerciseUpdate) : Parcelable {
    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(exerciseUpdate, flags)
    }

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<ExerciseUpdateResponse> =
            object : Parcelable.Creator<ExerciseUpdateResponse> {
                override fun createFromParcel(source: Parcel): ExerciseUpdateResponse? {
                    val parcelable =
                        source.readParcelable<ExerciseUpdate>(
                            ExerciseUpdate::class.java.classLoader
                        )
                            ?: return null
                    return ExerciseUpdateResponse(parcelable)
                }

                override fun newArray(size: Int): Array<ExerciseUpdateResponse?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
