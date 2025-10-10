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

package androidx.compose.remote.wear.material3

import android.annotation.SuppressLint
import android.graphics.Paint
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.remote.creation.compose.capture.RecordingCanvas
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.state.RemoteBitmap
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rememberRemoteDpValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme

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
public fun AvatarImage(
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
        RemoteImage(
            avatar,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier =
                modifier
                    .width(ImageDefaults.avatarSize())
                    .height(ImageDefaults.avatarSize())
                    .clip(ImageDefaults.avatarShape()),
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
public fun BackgroundImage(
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
            RemoteImage(
                remoteBitmap = background,
                contentDescription = contentDescription,
                modifier = imageModifier,
                contentScale = contentScale,
            )

            if (overlayColor != null) {
                BackgroundOverlay(modifier = imageModifier, overlayColor = overlayColor)
            }
        }
    }
}

@Composable
@RemoteComposable
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
        RemoteImage(
            remoteBitmap = background,
            contentDescription = contentDescription,
            modifier = imageModifier,
            contentScale = contentScale,
        )

        if (overlayColor != null) {
            BackgroundOverlay(modifier = imageModifier, overlayColor = overlayColor)
        }
    }
}

@Composable
@RemoteComposable
private fun FallbackAvatar(
    background: RemoteBitmap,
    contentDescription: RemoteString?,
    modifier: RemoteModifier = RemoteModifier,
    contentScale: ContentScale = ContentScale.FillBounds,
) {
    // TODO clipping effect.
    RemoteImage(
        remoteBitmap = background,
        contentDescription = contentDescription,
        modifier = modifier.fillMaxSize(),
        contentScale = contentScale,
    )
}

@Composable
@RemoteComposable
@SuppressLint("RestrictedApiAndroidX")
private fun BackgroundOverlay(modifier: RemoteModifier, overlayColor: RemoteColor) {
    RemoteCanvas(modifier = modifier.clip(ImageDefaults.backgroundShape())) {
        val canvas = drawContext.canvas.nativeCanvas
        val paint =
            RemotePaint().apply {
                remoteColor = overlayColor
                style = Paint.Style.FILL
            }
        if (canvas is RecordingCanvas) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                canvas.drawRect(0f, 0f, size.width, size.height, paint)
                return@RemoteCanvas
            }
            canvas.drawRoundRect(
                0f,
                0f,
                size.width,
                size.height,
                ImageDefaults.BACKGROUND_CORNER_RADIUS_DP.toPx(),
                ImageDefaults.BACKGROUND_CORNER_RADIUS_DP.toPx(),
                paint,
            )
        }
    }
}

/** Contains default values for Image. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object ImageDefaults {

    internal val AVATAR_SIZE_DP = 24.dp
    internal val BACKGROUND_CORNER_RADIUS_DP = 26.dp

    @RemoteComposable
    @Composable
    public fun avatarSize(): RemoteDp = rememberRemoteDpValue { AVATAR_SIZE_DP }

    @Composable public fun avatarShape(): RoundedCornerShape = RoundedCornerShape(AVATAR_SIZE_DP)

    @RemoteComposable
    @Composable
    public fun backgroundOverlayColor(): RemoteColor {
        val color = MaterialTheme.colorScheme.background.copy(alpha = 0.6f)
        return remember { RemoteColor(color.toArgb()) }
    }

    @Composable
    public fun backgroundShape(): RoundedCornerShape =
        RoundedCornerShape(BACKGROUND_CORNER_RADIUS_DP)
}
