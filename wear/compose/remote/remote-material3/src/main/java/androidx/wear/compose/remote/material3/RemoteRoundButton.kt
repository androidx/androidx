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

package androidx.wear.compose.remote.material3

import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.role
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.Role

/** Single-slot base button for [RemoteTextButton] and [RemoteIconButton]. */
@Composable
@RemoteComposable
internal fun RemoteRoundButton(
    onClick: Action,
    modifier: RemoteModifier = RemoteModifier,
    backgroundColor: RemoteColor,
    enabled: RemoteBoolean,
    border: RemoteDp?,
    borderColor: RemoteColor?,
    shape: RemoteShape,
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteBox(
        contentAlignment = RemoteAlignment.Center,
        modifier =
            modifier
                .drawWithContent {
                    drawShapedBackground(
                        shape = shape,
                        color = backgroundColor,
                        enabled = enabled,
                        containerPainter = null,
                        disabledContainerPainter = null,
                        borderColor = borderColor,
                        borderStrokeWidth = border?.value,
                    )
                    drawContent()
                }
                .clickable(onClick, enabled = enabled.constantValueOrNull ?: false)
                .semantics(mergeDescendants = true) { role = Role.Button },
        content = content,
    )
}
