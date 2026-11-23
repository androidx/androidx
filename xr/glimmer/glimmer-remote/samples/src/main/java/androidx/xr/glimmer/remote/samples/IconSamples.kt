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

package androidx.xr.glimmer.remote.samples

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.annotation.Sampled
import androidx.compose.remote.creation.compose.capture.RemoteImageVector
import androidx.compose.remote.creation.compose.capture.path
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.solidColor
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.asRemoteImageBitmap
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.vector.painterRemoteVector
import androidx.compose.remote.tooling.preview.RemotePreviewWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.xr.glimmer.remote.RemoteGlimmerTheme
import androidx.xr.glimmer.remote.RemoteIcon

@Sampled
@Composable
@RemoteComposable
fun RemoteIconImageVectorSample() {
    RemoteIcon(imageVector = FavoriteIcon, contentDescription = "Localized description".rs)
}

@Sampled
@Composable
@RemoteComposable
fun RemoteIconImageVectorWithSizeAndTintSample() {
    RemoteIcon(
        imageVector = FavoriteIcon,
        contentDescription = "Localized description".rs,
        modifier = RemoteModifier.size(RemoteGlimmerTheme.iconSizes.large),
        tint = RemoteGlimmerTheme.colors.primary,
    )
}

@Sampled
@Composable
@RemoteComposable
fun RemoteIconImageBitmapSample() {
    val bitmap = remember { createRemoteImageBitmap() }
    RemoteIcon(bitmap = bitmap, contentDescription = "Localized description".rs)
}

@Sampled
@Composable
@RemoteComposable
fun RemoteIconImageBitmapWithSizeAndTintSample() {
    val bitmap = remember { createRemoteImageBitmap() }
    RemoteIcon(
        bitmap = bitmap,
        contentDescription = "Localized description".rs,
        modifier = RemoteModifier.size(RemoteGlimmerTheme.iconSizes.large),
        tint = RemoteGlimmerTheme.colors.primary,
    )
}

@Sampled
@Composable
@RemoteComposable
fun RemoteIconPainterSample() {
    RemoteIcon(
        painter = painterRemoteVector(FavoriteIcon),
        contentDescription = "Localized description".rs,
    )
}

@Preview
@PreviewWrapper(RemotePreviewWrapper::class)
@Composable
private fun RemoteIconImageVectorPreview() {
    RemoteGlimmerTheme { RemoteIconImageVectorSample() }
}

@Preview
@PreviewWrapper(RemotePreviewWrapper::class)
@Composable
private fun RemoteIconImageVectorWithSizeAndTintPreview() {
    RemoteGlimmerTheme { RemoteIconImageVectorWithSizeAndTintSample() }
}

@Preview
@PreviewWrapper(RemotePreviewWrapper::class)
@Composable
private fun RemoteIconImageBitmapPreview() {
    RemoteGlimmerTheme { RemoteIconImageBitmapSample() }
}

@Preview
@PreviewWrapper(RemotePreviewWrapper::class)
@Composable
private fun RemoteIconImageBitmapWithSizeAndTintPreview() {
    RemoteGlimmerTheme { RemoteIconImageBitmapWithSizeAndTintSample() }
}

@Preview
@PreviewWrapper(RemotePreviewWrapper::class)
@Composable
private fun RemoteIconPainterPreview() {
    RemoteGlimmerTheme { RemoteIconPainterSample() }
}

/** Icon taken from material-icons-core */
internal val FavoriteIcon: RemoteImageVector =
    RemoteImageVector.Builder(
            name = "Favorite",
            defaultWidth = 24.rdp,
            defaultHeight = 24.rdp,
            viewportWidth = 24f.rf,
            viewportHeight = 24f.rf,
            tintColor = Color.Unspecified.rc,
        )
        .apply {
            path(fill = RemoteBrush.solidColor(Color.White.rc)) {
                moveTo(12.0f.rf, 21.35f.rf)
                lineToRelative((-1.45f).rf, (-1.32f).rf)
                curveTo(5.4f.rf, 15.36f.rf, 2.0f.rf, 12.28f.rf, 2.0f.rf, 8.5f.rf)
                curveTo(2.0f.rf, 5.42f.rf, 4.42f.rf, 3.0f.rf, 7.5f.rf, 3.0f.rf)
                curveToRelative(1.74f.rf, 0.0f.rf, 3.41f.rf, 0.81f.rf, 4.5f.rf, 2.09f.rf)
                curveTo(13.09f.rf, 3.81f.rf, 14.76f.rf, 3.0f.rf, 16.5f.rf, 3.0f.rf)
                curveTo(19.58f.rf, 3.0f.rf, 22.0f.rf, 5.42f.rf, 22.0f.rf, 8.5f.rf)
                curveToRelative(0.0f.rf, 3.78f.rf, (-3.4f).rf, 6.86f.rf, (-8.55f).rf, 11.54f.rf)
                lineTo(12.0f.rf, 21.35f.rf)
                close()
            }
        }
        .build()

/** Creates a single-color circle bitmap to be used as an icon. */
private fun createRemoteImageBitmap(): RemoteImageBitmap =
    Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        .apply {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
            Canvas(this).drawCircle(24f, 24f, 20f, paint)
        }
        .asImageBitmap()
        .asRemoteImageBitmap()
