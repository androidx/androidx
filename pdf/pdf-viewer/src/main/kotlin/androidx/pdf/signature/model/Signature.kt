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

package androidx.pdf.signature.model

import android.graphics.Bitmap
import android.graphics.Path
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import java.util.Objects

/**
 * Represents a Signature Model in PDF coordinates.
 *
 * @property id A unique id for the signature, used to track updates, selections, and deletions.
 * @property pageNum The 0-indexed PDF page number where this signature is currently placed.
 * @property xCoord represents the x-coordinate of the signature from the top left of the signature
 *   bounding box points in PDF points from the left of the page.
 * @property yCoord represents the y-coordinate of the signature from the top left of the signature
 *   bounding box points in PDF points from the top of the page.
 * @property width represents the width of the signature's bounding box in PDF points.
 * @property height represents the height of the signature's bounding box in PDF points.
 * @property isSelected represents whether the signature is selected or not.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class Signature internal constructor() {
    public abstract val id: String
    public abstract val pageNum: Int
    public abstract val xCoord: Float
    public abstract val yCoord: Float
    public abstract val width: Float
    public abstract val height: Float
    public abstract val isSelected: Boolean

    /** Returns a copy of the current signature with the specified selection state. */
    public abstract fun setSelection(isSelected: Boolean): Signature

    /**
     * A signature drawn by the user using vector paths.
     *
     * @property drawnPath The vector path representing the user's pen strokes, allowing the
     *   signature to be perfectly redrawn at any scale without pixelation.
     */
    public class DrawnSignature(
        override val id: String,
        override val pageNum: Int,
        override val xCoord: Float,
        override val yCoord: Float,
        override val width: Float,
        override val height: Float,
        override val isSelected: Boolean,
        public val drawnPath: Path,
    ) : Signature() {

        override fun setSelection(isSelected: Boolean): Signature =
            DrawnSignature(
                id = this.id,
                pageNum = this.pageNum,
                xCoord = this.xCoord,
                yCoord = this.yCoord,
                width = this.width,
                height = this.height,
                isSelected = isSelected,
                drawnPath = this.drawnPath,
            )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DrawnSignature) return false
            return id == other.id &&
                pageNum == other.pageNum &&
                xCoord == other.xCoord &&
                yCoord == other.yCoord &&
                width == other.width &&
                height == other.height &&
                isSelected == other.isSelected &&
                drawnPath == other.drawnPath
        }

        override fun hashCode(): Int {
            return Objects.hash(id, pageNum, xCoord, yCoord, width, height, isSelected, drawnPath)
        }
    }

    /**
     * A signature generated from text typed by the user.
     *
     * @property typedText The exact text string entered by the user.
     * @property typedFont The specific font asset identifier used to render the text.
     */
    public class TypedSignature(
        override val id: String,
        override val pageNum: Int,
        override val xCoord: Float,
        override val yCoord: Float,
        override val width: Float,
        override val height: Float,
        override val isSelected: Boolean,
        public val typedText: String,
        @property:TypedFont public val typedFont: Int,
    ) : Signature() {

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(FONT_SERIF, FONT_SANS_SERIF, FONT_CURSIVE, FONT_MONOSPACE)
        public annotation class TypedFont

        public companion object {
            public const val FONT_SERIF: Int = 0
            public const val FONT_SANS_SERIF: Int = 1
            public const val FONT_CURSIVE: Int = 2
            public const val FONT_MONOSPACE: Int = 3
        }

        override fun setSelection(isSelected: Boolean): Signature =
            TypedSignature(
                id = this.id,
                pageNum = this.pageNum,
                xCoord = this.xCoord,
                yCoord = this.yCoord,
                width = this.width,
                height = this.height,
                isSelected = isSelected,
                typedText = this.typedText,
                typedFont = this.typedFont,
            )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is TypedSignature) return false
            return id == other.id &&
                pageNum == other.pageNum &&
                xCoord == other.xCoord &&
                yCoord == other.yCoord &&
                width == other.width &&
                height == other.height &&
                isSelected == other.isSelected &&
                typedText == other.typedText &&
                typedFont == other.typedFont
        }

        override fun hashCode(): Int {
            return Objects.hash(
                id,
                pageNum,
                xCoord,
                yCoord,
                width,
                height,
                isSelected,
                typedText,
                typedFont,
            )
        }
    }

    /**
     * A signature imported from an image file on the user's device.
     *
     * @property imageBitmap The Bitmap representation of the signature image.
     */
    public class UploadedSignature(
        override val id: String,
        override val pageNum: Int,
        override val xCoord: Float,
        override val yCoord: Float,
        override val width: Float,
        override val height: Float,
        override val isSelected: Boolean,
        public val imageBitmap: Bitmap,
    ) : Signature() {

        override fun setSelection(isSelected: Boolean): Signature =
            UploadedSignature(
                id = this.id,
                pageNum = this.pageNum,
                xCoord = this.xCoord,
                yCoord = this.yCoord,
                width = this.width,
                height = this.height,
                isSelected = isSelected,
                imageBitmap = this.imageBitmap,
            )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is UploadedSignature) return false
            return id == other.id &&
                pageNum == other.pageNum &&
                xCoord == other.xCoord &&
                yCoord == other.yCoord &&
                width == other.width &&
                height == other.height &&
                isSelected == other.isSelected &&
                imageBitmap.sameAs(other.imageBitmap)
        }

        override fun hashCode(): Int {
            return Objects.hash(id, pageNum, xCoord, yCoord, width, height, isSelected, imageBitmap)
        }
    }
}
