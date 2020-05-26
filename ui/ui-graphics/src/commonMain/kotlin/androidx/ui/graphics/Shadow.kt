/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.graphics

import androidx.compose.Immutable
import androidx.compose.Stable
import androidx.ui.geometry.Offset
import androidx.ui.util.lerp

/**
 * A single shadow.
 */
@Immutable
data class Shadow(
    @Stable
    val color: Color = Color(0xFF000000),
    @Stable
    val offset: Offset = Offset.zero,
    @Stable
    val blurRadius: Float = 0.0f
) {
    companion object {
        /**
         * Constant for no shadow.
         */
        @Stable
        val None = Shadow()
    }
}

/**
 * Linearly interpolate two [Shadow]s.
 */
@Stable
fun lerp(start: Shadow, stop: Shadow, fraction: Float): Shadow {
    return Shadow(
        lerp(start.color, stop.color, fraction),
        Offset.lerp(start.offset, stop.offset, fraction),
        lerp(start.blurRadius, stop.blurRadius, fraction)
    )
}