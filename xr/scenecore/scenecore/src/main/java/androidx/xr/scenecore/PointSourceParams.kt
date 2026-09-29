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

package androidx.xr.scenecore

import androidx.annotation.FloatRange
import androidx.xr.runtime.RequiresSpatialApi
import androidx.xr.runtime.SpatialApiVersions
import androidx.xr.scenecore.runtime.PointSourceParams as RtPointSourceParams

/**
 * Configures a sound source to be spatialized at a 3D location.
 *
 * For more information, see
 * [Add positional audio to your app][https://developer.android.com/develop/xr/jetpack-xr-sdk/add-spatial-audio#add-positional].
 */
public class PointSourceParams
internal constructor(
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V4)
    public val distanceAttenuation: DistanceAttenuation,
    // Directivity properties
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V4)
    @get:FloatRange(from = 0.0, to = 1.0)
    public val directivityBalance: Float,
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V4)
    @get:FloatRange(from = 1.0)
    public val directivitySharpness: Float,
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V4)
    @get:FloatRange(from = 0.0, to = 360.0)
    public val spread: Float,
) {
    init {
        require(
            directivityBalance.isFinite() &&
                directivityBalance >= 0.0f &&
                directivityBalance <= 1.0f
        ) {
            "directivityBalance must be between 0.0 and 1.0"
        }
        require(directivitySharpness.isFinite() && directivitySharpness >= 1.0f) {
            "directivitySharpness must be >= 1.0"
        }
        require(spread.isFinite() && spread in 0.0f..360.0f) {
            "spread must be between 0.0 and 360.0"
        }
    }

    internal constructor(
        rtPointSourceParams: RtPointSourceParams
    ) : this(
        rtPointSourceParams.distanceAttenuation.toDistanceAttenuation(),
        rtPointSourceParams.directivityBalance,
        rtPointSourceParams.directivitySharpness,
        rtPointSourceParams.spread,
    )

    public constructor() : this(RtPointSourceParams())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PointSourceParams) return false
        return distanceAttenuation == other.distanceAttenuation &&
            directivityBalance == other.directivityBalance &&
            directivitySharpness == other.directivitySharpness &&
            spread == other.spread
    }

    override fun hashCode(): Int {
        var result = distanceAttenuation.hashCode()
        result = 31 * result + directivityBalance.hashCode()
        result = 31 * result + directivitySharpness.hashCode()
        result = 31 * result + spread.hashCode()
        return result
    }

    override fun toString(): String {
        return "PointSourceParams(" +
            "distanceAttenuation=$distanceAttenuation, " +
            "directivityBalance=$directivityBalance, " +
            "directivitySharpness=$directivitySharpness, " +
            "spread=$spread)"
    }

    internal val rtPointSourceParams =
        RtPointSourceParams(
            distanceAttenuationToRtDistanceAttenuation(distanceAttenuation),
            directivityBalance,
            directivitySharpness,
            spread,
        )

    /** Builder for creating customized [PointSourceParams] instances. */
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V4)
    public class Builder() {
        private var distanceAttenuation: DistanceAttenuation = DistanceAttenuation.Auto
        private var directivityBalance: Float = 0.0f
        private var directivitySharpness: Float = 1.0f
        private var spread: Float = 0.0f

        /**
         * Creates a new [Builder] initialized with the values from an existing [PointSourceParams]
         * object.
         */
        public constructor(params: PointSourceParams) : this() {
            this.distanceAttenuation = params.distanceAttenuation
            this.directivityBalance = params.directivityBalance
            this.directivitySharpness = params.directivitySharpness
            this.spread = params.spread
        }

        /**
         * Sets all parameters related to distance-based attenuation.
         *
         * @param distanceAttenuation [DistanceAttenuation] distance attenuation configuration
         */
        public fun setDistanceAttenuation(distanceAttenuation: DistanceAttenuation): Builder =
            apply {
                this.distanceAttenuation = distanceAttenuation
            }

        /**
         * Sets all parameters related to the emission directivity of the sound source.
         *
         * @param balance The balance between an omnidirectional (0.0f) and a fully directional
         *   (1.0f) sound.
         * @param sharpness Controls the sharpness of the directional sound cone. Higher values
         *   result in a more focused sound beam. Must be >= 1.0.
         * @throws IllegalArgumentException if balance is not between 0.0 and 1.0 or sharpness is
         *   less than 1.0
         */
        @Suppress("MissingGetterMatchingBuilder")
        public fun setDirectivity(
            @FloatRange(from = 0.0, to = 1.0) balance: Float,
            @FloatRange(from = 1.0) sharpness: Float,
        ): Builder = apply {
            require(balance in 0.0f..1.0f) { "balance must be between 0.0 and 1.0" }
            require(sharpness >= 1.0f) { "sharpness must be greater or equal to 1.0" }
            this.directivityBalance = balance
            this.directivitySharpness = sharpness
        }

        /**
         * Sets the spread of the point source, in degrees.
         *
         * @param spread The spread angle value in degrees between 0.0 and 360.0
         * @throws IllegalArgumentException if spread angle is less than 0 or greater than 360
         */
        public fun setSpread(@FloatRange(from = 0.0, to = 360.0) spread: Float): Builder = apply {
            require(spread in 0.0f..360.0f) { "spread must be between 0.0 and 360.0" }
            this.spread = spread
        }

        /** Builds a new [PointSourceParams] object. */
        public fun build(): PointSourceParams {
            return PointSourceParams(
                distanceAttenuation = this.distanceAttenuation,
                directivityBalance = this.directivityBalance,
                directivitySharpness = this.directivitySharpness,
                spread = this.spread,
            )
        }
    }
}
