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

import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.abs
import androidx.compose.remote.creation.compose.state.cos
import androidx.compose.remote.creation.compose.state.floor
import androidx.compose.remote.creation.compose.state.lerp
import androidx.compose.remote.creation.compose.state.max
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.sin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap

/** An interface for connection between Pager and remote page indicators. */
public interface RemotePageIndicatorState {
    /** Total number of pages. */
    public val pageCount: Int

    /** The currently selected page index. */
    public val selectedPage: RemoteInt

    /** The current offset from the start of the selected page, as a ratio of the page width. */
    public val pageOffset: RemoteFloat
}

/**
 * Default implementation of [RemotePageIndicatorState].
 *
 * @property selectedPage The currently selected page index as a [RemoteInt].
 * @property pageCount Total number of pages.
 * @property pageOffset The current offset from the start of the selected page, as a ratio of the
 *   page width.
 */
public class DefaultRemotePageIndicatorState(
    override val pageCount: Int,
    override val selectedPage: RemoteInt = 0.ri,
    override val pageOffset: RemoteFloat = 0f.rf,
) : RemotePageIndicatorState

/**
 * Creates and remembers a [RemotePageIndicatorState].
 *
 * @param selectedPage The currently selected page index.
 * @param pageCount Total number of pages.
 * @param pageOffset The current offset from the start of the selected page, as a ratio of the page
 *   width.
 */
@Composable
public fun rememberRemotePageIndicatorState(
    pageCount: Int,
    selectedPage: RemoteInt = 0.ri,
    pageOffset: RemoteFloat = 0f.rf,
): RemotePageIndicatorState {
    return remember(selectedPage, pageOffset, pageCount) {
        DefaultRemotePageIndicatorState(
            selectedPage = selectedPage,
            pageCount = pageCount,
            pageOffset = pageOffset,
        )
    }
}

/** Contains default values used by remote page indicators. */
public object RemotePageIndicatorDefaults {
    /** Default color of the selected page indicator. */
    public val selectedColor: RemoteColor
        @Composable @RemoteComposable get() = RemoteMaterialTheme.colorScheme.onSurface

    /** Default color of unselected page indicators. */
    public val unselectedColor: RemoteColor
        @Composable @RemoteComposable get() = RemoteMaterialTheme.colorScheme.onSurfaceVariant

    /** Default color of the indicator background. */
    public val backgroundColor: RemoteColor
        @Composable
        @RemoteComposable
        get() = RemoteMaterialTheme.colorScheme.background.copy(alpha = 0.85f.rf)

    /** Default radius of each indicator dot. */
    public val indicatorRadius: RemoteDp = 3.rdp

    /**
     * Default padding of the page indicator from the edge of the screen (8dp aligns with standard
     * Compose edgePadding + background offset).
     */
    public val pageIndicatorPadding: RemoteDp = 8.rdp
}

/**
 * A horizontal page indicator curved along the bottom edge of the screen.
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteHorizontalPageIndicatorSample
 * @param state The state object of the page indicator.
 * @param modifier The modifier to be applied to the layout.
 * @param selectedColor The color of the selected page indicator.
 * @param unselectedColor The color of unselected page indicators.
 * @param indicatorRadius The base radius of each indicator dot.
 * @param padding The padding of the indicator from the bottom edge of the screen.
 * @param backgroundColor The color of the page indicator background.
 */
@RemoteComposable
@Composable
public fun RemoteHorizontalPageIndicator(
    state: RemotePageIndicatorState,
    modifier: RemoteModifier = RemoteModifier,
    selectedColor: RemoteColor = RemotePageIndicatorDefaults.selectedColor,
    unselectedColor: RemoteColor = RemotePageIndicatorDefaults.unselectedColor,
    indicatorRadius: RemoteDp = RemotePageIndicatorDefaults.indicatorRadius,
    padding: RemoteDp = RemotePageIndicatorDefaults.pageIndicatorPadding,
    backgroundColor: RemoteColor = RemotePageIndicatorDefaults.backgroundColor,
) {
    RemotePageIndicatorImpl(
        state = state,
        isHorizontal = true,
        modifier = modifier,
        selectedColor = selectedColor,
        unselectedColor = unselectedColor,
        backgroundColor = backgroundColor,
        indicatorRadius = indicatorRadius,
        padding = padding,
    )
}

