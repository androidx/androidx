/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.core

/**
 * Represents a rule to apply to scale a source rectangle to be inscribed into a destination
 */
@Deprecated("This API is replaced with ContentScale which provides better descriptions" +
        "of the underlying scaling algorithms",
    ReplaceWith("ContentScale")
)
interface ScaleFit : ContentScale {

    /**
     * Companion object containing commonly used [ScaleFit] implementations
     */
    companion object {

        /**
         * Scale the source maintaining the aspect ratio so that the bounds match the maximum of
         * the destination width or height. This can cover a larger area than the destination.
         */
        @Deprecated("This API was replaced with the ContentScale equivalent",
            ReplaceWith("ContentScale.Crop"))
        val FillMaxDimension = ContentScale.Crop

        /**
         * Scale the source maintaining the aspect ratio so that the bounds match the minimum of
         * the destination width or height. This will always fill an area smaller than or equal to
         * the destination.
         */
        @Deprecated("This API was replaced with the ContentScale equivalent",
            ReplaceWith("ContentScale.Fit"))
        val FillMinDimension = ContentScale.Fit

        /**
         * Scale the source maintaining the aspect ratio so that the bounds match the destination
         * height. This can cover a larger area than the destination if the height is larger than
         * the width.
         */
        @Deprecated("This API was replaced with the ContentScale equivalent",
            ReplaceWith("ContentScale.FillHeight"))
        val FillHeight = ContentScale.FillHeight

        /**
         * Scale the source maintaining the aspect ratio so that the bounds match the
         * destination width. This can cover a larger area than the destination if the width is
         * larger than the height.
         */
        @Deprecated("This API was replaced with the ContentScale equivalent",
            ReplaceWith("ContentScale.FillWidth"))
        val FillWidth = ContentScale.FillWidth

        /**
         * Scale the source to maintain the aspect ratio to fit within the destination bounds
         * if the source is larger than the destination. If the source is smaller than or equal
         * to the destination in both dimensions, this behaves similarly to [None]. This will
         * always be contained within the bounds of the destination.
         */
        @Deprecated("This API was replaced with the ContentScale equivalent",
            ReplaceWith("ContentScale.Inside"))
        val Fit = ContentScale.Inside

        /**
         * Do not apply any scaling to the source
         */
        @Deprecated("This API was replaced with the ContentScle equivalent",
            ReplaceWith("ContentScale.None"))
        val None = ContentScale.None
    }
}
