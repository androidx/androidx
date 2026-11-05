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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.wear.compose.remote.material3

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.action.RemoteAction
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemotePaddingValues
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.painter.RemotePainter
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/** @sample androidx.wear.compose.remote.material3.samples.RemoteTitleCardSample */
@RemoteComposable
@Composable
public fun RemoteTitleCard(
    onClick: RemoteAction,
    title: @Composable @RemoteComposable () -> Unit,
    modifier: RemoteModifier = RemoteModifier,
    time: (@Composable @RemoteComposable () -> Unit)? = null,
    subtitle: (@Composable @RemoteComposable () -> Unit)? = null,
    enabled: RemoteBoolean = true.rb,
    shape: RemoteShape = RemoteCardDefaults.shape,
    colors: RemoteCardColors = RemoteCardDefaults.cardColors(),
    border: RemoteBorderStroke? = null,
    contentPadding: RemotePaddingValues = RemoteCardDefaults.ContentPadding,
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
        border = border,
        contentPadding = contentPadding,
        shape = shape,
    ) {
        if (content == null && time != null) {
            timeWithTextStyle()
            RemoteBox(modifier = RemoteModifier.height(4.rdp))
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
                RemoteBox(modifier = RemoteModifier.width(4.rdp))
                timeWithTextStyle()
            }
        }
        content?.let {
            RemoteBox(modifier = RemoteModifier.height(2.rdp))
            CompositionLocalProvider(
                LocalRemoteContentColor provides colors.contentColor,
                LocalRemoteTextStyle provides RemoteCardTokens.ContentTypography,
                content = content,
            )
        }
        subtitle?.let {
            RemoteBox(
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

/** @sample androidx.wear.compose.remote.material3.samples.RemoteTitleCardWithImageSample */
@RemoteComposable
@Composable
public fun RemoteTitleCard(
    onClick: RemoteAction,
    containerPainter: RemotePainter,
    title: @Composable @RemoteComposable () -> Unit,
    modifier: RemoteModifier = RemoteModifier,
    time: (@Composable @RemoteComposable () -> Unit)? = null,
    subtitle: (@Composable @RemoteComposable () -> Unit)? = null,
    enabled: RemoteBoolean = true.rb,
    shape: RemoteShape = RemoteCardDefaults.shape,
    colors: RemoteCardColors = RemoteCardDefaults.cardWithContainerPainterColors(),
    border: RemoteBorderStroke? = null,
    contentPadding: RemotePaddingValues = RemoteCardDefaults.CardWithContainerPainterContentPadding,
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
        containerPainter = containerPainter,
        border = border,
        contentPadding = contentPadding,
        shape = shape,
    ) {
        if (content == null && time != null) {
            timeWithTextStyle()
            RemoteBox(modifier = RemoteModifier.height(4.rdp))
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
                RemoteBox(modifier = RemoteModifier.width(4.rdp))
                timeWithTextStyle()
            }
        }
        content?.let {
            RemoteBox(modifier = RemoteModifier.height(2.rdp))
            CompositionLocalProvider(
                LocalRemoteContentColor provides colors.contentColor,
                LocalRemoteTextStyle provides RemoteCardTokens.ContentTypography,
                content = content,
            )
        }
        subtitle?.let {
            RemoteBox(
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