/**
 * A vertical page indicator curved along the right edge of the screen.
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteVerticalPageIndicatorSample
 * @param state The state object of the page indicator.
 * @param modifier The modifier to be applied to the layout.
 * @param selectedColor The color of the selected page indicator.
 * @param unselectedColor The color of unselected page indicators.
 * @param indicatorRadius The base radius of each indicator dot.
 * @param padding The padding of the indicator from the right edge of the screen.
 * @param backgroundColor The color of the page indicator background.
 */
@RemoteComposable
@Composable
public fun RemoteVerticalPageIndicator(
    state: RemotePageIndicatorState,
    modifier: RemoteModifier = RemoteModifier,
    selectedColor: RemoteColor = RemotePageIndicatorDefaults.selectedColor,
    unselectedColor: RemoteColor = RemotePageIndicatorDefaults.unselectedColor,
    indicatorRadius: RemoteDp = RemotePageIndicatorDefaults.indicatorRadius,
    padding: RemoteDp = RemotePageIndicatorDefaults.pageIndicatorPadding,
    backgroundColor: RemoteColor = RemotePageIndicatorDefaults.backgroundColor,
) {
    RemotePageIndicatorImpl(
        state = state,
        isHorizontal = false,
        modifier = modifier,
        selectedColor = selectedColor,
        unselectedColor = unselectedColor,
        backgroundColor = backgroundColor,
        indicatorRadius = indicatorRadius,
        padding = padding,
    )
}

@RemoteComposable
@Composable
private fun RemotePageIndicatorImpl(
    state: RemotePageIndicatorState,
    isHorizontal: Boolean,
    modifier: RemoteModifier = RemoteModifier,
    selectedColor: RemoteColor,
    unselectedColor: RemoteColor,
    backgroundColor: RemoteColor,
    indicatorRadius: RemoteDp,
    padding: RemoteDp,
) {
    val rawPage = state.selectedPage.toRemoteFloat() + state.pageOffset
    val spacingPx = 4.rdp.toPx()
    RemoteCanvas(modifier = modifier.fillMaxSize()) {
        drawPageIndicators(
            pageCount = state.pageCount,
            currentPage = rawPage,
            selectedColor = selectedColor,
            unselectedColor = unselectedColor,
            backgroundColor = backgroundColor,
            radius = indicatorRadius.toPx(),
            padding = padding.toPx(),
            spacingPx = spacingPx,
            isHorizontal = isHorizontal,
        )
    }
}

private const val PI_FLOAT = 3.1415927f
private const val MAX_PAGES_ON_SCREEN = 6

private fun RemoteFloat.coerceIn(min: RemoteFloat, max: RemoteFloat): RemoteFloat {
    val temp = this.isLessThan(min).select(min, this)
    return max.isLessThan(temp).select(max, temp)
}

/**
 * Draws a circle using public [RemoteDrawScope.drawRoundRect] API to avoid shadowed restricted
 * member.
 */
private fun RemoteDrawScope.drawCircleIndicator(
    paint: RemotePaint?,
    center: RemoteOffset,
    radius: RemoteFloat,
) {
    val size = radius * 2f.rf
    drawRoundRect(
        paint = paint,
        topLeft = RemoteOffset(center.x - radius, center.y - radius),
        size = RemoteSize(size, size),
        cornerRadius = RemoteOffset(radius, radius),
    )
}

