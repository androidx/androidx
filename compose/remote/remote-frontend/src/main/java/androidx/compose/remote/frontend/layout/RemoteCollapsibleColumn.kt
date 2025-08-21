/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.frontend.layout

import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.remote.core.operations.layout.managers.CollapsiblePriority
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation.Type
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.frontend.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.frontend.capture.NoRemoteCompose
import androidx.compose.remote.frontend.capture.RecordingCanvas
import androidx.compose.remote.frontend.modifier.CollapsiblePriorityModifier
import androidx.compose.remote.frontend.modifier.HeightModifier
import androidx.compose.remote.frontend.modifier.RemoteModifier
import androidx.compose.remote.frontend.modifier.toComposeUi
import androidx.compose.remote.frontend.modifier.toComposeUiLayout
import androidx.compose.remote.frontend.state.RemoteFloat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas

/** Utility modifier to record the layout information */
class RemoteComposeCollapsibleColumnModifier(
    val modifier: RecordingModifier,
    val horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    val verticalArrangement: Arrangement.Vertical = Arrangement.Top,
) : DrawModifier {
    override fun ContentDrawScope.draw() {
        drawIntoCanvas {
            if (it.nativeCanvas is RecordingCanvas) {
                (it.nativeCanvas as RecordingCanvas).let {
                    it.document.startCollapsibleColumn(
                        modifier,
                        horizontalAlignment.toRemoteCompose(),
                        verticalArrangement.toRemoteCompose(),
                    )
                    drawContent()
                    it.document.endCollapsibleColumn()
                }
            } else {
                drawContent()
            }
        }
    }
}

class RemoteCollapsibleColumnScope {
    fun RemoteModifier.weight(weight: RemoteFloat): RemoteModifier =
        then(HeightModifier(Type.WEIGHT, weight))

    fun RemoteModifier.weight(weight: Float): RemoteModifier =
        then(HeightModifier(Type.WEIGHT, RemoteFloat(weight)))

    fun RemoteModifier.priority(priority: Float): RemoteModifier =
        then(CollapsiblePriorityModifier(CollapsiblePriority.VERTICAL, RemoteFloat(priority)))
}

/**
 * RemoteRow implements a row layout, delegating to the foundation Row layout as needed. This allows
 * RemoteRow to both work as a normal Row when called within a normal Compose tree, and capture the
 * layout information when called within a capture pass for RemoteCompose.
 */
@RemoteComposable
@Composable
fun RemoteCollapsibleColumn(
    modifier: RemoteModifier = RemoteModifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable RemoteCollapsibleColumnScope.() -> Unit,
) {
    val captureMode = LocalRemoteComposeCreationState.current
    val scope = remember { RemoteCollapsibleColumnScope() }

    if (captureMode is NoRemoteCompose) {
        androidx.compose.foundation.layout.Column(
            modifier.toComposeUi(),
            horizontalAlignment = horizontalAlignment.toComposeUi(),
            verticalArrangement = verticalArrangement.toComposeUi(),
        ) {
            content(scope)
        }
    } else {
        var composeModifiers =
            RemoteComposeCollapsibleColumnModifier(
                    modifier.toRemoteCompose(),
                    horizontalAlignment,
                    verticalArrangement,
                )
                .then(modifier.toComposeUiLayout())
        composeModifiers = composeModifiers.then(Modifier.wrapContentSize(unbounded = true))
        androidx.compose.foundation.layout.Column(
            composeModifiers,
            horizontalAlignment = horizontalAlignment.toComposeUi(),
            verticalArrangement = verticalArrangement.toComposeUi(),
        ) {
            content(scope)
        }
    }
}
