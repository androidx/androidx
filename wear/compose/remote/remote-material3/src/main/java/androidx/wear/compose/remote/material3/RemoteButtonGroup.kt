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

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemotePaddingValues
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteRowScope
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.runtime.Composable

/**
 * Layout component to implement an expressive group of buttons in a row.
 *
 * Example of 3 buttons, the middle one bigger:
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteButtonGroupThreeButtonSample
 * @param modifier Modifier to be applied to the button group
 * @param spacing the amount of spacing between buttons
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param verticalAlignment the vertical alignment of the button group's children.
 * @param content the content and properties of each button. The Ux guidance is to use no more than
 *   3 buttons within a ButtonGroup.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
@RemoteComposable
public fun RemoteButtonGroup(
    modifier: RemoteModifier = RemoteModifier,
    spacing: RemoteDp = RemoteButtonGroupDefaults.Spacing,
    contentPadding: RemotePaddingValues = RemoteButtonGroupDefaults.fullWidthPaddings(),
    verticalAlignment: RemoteAlignment.Vertical = RemoteAlignment.CenterVertically,
    content: @RemoteComposable @Composable (RemoteRowScope.() -> Unit),
) {
    RemoteButtonGroupImpl(modifier, spacing, contentPadding, verticalAlignment, content)
}

@Composable
@RemoteComposable
private fun RemoteButtonGroupImpl(
    modifier: RemoteModifier = RemoteModifier,
    spacing: RemoteDp = RemoteButtonGroupDefaults.Spacing,
    contentPadding: RemotePaddingValues = RemoteButtonGroupDefaults.fullWidthPaddings(),
    verticalAlignment: RemoteAlignment.Vertical = RemoteAlignment.CenterVertically,
    content: @RemoteComposable @Composable (RemoteRowScope.() -> Unit),
) {
    RemoteRow(
        modifier.padding(contentPadding),
        content = content,
        verticalAlignment = verticalAlignment,
        horizontalArrangement =
            RemoteArrangement.spacedBy(spacing, RemoteAlignment.CenterHorizontally),
    )
}

/** Contains the default values used by [RemoteButtonGroup] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteButtonGroupDefaults {
    /**
     * Return the recommended padding to use as the contentPadding of a [RemoteButtonGroup], when it
     * takes the full width of the screen.
     */
    public fun fullWidthPaddings(): RemotePaddingValues {
        // TODO(b/466078229): Use expression of the percentage of screen width as padding,
        // currently it's unable to use that since it's too long.
        return RemotePaddingValues(horizontal = 11.rdp, vertical = 0.rdp)
    }

    /** Spacing between buttons. */
    public val Spacing: RemoteDp = 4.rdp

    internal val minimumInteractiveComponentSize = 48.rdp

    /** Default for the minimum width of buttons in a RemoteButtonGroup */
    public val MinWidth: RemoteDp = minimumInteractiveComponentSize
}
