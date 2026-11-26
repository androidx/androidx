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

package androidx.compose.remote.player.compose.embedded.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.remote.core.operations.layout.managers.TextInputLayout
import androidx.compose.remote.player.compose.embedded.components.RcPlayerChildren
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * The innermost [TextInputLayout] being rendered, so that the text component bound to its string
 * renders as a text field (see `RcPlayerText`). Null outside of a text input layout.
 */
internal val LocalTextInputLayout = compositionLocalOf<TextInputLayout?> { null }

/**
 * Renders a text input layout as a box; the embedded player is a real Compose tree, so the text
 * component bound to the edited string becomes a `BasicTextField` in place and no host overlay is
 * needed. The rest of the content (label, icon, decorations) renders as usual.
 */
@Composable
internal fun RcPlayerTextInputLayout(layout: TextInputLayout, modifier: Modifier) {
    CompositionLocalProvider(LocalTextInputLayout provides layout) {
        Box(modifier = modifier, contentAlignment = Alignment.TopStart) { RcPlayerChildren(layout) }
    }
}
