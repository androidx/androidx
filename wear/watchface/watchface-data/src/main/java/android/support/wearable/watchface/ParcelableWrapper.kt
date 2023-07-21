/*
 * Copyright 2022 The Android Open Source Project
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

package android.support.wearable.watchface

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RestrictTo

/** Wraps a Parcelable. */
@SuppressLint("BanParcelableUsage")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ParcelableWrapper(val parcelable: Parcelable) : Parcelable {

    constructor(parcel: Parcel) : this(unparcel(parcel))

    override fun writeToParcel(dest: Parcel, flags: Int) {
        parcelable.writeToParcel(dest, flags)
    }

    override fun describeContents(): Int = parcelable.describeContents()

    public companion object {
        @JvmField
        @Suppress("DEPRECATION")
        public val CREATOR: Parcelable.Creator<ParcelableWrapper> =
            object : Parcelable.Creator<ParcelableWrapper> {
                override fun createFromParcel(parcel: Parcel) = ParcelableWrapper(parcel)

                override fun newArray(size: Int) = arrayOfNulls<ParcelableWrapper?>(size)
            }

        internal var unparcel: (Parcel) -> Parcelable = {
            throw RuntimeException("setUnparceler not called")
        }

        public fun setUnparceler(unparceler: (Parcel) -> Parcelable) {
            unparcel = unparceler
        }
    }
}
