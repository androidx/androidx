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

package androidx.xr.runtime.interfaces

import androidx.annotation.RestrictTo

/** A device capability that determines how virtual content is added to the real world. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class DisplayBlendMode private constructor(private val value: Int) {
    public companion object {
        /** Blending is not supported. */
        public val NO_DISPLAY: DisplayBlendMode = DisplayBlendMode(0)
        /**
         * Virtual content is added to the real world by adding the pixel values for each of Red,
         * Green, and Blue components. Alpha is ignored. Black pixels will appear transparent.
         */
        public val ADDITIVE: DisplayBlendMode = DisplayBlendMode(1)
        /**
         * Virtual content is added to the real world by alpha blending the pixel values based on
         * the Alpha component.
         */
        public val ALPHA_BLEND: DisplayBlendMode = DisplayBlendMode(2)
    }
}

/** A device capability representing how the device can track a user's hands. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class HandTrackingMode private constructor(public val mode: Int) {
    public companion object {
        /** Hands will not be tracked. */
        @JvmField public val DISABLED: HandTrackingMode = HandTrackingMode(0)
        /** Both the left and right hands will be tracked. */
        @JvmField public val BOTH: HandTrackingMode = HandTrackingMode(1)
    }
}

/** A device capability representing how the device can perform depth estimation. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class DepthEstimationMode private constructor(public val mode: Int) {
    public companion object {
        /** No information about scene depth will be provided. */
        @JvmField public val DISABLED: DepthEstimationMode = DepthEstimationMode(0)

        /** Depth estimation will be provided with raw depth and confidence. */
        @JvmField public val RAW_ONLY: DepthEstimationMode = DepthEstimationMode(1)

        /** Depth estimation will be provided with smooth depth and confidence. */
        @JvmField public val SMOOTH_ONLY: DepthEstimationMode = DepthEstimationMode(2)

        /** Depth estimation will be provided with both raw and smooth depth and confidence. */
        @JvmField public val SMOOTH_AND_RAW: DepthEstimationMode = DepthEstimationMode(3)
    }
}

/** A device capability representing the type of geospatial tracking supported by the device. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class GeospatialMode private constructor(public val mode: Int) {
    public companion object {
        /** Geospatial tracking will not be provided. */
        @JvmField public val DISABLED: GeospatialMode = GeospatialMode(0)

        /** The device will use a combination of VPS and GPS to provide geospatial tracking. */
        @JvmField public val VPS_AND_GPS: GeospatialMode = GeospatialMode(1)
    }
}

/** A device capability representing how the device can track the user's eyes. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class EyeTrackingMode private constructor(public val mode: Int) {
    public companion object {
        /** Eyes will not be tracked. */
        @JvmField public val DISABLED: EyeTrackingMode = EyeTrackingMode(0)
        /**
         * Device will perform coarse tracking, providing general gaze direction without high
         * precision.
         */
        @JvmField public val COARSE_TRACKING: EyeTrackingMode = EyeTrackingMode(1)
        /** Device will perform fine tracking, providing more precise gaze direction. */
        @JvmField public val FINE_TRACKING: EyeTrackingMode = EyeTrackingMode(2)
    }
}

/** A device capability that determines what type of rendering the device can provide. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class RenderingMode
private constructor(@get:RestrictTo(RestrictTo.Scope.LIBRARY) public val value: Int) {

    public companion object {
        /** The device will provide monocular rendering. */
        @JvmField public val MONO: RenderingMode = RenderingMode(0)
        /** The device will provide binocular (stereoscopic) rendering. */
        @JvmField public val STEREO: RenderingMode = RenderingMode(1)
    }
}
