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
import androidx.compose.foundation.layout.Box
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.frontend.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.frontend.capture.NoRemoteCompose
import androidx.compose.remote.frontend.capture.RecordingCanvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.OnGloballyPositionedModifier
import androidx.compose.ui.unit.IntSize

public fun Modifier.remoteComponent(): Modifier = then(RemoteComposeComponentModifier())

/** Allows us to encapsulate a normal composable as an opaque component with a fixed size */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteComposeComponentModifier() : DrawModifier, OnGloballyPositionedModifier {
    public val modifier: RecordingModifier = RecordingModifier()
    public var origin: Offset = Offset(-1f, -1f)
    public var asize: IntSize = IntSize(0, 0)

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        this.asize = coordinates.size
        this.origin = coordinates.localToWindow(Offset(0f, 0f))
    }

    override fun ContentDrawScope.draw() {
        drawIntoCanvas {
            if (it.nativeCanvas is RecordingCanvas) {
                (it.nativeCanvas as RecordingCanvas).let {
                    it.document.startBox(modifier.size(asize.width, asize.height))
                    drawContent()
                    it.document.endBox()
                }
            } else {
                drawContent()
            }
        }
    }
}

public fun Modifier.remoteDocument(content: (RemoteComposeWriter) -> Unit): Modifier =
    then(RemoteComposeDocumentModifier(content))

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteComposeDocumentModifier(public val content: (RemoteComposeWriter) -> Unit) :
    DrawModifier {
    public val modifier: RecordingModifier = RecordingModifier()

    override fun ContentDrawScope.draw() {
        drawIntoCanvas {
            if (it.nativeCanvas is RecordingCanvas) {
                (it.nativeCanvas as RecordingCanvas).let { content(it.document) }
            } else {
                drawContent()
            }
        }
    }
}

@RemoteComposable
@Composable
public fun Document(content: (RemoteComposeWriter) -> Unit) {
    val captureMode = LocalRemoteComposeCreationState.current
    if (captureMode !is NoRemoteCompose) {
        Box(modifier = Modifier.remoteDocument(content))
    }
}
