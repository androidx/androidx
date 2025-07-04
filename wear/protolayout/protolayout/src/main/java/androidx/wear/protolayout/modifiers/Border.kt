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

package androidx.wear.protolayout.modifiers

import androidx.annotation.Dimension
import androidx.annotation.Dimension.Companion.DP
import androidx.wear.protolayout.ModifiersBuilders.Border
import androidx.wear.protolayout.types.LayoutColor
import androidx.wear.protolayout.types.dp

/**
 * Adds a modifier to apply a border around an element.
 *
 * @param width The width of the border, in `DP`.
 * @param color The color of the border.
 */
fun LayoutModifier.border(@Dimension(DP) width: Float, color: LayoutColor): LayoutModifier =
    this then BaseBorderElement(width, color)

internal class BaseBorderElement(@Dimension(DP) val width: Float, val color: LayoutColor) :
    BaseProtoLayoutModifiersElement<Border.Builder> {
    override fun mergeTo(initialBuilder: Border.Builder?): Border.Builder =
        (initialBuilder ?: Border.Builder()).setWidth(width.dp).setColor(color.prop)
}
