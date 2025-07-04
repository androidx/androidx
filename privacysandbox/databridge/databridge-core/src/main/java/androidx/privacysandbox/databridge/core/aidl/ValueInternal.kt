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
public data class ValueInternal(
    val type: String,
    val isValueNull: Boolean = false,
    val value: Any?,
) : Parcelable {
    @RequiresApi(Build.VERSION_CODES.Q)
    @Suppress("UNCHECKED_CAST")
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(type)
        parcel.writeBoolean(isValueNull)
        if (isValueNull) {
            parcel.writeString(null)
            return
        }
        when (type) {
            "INT" -> parcel.writeInt(value as Int)
            "LONG" -> parcel.writeLong(value as Long)
            "FLOAT" -> parcel.writeFloat(value as Float)
            "DOUBLE" -> parcel.writeDouble(value as Double)
            "BOOLEAN" -> parcel.writeBoolean(value as Boolean)
            "STRING" -> parcel.writeString(value as String)
            "STRING_SET" -> {
                val valueSet = value as Set<*>
                parcel.writeStringList(valueSet.toList() as List<String?>?)
            }
            "BYTE_ARRAY" -> parcel.writeByteArray(value as ByteArray)
            else -> throw IllegalArgumentException("Unsupported type: $type")
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    public companion object CREATOR : Parcelable.Creator<ValueInternal> {
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @Suppress("UNCHECKED_CAST")
        override fun createFromParcel(parcel: Parcel): ValueInternal {
            val type = parcel.readString()!!
            val isValueNull = parcel.readBoolean()
            val value =
                if (!isValueNull) {
                    when (type) {
                        "INT" -> parcel.readInt()
                        "LONG" -> parcel.readLong()
                        "FLOAT" -> parcel.readFloat()
                        "DOUBLE" -> parcel.readDouble()
                        "BOOLEAN" -> parcel.readBoolean()
                        "STRING" -> parcel.readString()
                        "STRING_SET" -> parcel.createStringArrayList()?.toSet()
                        "BYTE_ARRAY" -> parcel.createByteArray()
                        else -> throw IllegalArgumentException("Unsupported type: $type")
                    }
                } else {
                    parcel.readString()
                }
            return ValueInternal(type, isValueNull, value)
        }

        override fun newArray(size: Int): Array<ValueInternal?> {
            return arrayOfNulls(size)
        }
    }
}
