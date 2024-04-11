/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.contextmenu

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Provides the colors for the context menu.
 *
 * Use with [CompositionLocalProvider] to apply your custom colors to the context menu.
 */
val LocalContextMenuColors: ProvidableCompositionLocal<ContextMenuColors?> =
    compositionLocalOf { null }

/**
 * Colors to be provided to [LocalContextMenuColors] to apply the colors to the context menu.
 *
 * @param backgroundColor Color of the background in the context menu
 * @param textColor Color of the text in context menu items
 * @param iconColor Color of any icons in context menu items
 * @param disabledTextColor Color of disabled text in context menu items
 * @param disabledIconColor Color of any disabled icons in context menu items
 */
@Stable
class ContextMenuColors(
    val backgroundColor: Color,
    val textColor: Color,
    val iconColor: Color,
    val disabledTextColor: Color,
    val disabledIconColor: Color,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ContextMenuColors) return false

        if (this.backgroundColor != other.backgroundColor) return false
        if (this.textColor != other.textColor) return false
        if (this.iconColor != other.iconColor) return false
        if (this.disabledTextColor != other.disabledTextColor) return false
        if (this.disabledIconColor != other.disabledIconColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backgroundColor.hashCode()
        result = 31 * result + textColor.hashCode()
        result = 31 * result + iconColor.hashCode()
        result = 31 * result + disabledTextColor.hashCode()
        result = 31 * result + disabledIconColor.hashCode()
        return result
    }

    override fun toString(): String = "ContextMenuColors(" +
        "backgroundColor=$backgroundColor, " +
        "textColor=$textColor, " +
        "iconColor=$iconColor, " +
        "disabledTextColor=$disabledTextColor, " +
        "disabledIconColor=$disabledIconColor" +
        ")"
}
