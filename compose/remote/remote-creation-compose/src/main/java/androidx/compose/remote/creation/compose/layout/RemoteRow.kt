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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.layout

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation.Type
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.WidthModifier
import androidx.compose.remote.creation.compose.modifier.toComposeUiLayout
import androidx.compose.remote.creation.compose.modifier.toRecordingModifier
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.v2.RemoteComposeApplierV2
import androidx.compose.remote.creation.compose.v2.RemoteRowV2
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope

/** Utility modifier to record the layout information */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteComposeRowModifier(
    public val modifier: RecordingModifier,
    public val horizontalArrangement: RemoteArrangement.Horizontal = RemoteArrangement.Start,
    public val verticalAlignment: RemoteAlignment.Vertical = RemoteAlignment.Top,
) : DrawModifier {
    override fun ContentDrawScope.draw() {
        drawIntoRemoteCanvas { canvas ->
            canvas.document.startRow(
                modifier,
                horizontalArrangement.toRemote(),
                verticalAlignment.toRemote(),
            )
            this@draw.drawContent()
            canvas.document.endRow()
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
    horizontalArrangement: RemoteArrangement.Horizontal = RemoteArrangement.Start,
    verticalAlignment: RemoteAlignment.Vertical = RemoteAlignment.Top,
    content: @Composable RemoteRowScope.() -> Unit,
) {
    if (currentComposer.applier is RemoteComposeApplierV2) {
        RemoteRowV2(modifier, horizontalArrangement, verticalAlignment) {
            // Bridge V1 scope to V2 scope
            val v1Scope = remember { RemoteRowScope() }
            v1Scope.content()
        }
        return
    }

    val creationState = LocalRemoteComposeCreationState.current
    val scope = remember { RemoteRowScope() }
    val composeModifiers =
        RemoteComposeRowModifier(
                creationState.toRecordingModifier(modifier),
                horizontalArrangement,
                verticalAlignment,
            )
            .then(modifier.toComposeUiLayout())

    @Suppress("COMPOSE_APPLIER_CALL_MISMATCH") // b/446706254
    androidx.compose.foundation.layout.Row(
        composeModifiers,
        horizontalArrangement = horizontalArrangement.toComposeUi(),
        verticalAlignment = verticalAlignment.toComposeUi(),
    ) {
        content(scope)
    }
}
