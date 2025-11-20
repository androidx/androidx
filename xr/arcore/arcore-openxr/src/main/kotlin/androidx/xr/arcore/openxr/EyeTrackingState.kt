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

internal class EyeTrackingState private constructor(private val value: Int) {
    public companion object {

        @JvmField public val NOT_TRACKING: EyeTrackingState = EyeTrackingState(0)

        @JvmField public val LEFT_ONLY: EyeTrackingState = EyeTrackingState(1)

        @JvmField public val RIGHT_ONLY: EyeTrackingState = EyeTrackingState(2)

        @JvmField public val BOTH: EyeTrackingState = EyeTrackingState(3)
    }

    @get:JvmName("hasLeft")
    public val hasLeft: Boolean
        get() = this == LEFT_ONLY || this == BOTH

    @get:JvmName("hasRight")
    public val hasRight: Boolean
        get() = this == RIGHT_ONLY || this == BOTH
}
