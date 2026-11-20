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
import android.content.res.Resources
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
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
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalResources
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
 * Provides dynamic Material You [RemoteColor] tokens shared by the Information Matrix components.
 *
 * Mirrors the RemoteViews matrix resources in `res/values-v31/remoteviews_colors.xml` and
 * `res/values/remoteviews_colors.xml`, encoding the light and dark `@android:color/system_*` tokens
 * directly into the document as themed [RemoteColor]s so the host resolves day/night and wallpaper
 * changes without recapturing.
 *
 * Workaround for `RemoteComposePlayer.setDocument()` running `applyDataOperations()` before
 * `ThemeSupport.mapColors()` and `CoreDocument` painting ellipsized `CoreText` on Frame 1 before
 * `VariableSupport` updates: on API 31+, the current dynamic system colors are baked in at capture
 * time as `lightFallback`/`darkFallback` so light-mode cold start and light/dark resize (when a
 * reused player already holds `mPaintTheme`) paint the active palette immediately. Dark-mode cold
 * start on a fresh player still reads `lightFallback` on Frame 1 because
 * `RemoteContext.mPaintTheme` defaults to `Theme.LIGHT`.
 */
@SuppressLint("InlinedApi")
internal object RemoteComposePalette {
    val primary: RemoteColor
        @Composable
        @ReadOnlyComposable
        get() =
            themedColor(
                lightColorRes = android.R.color.system_accent1_600,
                darkColorRes = android.R.color.system_accent1_200,
                lightFallback = Color(0xFF6750A4),
                darkFallback = Color(0xFFD0BCFF),
            )

    val tertiary: RemoteColor
        @Composable
        @ReadOnlyComposable
        get() =
            themedColor(
                lightColorRes = android.R.color.system_accent3_600,
                darkColorRes = android.R.color.system_accent3_200,
                lightFallback = Color(0xFF7D5260),
                darkFallback = Color(0xFFEFB8C8),
            )

    val onSurfaceVariant: RemoteColor
        @Composable
        @ReadOnlyComposable
        get() =
            themedColor(
                lightColorRes = android.R.color.system_neutral2_700,
                darkColorRes = android.R.color.system_neutral2_200,
                lightFallback = Color(0xFF49454F),
                darkFallback = Color(0xFFCAC4D0),
            )

    val widgetBackground: RemoteColor
        @Composable
        @ReadOnlyComposable
        get() =
            themedColor(
                lightColorRes = android.R.color.system_accent2_50,
                darkColorRes = android.R.color.system_accent2_800,
                lightFallback = Color(0xFFE0F3FF),
                darkFallback = Color(0xFF20333D),
            )

    val primaryContainer: RemoteColor
        @Composable
        @ReadOnlyComposable
        get() =
            themedColor(
                lightColorRes = android.R.color.system_accent1_100,
                darkColorRes = android.R.color.system_accent1_700,
                lightFallback = Color(0xFFEADDFF),
                darkFallback = Color(0xFF4F378B),
            )

    val onPrimaryContainer: RemoteColor
        @Composable
        @ReadOnlyComposable
        get() =
            themedColor(
                lightColorRes = android.R.color.system_accent1_700,
                darkColorRes = android.R.color.system_accent1_100,
                lightFallback = Color(0xFF4F378B),
                darkFallback = Color(0xFFEADDFF),
            )

    val secondaryContainer: RemoteColor
        @Composable
        @ReadOnlyComposable
        get() =
            themedColor(
                lightColorRes = android.R.color.system_accent2_100,
                darkColorRes = android.R.color.system_accent2_700,
                lightFallback = Color(0xFFE8DEF8),
                darkFallback = Color(0xFF4A4458),
            )

    val onSecondaryContainer: RemoteColor
        @Composable
        @ReadOnlyComposable
        get() =
            themedColor(
                lightColorRes = android.R.color.system_accent2_700,
                darkColorRes = android.R.color.system_accent2_100,
                lightFallback = Color(0xFF4A4458),
                darkFallback = Color(0xFFE8DEF8),
            )

    val surfaceContainerHighest: RemoteColor
        @Composable
        @ReadOnlyComposable
        get() =
            themedColor(
                lightColorRes = android.R.color.system_neutral1_100,
                darkColorRes = android.R.color.system_neutral1_800,
                lightFallback = Color(0xFFE6E0E9),
                darkFallback = Color(0xFF36343B),
            )

    val barMuted: RemoteColor
        @Composable @ReadOnlyComposable get() = primary.copy(alpha = BAR_MUTED_ALPHA.rf)

    val ringTrack: RemoteColor
        @Composable @ReadOnlyComposable get() = primary.copy(alpha = RingDefaults.TrackAlpha.rf)

    val supportingText: RemoteColor
        @Composable
        @ReadOnlyComposable
        get() = lerp(widgetBackground, primary, SUPPORTING_TEXT_ALPHA.rf)

    val tileSubtitle: RemoteColor
        @Composable
        @ReadOnlyComposable
        get() = lerp(primaryContainer, onPrimaryContainer, TILE_SUBTITLE_ALPHA.rf)

    @Composable
    @ReadOnlyComposable
    private fun themedColor(
        @ColorRes lightColorRes: Int,
        @ColorRes darkColorRes: Int,
        lightFallback: Color,
        darkFallback: Color,
    ): RemoteColor {
        val resources = LocalResources.current
        return RemoteColor.createThemedRemoteColor(
            lightColorRes = lightColorRes,
            darkColorRes = darkColorRes,
            lightFallback = resolveSystemColorOrFallback(resources, lightColorRes, lightFallback),
            darkFallback = resolveSystemColorOrFallback(resources, darkColorRes, darkFallback),
        )
    }

    // TODO(b/586433578): Remove dynamic fallback resolution once the platform-bundled launcher
    //  RemoteComposePlayer resolves ColorTheme before Frame 1.
    @ColorInt
    private fun resolveSystemColorOrFallback(
        resources: Resources,
        @ColorRes colorRes: Int,
        fallback: Color,
    ): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return fallback.toArgb()
        return try {
            resources.getColor(colorRes, null)
        } catch (_: Resources.NotFoundException) {
            // Non-AOSP or Robolectric resource tables may omit system Material You color entries.
            fallback.toArgb()
        }
    }
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
