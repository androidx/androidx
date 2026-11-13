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

package androidx.glance.adaptive.appwidget.components.badge

import androidx.annotation.ColorInt
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.shapes.RemoteCircleShape
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.asRdp
import androidx.compose.remote.creation.compose.state.max
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.glance.adaptive.appwidget.components.RemoteComposeFonts
import androidx.glance.adaptive.appwidget.components.RemoteComposePalette
import androidx.glance.adaptive.appwidget.components.SlotText
import androidx.glance.adaptive.appwidget.components.registerDynamicColor
import androidx.glance.adaptive.core.slots.Badge

/** Thickness of the badge pill's inset outline. */
private const val BADGE_OUTLINE_WIDTH_DP = 4f

/**
 * Vertical padding in SP inside the badge pill matching `TextView`'s default `includeFontPadding`
 * at `18sp` in RemoteViews (`~2.5sp` top and bottom), scaled with the active `fontScale` so the 4
 * dp inset outline frames the label cleanly at both default and large accessibility font scales.
 */
private const val BADGE_VERTICAL_FONT_PADDING_SP = 2.5f

/** Width of a status dot flanking the badge pill. */
private const val BADGE_DOT_WIDTH_DP = 7f

/** Height of a status dot flanking the badge pill. */
private const val BADGE_DOT_HEIGHT_DP = 8.8f

/** Horizontal gap separating status dots from the badge pill and from one another. */
private const val BADGE_DOT_GAP_DP = 1.4f

/**
 * Draws the badge for an action slot — the RemoteCompose counterpart of [Badge.buildRemoteViews].
 *
 * The 4 dp outline is drawn inside the pill's bounds behind the label via [drawWithContent] so it
 * frames the label without adding to the pill's height or overflowing its layout bounds. When
 * [Badge.outlineColor] is `null`, the outline uses [RemoteComposePalette.tertiary] so it follows
 * host theme changes dynamically. As in the design, the first status dot precedes the pill and the
 * remaining ones follow it.
 */
@RemoteComposable
@Composable
internal fun Badge.Content() {
    val fontScale = LocalResources.current.configuration.fontScale
    val resolvedOutline = outlineColor?.let { Color(it).rc } ?: RemoteComposePalette.tertiary
    RemoteRow(verticalAlignment = RemoteAlignment.CenterVertically) {
        if (dotColors.isNotEmpty()) {
            StatusDot(
                colorArgb = dotColors[0],
                modifier = RemoteModifier.padding(end = BADGE_DOT_GAP_DP.rdp),
            )
        }
        RemoteBox(
            modifier =
                RemoteModifier.registerDynamicColor(resolvedOutline)
                    .registerDynamicColor(RemoteComposePalette.widgetBackground)
                    .drawWithContent {
                        val inset = BADGE_OUTLINE_WIDTH_DP.rdp.toPx()
                        val outerRadius = height / 2f.rf
                        drawRoundRect(
                            paint = RemotePaint { color = resolvedOutline },
                            topLeft = RemoteOffset.Zero,
                            size = size,
                            cornerRadius = RemoteOffset(outerRadius, outerRadius),
                        )
                        val innerWidth = max(width - inset * 2f.rf, 0f.rf)
                        val innerHeight = max(height - inset * 2f.rf, 0f.rf)
                        val innerRadius = innerHeight / 2f.rf
                        drawRoundRect(
                            paint = RemotePaint { color = RemoteComposePalette.widgetBackground },
                            topLeft = RemoteOffset(inset, inset),
                            size = RemoteSize(innerWidth, innerHeight),
                            cornerRadius = RemoteOffset(innerRadius, innerRadius),
                        )
                        drawContent()
                    }
                    .padding(
                        horizontal = BadgeDefaults.HorizontalPadding.asRdp(),
                        vertical = (BADGE_VERTICAL_FONT_PADDING_SP * fontScale).rdp,
                    ),
            contentAlignment = RemoteAlignment.Center,
        ) {
            SlotText(
                text = label,
                fontSize = BadgeDefaults.LabelTextSize,
                fontWeight = RemoteComposeFonts.SemiBold,
                color = RemoteComposePalette.primary,
            )
        }
        for (index in 1 until dotColors.size) {
            StatusDot(
                colorArgb = dotColors[index],
                modifier = RemoteModifier.padding(start = BADGE_DOT_GAP_DP.rdp),
            )
        }
    }
}

/** One pill-shaped status dot flanking a [Badge]. */
@RemoteComposable
@Composable
private fun StatusDot(@ColorInt colorArgb: Int, modifier: RemoteModifier = RemoteModifier) {
    RemoteBox(
        modifier =
            modifier
                .size(width = BADGE_DOT_WIDTH_DP.rdp, height = BADGE_DOT_HEIGHT_DP.rdp)
                .clip(RemoteCircleShape)
                .background(Color(colorArgb).rc)
    )
}
