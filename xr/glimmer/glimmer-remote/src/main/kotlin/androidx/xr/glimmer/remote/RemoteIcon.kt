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

package androidx.xr.glimmer.remote

import android.annotation.SuppressLint
import androidx.compose.remote.creation.compose.capture.RemoteImageVector
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteContentScale
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.RemoteRole
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.role
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.painter.RemotePainter
import androidx.compose.remote.creation.compose.state.RemoteBlendMode
import androidx.compose.remote.creation.compose.state.RemoteBlendModeColorFilter
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteColorFilter
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.vector.painterRemoteVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.graphics.Color

/**
 * Draws [imageVector] with a default size of [LocalRemoteIconSize] and applies [tint].
 *
 * [RemoteIcon] is an opinionated component designed for single-color icons so that they can be
 * tinted consistently for the component they are placed in. Set a custom size explicitly using
 * [RemoteModifier.size][size], or provide [LocalRemoteIconSize] to configure the preferred size for
 * icons inside a component. For multicolored icons and icons that should not be tinted, pass
 * `Color.Unspecified.rc` ([Color.Unspecified]) for [tint].
 *
 * By default, the icon uses [LocalRemoteIconSize] and the default content color:
 *
 * @sample androidx.xr.glimmer.remote.samples.RemoteIconImageVectorSample
 *
 * Custom size and color example:
 *
 * @sample androidx.xr.glimmer.remote.samples.RemoteIconImageVectorWithSizeAndTintSample
 * @param imageVector [RemoteImageVector] to draw inside this icon
 * @param contentDescription text used by accessibility services to describe what this icon
 *   represents. Always provide this unless the icon is purely decorative and does not represent a
 *   meaningful action that a user can take. Localize this text, such as by using
 *   [androidx.compose.ui.res.stringResource] with
 *   [androidx.compose.remote.creation.compose.state.rs]
 * @param modifier the [RemoteModifier] to be applied to this icon
 * @param tint tint color applied to [imageVector]. Defaults to the current content color. If
 *   `Color.Unspecified.rc` ([Color.Unspecified]) is provided, no tint is applied
 * @see LocalRemoteIconSize
 */
@RemoteComposable
@Composable
public fun RemoteIcon(
    imageVector: RemoteImageVector,
    contentDescription: RemoteString?,
    modifier: RemoteModifier = RemoteModifier,
    tint: RemoteColor = RemoteGlimmerTheme.colors.content,
) {
    val painter = painterRemoteVector(imageVector, tint)
    RemoteIcon(contentDescription = contentDescription, modifier = modifier) {
        with(painter) { onDraw() }
    }
}

/**
 * Draws [bitmap] with a default size of [LocalRemoteIconSize] and applies [tint].
 *
 * [RemoteIcon] is an opinionated component designed for single-color icons so that they can be
 * tinted consistently for the component they are placed in. Set a custom size explicitly using
 * [RemoteModifier.size][size], or provide [LocalRemoteIconSize] to configure the preferred size for
 * icons inside a component. For multicolored icons and icons that should not be tinted, pass
 * `Color.Unspecified.rc` ([Color.Unspecified]) for [tint].
 *
 * By default, the icon uses [LocalRemoteIconSize] and the default content color:
 *
 * @sample androidx.xr.glimmer.remote.samples.RemoteIconImageBitmapSample
 *
 * Custom size and color example:
 *
 * @sample androidx.xr.glimmer.remote.samples.RemoteIconImageBitmapWithSizeAndTintSample
 * @param bitmap [RemoteImageBitmap] to draw inside this icon
 * @param contentDescription text used by accessibility services to describe what this icon
 *   represents. Always provide this unless the icon is purely decorative and does not represent a
 *   meaningful action that a user can take. Localize this text, such as by using
 *   [androidx.compose.ui.res.stringResource] with
 *   [androidx.compose.remote.creation.compose.state.rs]
 * @param modifier the [RemoteModifier] to be applied to this icon
 * @param tint tint color applied to [bitmap]. Defaults to the current content color. If
 *   `Color.Unspecified.rc` ([Color.Unspecified]) is provided, no tint is applied
 * @see LocalRemoteIconSize
 */
