/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.work.multiprocess.parcelable

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("BanParcelableUsage")
data class ParcelableInterruptRequest(val id: String, val stopReason: Int) : Parcelable {

    internal constructor(parcel: Parcel) : this(parcel.readString()!!, parcel.readInt())

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<ParcelableInterruptRequest> {
            override fun createFromParcel(parcel: Parcel): ParcelableInterruptRequest {
                return ParcelableInterruptRequest(parcel)
            }

            override fun newArray(size: Int): Array<ParcelableInterruptRequest?> {
                return arrayOfNulls(size)
            }
        }
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeInt(stopReason)
    }
}
