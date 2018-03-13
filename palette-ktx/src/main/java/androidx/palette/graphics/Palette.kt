/*
 * Copyright 2018 The Android Open Source Project
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

@file:Suppress("NOTHING_TO_INLINE") // Aliases to public API.

package androidx.palette.graphics

import android.graphics.Bitmap

/**
 * Create a [Palette.Builder] from this bitmap.
 *
 * @see Palette.from
 */
inline fun Bitmap.buildPalette() = Palette.Builder(this)

/**
 * Returns the selected swatch for the given target from the palette, or `null` if one
 * could not be found.
 *
 * @see Palette.getSwatchForTarget
 */
inline operator fun Palette.get(target: Target): Palette.Swatch? = getSwatchForTarget(target)
