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
import androidx.compose.remote.core.operations.layout.managers.CollapsiblePriority
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation.Type
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.modifier.CollapsiblePriorityModifier
import androidx.compose.remote.creation.compose.modifier.HeightModifier
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.toComposeUiLayout
import androidx.compose.remote.creation.compose.modifier.toRecordingModifier
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.v2.RemoteCollapsibleColumnV2
import androidx.compose.remote.creation.compose.v2.RemoteComposeApplierV2
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope

/** Utility modifier to record the layout information */
internal class RemoteComposeCollapsibleColumnModifier(
    public val modifier: RecordingModifier,
    public val horizontalAlignment: RemoteAlignment.Horizontal = RemoteAlignment.Start,
    public val verticalArrangement: RemoteArrangement.Vertical = RemoteArrangement.Top,
) : DrawModifier {
    override fun ContentDrawScope.draw() {
        drawIntoRemoteCanvas { canvas ->
            canvas.document.startCollapsibleColumn(
                modifier,
                horizontalAlignment.toRemote(this.layoutDirection),
                verticalArrangement.toRemote(),
            )
            this@draw.drawContent()
            canvas.document.endCollapsibleColumn()
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteCollapsibleColumnScope {
    public fun RemoteModifier.weight(weight: RemoteFloat): RemoteModifier =
        then(HeightModifier(Type.WEIGHT, weight))

    public fun RemoteModifier.weight(weight: Float): RemoteModifier =
        then(HeightModifier(Type.WEIGHT, RemoteFloat(weight)))

    public fun RemoteModifier.priority(priority: Float): RemoteModifier =
        then(CollapsiblePriorityModifier(CollapsiblePriority.VERTICAL, RemoteFloat(priority)))
}

/**
 * RemoteRow implements a row layout, delegating to the foundation Row layout as needed. This allows
 * RemoteRow to both work as a normal Row when called within a normal Compose tree, and capture the
 * layout information when called within a capture pass for RemoteCompose.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RemoteComposable
@Composable
public fun RemoteCollapsibleColumn(
    modifier: RemoteModifier = RemoteModifier,
    horizontalAlignment: RemoteAlignment.Horizontal = RemoteAlignment.Start,
    verticalArrangement: RemoteArrangement.Vertical = RemoteArrangement.Top,
    content: @Composable RemoteCollapsibleColumnScope.() -> Unit,
) {
    if (currentComposer.applier is RemoteComposeApplierV2) {
        RemoteCollapsibleColumnV2(modifier, horizontalAlignment, verticalArrangement, content)
        return
    }

    val creationState = LocalRemoteComposeCreationState.current
    val scope = remember { RemoteCollapsibleColumnScope() }

    val composeModifiers =
        RemoteComposeCollapsibleColumnModifier(
                creationState.toRecordingModifier(modifier),
                horizontalAlignment,
                verticalArrangement,
            )
            .then(modifier.toComposeUiLayout())
    @Suppress("COMPOSE_APPLIER_CALL_MISMATCH") // b/446706254
    androidx.compose.foundation.layout.Column(
        composeModifiers,
        horizontalAlignment = horizontalAlignment.toComposeUi(),
        verticalArrangement = verticalArrangement.toComposeUi(),
    ) {
        content(scope)
    }
}