@RemoteComposable
@Composable
public fun RemoteIcon(
    bitmap: RemoteImageBitmap,
    contentDescription: RemoteString?,
    modifier: RemoteModifier = RemoteModifier,
    tint: RemoteColor = RemoteGlimmerTheme.colors.content,
) {
    RemoteIcon(contentDescription = contentDescription, modifier = modifier) {
        val colorFilter = createColorFilter(tint)
        if (colorFilter != null) {
            @SuppressLint("RestrictedApiAndroidX")
            val paint = RemotePaint { this.colorFilter = colorFilter }
            usePaint(paint) {
                @SuppressLint("RestrictedApiAndroidX")
                drawScaledBitmap(image = bitmap, scaleType = RemoteContentScale.Fit)
            }
        } else {
            @SuppressLint("RestrictedApiAndroidX")
            drawScaledBitmap(image = bitmap, scaleType = RemoteContentScale.Fit)
        }
    }
}

/**
 * Draws [painter] with a default size of [LocalRemoteIconSize].
 *
 * Set a custom size explicitly using [RemoteModifier.size][size], or provide [LocalRemoteIconSize]
 * to configure the preferred size for icons inside a component.
 *
 * By default, the icon uses [LocalRemoteIconSize]:
 *
 * @sample androidx.xr.glimmer.remote.samples.RemoteIconPainterSample
 * @param painter [RemotePainter] to draw inside this icon
 * @param contentDescription text used by accessibility services to describe what this icon
 *   represents. Always provide this unless the icon is purely decorative and does not represent a
 *   meaningful action that a user can take. Localize this text, such as by using
 *   [androidx.compose.ui.res.stringResource] with
 *   [androidx.compose.remote.creation.compose.state.rs]
 * @param modifier the [RemoteModifier] to be applied to this icon
 * @see LocalRemoteIconSize
 */
// TODO: b/582241612 - Add a tint parameter once RemotePainter supports colorFilter.
@RemoteComposable
@Composable
public fun RemoteIcon(
    painter: RemotePainter,
    contentDescription: RemoteString?,
    modifier: RemoteModifier = RemoteModifier,
) {
    RemoteIcon(contentDescription = contentDescription, modifier = modifier) {
        with(painter) { onDraw() }
    }
}

/**
 * CompositionLocal containing the preferred size of an icon. [RemoteIcon] uses this value by
 * default - override it explicitly with [size]. The default provided value is medium icon size.
 *
 * @see RemoteIcon
 */
public val LocalRemoteIconSize: ProvidableCompositionLocal<RemoteDp> =
    compositionLocalOf(structuralEqualityPolicy()) { RemoteGlimmerTheme._iconSizes.medium }

@RemoteComposable
@Composable
private fun RemoteIcon(
    contentDescription: RemoteString?,
    modifier: RemoteModifier,
    onDraw: RemoteDrawScope.() -> Unit,
) {
    val semantics =
        if (contentDescription != null) {
            RemoteModifier.semantics {
                this.contentDescription = contentDescription
                this.role = RemoteRole.Image
            }
        } else {
            RemoteModifier
        }
    RemoteBox(
        modifier.size(LocalRemoteIconSize.current).then(semantics).drawWithContent {
            onDraw()
            // TODO: b/578385644 - Delete this workaround once RC framework fixes the color leaking.
            usePaint(RemotePaint()) {}
        }
    )
}

/**
 * Creates a [RemoteColorFilter] that tints the icon with [tint], or returns `null` if [tint] is
 * unspecified, so the icon is drawn with its original colors.
 */
@SuppressLint("RestrictedApiAndroidX")
private fun createColorFilter(tint: RemoteColor): RemoteColorFilter? =
    // TODO: b/588377603 - Check for `RemoteColor.Unspecified` once it's ready. Until then,
    //  `Color.Unspecified.rc` equals `RemoteColor.Transparent`.
    if (tint == RemoteColor.Transparent) {
        null
    } else {
        RemoteBlendModeColorFilter(tint, RemoteBlendMode.SrcIn)
    }
