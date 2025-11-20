/*
 * Copyright 2024 The Android Open Source Project
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
package androidx.camera.viewfinder.core

import android.util.LayoutDirection
import android.util.SizeF
import androidx.annotation.RestrictTo
import androidx.camera.viewfinder.core.impl.Alignment
import androidx.camera.viewfinder.core.impl.ContentScale
import androidx.camera.viewfinder.core.impl.OffsetF
import androidx.camera.viewfinder.core.impl.ScaleFactorF
import kotlin.math.max
import kotlin.math.min

/** Options for scaling the input frames vis-à-vis its container viewfinder. */
enum class ScaleType(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) val id: Int,
    internal val contentScale: ContentScale,
    internal val alignment: Alignment,
) {
    /**
     * Scale the input frames, maintaining the source aspect ratio, so it fills the entire
     * viewfinder, and align it to the start of the viewfinder, which is the top left corner in a
     * left-to-right (LTR) layout, or the top right corner in a right-to-left (RTL) layout.
     *
     * This may cause the input frames to be cropped if the input frame aspect ratio does not match
     * that of its container viewfinder.
     */
    FILL_START(id = 0, contentScale = Fill, alignment = Start),

    /**
     * Scale the input frames, maintaining the source aspect ratio, so it fills the entire
     * viewfinder, and center it in the viewfinder.
     *
     * This may cause the input frames to be cropped if the input frame aspect ratio does not match
     * that of its container viewfinder.
     */
    FILL_CENTER(id = 1, contentScale = Fill, alignment = Center),

    /**
     * Scale the input frames, maintaining the source aspect ratio, so it fills the entire
     * viewfinder, and align it to the end of the viewfinder, which is the bottom right corner in a
     * left-to-right (LTR) layout, or the bottom left corner in a right-to-left (RTL) layout.
     *
     * This may cause the input frames to be cropped if the input frame aspect ratio does not match
     * that of its container viewfinder.
     */
    FILL_END(id = 2, contentScale = Fill, alignment = End),

    /**
     * Scale the input frames, maintaining the source aspect ratio, so it is entirely contained
     * within the viewfinder, and align it to the start of the viewfinder, which is the top left
     * corner in a left-to-right (LTR) layout, or the top right corner in a right-to-left (RTL)
     * layout. The background area not covered by the input frames will be black or the background
     * color of the viewfinder.
     *
     * Both dimensions of the input frames will be equal or less than the corresponding dimensions
     * of its container viewfinder.
     */
    FIT_START(id = 3, contentScale = Fit, alignment = Start),

    /**
     * Scale the input frames, maintaining the source aspect ratio, so it is entirely contained
     * within the viewfinder, and center it inside the viewfinder. The background area not covered
     * by the input frames will be black or the background color of the viewfinder.
     *
     * Both dimensions of the input frames will be equal or less than the corresponding dimensions
     * of its container viewfinder.
     */
    FIT_CENTER(id = 4, contentScale = Fit, alignment = Center),

    /**
     * Scale the input frames, maintaining the source aspect ratio, so it is entirely contained
     * within the viewfinder, and align it to the end of the viewfinder, which is the bottom right
     * corner in a left-to-right (LTR) layout, or the bottom left corner in a right-to-left (RTL)
     * layout. The background area not covered by the input frames will be black or the background
     * color of the viewfinder.
     *
     * Both dimensions of the input frames will be equal or less than the corresponding dimensions
     * of its container viewfinder.
     */
    FIT_END(id = 5, contentScale = Fit, alignment = End);

    companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        fun fromId(id: Int): ScaleType {
            for (scaleType in values()) {
                if (scaleType.id == id) {
                    return scaleType
                }
            }
            throw IllegalArgumentException("Unknown scale type id $id")
        }
    }
}

/**
 * Represents a transformation that fits the content within the destination, maintaining the
 * original source aspect ratio.
 */
private val Fit =
    object : ContentScale {
        override fun computeScaleFactor(srcSize: SizeF, dstSize: SizeF): ScaleFactorF =
            computeFillMinDimension(srcSize, dstSize).let { ScaleFactorF(it, it) }
    }

/**
 * Represents a transformation that fills the destination with the content, maintaining the original
 * source aspect ratio and possibly cropping the content.
 */
private val Fill =
    object : ContentScale {
        override fun computeScaleFactor(srcSize: SizeF, dstSize: SizeF): ScaleFactorF =
            computeFillMaxDimension(srcSize, dstSize).let { ScaleFactorF(it, it) }
    }

private fun computeFillMaxDimension(srcSize: SizeF, dstSize: SizeF): Float {
    val widthScale = computeFillWidth(srcSize, dstSize)
    val heightScale = computeFillHeight(srcSize, dstSize)
    return max(widthScale, heightScale)
}

private fun computeFillMinDimension(srcSize: SizeF, dstSize: SizeF): Float {
    val widthScale = computeFillWidth(srcSize, dstSize)
    val heightScale = computeFillHeight(srcSize, dstSize)
    return min(widthScale, heightScale)
}

private fun computeFillWidth(srcSize: SizeF, dstSize: SizeF): Float = dstSize.width / srcSize.width

private fun computeFillHeight(srcSize: SizeF, dstSize: SizeF): Float =
    dstSize.height / srcSize.height

/** Alignment with equivalent placement to [android.graphics.Matrix.ScaleToFit.START] */
private val Start: Alignment = BiasAlignment(-1f, -1f)
/** Alignment with equivalent placement to [android.graphics.Matrix.ScaleToFit.CENTER] */
private val Center: Alignment = BiasAlignment(0f, 0f)
/** Alignment with equivalent placement to [android.graphics.Matrix.ScaleToFit.END] */
private val End: Alignment = BiasAlignment(1f, 1f)

/**
 * Represents an alignment with a horizontal and vertical bias.
 *
 * Borrowed from Compose's `Alignment` implementation since this module does not depend on Compose.
 */
private data class BiasAlignment(val horizontalBias: Float, val verticalBias: Float) : Alignment {
    override fun align(size: SizeF, space: SizeF, layoutDirection: Int): OffsetF {
        val centerX = (space.width - size.width) / 2f
        val centerY = (space.height - size.height) / 2f
        val resolvedHorizontalBias =
            if (layoutDirection == LayoutDirection.LTR) {
                horizontalBias
            } else {
                -1 * horizontalBias
            }

        val x = centerX * (1 + resolvedHorizontalBias)
        val y = centerY * (1 + verticalBias)
        return OffsetF(x, y)
    }
}
