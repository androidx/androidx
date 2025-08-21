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

import androidx.compose.remote.frontend.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.frontend.capture.NoRemoteCompose
import androidx.compose.remote.frontend.capture.RecordingCanvas
import androidx.compose.remote.frontend.modifier.BackgroundModifier
import androidx.compose.remote.frontend.modifier.RemoteModifier
import androidx.compose.remote.frontend.modifier.fillMaxSize
import androidx.compose.remote.frontend.modifier.toComposeUi
import androidx.compose.remote.frontend.modifier.toComposeUiLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas

/** Utility modifier to record the layout information */
class RemoteComposeFitBoxModifier(
    private val modifier: RemoteModifier,
    private val horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    private val verticalArrangement: Arrangement.Vertical = Arrangement.Top,
) : DrawModifier {
    override fun ContentDrawScope.draw() {
        drawIntoCanvas {
            if (it.nativeCanvas is RecordingCanvas) {
                (it.nativeCanvas as RecordingCanvas).let {
                    it.document.startFitBox(
                        modifier.toRemoteCompose(),
                        horizontalAlignment.toRemoteCompose(),
                        verticalArrangement.toRemoteCompose(),
                    )
                    drawContent()
                    it.document.endFitBox()
                }
            }
        }
    }
}

/**
 * FitBox implements a Box layout, delegating to the foundation Box layout as needed. This allows
 * FitBox to both work as a normal Box when called within a normal Compose tree, and capture the
 * layout information when called within a capture pass for RemoteCompose.
 */
@RemoteComposable
@Composable
fun FitBox(
    modifier: RemoteModifier = RemoteModifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalArrangement: Arrangement.Vertical = Arrangement.Center,
    content: @Composable () -> Unit,
) {
    val captureMode = LocalRemoteComposeCreationState.current
    if (captureMode is NoRemoteCompose) {
        androidx.compose.foundation.layout.Box(
            modifier.toComposeUi(),
            contentAlignment = boxAlignment(horizontalAlignment, verticalArrangement),
        ) {
            content()
        }
    } else {
        val background = modifier.find<BackgroundModifier>()
        androidx.compose.foundation.layout.Box(
            RemoteComposeFitBoxModifier(modifier, horizontalAlignment, verticalArrangement)
                .then(modifier.toComposeUiLayout())
        ) {
            if (background?.brush?.hasShader == true) {
                RemoteCanvas(RemoteModifier.fillMaxSize()) { drawRect(background.brush) }
            }

            content()
        }
    }
}
