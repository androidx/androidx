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

import androidx.xr.runtime.math.Pose

internal data class EyeData(val state: EyeStatus, val pose: Pose) {}

internal fun EyeStatus.Companion.fromOpenXrEyeState(nativeValue: Int): EyeStatus =
    when (nativeValue) {
        0 -> EyeStatus.INVALID
        1 -> EyeStatus.GAZING
        2 -> EyeStatus.SHUT
        else -> throw IllegalStateException("Unknown eye state")
    }

internal class EyeStatus(private val value: Int) {
    internal companion object {
        /** Value indicating information about the eye is unavailable, or invalid. */
        @JvmField val INVALID: EyeStatus = EyeStatus(0)
        /** Value indicating the eye is open and looking at something. */
        @JvmField val GAZING: EyeStatus = EyeStatus(1)
        /** Value indicating the eye is closed and not looking at something. */
        @JvmField val SHUT: EyeStatus = EyeStatus(2)
    }
}
