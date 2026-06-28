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

import androidx.annotation.RestrictTo
import java.util.Objects

/**
 * This is the plain model for a signature, acting as the Single Source of Truth (SSOT) in the
 * ViewModel. It ensures signatures can scale perfectly across different devices, screen densities,
 * and zoom levels.
 *
 * @property id A unique id for the signature, used to track updates, selections, and deletions.
 * @property pageNum The 0-indexed PDF page number where this signature is currently placed.
 * @property normX The horizontal anchor point (0.0f to 1.0f). Represents a percentage across the
 *   unscaled page width from the top-left of the page.
 * @property normY The vertical anchor point (0.0f to 1.0f). Represents a percentage down the
 *   unscaled page height from the top-left of the page.
 * @property widthDp The physical base width of the signature's bounding box in density-independent
 *   pixels (dp).
 * @property heightDp The physical base height of the signature's bounding box in
 *   density-independent pixels (dp).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class Signature internal constructor() {
    public abstract val id: String
    public abstract val pageNum: Int
    public abstract val normX: Float
    public abstract val normY: Float
    public abstract val widthDp: Int
    public abstract val heightDp: Int

    /**
     * A signature drawn by the user using vector paths.
     *
     * @property pathsString Serialized vector math and coordinates representing the user's pen
     *   strokes, used to perfectly redraw the signature at any scale.
     */
    public class Drawn(
        override val id: String,
        override val pageNum: Int,
        override val normX: Float,
        override val normY: Float,
        override val widthDp: Int,
        override val heightDp: Int,
        public val pathsString: String,
    ) : Signature() {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Drawn) return false
            return id == other.id &&
                pageNum == other.pageNum &&
                normX == other.normX &&
                normY == other.normY &&
                widthDp == other.widthDp &&
                heightDp == other.heightDp
        }

        override fun hashCode(): Int {
            return Objects.hash(id, pageNum, normX, normY, widthDp, heightDp)
        }

        override fun toString(): String {
            return "Drawn(id='$id', pageNum=$pageNum, normX=$normX, normY=$normY, widthDp=$widthDp, heightDp=$heightDp, pathsString='$pathsString')"
        }
    }

    /**
     * A signature generated from text typed by the user.
     *
     * @property typedText The exact text string entered by the user.
     * @property typedFont The specific font asset identifier or typeface name used to render the
     *   text.
     */
    public class Typed(
        override val id: String,
        override val pageNum: Int,
        override val normX: Float,
        override val normY: Float,
        override val widthDp: Int,
        override val heightDp: Int,
        public val typedText: String,
        public val typedFont: String,
    ) : Signature() {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Typed) return false
            return id == other.id &&
                pageNum == other.pageNum &&
                normX == other.normX &&
                normY == other.normY &&
                widthDp == other.widthDp &&
                heightDp == other.heightDp
        }

        override fun hashCode(): Int {
            return Objects.hash(id, pageNum, normX, normY, widthDp, heightDp)
        }

        override fun toString(): String {
            return "Typed(id='$id', pageNum=$pageNum, normX=$normX, normY=$normY, widthDp=$widthDp, heightDp=$heightDp, typedText='$typedText', typedFont='$typedFont')"
        }
    }

    /**
     * A signature imported from an image file on the user's device.
     *
     * @property imageUriString The `content://` URI address pointing to the source image file.
     */
    public class Uploaded(
        override val id: String,
        override val pageNum: Int,
        override val normX: Float,
        override val normY: Float,
        override val widthDp: Int,
        override val heightDp: Int,
        public val imageUriString: String,
    ) : Signature() {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Uploaded) return false
            return id == other.id &&
                pageNum == other.pageNum &&
                normX == other.normX &&
                normY == other.normY &&
                widthDp == other.widthDp &&
                heightDp == other.heightDp
        }

        override fun hashCode(): Int {
            return Objects.hash(id, pageNum, normX, normY, widthDp, heightDp)
        }

        override fun toString(): String {
            return "Uploaded(id='$id', pageNum=$pageNum, normX=$normX, normY=$normY, widthDp=$widthDp, heightDp=$heightDp, imageUriString='$imageUriString')"
        }
    }
}
