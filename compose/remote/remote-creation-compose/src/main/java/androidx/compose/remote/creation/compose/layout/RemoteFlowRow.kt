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
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.toRecordingModifier
import androidx.compose.remote.creation.compose.v2.RemoteComposeApplierV2
import androidx.compose.remote.creation.compose.v2.RemoteFlowRowV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope

internal class RemoteComposeFlowRowModifier(
    private val modifier: RemoteModifier,
    private val horizontalArrangement: RemoteArrangement.Horizontal = RemoteArrangement.Start,
    private val verticalArrangement: RemoteArrangement.Vertical = RemoteArrangement.Top,
) : DrawModifier {
    override fun ContentDrawScope.draw() {
        drawIntoRemoteCanvas { canvas ->
            canvas.document.startFlow(
                canvas.toRecordingModifier(modifier),
                horizontalArrangement.toRemote(),
                verticalArrangement.toRemote(),
            )
            this@draw.drawContent()
            canvas.document.endFlow()
        }
    }
}

/**
 * A layout composable that places its children in a horizontal flow.
 *
 * `RemoteFlow` allows you to wrap multiple children and position them using [horizontalArrangement]
 * and [verticalArrangement].
 *
 * @param modifier The modifier to be applied to this flow.
 * @param horizontalArrangement The horizontal arrangement of the children.
 * @param verticalArrangement The vertical arrangement of the children.
 * @param content The content of the flow.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RemoteComposable
@Composable
public fun RemoteFlowRow(
    modifier: RemoteModifier = RemoteModifier,
    horizontalArrangement: RemoteArrangement.Horizontal = RemoteArrangement.Start,
    verticalArrangement: RemoteArrangement.Vertical = RemoteArrangement.Top,
    content: @Composable () -> Unit,
) {
    require(currentComposer.applier is RemoteComposeApplierV2) {
        "This component is only supported with RemoteComposeApplierV2."
    }
    RemoteFlowRowV2(modifier, horizontalArrangement, verticalArrangement, content)
}
