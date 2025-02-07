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

package androidx.compose.material3.adaptive.layout

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.ConfigurationCompat
import java.util.Locale

@Composable
@ReadOnlyComposable
internal actual fun getString(string: Strings): String {
    // Do a no-op reading so when LocalConfiguration changes, this function will be recomposed.
    LocalConfiguration.current
    val resources = LocalContext.current.resources
    return resources.getString(string.value)
}

@Composable
@ReadOnlyComposable
internal actual fun getString(string: Strings, vararg formatArgs: Any): String {
    val raw = getString(string)
    val locale =
        ConfigurationCompat.getLocales(LocalConfiguration.current).get(0) ?: Locale.getDefault()
    return String.format(locale, raw, *formatArgs)
}

@JvmInline
@Immutable
internal actual value class Strings(@StringRes val value: Int) {
    actual companion object {
        actual inline val defaultPaneExpansionDragHandleContentDescription
            get() =
                Strings(R.string.m3_adaptive_default_pane_expansion_drag_handle_content_description)

        actual inline val defaultPaneExpansionDragHandleStateDescription
            get() =
                Strings(R.string.m3_adaptive_default_pane_expansion_drag_handle_state_description)

        actual inline val defaultPaneExpansionDragHandleActionDescription
            get() =
                Strings(R.string.m3_adaptive_default_pane_expansion_drag_handle_action_description)

        actual inline val defaultPaneExpansionProportionAnchorDescription
            get() =
                Strings(R.string.m3_adaptive_default_pane_expansion_proportion_anchor_description)

        actual inline val defaultPaneExpansionStartOffsetAnchorDescription
            get() =
                Strings(R.string.m3_adaptive_default_pane_expansion_start_offset_anchor_description)

        actual inline val defaultPaneExpansionEndOffsetAnchorDescription
            get() =
                Strings(R.string.m3_adaptive_default_pane_expansion_end_offset_anchor_description)
    }
}
