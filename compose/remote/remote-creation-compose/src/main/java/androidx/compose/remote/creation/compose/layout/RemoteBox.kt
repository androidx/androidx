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
import androidx.compose.remote.creation.compose.modifier.toComposeUiLayout
import androidx.compose.remote.creation.compose.modifier.toRecordingModifier
import androidx.compose.remote.creation.compose.v2.RemoteBoxV2
import androidx.compose.remote.creation.compose.v2.RemoteComposeApplierV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

/** Utility modifier to record the layout information */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteComposeBoxModifier(
    private val modifier: RemoteModifier,
    private val horizontalAlignment: RemoteAlignment.Horizontal = RemoteAlignment.Start,
    private val verticalArrangement: RemoteArrangement.Vertical = RemoteArrangement.Top,
) : DrawModifier {
    override fun ContentDrawScope.draw() {
        drawIntoRemoteCanvas { canvas ->
            canvas.document.startBox(
                canvas.toRecordingModifier(modifier),
                horizontalAlignment.toRemote(),
                verticalArrangement.toRemote(),
            )
            this@draw.drawContent()
            canvas.document.endBox()
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
public fun RemoteBox(
    modifier: RemoteModifier = RemoteModifier,
    horizontalAlignment: RemoteAlignment.Horizontal = RemoteAlignment.Start,
    verticalArrangement: RemoteArrangement.Vertical = RemoteArrangement.Top,
    content: @Composable () -> Unit,
) {
    if (currentComposer.applier is RemoteComposeApplierV2) {
        RemoteBoxV2(modifier, horizontalAlignment, verticalArrangement) { content() }
        return
    }
    @Suppress("COMPOSE_APPLIER_CALL_MISMATCH") // b/446706254
    androidx.compose.foundation.layout.Box(
        RemoteComposeBoxModifier(modifier, horizontalAlignment, verticalArrangement)
            .then(modifier.toComposeUiLayout())
    ) {
        content()
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public inline fun <reified T : RemoteModifier.Element> RemoteModifier.find(): T? {
    return this.foldIn<T?>(null) { result, element -> result ?: element as? T }
}

public fun boxAlignment(
    horizontal: RemoteAlignment.Horizontal,
    vertical: RemoteArrangement.Vertical,
): androidx.compose.ui.Alignment {
    return CombinedAlignment(horizontal.toComposeUi(), vertical.toComposeUiAlignment())
}

private fun RemoteArrangement.Vertical.toComposeUiAlignment():
    androidx.compose.ui.Alignment.Vertical {
    return when (this) {
        RemoteArrangement.Top -> androidx.compose.ui.Alignment.Top
        RemoteArrangement.Center -> androidx.compose.ui.Alignment.CenterVertically
        RemoteArrangement.Bottom -> androidx.compose.ui.Alignment.Bottom
        else -> {
            System.err.println("Unsupported RemoteArrangement $this")
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
public fun RemoteBox(modifier: RemoteModifier = RemoteModifier) {
    if (currentComposer.applier is RemoteComposeApplierV2) {
        RemoteBoxV2(modifier)
        return
    }
    RemoteBox(modifier) {}
}
