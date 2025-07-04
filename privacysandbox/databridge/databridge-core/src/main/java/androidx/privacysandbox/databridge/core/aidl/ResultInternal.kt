/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.privacysandbox.databridge.core.aidl

import android.annotation.SuppressLint
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo

@SuppressLint("BanParcelableUsage")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class ResultInternal(
    val keyName: String,
    val exceptionName: String?,
    val exceptionMessage: String?,
    val valueInternal: ValueInternal?,
) : Parcelable {
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(keyName)
        parcel.writeString(exceptionName)
        parcel.writeString(exceptionMessage)
        parcel.writeParcelable(valueInternal, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    public companion object CREATOR : Parcelable.Creator<ResultInternal> {
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun createFromParcel(parcel: Parcel): ResultInternal {
            val keyName = parcel.readString()!!
            val exceptionName = parcel.readString()
            val exceptionString = parcel.readString()
            val valueInternal =
                parcel.readParcelable(
                    ValueInternal::class.java.classLoader,
                    ValueInternal::class.java,
                )
            return ResultInternal(keyName, exceptionName, exceptionString, valueInternal)
        }

        override fun newArray(size: Int): Array<ResultInternal?> {
            return arrayOfNulls(size)
        }
    }
}
