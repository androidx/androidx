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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RecordingCanvas
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.toComposeUiLayout
import androidx.compose.remote.creation.compose.modifier.toRecordingModifier
import androidx.compose.remote.creation.compose.v2.RemoteCanvasV2
import androidx.compose.remote.creation.compose.v2.RemoteComposeApplierV2
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.nativeCanvas

/**
 * A Composable that provides a [RemoteDrawScope] for drawing operations in RemoteCompose.
 *
 * @param modifier The [RemoteModifier] to apply to this layout.
 * @param content The drawing commands to be executed on the remote canvas via [RemoteDrawScope].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RemoteComposable
@Composable
public fun RemoteCanvas(
    modifier: RemoteModifier = RemoteModifier,
    content: RemoteDrawScope.() -> Unit,
) {
    if (currentComposer.applier is RemoteComposeApplierV2) {
        RemoteCanvasV2(modifier, content)
        return
    }
    val creationState = LocalRemoteComposeCreationState.current
    @Suppress("COMPOSE_APPLIER_CALL_MISMATCH") // b/446706254
    Spacer(
        modifier =
            RemoteComposeCanvasModifier(creationState.toRecordingModifier(modifier))
                .drawBehind {
                    RemoteDrawScope(
                            remoteCanvas =
                                RemoteCanvas(
                                    this.drawContext.canvas.nativeCanvas as RecordingCanvas
                                )
                        )
                        .content()
                }
                .then(modifier.toComposeUiLayout())
    )
}

/** Utility modifier to record the layout information */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteComposeCanvasModifier(public val modifier: RecordingModifier) : DrawModifier {
    override fun ContentDrawScope.draw() {
        drawIntoRemoteCanvas { canvas ->
            canvas.document.startCanvas(modifier)
            this@draw.drawContent()
            canvas.document.endCanvas()
        }
    }
}

/** Provides access to draw directly with the underlying [RecordingCanvas]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public inline fun androidx.compose.ui.graphics.drawscope.DrawScope.drawIntoRemoteCanvas(
    block: (RecordingCanvas) -> Unit
) {
    (drawContext.canvas.nativeCanvas as? RecordingCanvas)?.let(block)
}
