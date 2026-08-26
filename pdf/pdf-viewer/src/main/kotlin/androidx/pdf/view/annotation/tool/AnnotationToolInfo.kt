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

package androidx.pdf.view.annotation.tool

import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.pdf.ExperimentalPdfApi

/**
 * Represents the configuration and state of a selected annotation tool.
 *
 * Subclasses encapsulate tool-specific drawing properties such as brush stroke width, color.
 */
@ExperimentalPdfApi public sealed interface AnnotationToolInfo

/**
 * Represents the Pen tool with its specific brush size and color.
 *
 * @property brushSize The stroke width of the pen in pixels. This value will always be positive.
 * @property color The color integer value of the selected pen stroke.
 */
@ExperimentalPdfApi
public class Pen
internal constructor(
    public val brushSize: Float,
    @get:ColorInt @param:ColorInt public val color: Int,
) : AnnotationToolInfo {

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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /** Creates a [Pen] instance for testing purposes. */
        @VisibleForTesting
        public fun createForTest(brushSize: Float, @ColorInt color: Int): Pen =
            Pen(brushSize, color)
    }
}

/**
 * Represents the Highlighter tool with its brush size and selected color.
 *
 * @property brushSize The stroke width of the highlighter in pixels. This value will always be
 *   positive.
 * @property color The color integer value of the selected translucent highlight.
 */
@ExperimentalPdfApi
public class Highlighter
internal constructor(
    public val brushSize: Float,
    @get:ColorInt @param:ColorInt public val color: Int,
) : AnnotationToolInfo {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Highlighter) return false

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
        return String.format("Highlighter(brushSize = %f, color = %d)", brushSize, color)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /** Creates a [Highlighter] instance for testing purposes. */
        @VisibleForTesting
        public fun createForTest(brushSize: Float, @ColorInt color: Int): Highlighter =
            Highlighter(brushSize, color)
    }
}

/** Represents the Eraser tool. */
@ExperimentalPdfApi public object Eraser : AnnotationToolInfo
