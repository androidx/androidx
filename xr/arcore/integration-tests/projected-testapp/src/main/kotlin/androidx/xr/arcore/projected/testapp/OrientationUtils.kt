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

package androidx.xr.arcore.projected.testapp

import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2

/** Returns a human-readable string describing the orientation of a [Quaternion]. */
internal fun getOrientationDescription(quaternion: Quaternion): String {
    val directionVector = quaternion * Vector3.Forward

    var tilt = asin(directionVector.y.coerceIn(-1f, 1f)) * (180f / PI.toFloat())
    if (tilt == -0f) tilt = 0f

    val verticalDirection =
        when {
            tilt > 15f -> "UP"
            tilt < -15f -> "DOWN"
            else -> "LEVEL"
        }

    var yaw = atan2(directionVector.x, -directionVector.z) * (180f / PI.toFloat())
    if (yaw < 0f) yaw += 360f
    if (yaw == -0f || yaw == 360f) yaw = 0f

    val compassDirection =
        when {
            yaw >= 337.5f || yaw < 22.5f -> "N"
            yaw >= 22.5f && yaw < 67.5f -> "NE"
            yaw >= 67.5f && yaw < 112.5f -> "E"
            yaw >= 112.5f && yaw < 157.5f -> "SE"
            yaw >= 157.5f && yaw < 202.5f -> "S"
            yaw >= 202.5f && yaw < 247.5f -> "SW"
            yaw >= 247.5f && yaw < 292.5f -> "W"
            else -> "NW"
        }

    return "Dir: $compassDirection (${"%.1f".format(yaw)}°) | Facing: $verticalDirection (${"%.1f".format(tilt)}°)"
}