private fun RemoteDrawScope.drawPageIndicators(
    pageCount: Int,
    currentPage: RemoteFloat,
    selectedColor: RemoteColor,
    unselectedColor: RemoteColor,
    backgroundColor: RemoteColor,
    radius: RemoteFloat,
    padding: RemoteFloat,
    spacingPx: RemoteFloat,
    isHorizontal: Boolean,
) {
    if (pageCount <= 0) return

    val screenRadius = width / 2f.rf
    val bigRadius = max(screenRadius - padding, 1f.rf)
    val backgroundPaddingPx = 3.rdp.toPx()
    val backgroundStrokeWidthPx = backgroundPaddingPx * 2f.rf + radius * 2f.rf

    if (pageCount == 1) {
        val centerAngleRad = (if (isHorizontal) 90f else 0f) * (PI_FLOAT / 180f)
        val x = width / 2f.rf + bigRadius * cos(centerAngleRad.rf)
        val y = height / 2f.rf + bigRadius * sin(centerAngleRad.rf)
        val bgPaint = RemotePaint {
            this.color = backgroundColor
            style = PaintingStyle.Fill
        }
        drawCircleIndicator(bgPaint, RemoteOffset(x, y), backgroundStrokeWidthPx / 2f.rf)
        val paint = RemotePaint {
            this.color = selectedColor
            style = PaintingStyle.Fill
        }
        drawCircleIndicator(paint, RemoteOffset(x, y), radius)
        return
    }

    val offset = RemoteOffset(width / 2f.rf - bigRadius, height / 2f.rf - bigRadius)
    val arcSize = RemoteSize(bigRadius * 2f.rf, bigRadius * 2f.rf)

    val stepDirection = if (isHorizontal) -1f else 1f
    val spacerSizePx = radius * 2f.rf + spacingPx
    val spacerAngleRad = spacerSizePx / bigRadius
    val spacerAngleDegrees = spacerAngleRad * (180f / PI_FLOAT).rf
    val shrinkThresholdStart = calculateShrinkThresholdStart(spacingPx, radius * 2f.rf)
    val shrinkThresholdEnd = calculateShrinkThresholdEnd(spacingPx, radius * 2f.rf)

    val pagesOnScreen = if (pageCount < MAX_PAGES_ON_SCREEN) pageCount else MAX_PAGES_ON_SCREEN
    val bgSpanDegrees = spacerAngleDegrees * (pagesOnScreen - 1).toFloat().rf
    val bgStartAngle =
        if (isHorizontal) 90f.rf - bgSpanDegrees / 2f.rf else 0f.rf - bgSpanDegrees / 2f.rf
    val bgPaint = RemotePaint {
        this.color = backgroundColor
        style = PaintingStyle.Stroke
        strokeWidth = backgroundStrokeWidthPx
        strokeCap = StrokeCap.Round
    }
    drawArc(
        paint = bgPaint,
        startAngle = bgStartAngle,
        sweepAngle = bgSpanDegrees,
        useCenter = false,
        topLeft = offset,
        size = arcSize,
    )

    val windowActivePage: RemoteFloat
    val shift: RemoteFloat
    val startAngle: RemoteFloat

    if (pageCount <= MAX_PAGES_ON_SCREEN) {
        val spanDegrees = spacerAngleDegrees * (pageCount - 1).toFloat().rf
        startAngle = if (isHorizontal) 90f.rf + spanDegrees / 2f.rf else 0f.rf - spanDegrees / 2f.rf
        windowActivePage = currentPage
        shift = 0f.rf

        val floorPage = floor(currentPage)
        val progression = currentPage - floorPage
        val inactivePaint = RemotePaint { style = PaintingStyle.Fill }
        for (i in 0 until pageCount) {
            val shrinkRatio =
                calculateAdjacentShrinkRatio(
                    dotIndex = i,
                    floorPage = floorPage,
                    progression = progression,
                    shrinkThresholdStart = shrinkThresholdStart,
                    shrinkThresholdEnd = shrinkThresholdEnd,
                )
            val alphaRatio = inverseLerp(0f.rf, 0.5f.rf, shrinkRatio)
            inactivePaint.color = unselectedColor.copy(alpha = unselectedColor.alpha * alphaRatio)

            val spacerOffsetRatio =
                calculateSpacerOffsetRatio(
                    dotIndex = i.toFloat().rf,
                    floorPage = floorPage,
                    progression = progression,
                    shrinkThresholdStart = shrinkThresholdStart,
                    shrinkThresholdEnd = shrinkThresholdEnd,
                )
            val dotRadius = radius * shrinkRatio
            val angleRad =
                (startAngle +
                    stepDirection.rf * spacerAngleDegrees * (i.toFloat().rf + spacerOffsetRatio)) *
                    (PI_FLOAT / 180f).rf

            val x = width / 2f.rf + bigRadius * cos(angleRad)
            val y = height / 2f.rf + bigRadius * sin(angleRad)

            drawCircleIndicator(inactivePaint, RemoteOffset(x, y), dotRadius)
        }
    } else {
        val totalPages = pageCount
        val maxHidden = (totalPages - MAX_PAGES_ON_SCREEN).toFloat()
        val continuousHidden = (currentPage - 4f.rf).coerceIn(0f.rf, maxHidden.rf)
        val floorHidden = floor(continuousHidden)
        shift = continuousHidden - floorHidden
        windowActivePage = currentPage - floorHidden
        startAngle =
            if (isHorizontal) 90f.rf + 2.5f.rf * spacerAngleDegrees
            else 0f.rf - 2.5f.rf * spacerAngleDegrees

        val floorPage = floor(windowActivePage)
        val progression = windowActivePage - floorPage
        val inactivePaint = RemotePaint { style = PaintingStyle.Fill }
        for (i in 0..MAX_PAGES_ON_SCREEN) {
            val pageIndex = floorHidden + i.toFloat().rf
            val isValidPage = pageIndex.isLessThan(totalPages.toFloat().rf)

            val slotAlpha =
                when (i) {
                    0 -> 1f.rf - shift
                    MAX_PAGES_ON_SCREEN -> shift
                    else -> 1f.rf
                }

            val slotSizeRatio =
                when (i) {
                    0 ->
                        (floorHidden.isLessThan(1f.rf)).select(
                            1f.rf - shift,
                            0.66f.rf * (1f.rf - shift),
                        )
                    1 -> lerp(1f.rf, 0.66f.rf, shift)
                    2,
                    3,
                    4 -> 1f.rf
                    5 ->
                        (floorHidden.isLessThan((totalPages - MAX_PAGES_ON_SCREEN).toFloat().rf))
                            .select(
                                lerp(0.66f.rf, 1f.rf, shift),
                                1f.rf,
                            )
                    MAX_PAGES_ON_SCREEN ->
                        (floorHidden.isLessThan(
                                (totalPages - (MAX_PAGES_ON_SCREEN + 1)).toFloat().rf
                            ))
                            .select(
                                0.66f.rf * shift,
                                shift,
                            )
                    else -> 1f.rf
                }

            val shrinkRatio =
                calculateAdjacentShrinkRatio(
                    dotIndex = i,
                    floorPage = floorPage,
                    progression = progression,
                    shrinkThresholdStart = shrinkThresholdStart,
                    shrinkThresholdEnd = shrinkThresholdEnd,
                )
            val finalSizeRatio = slotSizeRatio * shrinkRatio
            val alphaRatio = inverseLerp(0f.rf, 0.5f.rf, finalSizeRatio)
            inactivePaint.color =
                unselectedColor.copy(alpha = unselectedColor.alpha * slotAlpha * alphaRatio)

            val spacerOffsetRatio =
                calculateSpacerOffsetRatio(
                    dotIndex = i.toFloat().rf,
                    floorPage = floorPage,
                    progression = progression,
                    shrinkThresholdStart = shrinkThresholdStart,
                    shrinkThresholdEnd = shrinkThresholdEnd,
                )

            val dotRadius = radius * finalSizeRatio
            val angleRad =
                (startAngle +
                    stepDirection.rf *
                        (i.toFloat().rf - shift + spacerOffsetRatio) *
                        spacerAngleDegrees) * (PI_FLOAT / 180f).rf

            val x = width / 2f.rf + bigRadius * cos(angleRad)
            val y = height / 2f.rf + bigRadius * sin(angleRad)

            val slotPaint = RemotePaint {
                this.color = isValidPage.select(inactivePaint.color, Color.Transparent.rc)
                style = PaintingStyle.Fill
            }

            drawCircleIndicator(paint = slotPaint, center = RemoteOffset(x, y), radius = dotRadius)
        }
    }

    val floorPage = floor(windowActivePage)
    val progression = windowActivePage - floorPage
    val isProgressionLessThanHalf = progression.isLessThan(0.5f.rf)

    val wormStart =
        isProgressionLessThanHalf.select(floorPage, floorPage + 2f.rf * (progression - 0.5f.rf))
    val wormEnd =
        isProgressionLessThanHalf.select(floorPage + 2f.rf * progression, floorPage + 1f.rf)

    val a1 = startAngle + stepDirection.rf * (wormStart - shift) * spacerAngleDegrees
    val a2 = startAngle + stepDirection.rf * (wormEnd - shift) * spacerAngleDegrees

    val wormStartAngle = if (isHorizontal) a2 else a1
    val wormEndAngle = if (isHorizontal) a1 else a2
    val wormSweepAngle = wormEndAngle - wormStartAngle

    val degToRad = (PI_FLOAT / 180f).rf
    val startRad = wormStartAngle * degToRad
    val endRad = wormEndAngle * degToRad

    val activeFillPaint = RemotePaint {
        this.color = selectedColor
        style = PaintingStyle.Fill
    }

    val xStart = width / 2f.rf + bigRadius * cos(startRad)
    val yStart = height / 2f.rf + bigRadius * sin(startRad)
    drawCircleIndicator(activeFillPaint, RemoteOffset(xStart, yStart), radius)

    val xEnd = width / 2f.rf + bigRadius * cos(endRad)
    val yEnd = height / 2f.rf + bigRadius * sin(endRad)
    drawCircleIndicator(activeFillPaint, RemoteOffset(xEnd, yEnd), radius)

    val activePaint = RemotePaint {
        this.color = selectedColor
        style = PaintingStyle.Stroke
        strokeWidth = radius * 2f.rf
        strokeCap = StrokeCap.Round
    }

    drawArc(
        paint = activePaint,
        startAngle = wormStartAngle,
        sweepAngle = wormSweepAngle,
        useCenter = false,
        topLeft = offset,
        size = arcSize,
    )
}

