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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.wear.compose.remote.material3

import android.annotation.SuppressLint
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteImage as CreationRemoteImage
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.RemoteBitmap
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.layout.ContentScale

/**
 * A remote composable that displays a [RemoteBitmap] styled as an avatar.
 *
 * @param avatar The [RemoteBitmap] to be displayed.
 * @param contentDescription A text description for accessibility.
 * @param modifier The [RemoteModifier] to be applied to this composable.
 * @param contentScale The [ContentScale] to be applied to the image.
 */
@Composable
@RemoteComposable
@Suppress("RestrictedApiAndroidX")
public fun RemoteAvatarImage(
    avatar: RemoteBitmap,
    contentDescription: RemoteString?,
    modifier: RemoteModifier = RemoteModifier,
    contentScale: ContentScale = ContentScale.FillBounds,
) {
    // TODO (yschimke): enable in follow up with new capability API.
    val shouldFallback = false
    if (shouldFallback) {
        FallbackAvatar(background = avatar, contentDescription = contentDescription, modifier)
    } else {
        CreationRemoteImage(
            avatar,
            contentDescription = contentDescription,
            modifier =
                modifier
                    .width(ImageDefaults.avatarSize())
                    .height(ImageDefaults.avatarSize())
                    .clip(ImageDefaults.avatarShape()),
            contentScale = contentScale,
            alpha = DefaultAlpha.rf,
        )
    }
}

/**
 * A remote composable that displays a [RemoteBitmap] as a background.
 *
 * @param background The [RemoteBitmap] to be displayed as the background.
 * @param contentDescription A text description for accessibility.
 * @param modifier The [RemoteModifier] to be applied to this composable.
 * @param contentScale The [ContentScale] to be applied to the image.
 * @param overlayColor Optional [RemoteColor] to be displayed on top of background for contrast.
 */
@Composable
@RemoteComposable
@SuppressLint("RestrictedApiAndroidX")
public fun RemoteBackgroundImage(
    background: RemoteBitmap,
    contentDescription: RemoteString?,
    modifier: RemoteModifier = RemoteModifier,
    contentScale: ContentScale = ContentScale.FillBounds,
    overlayColor: RemoteColor? = ImageDefaults.backgroundOverlayColor(),
) {
    // TODO (yschimke): enable in follow up with new capability API
    val shouldFallback = false
    if (shouldFallback) {
        FallbackBackground(background, contentDescription, modifier, contentScale, overlayColor)
    } else {
        RemoteBox(modifier = modifier) {
            val imageModifier = RemoteModifier.fillMaxSize().clip(ImageDefaults.backgroundShape())
            CreationRemoteImage(
                remoteBitmap = background,
                contentDescription = contentDescription,
                modifier = imageModifier,
                contentScale = contentScale,
                alpha = DefaultAlpha.rf,
            )

            if (overlayColor != null) {
                BackgroundOverlay(modifier = imageModifier, overlayColor = overlayColor)
            }
        }
    }
}

@Composable
@RemoteComposable
@SuppressLint("RestrictedApiAndroidX")
private fun FallbackBackground(
    background: RemoteBitmap,
    contentDescription: RemoteString?,
    modifier: RemoteModifier = RemoteModifier,
    contentScale: ContentScale = ContentScale.FillBounds,
    overlayColor: RemoteColor? = ImageDefaults.backgroundOverlayColor(),
) {
    // TODO clipping effect.
    RemoteBox(modifier = modifier) {
        val imageModifier = RemoteModifier.fillMaxSize()
        CreationRemoteImage(
            remoteBitmap = background,
            contentDescription = contentDescription,
            modifier = imageModifier,
            contentScale = contentScale,
            alpha = DefaultAlpha.rf,
        )

        if (overlayColor != null) {
            BackgroundOverlay(modifier = imageModifier, overlayColor = overlayColor)
        }
    }
}

@Composable
@RemoteComposable
@SuppressLint("RestrictedApiAndroidX")
private fun FallbackAvatar(
    background: RemoteBitmap,
    contentDescription: RemoteString?,
    modifier: RemoteModifier = RemoteModifier,
    contentScale: ContentScale = ContentScale.FillBounds,
) {
    // TODO clipping effect.
    CreationRemoteImage(
        remoteBitmap = background,
        contentDescription = contentDescription,
        modifier = modifier.fillMaxSize(),
        contentScale = contentScale,
        alpha = DefaultAlpha.rf,
    )
}

@Composable
@RemoteComposable
@SuppressLint("RestrictedApiAndroidX")
private fun BackgroundOverlay(modifier: RemoteModifier, overlayColor: RemoteColor) {
    RemoteCanvas(modifier = modifier.clip(ImageDefaults.backgroundShape())) {
        val cornerRadius = ImageDefaults.BACKGROUND_CORNER_RADIUS_DP.toPx()
        drawRoundRect(
            paint = RemotePaint().apply { remoteColor = overlayColor },
            cornerRadius = RemoteOffset(cornerRadius, cornerRadius),
        )
    }
}

/** Contains default values for Image. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("RestrictedApiAndroidX")
public object ImageDefaults {

    internal val AVATAR_SIZE_DP = 24.rdp
    internal val BACKGROUND_CORNER_RADIUS_DP = 26.rdp

    public fun avatarSize(): RemoteDp = AVATAR_SIZE_DP

    public fun avatarShape(): RemoteRoundedCornerShape = RemoteRoundedCornerShape(AVATAR_SIZE_DP)

    @RemoteComposable
    @Composable
    public fun backgroundOverlayColor(): RemoteColor {
        return RemoteMaterialTheme.colorScheme.background.copy(alpha = 0.6f.rf)
    }

    public fun backgroundShape(): RemoteRoundedCornerShape =
        RemoteRoundedCornerShape(BACKGROUND_CORNER_RADIUS_DP)
}
