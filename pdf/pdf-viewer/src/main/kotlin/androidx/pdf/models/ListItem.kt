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

package androidx.pdf.models

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RestrictTo
import java.util.Objects

/** Represents a single option in a combo box or list box PDF form widget. */
@SuppressLint("BanParcelableUsage")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ListItem(public val label: String, public val selected: Boolean) : Parcelable {

    private constructor(
        parcel: Parcel?
    ) : this(label = parcel?.readString() ?: "", selected = parcel?.readBoolean() ?: false)

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(label)
        dest.writeBoolean(selected)
    }

    override fun hashCode(): Int {
        return Objects.hash(label, selected)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ListItem) return false

        return label == other.label && selected == other.selected
    }

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<ListItem> =
            object : Parcelable.Creator<ListItem> {
                override fun createFromParcel(parcel: Parcel?): ListItem {
                    return ListItem(parcel)
                }

                override fun newArray(size: Int): Array<ListItem?>? {
                    return arrayOfNulls(size)
                }
            }
    }
}
