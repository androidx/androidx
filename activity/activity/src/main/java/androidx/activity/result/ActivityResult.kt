/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.activity.result

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Parcel
import android.os.Parcelable

/**
 * A container for an activity result as obtained from [Activity.onActivityResult]
 *
 * @see Activity.onActivityResult
 */
@SuppressLint("BanParcelableUsage")
class ActivityResult(
    /** Status to indicate the success of the operation */
    val resultCode: Int,

    /** The intent that carries the result data */
    val data: Intent?
) : Parcelable {

    internal constructor(
        parcel: Parcel
    ) : this(
        parcel.readInt(),
        if (parcel.readInt() == 0) null else Intent.CREATOR.createFromParcel(parcel)
    )

    override fun toString(): String {
        return "ActivityResult{resultCode=${resultCodeToString(resultCode)}, data=$data}"
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(resultCode)
        dest.writeInt(if (data == null) 0 else 1)
        data?.writeToParcel(dest, flags)
    }

    override fun describeContents() = 0

    companion object {
        /**
         * A readable representation of standard activity result codes for the given [resultCode]
         *
         * @return RESULT_OK, RESULT_CANCELED, or the number otherwise
         */
        @JvmStatic
        fun resultCodeToString(resultCode: Int): String {
            return when (resultCode) {
                Activity.RESULT_OK -> "RESULT_OK"
                Activity.RESULT_CANCELED -> "RESULT_CANCELED"
                else -> resultCode.toString()
            }
        }

        @Suppress("unused")
        @JvmField
        val CREATOR =
            object : Parcelable.Creator<ActivityResult> {
                override fun createFromParcel(parcel: Parcel) = ActivityResult(parcel)

                override fun newArray(size: Int) = arrayOfNulls<ActivityResult>(size)
            }
    }
}

/**
 * Destructuring declaration for [ActivityResult] to provide the requestCode
 *
 * @return the resultCode of the [ActivityResult]
 */
operator fun ActivityResult.component1(): Int = resultCode

/**
 * Destructuring declaration for [ActivityResult] to provide the intent
 *
 * @return the intent of the [ActivityResult]
 */
operator fun ActivityResult.component2(): Intent? = data
