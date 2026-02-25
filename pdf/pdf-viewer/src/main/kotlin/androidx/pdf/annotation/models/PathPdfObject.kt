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
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PathPdfObject(
    public val brushColor: Int,
    public val brushWidth: Float,
    public val inputs: List<PathInput>,
) : PdfObject {

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
    public override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        val inputs: List<PathInput> = inputs
        dest.writeInt(brushColor)
        dest.writeFloat(brushWidth)
        dest.writeInt(inputs.size)
        for (input in inputs) {
            input.writeToParcel(dest, flags)
        }
    }

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<PathPdfObject> =
            object : Parcelable.Creator<PathPdfObject> {
                override fun createFromParcel(parcel: Parcel): PathPdfObject {
                    val brushColor = parcel.readInt()
                    val brushWidth = parcel.readFloat()
                    val inputSize = parcel.readInt()
                    val inputs = mutableListOf<PathInput>()
                    for (i in 0 until inputSize) {
                        val input = PathInput.CREATOR.createFromParcel(parcel)
                        inputs.add(input)
                    }
                    return PathPdfObject(brushColor, brushWidth, inputs)
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
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @SuppressLint("BanParcelableUsage")
    public class PathInput(
        public val x: Float,
        public val y: Float,
        @PathOp public val command: Int,
    ) : Parcelable {
        override fun equals(other: Any?): Boolean {
            return (other is PathInput) && other.x == x && other.y == y && other.command == command
        }

        override fun hashCode(): Int {
            var result = x.hashCode()
            result = 31 * result + y.hashCode()
            result = 31 * result + command
            return result
        }

        public override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeFloat(x)
            parcel.writeFloat(y)
            parcel.writeInt(command)
        }

        public override fun describeContents(): Int = 0

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(MOVE_TO, LINE_TO)
        public annotation class PathOp

        public companion object {
            /** Starts a new sub-path from the given coordinate. */
            public const val MOVE_TO: Int = 0

            /** Draws a line from the previous point to the given coordinate. */
            public const val LINE_TO: Int = 1

            @JvmField
            public val CREATOR: Parcelable.Creator<PathInput> =
                object : Parcelable.Creator<PathInput> {
                    override fun createFromParcel(parcel: Parcel): PathInput {
                        return PathInput(parcel.readFloat(), parcel.readFloat(), parcel.readInt())
                    }

                    override fun newArray(size: Int): Array<PathInput?> = arrayOfNulls(size)
                }
        }
    }
}
