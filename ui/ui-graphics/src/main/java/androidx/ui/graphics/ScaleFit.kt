/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.graphics

import androidx.ui.unit.PxSize

private const val OriginalScale = 1.0f

/**
 * Represents a rule to apply to scale a source rectangle to be inscribed into a destination
 */
interface ScaleFit {

    /**
     * Computes the scale factor to apply to both dimensions in order to fit the source
     * appropriately with the given destination size
     */
    fun scale(srcSize: PxSize, dstSize: PxSize): Float

    /**
     * Companion object containing commonly used [ScaleFit] implementations
     */
    companion object {

        /**
         * Scale the source maintaining the aspect ratio so that the bounds match the maximum of
         * the destination width or height. This can cover a larger area than the destination.
         */
        val FillMaxDimension = object : ScaleFit {
            override fun scale(srcSize: PxSize, dstSize: PxSize): Float =
                computeFillMaxDimension(srcSize, dstSize)
        }

        /**
         * Scale the source maintaining the aspect ratio so that the bounds match the minimum of
         * the destination width or height. This will always fill an area smaller than or equal to
         * the destination.
         */
        val FillMinDimension = object : ScaleFit {
            override fun scale(srcSize: PxSize, dstSize: PxSize): Float =
                computeFillMinDimension(srcSize, dstSize)
        }

        /**
         * Scale the source maintaining the aspect ratio so that the bounds match the destination
         * height. This can cover a larger area than the destination if the height is larger than
         * the width.
         */
        val FillHeight = object : ScaleFit {
            override fun scale(srcSize: PxSize, dstSize: PxSize): Float =
                computeFillHeight(srcSize, dstSize)
        }

        /**
         * Scale the source maintaining the aspect ratio so that the bounds match the
         * destination width. This can cover a larger area than the destination if the width is
         * larger than the height.
         */
        val FillWidth = object : ScaleFit {
            override fun scale(srcSize: PxSize, dstSize: PxSize): Float =
                computeFillWidth(srcSize, dstSize)
        }

        /**
         * Scale the source to maintain the aspect ratio to fit within the destination bounds
         * if the source is larger than the destination. If the source is smaller than or equal
         * to the destination in both dimensions, this behaves similarly to [None]. This will
         * always be contained within the bounds of the destination.
         */
        val Fit = object : ScaleFit {
            override fun scale(srcSize: PxSize, dstSize: PxSize): Float =
                if (srcSize.width <= dstSize.width && srcSize.height <= dstSize.height) {
                    OriginalScale
                } else {
                    computeFillMinDimension(srcSize, dstSize)
                }
        }

        /**
         * Do not apply any scaling to the source
         */
        val None = FixedScale(OriginalScale)
    }
}

/**
 * [ScaleFit] implementation that always scales the dimension by the provided
 * fixed floating point value
 */
data class FixedScale(val value: Float) : ScaleFit {
    override fun scale(srcSize: PxSize, dstSize: PxSize): Float = value
}

private fun computeFillMaxDimension(srcSize: PxSize, dstSize: PxSize): Float =
    if (dstSize.width > dstSize.height) {
        computeFillWidth(srcSize, dstSize)
    } else {
        computeFillHeight(srcSize, dstSize)
    }

private fun computeFillMinDimension(srcSize: PxSize, dstSize: PxSize): Float =
    if (dstSize.width < dstSize.height) {
        computeFillWidth(srcSize, dstSize)
    } else {
        computeFillHeight(srcSize, dstSize)
    }

private fun computeFillWidth(srcSize: PxSize, dstSize: PxSize): Float =
    dstSize.width.value / srcSize.width.value

private fun computeFillHeight(srcSize: PxSize, dstSize: PxSize): Float =
    dstSize.height.value / srcSize.height.value
