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

package androidx.wear.compose.remote.material3.internal

import androidx.annotation.ColorRes
import androidx.compose.remote.creation.compose.capture.currentProfileSupportsThemedColors
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.createNamedRemoteColor
import androidx.compose.remote.creation.compose.state.createThemedRemoteColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * A color role in a remote design system's color scheme, such as primary or onSurface.
 *
 * This doesn't depend on any particular design system, so it can back the color schemes of other
 * remote design systems.
 *
 * @property name The named color used when the color is not themed, e.g. `"WearM3.primary"`. A host
 *   can override named colors.
 * @property lightToken The Android system color resource used in light mode, e.g.
 *   `android.R.color.system_primary_light`.
 * @property darkToken The Android system color resource used in dark mode, e.g.
 *   `android.R.color.system_primary_dark`. Design systems that are always dark can use the same
 *   token for both modes.
 */
internal class RemoteColorRole(
    val name: String,
    @ColorRes val lightToken: Int,
    @ColorRes val darkToken: Int = lightToken,
)

/** How the [RemoteColor] of each [RemoteColorRole] in a color scheme is resolved. */
internal enum class RemoteColorSource {
    /** Named colors, which resolve to their fallback unless the host overrides them. */
    Named,

    /** Themed colors, which follow the player's Android system (dynamic) colors. */
    Themed;

    /** Returns the [RemoteColor] for [role], using [fallback] when it can't be resolved. */
    fun resolve(role: RemoteColorRole, fallback: Color): RemoteColor =
        when (this) {
            Named -> createNamedRemoteColor(role.name, fallback)
            Themed ->
                RemoteColor.createThemedRemoteColor(
                    lightColorRes = role.lightToken,
                    darkColorRes = role.darkToken,
                    lightFallback = fallback,
                    darkFallback = fallback,
                )
        }
}

/**
 * Returns the [RemoteColorSource] for a dynamic color scheme: [RemoteColorSource.Themed] if the
 * current creation profile supports themed colors, otherwise [RemoteColorSource.Named].
 *
 * Support is checked once for the whole scheme, so a scheme never mixes themed and named colors.
 */
@Composable
@RemoteComposable
internal fun dynamicRemoteColorSource(): RemoteColorSource =
    if (currentProfileSupportsThemedColors()) RemoteColorSource.Themed else RemoteColorSource.Named
