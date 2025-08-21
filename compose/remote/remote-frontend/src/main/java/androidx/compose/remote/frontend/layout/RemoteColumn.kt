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
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation.Type
import androidx.compose.remote.frontend.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.frontend.capture.NoRemoteCompose
import androidx.compose.remote.frontend.capture.RecordingCanvas
import androidx.compose.remote.frontend.modifier.HeightModifier
import androidx.compose.remote.frontend.modifier.RemoteModifier
import androidx.compose.remote.frontend.modifier.ScrollModifier
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
class RemoteComposeColumnModifier(
    val modifier: RemoteModifier = RemoteModifier,
    val horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    val verticalArrangement: Arrangement.Vertical = Arrangement.Top,
) : DrawModifier {
    override fun ContentDrawScope.draw() {
        drawIntoCanvas {
            if (it.nativeCanvas is RecordingCanvas) {
                (it.nativeCanvas as RecordingCanvas).let {
                    it.document.startColumn(
                        modifier.toRemoteCompose(),
                        horizontalAlignment.toRemoteCompose(),
                        verticalArrangement.toRemoteCompose(),
                    )
                    drawContent()
                    it.document.endColumn()
                }
            } else {
                drawContent()
            }
        }
    }
}

class RemoteColumnScope {
    fun RemoteModifier.weight(weight: RemoteFloat): RemoteModifier =
        then(HeightModifier(Type.WEIGHT, weight))

    fun RemoteModifier.weight(weight: Float): RemoteModifier =
        then(HeightModifier(Type.WEIGHT, RemoteFloat(weight)))
}

/**
 * RemoteColumn implements a Column layout, delegating to the foundation Column layout as needed.
 * This allows RemoteColumn to both work as a normal Column when called within a normal Compose
 * tree, and capture the layout information when called within a capture pass for RemoteCompose.
 */
@RemoteComposable
@Composable
fun RemoteColumn(
    modifier: RemoteModifier = RemoteModifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable RemoteColumnScope.() -> Unit,
) {
    val scope = remember { RemoteColumnScope() }
    val captureMode = LocalRemoteComposeCreationState.current
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
            RemoteComposeColumnModifier(modifier, horizontalAlignment, verticalArrangement)
                .then(modifier.toComposeUiLayout())
        if (modifier.any { element -> element is ScrollModifier }) {
            composeModifiers = composeModifiers.then(Modifier.wrapContentSize(unbounded = true))
        }
        androidx.compose.foundation.layout.Column(
            composeModifiers,
            horizontalAlignment = horizontalAlignment.toComposeUi(),
            verticalArrangement = verticalArrangement.toComposeUi(),
        ) {
            content(scope)
        }
    }
}
