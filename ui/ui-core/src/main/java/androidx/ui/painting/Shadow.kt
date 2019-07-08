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

package androidx.ui.painting

import androidx.ui.core.Px
import androidx.ui.core.lerp
import androidx.ui.core.px
import androidx.ui.engine.geometry.Offset
import androidx.ui.graphics.Color
import androidx.ui.graphics.lerp

/**
 * A single shadow.
 */
data class Shadow(
    val color: Color = Color(0xFF000000.toInt()),
    val offset: Offset = Offset.zero,
    val blurRadius: Px = 0.px
)

/**
 * Linearly interpolate two [Shadow]s.
 */
fun lerp(a: Shadow, b: Shadow, t: Float): Shadow {
    return Shadow(
        lerp(a.color, b.color, t),
        Offset.lerp(a.offset, b.offset, t)!!,
        lerp(a.blurRadius, b.blurRadius, t)
    )
}