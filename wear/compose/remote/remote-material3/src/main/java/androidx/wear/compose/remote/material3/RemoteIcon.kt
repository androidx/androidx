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

package androidx.wear.compose.remote.material3

import androidx.compose.remote.creation.compose.capture.RemoteImageVector
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.foundation.icon.RemoteBasicIcon
import androidx.compose.runtime.Composable

/**
 * Displays a Material Design icon from a [RemoteImageVector] in a remote layout.
 *
 * Sizes the icon to [imageVector]'s intrinsic dimensions, or `24.dp` when unspecified. Unlike
 * [RemoteBasicIcon], which defaults [tint] to white, [RemoteIcon] defaults [tint] to the ambient
 * content color provided by the surrounding Wear Material 3 component (or white if none is
 * provided).
 *
 * Provide a localized [contentDescription] (for example, via
 * [androidx.compose.ui.res.stringResource]) whenever the icon conveys meaning or represents an
 * action. Pass `null` only for purely decorative icons, which omits the accessibility content
 * description so accessibility services ignore it.
 *
 * Example of displaying a [RemoteIcon]:
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteIconSimpleSample
 * @param imageVector the [RemoteImageVector] to draw inside this icon
 * @param contentDescription localized text used by accessibility services to describe this icon, or
 *   `null` if the icon is purely decorative and has no accessibility description
 * @param modifier the [RemoteModifier] applied to this icon
 * @param tint color applied to [imageVector] using a color filter with the vector's tint blend mode
 *   ([androidx.compose.ui.graphics.BlendMode.SrcIn] by default, which replaces the icon's RGB
 *   colors with [tint] while preserving alpha rather than multiplying RGBA). Defaults to the
 *   current content color
 */
@RemoteComposable
@Composable
public fun RemoteIcon(
    imageVector: RemoteImageVector,
    contentDescription: RemoteString?,
    modifier: RemoteModifier = RemoteModifier,
    tint: RemoteColor = LocalRemoteContentColor.current,
) {
    RemoteBasicIcon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
    )
}
