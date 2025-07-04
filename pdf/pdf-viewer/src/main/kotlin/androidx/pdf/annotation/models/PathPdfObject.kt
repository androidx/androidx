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

package androidx.pdf.annotation.models

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PathPdfObject(
    public val brushColor: Int,
    public val brushWidth: Float,
    public val inputs: List<PathInput>,
) : PdfObject {

    /** Flattens this object in to a Parcel. */
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        val inputs: List<PathInput> = inputs
        parcel.writeInt(brushColor)
        parcel.writeFloat(brushWidth)
        parcel.writeInt(inputs.size)
        for (input in inputs) {
            input.writeToParcel(parcel, flags)
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
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @SuppressLint("BanParcelableUsage")
    public class PathInput(public val x: Float, public val y: Float) : Parcelable {
        public override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeFloat(x)
            parcel.writeFloat(y)
        }

        public override fun describeContents(): Int = 0

        /** Parcelable creator for [PathInput]. */
        public companion object {
            @JvmField
            public val CREATOR: Parcelable.Creator<PathInput> =
                object : Parcelable.Creator<PathInput> {
                    override fun createFromParcel(parcel: Parcel): PathInput {
                        return PathInput(parcel.readFloat(), parcel.readFloat())
                    }

                    override fun newArray(size: Int): Array<PathInput?> = arrayOfNulls(size)
                }
        }
    }
}
