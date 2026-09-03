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

package androidx.xr.compose.subspace.animation.follow

import androidx.xr.compose.spatial.ExperimentalFollowingSubspaceApi

/**
 * Defines a movement threshold range for [FollowMode] within which follow motion is not triggered.
 *
 * When the following entity is within these thresholds relative to the target pose, it remains
 * stationary. Once the target moves beyond a threshold along any dimension, follow motion begins.
 *
 * Setting these values low (but above zero) helps filter out micro-movements while the user is
 * relatively stationary. Setting the values high allows the content to remain anchored until the
 * user makes a significant movement, such as relocating to another area.
 *
 * @param translationMeters The translation threshold, in meters, required to trigger follow motion.
 * @param pitchDegrees The pitch rotation threshold, in degrees, required to trigger follow motion.
 * @param yawDegrees The yaw rotation threshold, in degrees, required to trigger follow motion.
 * @param rollDegrees The roll rotation threshold, in degrees, required to trigger follow motion.
 */
@ExperimentalFollowingSubspaceApi
// TODO(b/556372003): Add float range to FollowThresholds
public class FollowThresholds(
    public val translationMeters: Float = 0.0f,
    public val pitchDegrees: Float = 0.0f,
    public val yawDegrees: Float = 0.0f,
    public val rollDegrees: Float = 0.0f,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FollowThresholds) return false

        if (translationMeters != other.translationMeters) return false
        if (pitchDegrees != other.pitchDegrees) return false
        if (yawDegrees != other.yawDegrees) return false
        if (rollDegrees != other.rollDegrees) return false

        return true
    }

    override fun hashCode(): Int {
        var result = translationMeters.hashCode()
        result = 31 * result + pitchDegrees.hashCode()
        result = 31 * result + yawDegrees.hashCode()
        result = 31 * result + rollDegrees.hashCode()
        return result
    }

    override fun toString(): String {
        return "FollowThresholds(translationMeters=$translationMeters, " +
            "pitchDegrees=$pitchDegrees, yawDegrees=$yawDegrees, rollDegrees=$rollDegrees)"
    }

    public companion object {
        public val Zero: FollowThresholds = FollowThresholds()
    }
}
