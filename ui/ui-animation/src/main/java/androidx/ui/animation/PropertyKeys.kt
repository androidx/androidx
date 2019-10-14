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

package androidx.ui.animation

import androidx.animation.PropKey
import androidx.ui.core.Dp
import androidx.ui.core.Px
import androidx.ui.core.PxPosition
import androidx.ui.core.lerp
import androidx.ui.graphics.Color
import androidx.ui.graphics.lerp

/**
 * Built-in property key for [Px] properties.
 */
class PxPropKey : PropKey<Px> {
    override fun interpolate(a: Px, b: Px, fraction: Float): Px =
        lerp(a, b, fraction)
}

/**
 * Built-in property key for [Dp] properties.
 */
class DpPropKey : PropKey<Dp> {
    override fun interpolate(a: Dp, b: Dp, fraction: Float): Dp =
        lerp(a, b, fraction)
}

/**
 * Built-in property key for [PxPosition] properties.
 */
class PxPositionPropKey : PropKey<PxPosition> {
    override fun interpolate(a: PxPosition, b: PxPosition, fraction: Float): PxPosition =
        lerp(a, b, fraction)
}

/**
 * Built-in property key for [Color] properties.
 */
class ColorPropKey : PropKey<Color> {
    override fun interpolate(a: Color, b: Color, fraction: Float): Color {
        return lerp(a, b, fraction)
    }
}
