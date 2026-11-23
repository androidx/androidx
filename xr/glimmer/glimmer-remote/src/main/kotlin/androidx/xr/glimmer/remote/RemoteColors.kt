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

package androidx.xr.glimmer.remote

import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * A set of named color parameters for a [RemoteGlimmerTheme]. Colors are not configurable.
 *
 * @property primary The primary color is an accent color. It should typically be used with text and
 *   icons for emphasis, or with borders to accentuate a particular component - it should not be
 *   used to fill surfaces.
 * @property secondary The secondary color is an accent color used for brand expression. It should
 *   typically be used with text and icons for emphasis, or with borders to accentuate a particular
 *   component - it should not be used to fill surfaces.
 * @property positive The positive color is used to indicate positive or affirmative actions. For
 *   example, the border of a confirmation button. It should not be used to fill surfaces.
 * @property negative The negative color is used to indicate negative actions. For example, the
 *   border of a cancel button. It should not be used to fill surfaces.
 * @property background The background color that appears behind components. This is [Color.Black]
 *   to ensure maximum contrast.
 * @property surface The surface color that's applied to surfaces of components, such as buttons,
 *   cards, and list items.
 */
@Immutable
public class RemoteColors internal constructor() {
    public val primary: RemoteColor = Color(0xFF9BBFFF).rc
    public val secondary: RemoteColor = Color(0xFF4C88E9).rc
    public val positive: RemoteColor = Color(0xFF63FEA8).rc
    public val negative: RemoteColor = Color(0xFFFFA7A0).rc
    public val background: RemoteColor = Color.Black.rc
    public val surface: RemoteColor = Color(0xFF303030).rc
    // TODO: b/568385462 - Remove hardcoded content color, once customization is available.
    public val content: RemoteColor = Color.White.rc

    override fun toString(): String {
        return "RemoteColors(primary=$primary, secondary=$secondary, positive=$positive, negative=$negative, background=$background, surface=$surface, content=$content)"
    }
}
