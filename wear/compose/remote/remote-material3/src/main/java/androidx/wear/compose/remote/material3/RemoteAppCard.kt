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
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
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
 * Opinionated Wear Material 3 [RemoteCard] that offers a specific 5 slot layout to show information
 * about an application, e.g. a notification.
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteAppCardSample
 * @param appName A slot for displaying the application name.
 * @param title A slot for displaying the title of the card.
 * @param modifier Modifier to be applied to the card
 * @param shape Defines the card's shape.
 * @param colors [RemoteCardColors] that will be used to resolve the colors used for this card.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param appImage A slot for a small ([RemoteCardDefaults.AppImageSize]) image associated with the
 *   app.
 * @param time A slot for displaying the time relevant to the contents of the card.
 * @param content The main slot for a content of this card
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("RestrictedApiAndroidX")
@RemoteComposable
@Composable
public fun RemoteAppCard(
    onClick: Action,
    appName: @Composable @RemoteComposable () -> Unit,
    title: @Composable @RemoteComposable () -> Unit,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = true.rb,
    shape: RemoteShape = RemoteCardDefaults.shape,
    colors: RemoteCardColors = RemoteCardDefaults.cardColors(),
    contentPadding: RemoteDp = RemoteCardDefaults.ContentPadding,
    appImage: (@Composable @RemoteComposable () -> Unit)? = null,
    time: (@Composable @RemoteComposable () -> Unit)? = null,
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteCardImpl(
        onClick = onClick,
        modifier = modifier,
        colors = colors,
        enabled = enabled,
        contentPadding = contentPadding,
        shape = shape,
    ) {
        RemoteRow(
            modifier = RemoteModifier.fillMaxWidth(),
            verticalAlignment = RemoteAlignment.CenterVertically,
            horizontalArrangement = RemoteArrangement.SpaceBetween,
        ) {
            RemoteRow(
                modifier = RemoteModifier.weight(1f.rf),
                verticalAlignment = RemoteAlignment.CenterVertically,
            ) {
                appImage?.let {
                    appImage()
                    RemoteSpacer(RemoteModifier.width(4.rdp))
                }
                CompositionLocalProvider(
                    LocalRemoteContentColor provides colors.appNameColor,
                    LocalRemoteTextStyle provides RemoteAppCardTokens.AppNameTypography,
                    content = appName,
                )
            }

            time?.let {
                RemoteSpacer(modifier = RemoteModifier.width(6.rdp))
                CompositionLocalProvider(
                    LocalRemoteContentColor provides colors.timeColor,
                    LocalRemoteTextStyle provides RemoteAppCardTokens.TimeTypography,
                    content = time,
                )
            }
        }
        RemoteSpacer(modifier = RemoteModifier.height(6.rdp))
        RemoteRow(modifier = RemoteModifier.fillMaxWidth()) {
            CompositionLocalProvider(
                LocalRemoteContentColor provides colors.titleColor,
                LocalRemoteTextStyle provides RemoteAppCardTokens.TitleTypography,
                content = title,
            )
        }
        RemoteSpacer(modifier = RemoteModifier.height(2.rdp))
        CompositionLocalProvider(
            LocalRemoteContentColor provides colors.contentColor,
            LocalRemoteTextStyle provides RemoteCardTokens.ContentTypography,
            content = content,
        )
    }
}

internal object RemoteAppCardTokens {
    val AppNameTypography
        @Composable @RemoteComposable get() = RemoteMaterialTheme.typography.titleSmall

    val TimeTypography
        @Composable @RemoteComposable get() = RemoteMaterialTheme.typography.bodyMedium

    val TitleTypography
        @Composable @RemoteComposable get() = RemoteMaterialTheme.typography.titleMedium
}
