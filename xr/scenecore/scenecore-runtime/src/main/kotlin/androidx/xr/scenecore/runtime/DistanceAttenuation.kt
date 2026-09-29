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
package androidx.xr.scenecore.runtime

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

/**
 * Defines how a signal attenuates with distance from a source.
 *
 * @param distanceRolloffModel Determines the type of curve for the attenuation. Defaults to
 *   ROLLOFF_MODEL_AUTO. See androidx.xr.scenecore.DistanceAttenuation for the valid values.
 * @param minDistance Distance in meters where attenuation begins (gain is 1.0). Must be >= 0.1.
 *   Defaults to 1.0m.
 * @param maxDistance Distance in meters where attenuation reaches its maximum. Defaults to 500.0m.
 * @param gainAtMaxDistance Gain at `maxDistance`. Must be in range [0.0, 1.0]. Defaults to 0.0f.
 * @param rolloffFactor Custom curve shape: < 0 for inverse, > 0 for parabolic, 0 for linear.
 *   Defaults to 1.0f.
 * @see androidx.xr.scenecore.DistanceAttenuation for details
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class DistanceAttenuation(
    @DistanceRolloffModelValue public val distanceRolloffModel: Int = ROLLOFF_MODEL_AUTO,
    public val minDistance: Float = 1.0f,
    public val maxDistance: Float = 500.0f,
    public val gainAtMaxDistance: Float = 0.0f,
    public val rolloffFactor: Float = 1.0f,
) {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        ROLLOFF_MODEL_AUTO,
        ROLLOFF_MODEL_NONE,
        ROLLOFF_MODEL_NATURAL,
        ROLLOFF_MODEL_LINEAR,
        ROLLOFF_MODEL_CUSTOM,
    )
    public annotation class DistanceRolloffModelValue

    public companion object {
        /** The rolloff model is automatically selected. */
        public const val ROLLOFF_MODEL_AUTO: Int = 0
        /** No distance attenuation is applied. */
        public const val ROLLOFF_MODEL_NONE: Int = 1
        /** A natural-sounding (1/distance^2) rolloff curve. */
        public const val ROLLOFF_MODEL_NATURAL: Int = 2
        /** A linear gain rolloff between a min and max distance. */
        public const val ROLLOFF_MODEL_LINEAR: Int = 3
        /** A custom rolloff curve defined by the user. */
        public const val ROLLOFF_MODEL_CUSTOM: Int = 4
    }
}
