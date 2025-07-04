/*
 * Copyright 2024 The Android Open Source Project
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
import android.app.Notification
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.work.ForegroundInfo

/** [androidx.work.ForegroundInfo] but [android.os.Parcelable]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("BanParcelableUsage")
public data class ParcelableForegroundInfo(val foregroundInfo: ForegroundInfo) : Parcelable {

    public constructor(
        parcel: Parcel
    ) : this(
        foregroundInfo =
            ForegroundInfo(
                parcel.readInt(),
                ParcelUtils.readParcelable(parcel, Notification::class.java),
                parcel.readInt(),
            )
    )

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(foregroundInfo.notificationId)
        parcel.writeParcelable(foregroundInfo.notification, flags)
        parcel.writeInt(foregroundInfo.foregroundServiceType)
    }

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<ParcelableForegroundInfo> =
            object : Parcelable.Creator<ParcelableForegroundInfo> {
                override fun createFromParcel(parcel: Parcel): ParcelableForegroundInfo {
                    return ParcelableForegroundInfo(parcel)
                }

                override fun newArray(size: Int): Array<ParcelableForegroundInfo?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
