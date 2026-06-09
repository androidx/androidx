/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.pdf.DraftEditOperation as ParcelableDraftEditOperation
import androidx.pdf.InsertDraftEditOperation as ParcelableInsertDraftEditOperation
import androidx.pdf.RemoveDraftEditOperation as ParcelableRemoveDraftEditOperation
import androidx.pdf.UpdateDraftEditOperation as ParcelableUpdateDraftEditOperation
import androidx.pdf.annotation.content.DraftEditOperation
import androidx.pdf.annotation.content.HighlightAnnotation
import androidx.pdf.annotation.content.ImagePdfObject
import androidx.pdf.annotation.content.InsertDraftEditOperation
import androidx.pdf.annotation.content.KeyedPdfAnnotation
import androidx.pdf.annotation.content.KeyedPdfObject
import androidx.pdf.annotation.content.PathPdfObject
import androidx.pdf.annotation.content.PdfAnnotation
import androidx.pdf.annotation.content.PdfObject
import androidx.pdf.annotation.content.RemoveDraftEditOperation
import androidx.pdf.annotation.content.StampAnnotation
import androidx.pdf.annotation.content.UpdateDraftEditOperation
import androidx.pdf.annotation.models.HighlightAnnotation as ParcelableHighlight
import androidx.pdf.annotation.models.ImagePdfObject as ParcelableImage
import androidx.pdf.annotation.models.KeyedPdfAnnotation as ParcelableKeyedPdfAnnotation
import androidx.pdf.annotation.models.KeyedPdfObject as ParcelableKeyedObject
import androidx.pdf.annotation.models.PathPdfObject as ParcelablePath
import androidx.pdf.annotation.models.PdfAnnotation as ParcelableAnnotation
import androidx.pdf.annotation.models.PdfObject as ParcelablePdfObject
import androidx.pdf.annotation.models.StampAnnotation as ParcelableStamp

/**
 * Utility functions to convert between public models and their internal Parcelable representations.
 */
internal object PdfModelMapper {

    /**
     * Converts a [androidx.pdf.annotation.content.PdfAnnotation] to its internal Parcelable
     * representation.
     */
    fun PdfAnnotation.toParcelable(): ParcelableAnnotation {
        return when (this) {
            is HighlightAnnotation -> {
                ParcelableHighlight(pageNum, bounds, color)
            }
            is StampAnnotation -> {
                ParcelableStamp(pageNum, bounds, pdfObjects.map { it.toParcelable() })
            }
            else -> throw IllegalArgumentException("Unknown annotation type")
        }
    }

    /** Converts a Parcelable [ParcelableAnnotation] to its public representation. */
    fun ParcelableAnnotation.toContent(): PdfAnnotation {
        return when (this) {
            is ParcelableHighlight -> {
                HighlightAnnotation(pageNum, bounds, color)
            }
            is ParcelableStamp -> {
                StampAnnotation(pageNum, bounds, pdfObjects.map { it.toContent() })
            }
            else -> throw IllegalArgumentException("Unknown Parcelable annotation type")
        }
    }

    /**
     * Converts a [androidx.pdf.annotation.content.PdfObject] to its internal Parcelable
     * representation.
     */
    fun PdfObject.toParcelable(): ParcelablePdfObject {
        return when (this) {
            is PathPdfObject -> {
                ParcelablePath(
                    brushColor,
                    brushWidth,
                    inputs.map { ParcelablePath.PathInput(it.x, it.y, it.command) },
                )
            }
            is ImagePdfObject -> {
                ParcelableImage(bitmap, bounds)
            }
        }
    }

    /** Converts a Parcelable [ParcelablePdfObject] to its public representation. */
    fun ParcelablePdfObject.toContent(): PdfObject {
        return when (this) {
            is ParcelablePath -> {
                PathPdfObject(
                    brushColor,
                    brushWidth,
                    inputs.map { PathPdfObject.PathInput(it.x, it.y, it.command) },
                )
            }
            is ParcelableImage -> {
                ImagePdfObject(bitmap, bounds)
            }
        }
    }

    /**
     * Converts a [androidx.pdf.annotation.content.DraftEditOperation] to its internal Parcelable
     * representation.
     */
    fun DraftEditOperation.toParcelable(): ParcelableDraftEditOperation {
        if (this is ParcelableDraftEditOperation) return this
        return when (this) {
            is InsertDraftEditOperation ->
                ParcelableInsertDraftEditOperation(annotation.toParcelable())
            is UpdateDraftEditOperation ->
                ParcelableUpdateDraftEditOperation(id, annotation.toParcelable())
            is RemoveDraftEditOperation -> ParcelableRemoveDraftEditOperation(id, pageNum)
            else ->
                throw IllegalArgumentException("Unknown DraftEditOperation type: ${this.javaClass}")
        }
    }

    /** Converts a Parcelable [ParcelableKeyedPdfAnnotation] to its public representation. */
    fun ParcelableKeyedPdfAnnotation.toContent(): KeyedPdfAnnotation {
        return KeyedPdfAnnotation(key, annotation.toContent())
    }

    /** Converts a [KeyedPdfAnnotation] to its internal Parcelable representation. */
    fun KeyedPdfAnnotation.toParcelable(): ParcelableKeyedPdfAnnotation {
        return ParcelableKeyedPdfAnnotation(key, annotation.toParcelable())
    }

    /** Converts a [ParcelableKeyedObject] to its public representation. */
    fun ParcelableKeyedObject.toContent(): KeyedPdfObject {
        return KeyedPdfObject(key, pdfObject.toContent())
    }

    /** Converts a [KeyedPdfObject] to its internal Parcelable representation. */
    fun KeyedPdfObject.toParcelable(): ParcelableKeyedObject {
        return ParcelableKeyedObject(key, pdfObject.toParcelable())
    }
}
