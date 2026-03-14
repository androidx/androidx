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

package androidx.wear.compose.remote.material3

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteSpacer
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Opinionated Wear Material 3 [RemoteCard] that offers a specific layout to show interactive
 * information about an application, e.g. a message.
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteTitleCardSample
 * @param title A slot for displaying the title of the card
 * @param modifier Modifier to be applied to the card
 * @param time An optional slot for displaying the time relevant to the contents of the card
 * @param subtitle An optional slot for displaying the subtitle of the card
 * @param shape Defines the card's shape.
 * @param colors [RemoteCardColors] that will be used to resolve the colors used for this card.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param content The optional body content of the card.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("RestrictedApiAndroidX")
@RemoteComposable
@Composable
public fun RemoteTitleCard(
    onClick: Action,
    title: @Composable @RemoteComposable () -> Unit,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = true.rb,
    time: (@Composable @RemoteComposable () -> Unit)? = null,
    subtitle: (@Composable @RemoteComposable () -> Unit)? = null,
    shape: RemoteShape = RemoteCardDefaults.shape,
    colors: RemoteCardColors = RemoteCardDefaults.cardColors(),
    contentPadding: RemoteDp = RemoteCardDefaults.ContentPadding,
    content: (@Composable @RemoteComposable () -> Unit)? = null,
) {
    val timeWithTextStyle: @Composable @RemoteComposable () -> Unit = {
        time?.let {
            CompositionLocalProvider(
                LocalRemoteContentColor provides colors.timeColor,
                LocalRemoteTextStyle provides RemoteTitleCardTokens.TimeTypography,
                content = time,
            )
        }
    }

    RemoteCardImpl(
        onClick = onClick,
        modifier = modifier,
        colors = colors,
        enabled = enabled,
        contentPadding = contentPadding,
        shape = shape,
    ) {
        if (content == null && time != null) {
            timeWithTextStyle()
            RemoteSpacer(modifier = RemoteModifier.height(4.rdp))
        }
        RemoteRow(
            modifier = RemoteModifier.fillMaxWidth(),
            verticalAlignment = RemoteAlignment.Top,
        ) {
            RemoteRow(modifier = RemoteModifier.weight(1f.rf)) {
                CompositionLocalProvider(
                    LocalRemoteContentColor provides colors.titleColor,
                    LocalRemoteTextStyle provides RemoteTitleCardTokens.TitleTypography,
                    content = title,
                )
            }
            if (content != null && time != null) {
                RemoteSpacer(modifier = RemoteModifier.width(4.rdp))
                timeWithTextStyle()
            }
        }
        content?.let {
            RemoteSpacer(modifier = RemoteModifier.height(2.rdp))
            CompositionLocalProvider(
                LocalRemoteContentColor provides colors.contentColor,
                LocalRemoteTextStyle provides RemoteCardTokens.ContentTypography,
                content = content,
            )
        }
        subtitle?.let {
            RemoteSpacer(
                modifier =
                    RemoteModifier.height(if (time == null && content == null) 2.rdp else 6.rdp)
            )
            CompositionLocalProvider(
                LocalRemoteContentColor provides colors.subtitleColor,
                LocalRemoteTextStyle provides RemoteTitleCardTokens.SubtitleTypography,
                content = subtitle,
            )
        }
    }
}

internal object RemoteTitleCardTokens {
    val TimeTypography
        @Composable @RemoteComposable get() = RemoteMaterialTheme.typography.bodyMedium

    val TitleTypography
        @Composable @RemoteComposable get() = RemoteMaterialTheme.typography.titleMedium

    val SubtitleTypography
        @Composable @RemoteComposable get() = RemoteMaterialTheme.typography.labelMedium
}
