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

package androidx.xr.runtime.internal

import androidx.annotation.RestrictTo

// TODO(b/418014995): Move this and PixelDimensions as top-level Runtime classes
/**
 * Represents the outcome of an attempt to calculate the perceived resolution of an entity. This
 * sealed class encapsulates the different states: success with the calculated dimensions, or
 * specific failure reasons.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public sealed class PerceivedResolutionResult {

    /**
     * Indicates that the perceived resolution was successfully calculated.
     *
     * @property perceivedResolution The calculated pixel dimensions (width and height) that the
     *   entity is perceived to occupy on the display.
     */
    public class Success(public val perceivedResolution: PixelDimensions) :
        PerceivedResolutionResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Success) return false
            if (perceivedResolution != other.perceivedResolution) return false

            return true
        }

        override fun hashCode(): Int {
            return perceivedResolution.hashCode()
        }

        override fun toString(): String {
            return "PerceivedResolutionResult.Success(PerceivedResolution(${perceivedResolution.width}x${perceivedResolution.height}))"
        }
    }

    /**
     * Indicates that the perceived resolution could not be calculated because the entity is too
     * close to the camera. In such cases, the perceived size would be excessively large or
     * undefined. Consider falling back to the maximum resolution possible for this display.
     */
    public class EntityTooClose : PerceivedResolutionResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is EntityTooClose) return false

            return true
        }

        override fun hashCode(): Int {
            // All instances of this class should have the same hash code.
            return javaClass.hashCode()
        }
    }

    /**
     * Indicates that the perceived resolution could not be calculated because the required camera
     * view information was invalid or insufficient for the calculation. This could be due to the
     * spatial user's camera view not being initialized yet. Consider falling back to a predefined
     * resolution.
     */
    public class InvalidCameraView : PerceivedResolutionResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is InvalidCameraView) return false

            return true
        }

        override fun hashCode(): Int {
            // All instances of this class should have the same hash code.
            return javaClass.hashCode()
        }
    }
}
