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
import androidx.xr.runtime.RequiresSpatialApi
import androidx.xr.runtime.SpatialApiVersions

/** Configures how sound attenuation changes over distance. */
@RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V4)
public sealed interface DistanceAttenuation {

    // Hidden private subclass prevents exhaustive `when` expressions in external client code,
    // allowing future additions of DistanceAttenuation subclasses without breaking binary/source
    // compatibility.
    private class Hidden : DistanceAttenuation

    /**
     * Configures a natural-sounding rolloff curve.
     *
     * @param minDistance Distance where attenuation begins (gain is 1.0). Must be >= 0.1.
     * @throws IllegalArgumentException if the minDistance is less than 0.1
     */
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V4)
    public class Natural(@get:FloatRange(from = 0.1) public val minDistance: Float) :
        DistanceAttenuation {
        init {
            require(minDistance.isFinite() && minDistance >= 0.1f) { "minDistance must be >= 0.1" }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Natural) return false
            return minDistance == other.minDistance
        }

        override fun hashCode(): Int = minDistance.hashCode()

        override fun toString(): String = "Natural(minDistance=$minDistance)"
    }

    /**
     * Configures linear rolloff between [minDistance] and [maxDistance].
     *
     * @param minDistance Distance where attenuation begins (gain is 1.0). Must be >= 0.1.
     * @param maxDistance Distance where attenuation reaches its maximum.
     * @param gainAtMaxDistance Gain at `maxDistance`. Must be in range [0.0, 1.0].
     * @throws IllegalArgumentException if minDistance is less than 0.1, maxDistance is less than
     *   minDistance or gainAtMaxDistance is not between 0.0 and 1.0
     */
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V4)
    public class Linear(
        @get:FloatRange(from = 0.1) public val minDistance: Float,
        @get:FloatRange(from = 0.1) public val maxDistance: Float,
        @get:FloatRange(from = 0.0, to = 1.0) public val gainAtMaxDistance: Float,
    ) : DistanceAttenuation {
        init {
            require(minDistance.isFinite() && minDistance >= 0.1f) { "minDistance must be >= 0.1" }
            require(maxDistance.isFinite() && maxDistance > minDistance) {
                "maxDistance must be greater than minDistance"
            }
            require(
                gainAtMaxDistance.isFinite() &&
                    gainAtMaxDistance >= 0.0f &&
                    gainAtMaxDistance <= 1.0f
            ) {
                "gainAtMaxDistance must be between 0.0 and 1.0"
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Linear) return false
            return minDistance == other.minDistance &&
                maxDistance == other.maxDistance &&
                gainAtMaxDistance == other.gainAtMaxDistance
        }

        override fun hashCode(): Int {
            var result = minDistance.hashCode()
            result = 31 * result + maxDistance.hashCode()
            result = 31 * result + gainAtMaxDistance.hashCode()
            return result
        }

        override fun toString(): String =
            "Linear(minDistance=$minDistance, " +
                "maxDistance=$maxDistance, " +
                "gainAtMaxDistance=$gainAtMaxDistance)"
    }

    /**
     * Configures a custom distance attenuation model
     *
     * @param minDistance Distance where attenuation begins (gain is 1.0). Must be >= 0.1.
     * @param maxDistance Distance where attenuation reaches its maximum. Must be > minDistance.
     * @param gainAtMaxDistance Gain at `maxDistance`. Must be in range [0.0, 1.0].
     * @param rolloffFactor Custom curve shape: < 0 for inverse, > 0 for parabolic, 0 for linear.
     * @throws IllegalArgumentException if minDistance is less than 0.1, maxDistance is less than
     *   minDistance or gainAtMaxDistance is not between 0.0 and 1.0
     */
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V4)
    public class Custom(
        @get:FloatRange(from = 0.1) public val minDistance: Float,
        @get:FloatRange(from = 0.1) public val maxDistance: Float,
        @get:FloatRange(from = 0.0, to = 1.0) public val gainAtMaxDistance: Float,
        public val rolloffFactor: Float,
    ) : DistanceAttenuation {
        init {
            require(minDistance.isFinite() && minDistance >= 0.1f) { "minDistance must be >= 0.1" }
            require(maxDistance.isFinite() && maxDistance > minDistance) {
                "maxDistance must be greater than minDistance"
            }
            require(
                gainAtMaxDistance.isFinite() &&
                    gainAtMaxDistance >= 0.0f &&
                    gainAtMaxDistance <= 1.0f
            ) {
                "gainAtMaxDistance must be between 0.0 and 1.0"
            }
            require(rolloffFactor.isFinite()) { "rolloffFactor must be finite" }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Custom) return false
            return minDistance == other.minDistance &&
                maxDistance == other.maxDistance &&
                gainAtMaxDistance == other.gainAtMaxDistance &&
                rolloffFactor == other.rolloffFactor
        }

        override fun hashCode(): Int {
            var result = minDistance.hashCode()
            result = 31 * result + maxDistance.hashCode()
            result = 31 * result + gainAtMaxDistance.hashCode()
            result = 31 * result + rolloffFactor.hashCode()
            return result
        }

        override fun toString(): String =
            "Custom(minDistance=$minDistance, " +
                "maxDistance=$maxDistance, " +
                "gainAtMaxDistance=$gainAtMaxDistance, " +
                "rolloffFactor=$rolloffFactor)"
    }

    /** Allows the system to choose the most appropriate attenuation curve. */
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V4)
    public object Auto : DistanceAttenuation {
        override fun toString(): String = "Auto"
    }

    /** Disables distance attenuation for a sound source. */
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V4)
    public object None : DistanceAttenuation {
        override fun toString(): String = "None"
    }
}
