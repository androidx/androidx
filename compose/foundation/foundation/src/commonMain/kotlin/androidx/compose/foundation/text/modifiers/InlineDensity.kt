/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.text.modifiers

import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2

/**
 * [Density] is an interface, not a final class. When you want to have a snapshot of Density values
 * that may change over time but you do not want the inherit mutability, use this inline class to
 * cache the Density values.
 */
@JvmInline
internal value class InlineDensity private constructor(private val packedValue: Long) {

    constructor(density: Float, fontScale: Float) : this(packFloats(density, fontScale))

    constructor(density: Density) : this(density.density, density.fontScale)

    val density: Float
        get() = unpackFloat1(packedValue)

    val fontScale: Float
        get() = unpackFloat2(packedValue)

    override fun toString(): String {
        return "InlineDensity(density=$density, fontScale=$fontScale)"
    }

    companion object {
        val Unspecified = InlineDensity(Float.NaN, Float.NaN)
    }
}
