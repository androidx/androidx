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

package androidx.pdf.ink.view.tool

import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo

/** Represents the configuration and state of a selected annotation tool. */
@RestrictTo(RestrictTo.Scope.LIBRARY) public abstract class AnnotationToolInfo

/**
 * Represents the Pen tool with its specific brush size and color.
 *
 * @property brushSize The stroke width of the pen in pixels.
 * @property color The integer value of the selected color.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class Pen(public val brushSize: Float, public val color: Int) : AnnotationToolInfo() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Pen) return false

        if (brushSize != other.brushSize) return false
        if (color != other.color) return false

        return true
    }

    override fun hashCode(): Int {
        var result = brushSize.hashCode()
        result = 31 * result + color
        return result
    }

    override fun toString(): String {
        return String.format("Pen(brushSize = %f, color = %d)", brushSize, color)
    }
}

/**
 * Represents the Highlighter tool with its brush size and selected color or emoji.
 *
 * A highlighter can be configured with either a translucent color or an emoji drawable.
 *
 * @property brushSize The stroke width of the highlighter in pixels.
 * @property color The integer value of the selected translucent color, or null if an emoji is
 *   selected.
 * @property emoji The resource ID of the selected emoji drawable, or null if a color is selected.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class Highlighter(
    public val brushSize: Float,
    public val color: Int?,
    @param:DrawableRes public val emoji: Int?,
) : AnnotationToolInfo() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Highlighter) return false

        if (brushSize != other.brushSize) return false
        if (color != other.color) return false
        if (emoji != other.emoji) return false

        return true
    }

    override fun hashCode(): Int {
        var result = brushSize.hashCode()
        result = 31 * result + (color ?: 0)
        result = 31 * result + (emoji ?: 0)
        return result
    }

    override fun toString(): String {
        return String.format(
            "Highlighter(brushSize = %f, color = %d, emoji = %d)",
            brushSize,
            color,
            emoji,
        )
    }
}

/** Represents the Eraser tool, which currently has no configurable properties. */
@RestrictTo(RestrictTo.Scope.LIBRARY) public object Eraser : AnnotationToolInfo()
