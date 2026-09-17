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

package androidx.glance.adaptive.appwidget.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlin.math.ceil

/** Ratio of icon edge to ring edge, from the design's 30.5 dp glyph inside a 56 dp ring. */
private const val RING_ICON_RATIO = 0.545f

/**
 * Ratio of icon edge to ring edge inside the contained ring, from the design's 16 dp glyph inside
 * an 80 dp ring.
 *
 * Far smaller than [RING_ICON_RATIO] because here the glyph shares the ring with the metric rather
 * than filling it alone.
 */
private const val CONTAINED_RING_ICON_RATIO = 0.2f

/** Opacity of the unfilled remainder of the progress ring. */
private const val RING_TRACK_ALPHA = 0.18f

/**
 * Type size of the unit label beside the headline metric.
 *
 * Shared rather than inlined because the metric's width budget has to deduct the label's run.
 */
internal const val TERTIARY_TEXT_SIZE_SP = 12f

/** Opacity of a chart bar that has not reached its goal. */
private const val BAR_MUTED_ALPHA = 0.35f

/**
 * Most children a Glance container translates to.
 *
 * `RemoteViews` layouts are pre-generated, so a `Row` or `Column` past this limit is truncated —
 * silently, with only an `E/GlanceAppWidget` log to show for it. See [chunkBars].
 */
private const val MAX_CONTAINER_CHILDREN = 10

/**
 * Ring thickness as a fraction of its diameter, from the design's 4 dp stroke on a 56 dp ring.
 *
 * The contained ring at the narrowest breakpoint is drawn far larger but keeps the same ratio, so
 * the stroke is derived rather than fixed.
 */
private const val RING_STROKE_RATIO = 4f / 56f

/** Fully rounded ends, matching the design's `rounded-[100px]` bars. */
private val BarCornerRadius = 100.dp

/** Gap between the contained ring's glyph and the metric beneath it. */
private val ContainedRingIconSpacing = 2.dp

/** Space between a chart bar and its axis label. */
private val BarLabelSpacing = 4.dp

/** Gap between the chart's bars and its description row. */
private val ChartDescriptionSpacing = 8.dp

/** Shortest bar drawn, so that a zero value still reads as a bar rather than as missing data. */
private val MinBarHeight = 4.dp

/**
 * One column of the Track hero chart.
 *
 * @param label Axis label, e.g. a weekday initial.
 * @param value Bar height as a fraction of the tallest bar, in `[0.0, 1.0]`.
 * @param emphasized Whether the bar met its goal and takes the accent color.
 */
internal data class TrackBar(
    val label: String,
    @FloatRange(from = 0.0, to = 1.0) val value: Float,
    val emphasized: Boolean = false,
)

/**
 * Determinate progress ring with an optional glyph at its center — the design's
 * `ContentBlock_progress_ring`.
 *
 * The arc is rasterized (see [createProgressRingBitmap]) because Glance 1.1.1 has no determinate
 * circular indicator. The bitmap is cached against every input that affects it, so a recomposition
 * that changes nothing reuses the previous allocation instead of redrawing and re-marshalling it.
 *
 * @param progress Normalized progress in `[0.0, 1.0]`, or `null` to draw an empty ring.
 * @param modifier Modifier applied to the ring.
 * @param size Edge length of the ring.
 * @param iconRes Optional glyph drawn at the center, tinted with the primary color.
 */
@Composable
internal fun ProgressRingBlock(
    @FloatRange(from = 0.0, to = 1.0) progress: Float?,
    modifier: GlanceModifier = GlanceModifier,
    size: Dp = 56.dp,
    @DrawableRes iconRes: Int? = null,
) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val primary = GlanceTheme.colors.primary.getColor(context)
    val containerColor = GlanceTheme.colors.secondaryContainer.getColor(context).toArgb()
    val progressColor = primary.toArgb()
    val trackColor = primary.copy(alpha = RING_TRACK_ALPHA).toArgb()
    val sizePx = (size.value * density).toInt()
    val strokePx = size.value * RING_STROKE_RATIO * density

    val bitmap =
        remember(sizePx, strokePx, progress, trackColor, progressColor, containerColor) {
            createProgressRingBitmap(
                sizePx = sizePx,
                strokeWidthPx = strokePx,
                progress = progress,
                trackColor = trackColor,
                progressColor = progressColor,
                containerColor = containerColor,
            )
        }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = null,
            modifier = GlanceModifier.size(size),
        )
        if (iconRes != null) {
            Image(
                provider = ImageProvider(iconRes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                modifier = GlanceModifier.size(size * RING_ICON_RATIO),
            )
        }
    }
}

