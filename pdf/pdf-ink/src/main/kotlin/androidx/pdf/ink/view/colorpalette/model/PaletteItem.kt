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

package androidx.pdf.ink.view.colorpalette.model

import android.os.Parcel
import android.os.Parcelable

/**
 * Base class for an item displayed in the color palette. This class is parcelable to allow it to be
 * saved and restored.
 *
 * @param contentDescription A description of the item, used for accessibility.
 */
internal abstract class PaletteItem(open val contentDescription: String) : Parcelable {

    override fun describeContents(): Int = 0

    companion object {
        const val TYPE_COLOR = 1
        const val TYPE_EMOJI = 2

        @JvmField
        val CREATOR: Parcelable.Creator<PaletteItem> =
            object : Parcelable.Creator<PaletteItem> {
                /**
                 * Creates a [PaletteItem] subclass instance from a [Parcel]. It reads a type
                 * identifier first and then delegates the rest of the creation to the appropriate
                 * subclass.
                 */
                override fun createFromParcel(parcel: Parcel): PaletteItem {
                    return when (val type = parcel.readInt()) {
                        TYPE_COLOR -> Color(parcel)
                        TYPE_EMOJI -> Emoji(parcel)
                        else ->
                            throw IllegalArgumentException(
                                "Invalid PaletteItem type in Parcel: $type"
                            )
                    }
                }

                /** Creates a new array of [PaletteItem]. */
                override fun newArray(size: Int): Array<PaletteItem?> = arrayOfNulls(size)
            }
    }
}

/**
 * Data class representing a solid color item in the palette.
 *
 * @param color The integer value of the color.
 * @param outlineColor The color of the stroke to be drawn around the color swatch.
 * @param tickColor The color of the selection tick mark.
 * @param contentDescription A description of the color, used for accessibility.
 */
internal data class Color(
    val color: Int,
    val outlineColor: Int,
    val tickColor: Int,
    override val contentDescription: String,
) : PaletteItem(contentDescription) {

    /**
     * Secondary constructor used exclusively for un-parceling the object. It reads the object's
     * properties from the provided [Parcel].
     */
    internal constructor(
        parcel: Parcel
    ) : this(
        color = parcel.readInt(),
        outlineColor = parcel.readInt(),
        tickColor = parcel.readInt(),
        contentDescription = parcel.readString() ?: "",
    )

    /**
     * Writes the object's data to the provided [Parcel]. It first writes a type identifier for this
     * subclass, followed by its properties.
     */
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(TYPE_COLOR)
        parcel.writeInt(color)
        parcel.writeInt(outlineColor)
        parcel.writeInt(tickColor)
        parcel.writeString(contentDescription)
    }
}

/**
 * Data class representing an emoji or stamp item in the palette.
 *
 * @param emoji The resource ID of the emoji drawable.
 * @param contentDescription A description of the emoji, used for accessibility.
 */
internal data class Emoji(
    val emoji: Int,
    // TODO(b/452270462) Remove default string and correct contentDesc
    override val contentDescription: String = "",
) : PaletteItem(contentDescription) {

    /**
     * Secondary constructor used exclusively for un-parceling the object. It reads the object's
     * properties from the provided [Parcel].
     */
    internal constructor(parcel: Parcel) : this(parcel.readInt(), parcel.readString() ?: "")

    /**
     * Writes the object's data to the provided [Parcel]. It first writes a type identifier for this
     * subclass, followed by its properties.
     */
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(TYPE_EMOJI)
        parcel.writeInt(emoji)
        parcel.writeString(contentDescription)
    }
}
