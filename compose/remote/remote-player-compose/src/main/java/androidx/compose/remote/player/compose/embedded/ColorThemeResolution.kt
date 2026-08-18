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

import androidx.compose.remote.core.operations.Theme

/**
 * Resolves an authored or overridden theme integer to a concrete [Theme.LIGHT] or [Theme.DARK]
 * mode.
 *
 * In remote-core, [androidx.compose.remote.core.operations.ColorTheme.apply] treats anything other
 * than [Theme.LIGHT] as dark mode. Resolving [Theme.SYSTEM] and [Theme.UNSPECIFIED] against the
 * host's [isSystemInDarkTheme] ensures documents render in light mode when the host is in light
 * mode.
 */
internal fun resolveThemeMode(theme: Int, isSystemInDarkTheme: Boolean): Int {
    return when (theme) {
        Theme.LIGHT -> Theme.LIGHT
        Theme.DARK -> Theme.DARK
        Theme.SYSTEM,
        Theme.UNSPECIFIED -> if (isSystemInDarkTheme) Theme.DARK else Theme.LIGHT
        else -> theme
    }
}