/**
 * The whole widget as one progress ring — the design's `Progress.Ring_Contained`.
 *
 * At the shortest narrow breakpoint there is no room to stack a ring above a metric, so the design
 * drops the card and puts the metric *inside* a ring drawn at the container's full width. That
 * makes this a different composition from [ProgressRingBlock] rather than a size of it: here the
 * ring is the background and the content sits over it, so the two are kept separate.
 *
 * @param progress Normalized progress in `[0.0, 1.0]`, or `null` to draw an empty ring.
 * @param value Headline metric drawn inside the ring.
 * @param size Outer diameter of the ring.
 * @param fontSize Type size of [value].
 * @param modifier Modifier applied to the ring.
 * @param iconRes Optional glyph drawn above [value], tinted with the primary color.
 */
@Composable
internal fun ContainedRingBlock(
    @FloatRange(from = 0.0, to = 1.0) progress: Float?,
    value: String,
    size: Dp,
    fontSize: TextUnit,
    modifier: GlanceModifier = GlanceModifier,
    @DrawableRes iconRes: Int? = null,
) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val primary = GlanceTheme.colors.primary.getColor(context)
    val containerColor = GlanceTheme.colors.widgetBackground.getColor(context).toArgb()
    val progressColor = primary.toArgb()
    val trackColor = primary.copy(alpha = RING_TRACK_ALPHA).toArgb()
    val sizePx = (size.value * density).toInt()
    val strokePx = size.value * RING_STROKE_RATIO * density

    val bitmap =
        remember(sizePx, strokePx, progress, trackColor, progressColor, containerColor) {
            createProgressRingBitmap(
                sizePx = sizePx,
                strokeWidthPx = strokePx,
                progress = progress,
                trackColor = trackColor,
                progressColor = progressColor,
                containerColor = containerColor,
            )
        }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = null,
            modifier = GlanceModifier.size(size),
        )
        Column(horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
            if (iconRes != null) {
                Image(
                    provider = ImageProvider(iconRes),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                    modifier = GlanceModifier.size(size * CONTAINED_RING_ICON_RATIO),
                )
                Spacer(GlanceModifier.height(ContainedRingIconSpacing))
            }
            Text(
                text = value,
                maxLines = 1,
                style =
                    TextStyle(
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.primary,
                        textAlign = TextAlign.Center,
                    ),
            )
        }
    }
}

/**
 * The headline metric — the design's `ContentBlock_primary_text`.
 *
 * @param value Text to display, truncated to a single line.
 * @param fontSize Type size, which grows with the width breakpoint.
 * @param modifier Modifier applied to the text.
 */
@Composable
internal fun PrimaryValueBlock(
    value: String,
    fontSize: TextUnit,
    modifier: GlanceModifier = GlanceModifier,
) {
    Text(
        text = value,
        maxLines = 1,
        modifier = modifier,
        style =
            TextStyle(
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = GlanceTheme.colors.primary,
            ),
    )
}

/**
 * The trailing unit label beside the headline metric — the design's `ContentBlock_tertiary_text`.
 *
 * @param label Text to display, truncated to a single line.
 * @param modifier Modifier applied to the text.
 */
@Composable
internal fun TertiaryLabelBlock(label: String, modifier: GlanceModifier = GlanceModifier) {
    Text(
        text = label,
        maxLines = 1,
        modifier = modifier,
        style =
            TextStyle(
                fontSize = TERTIARY_TEXT_SIZE_SP.sp,
                fontWeight = FontWeight.Medium,
                color = GlanceTheme.colors.primary,
                textAlign = TextAlign.Center,
            ),
    )
}

/**
 * One value in the bottom supporting row — the design's `ContentBlock_additional_*`.
 *
 * @param value Text to display, truncated to a single line.
 * @param color Text color; the design alternates between a muted and an accented value.
 * @param modifier Modifier applied to the text.
 */
