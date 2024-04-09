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

package androidx.compose.material

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.contextmenu.ContextMenuColors
import androidx.compose.material.ContentAlpha.contentAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

/**
 * Remembers a [ContextMenuColors] based on [colors].
 *
 * The background color will be [Colors.surface] and all the other colors will be
 * [Colors.onSurface] with alpha applied. This matches [DropdownMenuContent] and
 * [DropdownMenuItemContent]. The alpha values match What is seen in [ContentAlpha].
 */
@Composable
internal fun rememberContextMenuColors(colors: Colors): ContextMenuColors {
    val backgroundColor = colors.surface
    val contentColor = colors.onSurface
    val isLight = colors.isLight
    return remember(backgroundColor, contentColor, isLight) {
        val textAlpha = Alpha.Content.enabled(contentColor, isLight)
        val iconAlpha = Alpha.Icon.enabled(isLight)
        val disabledTextAlpha = Alpha.Content.disabled(contentColor, isLight)
        val disabledIconAlpha = Alpha.Icon.disabled(isLight)
        ContextMenuColors(
            backgroundColor = backgroundColor,
            textColor = contentColor.copy(alpha = textAlpha),
            iconColor = contentColor.copy(alpha = iconAlpha),
            disabledTextColor = contentColor.copy(alpha = disabledTextAlpha),
            disabledIconColor = contentColor.copy(alpha = disabledIconAlpha),
        )
    }
}

@VisibleForTesting
internal object Alpha {
    object Content {
        fun enabled(contentColor: Color, isLight: Boolean): Float =
            contentAlpha(contentColor, isLight, HighContrast.ENABLED, LowContrast.ENABLED)

        fun disabled(contentColor: Color, isLight: Boolean): Float =
            contentAlpha(contentColor, isLight, HighContrast.DISABLED, LowContrast.DISABLED)

        /** Values taken from [HighContrastContentAlpha]. */
        object HighContrast {
            const val ENABLED: Float = 1f
            const val DISABLED: Float = 0.38f
        }

        /** Values taken from [LowContrastContentAlpha]. */
        object LowContrast {
            const val ENABLED: Float = 0.87f
            const val DISABLED: Float = 0.38f
        }
    }

    /**
     * Values taken from
     * [material icons](https://m2.material.io/design/iconography/system-icons.html#color).
     */
    object Icon {
        fun enabled(isLight: Boolean): Float = if (isLight) Light.ENABLED else Dark.ENABLED
        fun disabled(isLight: Boolean): Float = if (isLight) Light.DISABLED else Dark.DISABLED

        object Light {
            const val ENABLED: Float = 0.54f
            const val DISABLED: Float = 0.38f
        }

        object Dark {
            const val ENABLED: Float = 0.7f
            const val DISABLED: Float = 0.5f
        }
    }
}
