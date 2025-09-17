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

package androidx.compose.material3.adaptive.navigation3

import android.os.Parcel
import android.os.Parcelable

sealed interface TestKey

data object HomeKey : Parcelable, TestKey {
    override fun writeToParcel(parcel: Parcel, flags: Int) = Unit

    override fun describeContents(): Int = 0

    @JvmField
    val CREATOR: Parcelable.Creator<HomeKey> =
        object : Parcelable.Creator<HomeKey> {
            override fun createFromParcel(parcel: Parcel): HomeKey {
                return HomeKey
            }

            override fun newArray(size: Int): Array<HomeKey?> {
                return arrayOfNulls(size)
            }
        }
}

data object ListKey : Parcelable, TestKey {
    override fun writeToParcel(parcel: Parcel, flags: Int) = Unit

    override fun describeContents(): Int = 0

    @JvmField
    val CREATOR: Parcelable.Creator<ListKey> =
        object : Parcelable.Creator<ListKey> {
            override fun createFromParcel(parcel: Parcel): ListKey {
                return ListKey
            }

            override fun newArray(size: Int): Array<ListKey?> {
                return arrayOfNulls(size)
            }
        }
}

data class DetailKey(val id: String) : Parcelable, TestKey {
    constructor(parcel: Parcel) : this(parcel.readString()!!)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<DetailKey> {
        override fun createFromParcel(parcel: Parcel): DetailKey {
            return DetailKey(parcel)
        }

        override fun newArray(size: Int): Array<DetailKey?> {
            return arrayOfNulls(size)
        }
    }
}

data class ExtraKey(val id: String) : Parcelable, TestKey {
    constructor(parcel: Parcel) : this(parcel.readString()!!)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ExtraKey> {
        override fun createFromParcel(parcel: Parcel): ExtraKey {
            return ExtraKey(parcel)
        }

        override fun newArray(size: Int): Array<ExtraKey?> {
            return arrayOfNulls(size)
        }
    }
}

data object MainKey : Parcelable, TestKey {
    override fun writeToParcel(parcel: Parcel, flags: Int) = Unit

    override fun describeContents(): Int = 0

    @JvmField
    val CREATOR: Parcelable.Creator<MainKey> =
        object : Parcelable.Creator<MainKey> {
            override fun createFromParcel(parcel: Parcel): MainKey {
                return MainKey
            }

            override fun newArray(size: Int): Array<MainKey?> {
                return arrayOfNulls(size)
            }
        }
}

data object SupportingKey : Parcelable, TestKey {
    override fun writeToParcel(parcel: Parcel, flags: Int) = Unit

    override fun describeContents(): Int = 0

    @JvmField
    val CREATOR: Parcelable.Creator<SupportingKey> =
        object : Parcelable.Creator<SupportingKey> {
            override fun createFromParcel(parcel: Parcel): SupportingKey {
                return SupportingKey
            }

            override fun newArray(size: Int): Array<SupportingKey?> {
                return arrayOfNulls(size)
            }
        }
}
