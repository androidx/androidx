/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.pdf.annotation.models

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import androidx.pdf.constants.PathOp

internal class PathPdfObject(
    val brushColor: Int,
    val brushWidth: Float,
    val inputs: List<PathInput>,
) : PdfObject {

    internal constructor(
        parcel: Parcel
    ) : this(
        brushColor = parcel.readInt(),
        brushWidth = parcel.readFloat(),
        inputs =
            mutableListOf<PathInput>().apply {
                val inputSize = parcel.readInt()
                for (i in 0 until inputSize) {
                    add(PathInput.CREATOR.createFromParcel(parcel))
                }
            },
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PathPdfObject) return false

        if (brushColor != other.brushColor) return false
        if (brushWidth != other.brushWidth) return false
        if (inputs != other.inputs) return false

        return true
    }

    override fun hashCode(): Int {
        var result = brushColor
        result = 31 * result + brushWidth.hashCode()
        result = 31 * result + inputs.hashCode()
        return result
    }

    /** Flattens this object in to a Parcel. */
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(TYPE)
        dest.writeInt(brushColor)
        dest.writeFloat(brushWidth)
        dest.writeInt(inputs.size)
        for (input in inputs) {
            input.writeToParcel(dest, flags)
        }
    }

    companion object {
        /** Constant representing a path PDF object type. */
        internal const val TYPE: Int = 1

        @JvmField
        val CREATOR: Parcelable.Creator<PathPdfObject> =
            object : Parcelable.Creator<PathPdfObject> {
                override fun createFromParcel(parcel: Parcel): PathPdfObject {
                    val type = parcel.readInt()
                    return PathPdfObject(parcel)
                }

                override fun newArray(size: Int): Array<PathPdfObject?> {
                    return arrayOfNulls(size)
                }
            }
    }

    /**
     * Data model for a single coordinate in a [PathPdfObject].
     *
     * @param x is property the x-coordinate of the point.
     * @param y is property the y-coordinate of the point.
     * @param command The type of path operation (e.g., [MOVE_TO] or [LINE_TO]).
     */
    @SuppressLint("BanParcelableUsage")
    internal class PathInput(val x: Float, val y: Float, @PathOp val command: Int) : Parcelable {
        override fun equals(other: Any?): Boolean {
            return (other is PathInput) && other.x == x && other.y == y && other.command == command
        }

        override fun hashCode(): Int {
            var result = x.hashCode()
            result = 31 * result + y.hashCode()
            result = 31 * result + command
            return result
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeFloat(x)
            parcel.writeFloat(y)
            parcel.writeInt(command)
        }

        override fun describeContents(): Int = 0

        companion object {
            /** Starts a new sub-path from the given coordinate. */
            const val MOVE_TO: Int = androidx.pdf.constants.PathOps.MOVE_TO

            /** Draws a line from the previous point to the given coordinate. */
            const val LINE_TO: Int = androidx.pdf.constants.PathOps.LINE_TO

            @JvmField
            val CREATOR: Parcelable.Creator<PathInput> =
                object : Parcelable.Creator<PathInput> {
                    override fun createFromParcel(parcel: Parcel): PathInput {
                        return PathInput(parcel.readFloat(), parcel.readFloat(), parcel.readInt())
                    }

                    override fun newArray(size: Int): Array<PathInput?> = arrayOfNulls(size)
                }
        }
    }
}
