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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.frontend.layout

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation.Type
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.frontend.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.frontend.capture.NoRemoteCompose
import androidx.compose.remote.frontend.capture.RecordingCanvas
import androidx.compose.remote.frontend.modifier.RemoteModifier
import androidx.compose.remote.frontend.modifier.ScrollModifier
import androidx.compose.remote.frontend.modifier.WidthModifier
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteComposeRowModifier(
    public val modifier: RecordingModifier,
    public val horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    public val verticalAlignment: Alignment.Vertical = Alignment.Top,
) : DrawModifier {
    override fun ContentDrawScope.draw() {
        drawIntoCanvas {
            if (it.nativeCanvas is RecordingCanvas) {
                (it.nativeCanvas as RecordingCanvas).let {
                    it.document.startRow(
                        modifier,
                        horizontalArrangement.toRemoteCompose(),
                        verticalAlignment.toRemoteCompose(),
                    )
                    drawContent()
                    it.document.endRow()
                }
            }
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteRowScope {
    public fun RemoteModifier.weight(weight: RemoteFloat): RemoteModifier =
        then(WidthModifier(Type.WEIGHT, weight))

    public fun RemoteModifier.weight(weight: Float): RemoteModifier =
        then(WidthModifier(Type.WEIGHT, RemoteFloat(weight)))
}

/**
 * RemoteRow implements a row layout, delegating to the foundation Row layout as needed. This allows
 * RemoteRow to both work as a normal Row when called within a normal Compose tree, and capture the
 * layout information when called within a capture pass for RemoteCompose.
 */
@RemoteComposable
@Composable
public fun RemoteRow(
    modifier: RemoteModifier = RemoteModifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable RemoteRowScope.() -> Unit,
) {
    val captureMode = LocalRemoteComposeCreationState.current
    val scope = remember { RemoteRowScope() }

    if (captureMode is NoRemoteCompose) {
        androidx.compose.foundation.layout.Row(
            modifier.toComposeUi(),
            horizontalArrangement = horizontalArrangement.toComposeUi(),
            verticalAlignment = verticalAlignment.toComposeUi(),
        ) {
            content(scope)
        }
    } else {
        var composeModifiers =
            RemoteComposeRowModifier(
                    modifier.toRemoteCompose(),
                    horizontalArrangement,
                    verticalAlignment,
                )
                .then(modifier.toComposeUiLayout())

        if (modifier.any { element -> element is ScrollModifier }) {
            composeModifiers = composeModifiers.then(Modifier.wrapContentSize(unbounded = true))
        }

        androidx.compose.foundation.layout.Row(
            composeModifiers,
            horizontalArrangement = horizontalArrangement.toComposeUi(),
            verticalAlignment = verticalAlignment.toComposeUi(),
        ) {
            content(scope)
        }
    }
}
