/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.brush

import androidx.annotation.RestrictTo
import androidx.ink.brush.color.Color as ComposeColor
import androidx.ink.brush.color.colorspace.ColorSpace as ComposeColorSpace
import androidx.ink.brush.color.colorspace.ColorSpaces as ComposeColorSpaces

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun ComposeColor.toColorInInkSupportedColorSpace(): ComposeColor {
    return if (this.colorSpace.isSupportedInInk()) {
        this
    } else {
        this.convert(ComposeColorSpaces.DisplayP3)
    }
}

internal fun ComposeColorSpace.toInkColorSpaceId() =
    when (this) {
        ComposeColorSpaces.Srgb -> 0
        ComposeColorSpaces.DisplayP3 -> 1
        else -> throw IllegalArgumentException("Unsupported Compose color space")
    }

internal fun ComposeColorSpace.isSupportedInInk() =
    (this == ComposeColorSpaces.Srgb || this == ComposeColorSpaces.DisplayP3)
