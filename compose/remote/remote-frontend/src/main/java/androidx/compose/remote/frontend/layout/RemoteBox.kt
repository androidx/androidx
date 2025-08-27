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
import androidx.annotation.RestrictTo.Scope
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

/** Utility modifier to record the layout information */
class RemoteComposeBoxModifier(
    private val modifier: RemoteModifier,
    private val horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    private val verticalArrangement: Arrangement.Vertical = Arrangement.Top,
) : DrawModifier {
    override fun ContentDrawScope.draw() {
        drawIntoCanvas {
            if (it.nativeCanvas is RecordingCanvas) {
                (it.nativeCanvas as RecordingCanvas).let {
                    it.document.startBox(
                        modifier.toRemoteCompose(),
                        horizontalAlignment.toRemoteCompose(),
                        verticalArrangement.toRemoteCompose(),
                    )
                    drawContent()
                    it.document.endBox()
                }
            }
        }
    }
}

/**
 * RemoteBox implements a Box layout, delegating to the foundation Box layout as needed. This allows
 * RemoteBox to both work as a normal Box when called within a normal Compose tree, and capture the
 * layout information when called within a capture pass for RemoteCompose.
 */
@RemoteComposable
@Composable
fun RemoteBox(
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
            RemoteComposeBoxModifier(modifier, horizontalAlignment, verticalArrangement)
                .then(modifier.toComposeUiLayout())
        ) {
            if (background?.brush?.hasShader == true) {
                RemoteCanvas(RemoteModifier.fillMaxSize()) { drawRect(background.brush) }
            }

            content()
        }
    }
}

inline fun <reified T : RemoteModifier.Element> RemoteModifier.find(): T? {
    return this.foldIn<T?>(null) { result, element -> result ?: element as? T }
}

fun boxAlignment(
    horizontal: Alignment.Horizontal,
    vertical: Arrangement.Vertical,
): androidx.compose.ui.Alignment {
    return CombinedAlignment(horizontal.toComposeUi(), vertical.toComposeUiAlignment())
}

private fun Arrangement.Vertical.toComposeUiAlignment(): androidx.compose.ui.Alignment.Vertical {
    return when (this) {
        Arrangement.Top -> androidx.compose.ui.Alignment.Top
        Arrangement.Center -> androidx.compose.ui.Alignment.CenterVertically
        Arrangement.Bottom -> androidx.compose.ui.Alignment.Bottom
        else -> {
            System.err.println("Unsupported Arrangement $this")
            androidx.compose.ui.Alignment.CenterVertically
        }
    }
}

private class CombinedAlignment(
    private val horizontal: androidx.compose.ui.Alignment.Horizontal,
    private val vertical: androidx.compose.ui.Alignment.Vertical,
) : androidx.compose.ui.Alignment {
    override fun align(size: IntSize, space: IntSize, layoutDirection: LayoutDirection): IntOffset {
        val x = horizontal.align(size.width, space.width, layoutDirection)
        val y = vertical.align(size.height, space.height)
        return IntOffset(x, y)
    }
}

/** Utility function to support RemoteBox with no provided content */
@RemoteComposable
@Composable
fun RemoteBox(modifier: RemoteModifier = RemoteModifier) {
    RemoteBox(modifier) {}
}
