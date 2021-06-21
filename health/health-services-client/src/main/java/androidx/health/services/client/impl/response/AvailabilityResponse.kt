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
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType

/**
 * Response sent on MeasureCallback with a [DataType] and its associated [Availability] status.
 *
 * @hide
 */
public data class AvailabilityResponse(
    /** [DataType] of the [AvailabilityResponse]. */
    val dataType: DataType,
    /** [Availability] of the [AvailabilityResponse]. */
    val availability: Availability,
) : Parcelable {
    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(dataType, flags)
        dest.writeInt(availability.id)
    }

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<AvailabilityResponse> =
            object : Parcelable.Creator<AvailabilityResponse> {
                override fun createFromParcel(source: Parcel): AvailabilityResponse? {
                    val parcelable =
                        source.readParcelable<DataType>(DataType::class.java.classLoader)
                            ?: return null
                    val availability = Availability.fromId(source.readInt()) ?: return null
                    return AvailabilityResponse(parcelable, availability)
                }

                override fun newArray(size: Int): Array<AvailabilityResponse?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
