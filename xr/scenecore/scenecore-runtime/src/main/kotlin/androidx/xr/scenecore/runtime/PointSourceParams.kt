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

package androidx.xr.scenecore.runtime

import androidx.annotation.RestrictTo

/**
 * Represents a XR Runtime PointSourceParams
 *
 * @param distanceAttenuation [DistanceAttenuation] distance attenuation configuration
 * @param directivityBalance The balance between an omnidirectional (0.0f) and a fully directional
 *   (1.0f) sound.
 * @param directivitySharpness Controls the sharpness of the directional sound cone. Higher values
 *   result in a more focused sound beam. Must be >= 1.0.
 * @param spread The spread angle value in degrees between 0.0 and 360.0
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class PointSourceParams(
    public val distanceAttenuation: DistanceAttenuation = DistanceAttenuation(),
    public val directivityBalance: Float = 0.0f,
    public val directivitySharpness: Float = 1.0f,
    public val spread: Float = 0.0f,
)