@Composable
internal fun SupportingValueBlock(
    value: String,
    color: ColorProvider,
    modifier: GlanceModifier = GlanceModifier,
) {
    Text(
        text = value,
        maxLines = 1,
        modifier = modifier,
        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color),
    )
}

/**
 * Outlined pill flanked by status dots — the design's `Special_badge`.
 *
 * Glance has no border modifier, so the outline is a filled rounded container with an inset
 * background-colored container on top of it.
 *
 * @param label Text inside the pill.
 * @param outlineColor Color of the pill outline.
 * @param dotColors Colors of the dots drawn either side of the pill.
 * @param modifier Modifier applied to the badge.
 */
@Composable
internal fun SpecialBadgeBlock(
    label: String,
    outlineColor: ColorProvider,
    dotColors: List<ColorProvider>,
    modifier: GlanceModifier = GlanceModifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.Vertical.CenterVertically) {
        // The design puts one dot before the pill and the remainder after it.
        if (dotColors.isNotEmpty()) {
            StatusDot(dotColors[0])
            Spacer(GlanceModifier.width(2.dp))
        }
        Box(
            modifier = GlanceModifier.background(outlineColor).cornerRadius(BarCornerRadius),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    GlanceModifier.padding(2.dp)
                        .background(GlanceTheme.colors.widgetBackground)
                        .cornerRadius(BarCornerRadius)
                        .padding(horizontal = 8.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    maxLines = 1,
                    style =
                        TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.primary,
                        ),
                )
            }
        }
        for (i in 1 until dotColors.size) {
            Spacer(GlanceModifier.width(2.dp))
            StatusDot(dotColors[i])
        }
    }
}

/** A single 7x9 dp rounded status dot from [SpecialBadgeBlock]. */
@Composable
private fun StatusDot(color: ColorProvider) {
    Spacer(GlanceModifier.width(7.dp).height(9.dp).cornerRadius(BarCornerRadius).background(color))
}

/**
 * The hero bar chart — the design's `ContentBlock_chart` / `Progress.BarChart`.
 *
 * Two constraints shape this layout, both of them Glance's:
 * - **Bars cannot be bottom-aligned by gravity.** `verticalAlignment = Bottom` on a column that
 *   fills its parent's height is dropped in translation to `RemoteViews`, which left every bar
 *   top-aligned and each axis label at whatever baseline its own bar happened to end on. Instead
 *   each column reserves its unfilled remainder as an explicit [headroom][barMaxHeight] spacer
 *   above the bar. Every column is then exactly as tall as every other, so the bars sit on a shared
 *   floor and the labels on a shared baseline, without depending on alignment at all.
 * - **A container holds at most ten children, and silently drops the rest.** Columns are therefore
 *   [chunked][chunkBars] into nested rows instead of being emitted into one flat row.
 *
 * @param bars Chart columns, laid out with equal widths.
 * @param barMaxHeight Height of a bar at `value == 1.0`.
 * @param barSpacing Gap between adjacent bars.
 * @param showLabels Whether the axis label row is drawn beneath the bars.
 * @param description Optional leading/trailing caption pair drawn under the chart.
 * @param modifier Modifier applied to the chart.
 */
@Composable
internal fun BarChartBlock(
    bars: List<TrackBar>,
    barMaxHeight: Dp,
    barSpacing: Dp,
    showLabels: Boolean,
    description: Pair<String, String>?,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    val mutedBar =
        ColorProvider(GlanceTheme.colors.primary.getColor(context).copy(alpha = BAR_MUTED_ALPHA))
    val chunks = remember(bars) { chunkBars(bars) }

    Column(
        modifier = modifier,
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.Bottom,
        ) {
            for (chunkIndex in chunks.indices) {
                val chunk = chunks[chunkIndex]
                Row(
                    modifier = GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.Vertical.Bottom,
                ) {
                    for (indexInChunk in chunk.indices) {
                        val bar = chunk[indexInChunk]
                        // Spacing is padding rather than interleaved spacers, which would double
                        // the child count for no benefit. Only the chart's outer edges go flush.
                        // Chunks are equally sized, so the position in the chart follows from the
                        // position in the chunk. The last chunk can be padded with nulls, so the
                        // flush right edge is the end of the last chunk, not the last bar.
                        val index = chunkIndex * chunk.size + indexInChunk
                        val isLastColumn =
                            chunkIndex == chunks.lastIndex && indexInChunk == chunk.lastIndex
                        val columnModifier =
                            GlanceModifier.defaultWeight()
                                .padding(
                                    start = if (index == 0) 0.dp else barSpacing / 2,
                                    end = if (isLastColumn) 0.dp else barSpacing / 2,
                                )
                        if (bar == null) {
                            // Padding out an uneven final chunk. Chunks share the width equally, so
                            // without this the last chunk's bars would be wider than the rest.
                            Spacer(columnModifier)
                        } else {
                            BarColumn(
                                bar = bar,
                                barMaxHeight = barMaxHeight,
                                showLabel = showLabels,
                                mutedColor = mutedBar,
                                modifier = columnModifier,
                            )
                        }
                    }
                }
            }
        }
        if (description != null) {
            Spacer(GlanceModifier.height(ChartDescriptionSpacing))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                ChartCaption(description.first)
                Spacer(GlanceModifier.defaultWeight())
                ChartCaption(description.second)
            }
        }
    }
}

