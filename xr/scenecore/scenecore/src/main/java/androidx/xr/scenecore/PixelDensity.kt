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

package androidx.xr.scenecore

import androidx.annotation.FloatRange

/**
 * Represents a pixel density, analogous to the pixel density of a physical display like a computer
 * monitor or TV. It defines the mapping between pixels and physical distance in meters.
 *
 * Use this class to perform consistent unit conversions between the pixel-based coordinate systems
 * and physical spatial dimensions.
 */
public class PixelDensity
internal constructor(
    /**
     * The number of pixels that correspond to one physical meter. Must be a positive and finite
     * value.
     */
    @FloatRange(from = 0.0, fromInclusive = false) public val pixelsPerMeter: Float
) {
    init {
        require(pixelsPerMeter > 0f && pixelsPerMeter.isFinite()) {
            "pixelsPerMeter must be positive and finite."
        }
    }

    /** Converts a physical distance in meters to a pixel count using this density. */
    public fun convertMetersToPixels(meters: Float): Float = meters * pixelsPerMeter

    /** Converts a pixel count to a physical distance in meters using this density. */
    public fun convertPixelsToMeters(pixels: Float): Float = pixels / pixelsPerMeter
}