private fun calculateShrinkThresholdStart(
    spacingPx: RemoteFloat,
    indicatorSizePx: RemoteFloat,
): RemoteFloat = spacingPx / max(0.001f.rf, spacingPx + indicatorSizePx) / 4f.rf

private fun calculateShrinkThresholdEnd(
    spacingPx: RemoteFloat,
    indicatorSizePx: RemoteFloat,
): RemoteFloat =
    (spacingPx / 2f.rf + indicatorSizePx) / max(0.001f.rf, spacingPx + indicatorSizePx) / 2f.rf

private fun inverseLerp(start: RemoteFloat, stop: RemoteFloat, value: RemoteFloat): RemoteFloat {
    return ((value - start) / max(0.001f.rf, stop - start)).coerceIn(0f.rf, 1f.rf)
}

private fun calculateAdjacentShrinkRatio(
    dotIndex: Int,
    floorPage: RemoteFloat,
    progression: RemoteFloat,
    shrinkThresholdStart: RemoteFloat,
    shrinkThresholdEnd: RemoteFloat,
): RemoteFloat {
    val isPrevDot = abs(dotIndex.toFloat().rf - floorPage).isLessThan(0.01f.rf)
    val isNextDot = abs(dotIndex.toFloat().rf - (floorPage + 1f.rf)).isLessThan(0.01f.rf)

    val prevShrink =
        1f.rf - inverseLerp(1f.rf - shrinkThresholdStart, 1f.rf - shrinkThresholdEnd, progression)
    val nextShrink = 1f.rf - inverseLerp(shrinkThresholdStart, shrinkThresholdEnd, progression)

    return isPrevDot.select(prevShrink, isNextDot.select(nextShrink, 1f.rf))
}

private fun calculateSpacerOffsetRatio(
    dotIndex: RemoteFloat,
    floorPage: RemoteFloat,
    progression: RemoteFloat,
    shrinkThresholdStart: RemoteFloat,
    shrinkThresholdEnd: RemoteFloat,
): RemoteFloat {
    val isPrevDot = abs(dotIndex - floorPage).isLessThan(0.01f.rf)
    val isNextDot = abs(dotIndex - (floorPage + 1f.rf)).isLessThan(0.01f.rf)

    val prevShrink =
        1f.rf - inverseLerp(1f.rf - shrinkThresholdStart, 1f.rf - shrinkThresholdEnd, progression)
    val nextShrink = 1f.rf - inverseLerp(shrinkThresholdStart, shrinkThresholdEnd, progression)

    val prevOffset = 0f.rf - (1f.rf - prevShrink) / 3f.rf
    val nextOffset = (1f.rf - nextShrink) / 3f.rf

    return isPrevDot.select(prevOffset, isNextDot.select(nextOffset, 0f.rf))
}
