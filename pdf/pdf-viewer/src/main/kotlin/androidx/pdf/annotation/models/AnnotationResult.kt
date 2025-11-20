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

/**
 * Data class representing the result of an annotation operation.
 *
 * This class encapsulates the outcome of processing multiple annotations, separating them into
 * those that were successfully processed and those that failed.
 *
 * @property success A map of successfully processed annotations, where the key is AnnotationId
 *   returned by the PDF Renderer and the value is the [PdfAnnotation].
 * @property failures A list of annotations that failed to be processed.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("BanParcelableUsage")
public class AnnotationResult(
    public override val success: List<PdfAnnotationData>,
    public override val failures: List<PdfAnnotation>,
) : Parcelable, PdfEditResult<PdfAnnotationData, PdfAnnotation> {

    /** Default implementation for [Parcelable.describeContents], returning 0. */
    override fun describeContents(): Int = 0

    /** Flattens this object in to a Parcel. */
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(success.size)
        for (pdfAnnotationData in success) {
            pdfAnnotationData.writeToParcel(dest, flags)
        }
        dest.writeInt(failures.size)
        failures.forEach { it.writeToParcel(dest, flags) }
    }

    /** Companion object for creating [AnnotationResult] instances from a [Parcel]. */
    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<AnnotationResult> =
            object : Parcelable.Creator<AnnotationResult> {
                /**
                 * Creates an [AnnotationResult] instance from a [Parcel].
                 *
                 * @param parcel The parcel to read the object's data from.
                 * @return A new instance of [AnnotationResult], or null if creation fails.
                 */
                override fun createFromParcel(parcel: Parcel): AnnotationResult? {
                    val successSize = parcel.readInt()
                    val success = mutableListOf<PdfAnnotationData>()
                    for (i in 0 until successSize) {
                        PdfAnnotationData.CREATOR.createFromParcel(parcel)?.let { pdfAnnotationData
                            ->
                            success.add(pdfAnnotationData)
                        }
                    }
                    val failuresSize = parcel.readInt()
                    val failures = mutableListOf<PdfAnnotation>()
                    for (i in 0 until failuresSize) {
                        PdfAnnotation.CREATOR.createFromParcel(parcel)?.let { annotation ->
                            failures.add(annotation)
                        }
                    }
                    return AnnotationResult(success, failures)
                }

                /**
                 * Creates a new array of [AnnotationResult].
                 *
                 * @param size The size of the array.
                 * @return An array of [AnnotationResult] of the specified size, with all elements
                 *   initialized to null.
                 */
                override fun newArray(size: Int): Array<AnnotationResult?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
