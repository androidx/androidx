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
 * Composable function that displays an icon using an [RemoteImageVector].
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteIconSimpleSample
 *
 * This function provides a way to display icons consistently across both local and remote Compose
 * environments.
 *
 * @param imageVector The [RemoteImageVector] representing the icon to display.
 * @param contentDescription Text used by accessibility services to describe what this icon
 *   represents. This should always be provided unless this icon is used for decorative purposes,
 *   and does not represent a meaningful action that a user can take. This text should be localized,
 *   such as by using [androidx.compose.ui.res.stringResource] or similar.
 * @param modifier The [RemoteModifier] to apply to the icon.
 * @param tint The color to apply to the icon. Defaults to the current content color.
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
