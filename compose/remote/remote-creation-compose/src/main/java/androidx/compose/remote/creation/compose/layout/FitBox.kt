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
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.toComposeUiLayout
import androidx.compose.remote.creation.compose.modifier.toRecordingModifier
import androidx.compose.remote.creation.compose.v2.FitBoxV2
import androidx.compose.remote.creation.compose.v2.RemoteComposeApplierV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope

/** Utility modifier to record the layout information */
internal class RemoteComposeFitBoxModifier(
    private val modifier: RemoteModifier,
    private val horizontalAlignment: RemoteAlignment.Horizontal = RemoteAlignment.Start,
    private val verticalArrangement: RemoteArrangement.Vertical = RemoteArrangement.Top,
) : DrawModifier {
    override fun ContentDrawScope.draw() {
        drawIntoRemoteCanvas { canvas ->
            canvas.document.startFitBox(
                canvas.toRecordingModifier(modifier),
                horizontalAlignment.toRemote(),
                verticalArrangement.toRemote(),
            )
            this@draw.drawContent()
            canvas.document.endFitBox()
        }
    }
}

/**
 * FitBox implements a Box layout, delegating to the foundation Box layout as needed. This allows
 * FitBox to both work as a normal Box when called within a normal Compose tree, and capture the
 * layout information when called within a capture pass for RemoteCompose.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RemoteComposable
@Composable
public fun FitBox(
    modifier: RemoteModifier = RemoteModifier,
    horizontalAlignment: RemoteAlignment.Horizontal = RemoteAlignment.CenterHorizontally,
    verticalArrangement: RemoteArrangement.Vertical = RemoteArrangement.Center,
    content: @RemoteComposable @Composable () -> Unit,
) {
    if (currentComposer.applier is RemoteComposeApplierV2) {
        FitBoxV2(modifier, horizontalAlignment, verticalArrangement, content)
        return
    }
    @Suppress("COMPOSE_APPLIER_CALL_MISMATCH") // b/446706254
    androidx.compose.foundation.layout.Box(
        RemoteComposeFitBoxModifier(modifier, horizontalAlignment, verticalArrangement)
            .then(modifier.toComposeUiLayout())
    ) {
        content()
    }
}
