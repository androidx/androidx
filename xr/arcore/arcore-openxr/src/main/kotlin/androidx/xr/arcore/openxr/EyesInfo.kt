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

package androidx.xr.arcore.openxr

internal data class EyesInfo(val eyes: Array<EyeData>, val trackingState: EyeTrackingState) {

    init {
        check(eyes.size == 2) { "eyes array contains incorrect number of elements: ${eyes.size}" }
    }

    override fun equals(other: Any?): Boolean {
        val otherEyeInfo: EyesInfo? = other as? EyesInfo
        if (otherEyeInfo == null) return false
        if (trackingState != otherEyeInfo.trackingState) return false
        if (eyes.size != otherEyeInfo.eyes.size) return false
        if (eyes[0] != otherEyeInfo.eyes[0]) return false
        if (eyes[1] != otherEyeInfo.eyes[1]) return false
        return true
    }

    override fun hashCode(): Int {
        var hash = 1
        hash += trackingState.hashCode() * 17
        hash += eyes[0].hashCode() * 17
        hash += eyes[1].hashCode() * 17
        return hash
    }
}

internal fun EyeTrackingState.Companion.fromOpenXrEyeTrackingMode(
    nativeValue: Int
): EyeTrackingState =
    when (nativeValue) {
        0 -> EyeTrackingState.NOT_TRACKING
        1 -> EyeTrackingState.LEFT_ONLY
        2 -> EyeTrackingState.RIGHT_ONLY
        3 -> EyeTrackingState.BOTH
        else -> throw IllegalStateException("Unknown eye tracking mode")
    }
