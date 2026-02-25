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

package androidx.compose.material3

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.tokens.ScrimTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex

/**
 * A Scrim that obscures content behind a modal surface.
 *
 * @param contentDescription text used by accessibility services to describe what this Scrim can
 *   dismiss. This should always be provided unless the scrim is used for decorative purposes and
 *   [onClick] is null.
 * @param modifier Optional [Modifier] for the scrim.
 * @param onClick Optional callback invoked when the user clicks on the scrim, if set to null, no
 *   click semantics are provided.
 * @param alpha Opacity to be applied to the [color] from 0.0f to 1.0f representing fully
 *   transparent to fully opaque respectively. This is always coerced between 0.0f and 1.0f.
 * @param color The color of the scrim. This color must not be [Color.Unspecified] for this scrim to
 *   be drawn.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Scrim(
    contentDescription: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = {},
    alpha: () -> Float = { 1f },
    color: Color = ScrimDefaults.color,
) {
    if (color.isSpecified) {
        val dismissEnabled = onClick != null
        val dismissModifier =
            if (dismissEnabled) {
                Modifier.pointerInput(onClick) { detectTapGestures { onClick() } }
                    .semantics(mergeDescendants = true) {
                        traversalIndex = 1f
                        if (contentDescription != null) {
                            this.contentDescription = contentDescription
                        }
                        onClick {
                            onClick()
                            true
                        }
                    }
            } else {
                Modifier
            }
        Canvas(modifier = modifier.fillMaxSize().then(dismissModifier)) {
            drawRect(color = color, alpha = alpha().coerceIn(0f, 1f))
        }
    }
}

/** Object containing default values for the [Scrim] component. */
object ScrimDefaults {
    /** The default color and opacity for [Scrim]. */
    val color: Color
        @Composable
        @ReadOnlyComposable
        get() = ScrimTokens.ContainerColor.value.copy(alpha = ScrimTokens.ContainerOpacity)
}
