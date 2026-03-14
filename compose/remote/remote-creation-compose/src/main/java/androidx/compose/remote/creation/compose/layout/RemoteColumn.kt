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
package androidx.compose.remote.creation.compose.layout

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation.Type
import androidx.compose.remote.creation.compose.modifier.HeightModifier
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.toComposeUiLayout
import androidx.compose.remote.creation.compose.modifier.toRecordingModifier
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.v2.RemoteColumnV2
import androidx.compose.remote.creation.compose.v2.RemoteComposeApplierV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope

/** Utility modifier to record the layout information */
internal class RemoteComposeColumnModifier(
    public val modifier: RemoteModifier = RemoteModifier,
    public val horizontalAlignment: RemoteAlignment.Horizontal = RemoteAlignment.Start,
    public val verticalArrangement: RemoteArrangement.Vertical = RemoteArrangement.Top,
) : DrawModifier {
    override fun ContentDrawScope.draw() {
        drawIntoRemoteCanvas { canvas ->
            canvas.document.startColumn(
                canvas.toRecordingModifier(modifier),
                horizontalAlignment.toRemote(this.layoutDirection),
                verticalArrangement.toRemote(),
            )
            this@draw.drawContent()
            canvas.document.endColumn()
        }
    }
}

/** Receiver scope used by [RemoteColumn] for its content. */
public class RemoteColumnScope {
    /**
     * Sets the vertical weight of this element relative to its siblings in the [RemoteColumn].
     *
     * @param weight The proportional height to allocate to this element.
     */
    public fun RemoteModifier.weight(weight: RemoteFloat): RemoteModifier =
        then(HeightModifier(Type.WEIGHT, weight))

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun RemoteModifier.weight(weight: Float): RemoteModifier =
        then(HeightModifier(Type.WEIGHT, RemoteFloat(weight)))
}

/**
 * A layout composable that positions its children in a vertical sequence.
 *
 * `RemoteColumn` allows you to arrange children vertically and control their [verticalArrangement]
 * (spacing) and [horizontalAlignment].
 *
 * @param modifier The modifier to be applied to this column.
 * @param verticalArrangement The vertical arrangement of the children.
 * @param horizontalAlignment The horizontal alignment of the children.
 * @param content The content of the column, which has access to [RemoteColumnScope].
 */
@RemoteComposable
@Composable
public fun RemoteColumn(
    modifier: RemoteModifier = RemoteModifier,
    verticalArrangement: RemoteArrangement.Vertical = RemoteArrangement.Top,
    horizontalAlignment: RemoteAlignment.Horizontal = RemoteAlignment.Start,
    content: @Composable RemoteColumnScope.() -> Unit,
) {
    if (currentComposer.applier is RemoteComposeApplierV2) {
        RemoteColumnV2(modifier, verticalArrangement, horizontalAlignment) {
            // Bridge V1 scope to V2 scope
            val v1Scope = remember { RemoteColumnScope() }
            v1Scope.content()
        }
        return
    }

    val scope = remember { RemoteColumnScope() }

    val composeModifiers =
        RemoteComposeColumnModifier(modifier, horizontalAlignment, verticalArrangement)
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
