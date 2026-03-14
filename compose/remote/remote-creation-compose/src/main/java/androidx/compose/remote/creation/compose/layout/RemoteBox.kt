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

/** Utility modifier to record the layout information */
internal class RemoteComposeBoxModifier(
    private val modifier: RemoteModifier,
    private val contentAlignment: RemoteAlignment = RemoteAlignment.TopStart,
) : DrawModifier {
    override fun ContentDrawScope.draw() {
        drawIntoRemoteCanvas { canvas ->
            canvas.document.startBox(
                canvas.toRecordingModifier(modifier),
                contentAlignment.horizontal.toRemote(this.layoutDirection),
                contentAlignment.vertical.toRemote(),
            )
            this@draw.drawContent()
            canvas.document.endBox()
        }
    }
}

/**
 * A layout composable that positions its children relative to its own edges.
 *
 * `RemoteBox` allows you to wrap multiple children and position them using [contentAlignment]. In
 * Remote Compose, this layout is recorded as a Box command.
 *
 * @param modifier The modifier to be applied to this box.
 * @param contentAlignment The default alignment inside the Box.
 * @param content The content of the box.
 */
@RemoteComposable
@Composable
public fun RemoteBox(
    modifier: RemoteModifier = RemoteModifier,
    contentAlignment: RemoteAlignment = RemoteAlignment.TopStart,
    content: @Composable () -> Unit,
) {
    if (currentComposer.applier is RemoteComposeApplierV2) {
        RemoteBoxV2(modifier, contentAlignment) { content() }
        return
    }
    @Suppress("COMPOSE_APPLIER_CALL_MISMATCH") // b/446706254
    androidx.compose.foundation.layout.Box(
        RemoteComposeBoxModifier(modifier, contentAlignment).then(modifier.toComposeUiLayout())
    ) {
        content()
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public inline fun <reified T : RemoteModifier.Element> RemoteModifier.find(): T? {
    return this.foldIn<T?>(null) { result, element -> result ?: element as? T }
}

/**
 * A version of [RemoteBox] with no content, often used as a spacer or a background placeholder.
 *
 * @param modifier The modifier to be applied to this box.
 */
@RemoteComposable
@Composable
public fun RemoteBox(modifier: RemoteModifier = RemoteModifier) {
    if (currentComposer.applier is RemoteComposeApplierV2) {
        RemoteBoxV2(modifier)
        return
    }
    RemoteBox(modifier) {}
}
