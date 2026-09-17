/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.player.compose.embedded

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.operations.ColorTheme
import androidx.compose.remote.creation.compose.state.AndroidSystemColorMap

/**
 * Resolves Android system color resources for [ColorTheme] operations.
 *
 * In remote-core documents, Android theme colors are stored as indexed tokens referencing framework
 * color resources. This resolver maps the 196 standard [android.R.color] resources into the
 * document's [ColorTheme] operations to match the View player's theme resolution.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object AndroidColorThemeResolver {
    fun mapColors(context: Context, document: CoreDocument) {
        val themedColors = document.themedColors ?: return
        mapColors(context, themedColors)
    }

    fun mapColors(context: Context, themedColors: List<ColorTheme>) {
        for (i in themedColors.indices) {
            val theme = themedColors[i]
            // Skip non-Android color groups (or null group names) to match View player's
            // mColorEngineMap lookup.
            if (theme.mColorGroupName != "android") {
                continue
            }
            val darkIndex = theme.mDarkModeIndex.toInt()
            if (AndroidSystemColorMap.isValidIndex(darkIndex)) {
                try {
                    theme.mDarkMode =
                        context.getColor(AndroidSystemColorMap.getResourceId(darkIndex))
                } catch (_: Exception) {
                    // Fall back to authored color
                }
            }
            val lightIndex = theme.mLightModeIndex.toInt()
            if (AndroidSystemColorMap.isValidIndex(lightIndex)) {
                try {
                    theme.mLightMode =
                        context.getColor(AndroidSystemColorMap.getResourceId(lightIndex))
                } catch (_: Exception) {
                    // Fall back to authored color
                }
            }
            theme.markDirty()
        }
    }
}
