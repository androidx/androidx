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

package androidx.pdf.annotation.content

import androidx.annotation.RestrictTo
import androidx.pdf.annotation.content.PathPdfObject.PathInput.Companion.LINE_TO
import androidx.pdf.annotation.content.PathPdfObject.PathInput.Companion.MOVE_TO
import androidx.pdf.constants.PathOp

/**
 * Represents a path PDF object with a PDF document.
 *
 * @property brushColor The color of the path.
 * @property brushWidth The width of the path's stroke.
 * @property inputs The list of coordinates and commands that define the path.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PathPdfObject(
    public val brushColor: Int,
    public val brushWidth: Float,
    public val inputs: List<PathInput>,
) : PdfObject {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PathPdfObject) return false
        return brushColor == other.brushColor &&
            brushWidth == other.brushWidth &&
            inputs == other.inputs
    }

    override fun hashCode(): Int {
        var result = brushColor
        result = 31 * result + brushWidth.hashCode()
        result = 31 * result + inputs.hashCode()
        return result
    }

    /**
     * Data model for a single coordinate in a [PathPdfObject].
     *
     * @property x The x-coordinate of the point.
     * @property y The y-coordinate of the point.
     * @property command The type of path operation (e.g., [MOVE_TO] or [LINE_TO]).
     */
    public class PathInput(
        public val x: Float,
        public val y: Float,
        @PathOp public val command: Int,
    ) {
        public companion object {
            /** Starts a new sub-path from the given coordinate. */
            public const val MOVE_TO: Int = androidx.pdf.constants.PathOps.MOVE_TO

            /** Draws a line from the previous point to the given coordinate. */
            public const val LINE_TO: Int = androidx.pdf.constants.PathOps.LINE_TO
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PathInput) return false
            return x == other.x && y == other.y && command == other.command
        }

        override fun hashCode(): Int {
            var result = x.hashCode()
            result = 31 * result + y.hashCode()
            result = 31 * result + command
            return result
        }
    }
}
