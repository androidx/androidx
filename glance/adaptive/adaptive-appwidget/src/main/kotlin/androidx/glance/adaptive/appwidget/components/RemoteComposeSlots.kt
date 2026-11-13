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

package androidx.glance.adaptive.appwidget.components

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.compose.remote.creation.compose.capture.RemoteImageVector
import androidx.compose.remote.creation.compose.capture.vectorResource
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.asRdp
import androidx.compose.remote.creation.compose.state.asRemoteTextUnit
import androidx.compose.remote.creation.compose.state.createThemedRemoteColor
import androidx.compose.remote.creation.compose.state.lerp
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.text.RemoteFontWeight
import androidx.compose.remote.creation.compose.text.RemoteTextOverflow
import androidx.compose.remote.creation.compose.vector.painterRemoteVector
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.glance.adaptive.appwidget.components.ring.RingDefaults

/*
 * Building blocks shared by the RemoteCompose slot components and the Information Matrix layouts
 * that host them.
 */

/** Opacity of supporting action values blended over the card background. */
private const val SUPPORTING_TEXT_ALPHA = 0.7f

/** Opacity of chart bars and dots that are not emphasized. */
private const val BAR_MUTED_ALPHA = 0.35f

/** Opacity of an icon tile's secondary state subtitle blended over its tile background. */
private const val TILE_SUBTITLE_ALPHA = 0.5f

/**
 * Dynamic Material You palette shared by the Information Matrix RemoteCompose components.
 *
 * Mirrors the RemoteViews matrix resources in `res/values-v31/remoteviews_colors.xml` and
 * `res/values/remoteviews_colors.xml`, encoding the light and dark `@android:color/system_*` tokens
 * directly into the document as themed [RemoteColor]s so the host resolves day/night and wallpaper
 * changes without recapturing.
 */
@SuppressLint("InlinedApi")
internal object RemoteComposePalette {
    val primary: RemoteColor =
        RemoteColor.createThemedRemoteColor(
            lightColorRes = android.R.color.system_accent1_600,
            darkColorRes = android.R.color.system_accent1_200,
            lightFallback = Color(0xFF6750A4),
            darkFallback = Color(0xFFD0BCFF),
        )

    val secondaryContainer: RemoteColor =
        RemoteColor.createThemedRemoteColor(
            lightColorRes = android.R.color.system_accent2_100,
            darkColorRes = android.R.color.system_accent2_700,
            lightFallback = Color(0xFFE8DEF8),
            darkFallback = Color(0xFF4A4458),
        )

    val tertiary: RemoteColor =
        RemoteColor.createThemedRemoteColor(
            lightColorRes = android.R.color.system_accent3_600,
            darkColorRes = android.R.color.system_accent3_200,
            lightFallback = Color(0xFF7D5260),
            darkFallback = Color(0xFFEFB8C8),
        )

    val onSurfaceVariant: RemoteColor =
        RemoteColor.createThemedRemoteColor(
            lightColorRes = android.R.color.system_neutral2_700,
            darkColorRes = android.R.color.system_neutral2_200,
            lightFallback = Color(0xFF49454F),
            darkFallback = Color(0xFFCAC4D0),
        )

    val widgetBackground: RemoteColor =
        RemoteColor.createThemedRemoteColor(
            lightColorRes = android.R.color.system_accent2_50,
            darkColorRes = android.R.color.system_accent2_800,
            lightFallback = Color(0xFFE0F3FF),
            darkFallback = Color(0xFF20333D),
        )

    val primaryContainer: RemoteColor =
        RemoteColor.createThemedRemoteColor(
            lightColorRes = android.R.color.system_accent1_100,
            darkColorRes = android.R.color.system_accent1_700,
            lightFallback = Color(0xFFEADDFF),
            darkFallback = Color(0xFF4F378B),
        )

    val onPrimaryContainer: RemoteColor =
        RemoteColor.createThemedRemoteColor(
            lightColorRes = android.R.color.system_accent1_700,
            darkColorRes = android.R.color.system_accent1_100,
            lightFallback = Color(0xFF4F378B),
            darkFallback = Color(0xFFEADDFF),
        )

