/*
 * Copyright 2021 The Android Open Source Project
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
import androidx.work.Data

/** [androidx.work.Data] but [android.os.Parcelable]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("BanParcelableUsage")
class ParcelableData(val data: Data) : Parcelable {

    constructor(
        inParcel: Parcel
    ) : this(inParcel.createByteArray()?.let { Data.fromByteArray(it) } ?: Data.EMPTY)

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByteArray(data.toByteArray())
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ParcelableData> =
            object : Parcelable.Creator<ParcelableData> {
                override fun createFromParcel(inParcel: Parcel): ParcelableData {
                    return ParcelableData(inParcel)
                }

                override fun newArray(size: Int): Array<ParcelableData?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
