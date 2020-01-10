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
import androidx.ui.geometry.Offset
import androidx.ui.unit.Px
import androidx.ui.unit.lerp
import androidx.ui.unit.px

/**
 * A single shadow.
 */
@Immutable
data class Shadow(
    val color: Color = Color(0xFF000000),
    val offset: Offset = Offset.zero,
    val blurRadius: Px = 0.px
)

/**
 * Linearly interpolate two [Shadow]s.
 */
fun lerp(start: Shadow, stop: Shadow, fraction: Float): Shadow {
    return Shadow(
        lerp(start.color, stop.color, fraction),
        Offset.lerp(start.offset, stop.offset, fraction),
        lerp(start.blurRadius, stop.blurRadius, fraction)
    )
}