    val onSecondaryContainer: RemoteColor =
        RemoteColor.createThemedRemoteColor(
            lightColorRes = android.R.color.system_accent2_700,
            darkColorRes = android.R.color.system_accent2_100,
            lightFallback = Color(0xFF4A4458),
            darkFallback = Color(0xFFE8DEF8),
        )

    val surfaceContainerHighest: RemoteColor =
        RemoteColor.createThemedRemoteColor(
            lightColorRes = android.R.color.system_neutral1_100,
            darkColorRes = android.R.color.system_neutral1_800,
            lightFallback = Color(0xFFE6E0E9),
            darkFallback = Color(0xFF36343B),
        )

    val barMuted: RemoteColor = primary.copy(alpha = BAR_MUTED_ALPHA.rf)

    val ringTrack: RemoteColor = primary.copy(alpha = RingDefaults.TrackAlpha.rf)

    val supportingText: RemoteColor = lerp(widgetBackground, primary, SUPPORTING_TEXT_ALPHA.rf)

    val tileSubtitle: RemoteColor =
        lerp(primaryContainer, onPrimaryContainer, TILE_SUBTITLE_ALPHA.rf)
}

/**
 * The design's `650` font weight for supporting values, badges, and compact headers, matching
 * `GlanceAdaptiveAppwidgetRemoteviews.TextAppearance.SemiBold` (`android:textFontWeight="650"`) in
 * `res/values-v29/remoteviews_styles.xml` rather than Material 3's `600`
 * [RemoteFontWeight.SemiBold].
 */
private const val DESIGN_SEMIBOLD_WEIGHT = 650

/** Font weights shared by the Information Matrix text slots, matching `remoteviews_styles.xml`. */
internal object RemoteComposeFonts {
    val Bold: RemoteFontWeight = RemoteFontWeight.Bold
    val SemiBold: RemoteFontWeight = RemoteFontWeight(DESIGN_SEMIBOLD_WEIGHT)
    val Medium: RemoteFontWeight = RemoteFontWeight.Medium
}

/**
 * Draws a [RemoteText] ellipsized at its end, matching `GlanceAdaptiveAppwidgetRemoteviews.Text` in
 * `remoteviews_styles.xml`.
 */
@RemoteComposable
@Composable
internal fun SlotText(
    text: String,
    fontSize: TextUnit,
    fontWeight: RemoteFontWeight,
    color: RemoteColor,
    modifier: RemoteModifier = RemoteModifier,
    maxLines: Int = 1,
) {
    RemoteText(
        text = text.rs,
        modifier = modifier,
        color = color,
        fontSize = fontSize.asRemoteTextUnit(),
        fontWeight = fontWeight,
        overflow = RemoteTextOverflow.Ellipsis,
        maxLines = maxLines,
    )
}

/** Draws vector drawable [icon] tinted with [tint], [size] across. */
@RemoteComposable
@Composable
internal fun RemoteVectorGlyph(
    @DrawableRes icon: Int,
    size: Dp,
    tint: RemoteColor,
    modifier: RemoteModifier = RemoteModifier,
) {
    val vector = RemoteImageVector.vectorResource(icon)
    RemoteBox(
        modifier =
            modifier
                .size(size.asRdp())
                .registerDynamicColor(tint)
                .background(painterRemoteVector(vector, tintColor = tint))
    )
}

/**
 * Registers [color] on the enclosing layout component before a `drawWithContent` modifier opens a
 * `CanvasOperations` block, so `CoreDocument.getThemedColors()` discovers any [RemoteColor] backed
 * by a `ColorTheme` even when its first visual use is on a canvas or vector painter.
 */
internal fun RemoteModifier.registerDynamicColor(color: RemoteColor): RemoteModifier {
    if (color.constantValueOrNull != null) return this
    return background(lerp(color, Color.Transparent.rc, 1f.rf))
}
