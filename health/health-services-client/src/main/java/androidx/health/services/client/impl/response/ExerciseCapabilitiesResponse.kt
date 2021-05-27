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
import androidx.health.services.client.data.ExerciseCapabilities

/**
 * Response containing the [ExerciseCapabilities] of the Health Services exercise client on the
 * device.
 *
 * @hide
 */
public data class ExerciseCapabilitiesResponse(
    /** [ExerciseCapabilities] supported by this device. */
    val exerciseCapabilities: ExerciseCapabilities,
) : Parcelable {
    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(exerciseCapabilities, flags)
    }

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<ExerciseCapabilitiesResponse> =
            object : Parcelable.Creator<ExerciseCapabilitiesResponse> {
                override fun createFromParcel(source: Parcel): ExerciseCapabilitiesResponse? {
                    val parcelable =
                        source.readParcelable<ExerciseCapabilities>(
                            ExerciseCapabilities::class.java.classLoader
                        )
                            ?: return null
                    return ExerciseCapabilitiesResponse(parcelable)
                }

                override fun newArray(size: Int): Array<ExerciseCapabilitiesResponse?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
