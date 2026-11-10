/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.foundation.icon

import androidx.compose.remote.creation.compose.capture.RemoteImageVector
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.painter.RemotePainter
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.asRemoteDp
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.vector.painterRemoteVector
import androidx.compose.runtime.Composable

/**
 * Displays a vector icon from a [RemoteImageVector] in a remote layout.
 *
 * Sizes the icon to [imageVector]'s intrinsic dimensions, or `24.dp` when unspecified. Unlike
 * Material icon components (such as [androidx.wear.compose.remote.material3.RemoteIcon]) that read
 * a theme content color, [RemoteBasicIcon] is a design-system-agnostic building block that defaults
 * [tint] to opaque white (`0xFFFFFFFF`).
 *
 * Provide a localized [contentDescription] (for example, via `stringResource`) whenever the icon
 * conveys meaning or represents an action. Pass `null` only for purely decorative icons, which
 * omits the accessibility content description so accessibility services ignore it.
 *
 * Example of a tinted [RemoteBasicIcon]:
 *
 * @sample androidx.compose.remote.foundation.samples.RemoteBasicIconSample
 * @param imageVector the [RemoteImageVector] to draw inside this icon
 * @param contentDescription localized text used by accessibility services to describe this icon, or
 *   `null` if the icon is purely decorative and has no accessibility description
 * @param modifier the [RemoteModifier] applied to this icon
 * @param tint color applied to [imageVector] using a color filter with the vector's tint blend mode
 *   ([androidx.compose.remote.creation.compose.state.RemoteBlendMode.SrcIn] by default, which
 *   replaces the icon's RGB colors with [tint] while preserving alpha rather than multiplying
 *   RGBA). Defaults to opaque white (`0xFFFFFFFF`)
 */
@RemoteComposable
@Composable
public fun RemoteBasicIcon(
    imageVector: RemoteImageVector,
    contentDescription: RemoteString?,
    modifier: RemoteModifier = RemoteModifier,
    tint: RemoteColor = 0xFFFFFFFF.toInt().rc,
) {
    val painter = painterRemoteVector(imageVector, tint)
    RemoteBox(
        modifier.semantics { this.contentDescription = contentDescription }.defaultSizeFor(painter)
    ) {
        RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
            with(painter) { onDraw() }
            // TODO(b/474687917): Temporary fix to reset tinted paint
            usePaint(RemotePaint()) {}
        }
    }
}

/** Sets a default icon size if painter doesn't specify a size, else sets to intrinsic size. */
private fun RemoteModifier.defaultSizeFor(painter: RemotePainter): RemoteModifier {
    val intrinsicSize = painter.intrinsicSize
    return this.then(
        if (intrinsicSize != null) {
            RemoteModifier.size(
                width = intrinsicSize.width.asRemoteDp(),
                height = intrinsicSize.height.asRemoteDp(),
            )
        } else {
            RemoteModifier.size(24.rdp)
        }
    )
}