/**
 * One chart column: headroom, then the bar, then its axis label.
 *
 * The headroom is what makes the column a fixed [barMaxHeight] tall regardless of [bar]'s value.
 * See [BarChartBlock] for why that is done here rather than with an alignment.
 */
@Composable
private fun BarColumn(
    bar: TrackBar,
    barMaxHeight: Dp,
    showLabel: Boolean,
    mutedColor: ColorProvider,
    modifier: GlanceModifier = GlanceModifier,
) {
    val filled = (barMaxHeight * bar.value.coerceIn(0f, 1f)).coerceAtLeast(MinBarHeight)
    val headroom = (barMaxHeight - filled).coerceAtLeast(0.dp)

    Column(modifier = modifier, horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
        Spacer(GlanceModifier.height(headroom))
        Spacer(
            GlanceModifier.fillMaxWidth()
                .height(filled)
                .cornerRadius(BarCornerRadius)
                .background(if (bar.emphasized) GlanceTheme.colors.tertiary else mutedColor)
        )
        if (showLabel) {
            Spacer(GlanceModifier.height(BarLabelSpacing))
            Text(
                text = bar.label,
                maxLines = 1,
                style =
                    TextStyle(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    ),
            )
        }
    }
}

/**
 * Splits [bars] into balanced groups small enough to nest inside a Glance container.
 *
 * A Glance `Row` or `Column` translates to at most [MAX_CONTAINER_CHILDREN] children and drops any
 * beyond that with nothing but a log line, so a chart of more than ten columns cannot be emitted
 * flat. Nesting lifts the ceiling to [MAX_CONTAINER_CHILDREN] squared.
 *
 * The groups are balanced rather than greedily filled — fourteen bars give two rows of seven, not a
 * row of ten and a row of four — because each group claims an equal share of the width. For the
 * same reason an uneven remainder is padded with `null`, which [BarChartBlock] renders as an
 * invisible column so that every bar keeps the same width.
 */
@VisibleForTesting
internal fun chunkBars(bars: List<TrackBar>): List<List<TrackBar?>> {
    if (bars.isEmpty()) return emptyList()
    val maxVisible = MAX_CONTAINER_CHILDREN * MAX_CONTAINER_CHILDREN
    val visibleCount = if (bars.size > maxVisible) maxVisible else bars.size
    val chunkCount = ceil(visibleCount / MAX_CONTAINER_CHILDREN.toFloat()).toInt()
    val perChunk = ceil(visibleCount / chunkCount.toFloat()).toInt()
    val chunks = ArrayList<List<TrackBar?>>(chunkCount)
    for (chunkIndex in 0 until chunkCount) {
        val chunk = ArrayList<TrackBar?>(perChunk)
        for (indexInChunk in 0 until perChunk) {
            val index = chunkIndex * perChunk + indexInChunk
            // An uneven remainder pads with null, which BarChartBlock renders as an invisible
            // column so that every bar keeps the same width.
            chunk.add(if (index < visibleCount) bars[index] else null)
        }
        chunks.add(chunk)
    }
    return chunks
}

/** One end of the chart's description row. */
@Composable
private fun ChartCaption(text: String) {
    Text(
        text = text,
        maxLines = 1,
        style =
            TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = GlanceTheme.colors.primary,
            ),
    )
}
