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

import androidx.annotation.RestrictTo
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemotePageIndicatorDefaults {
    /** Default color of the selected page indicator. */
    public val selectedColor: RemoteColor
        @Composable @RemoteComposable get() = RemoteMaterialTheme.colorScheme.onSurface

    /** Default color of unselected page indicators. */
    public val unselectedColor: RemoteColor
        @Composable @RemoteComposable get() = RemoteMaterialTheme.colorScheme.onSurfaceVariant

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
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RemoteComposable
@Composable
public fun RemoteHorizontalPageIndicator(
    state: RemotePageIndicatorState,
    modifier: RemoteModifier = RemoteModifier,
    selectedColor: RemoteColor = RemotePageIndicatorDefaults.selectedColor,
    unselectedColor: RemoteColor = RemotePageIndicatorDefaults.unselectedColor,
    indicatorRadius: RemoteDp = RemotePageIndicatorDefaults.indicatorRadius,
    padding: RemoteDp = RemotePageIndicatorDefaults.pageIndicatorPadding,
) {
    RemotePageIndicatorImpl(
        state = state,
        isHorizontal = true,
        modifier = modifier,
        selectedColor = selectedColor,
        unselectedColor = unselectedColor,
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
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RemoteComposable
@Composable
public fun RemoteVerticalPageIndicator(
    state: RemotePageIndicatorState,
    modifier: RemoteModifier = RemoteModifier,
    selectedColor: RemoteColor = RemotePageIndicatorDefaults.selectedColor,
    unselectedColor: RemoteColor = RemotePageIndicatorDefaults.unselectedColor,
    indicatorRadius: RemoteDp = RemotePageIndicatorDefaults.indicatorRadius,
    padding: RemoteDp = RemotePageIndicatorDefaults.pageIndicatorPadding,
) {
    RemotePageIndicatorImpl(
        state = state,
        isHorizontal = false,
        modifier = modifier,
        selectedColor = selectedColor,
        unselectedColor = unselectedColor,
        indicatorRadius = indicatorRadius,
        padding = padding,
    )
}

@Suppress("RestrictedApiAndroidX")
@RemoteComposable
@Composable
private fun RemotePageIndicatorImpl(
    state: RemotePageIndicatorState,
    isHorizontal: Boolean,
    modifier: RemoteModifier = RemoteModifier,
    selectedColor: RemoteColor,
    unselectedColor: RemoteColor,
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
            radius = indicatorRadius.toPx(),
            padding = padding.toPx(),
            spacingPx = spacingPx,
            isHorizontal = isHorizontal,
        )
    }
}

private const val PI_FLOAT = 3.1415927f

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
    radius: RemoteFloat,
    padding: RemoteFloat,
    spacingPx: RemoteFloat,
    isHorizontal: Boolean,
) {
    if (pageCount <= 0) return

    val screenRadius = width / 2f.rf
    val bigRadius = screenRadius - padding

    if (pageCount == 1) {
        val paint = RemotePaint {
            this.color = selectedColor
            style = PaintingStyle.Fill
        }
        val centerAngleRad = (if (isHorizontal) 90f else 0f) * (PI_FLOAT / 180f)
        val x = width / 2f.rf + bigRadius * cos(centerAngleRad.rf)
        val y = height / 2f.rf + bigRadius * sin(centerAngleRad.rf)
        drawCircleIndicator(paint, RemoteOffset(x, y), radius)
        return
    }

    val offset = RemoteOffset(width / 2f.rf - bigRadius, height / 2f.rf - bigRadius)
    val arcSize = RemoteSize(bigRadius * 2f.rf, bigRadius * 2f.rf)

    val stepDirection = if (isHorizontal) -1f else 1f
    val spacerSizePx = radius * 2f.rf + spacingPx
    val spacerAngleRad = spacerSizePx / bigRadius
    val spacerAngleDegrees = spacerAngleRad * (180f / PI_FLOAT).rf

    val windowActivePage: RemoteFloat
    val shift: RemoteFloat
    val startAngle: RemoteFloat

    if (pageCount <= 6) {
        val spanDegrees = spacerAngleDegrees * (pageCount - 1).toFloat().rf
        startAngle = if (isHorizontal) 90f.rf + spanDegrees / 2f.rf else 0f.rf - spanDegrees / 2f.rf
        windowActivePage = currentPage
        shift = 0f.rf

        val inactivePaint = RemotePaint { style = PaintingStyle.Fill }
        for (i in 0 until pageCount) {
            val distance = abs(currentPage - i.toFloat().rf)
            val fraction = distance.isLessThan(1f.rf).select(1f.rf - distance, 0f.rf)
            inactivePaint.color =
                unselectedColor.copy(alpha = lerp(unselectedColor.alpha, 0f.rf, fraction))

            val dotRadius = radius * (1f.rf - fraction)
            val angleRad =
                (startAngle + stepDirection.rf * spacerAngleDegrees * i.toFloat().rf) *
                    (PI_FLOAT / 180f).rf
            val x = width / 2f.rf + bigRadius * cos(angleRad)
            val y = height / 2f.rf + bigRadius * sin(angleRad)

            drawCircleIndicator(inactivePaint, RemoteOffset(x, y), dotRadius)
        }
    } else {
        val totalPages = pageCount
        val maxHidden = (totalPages - 6).toFloat()
        val continuousHidden = (currentPage - 4f.rf).coerceIn(0f.rf, maxHidden.rf)
        val floorHidden = floor(continuousHidden)
        shift = continuousHidden - floorHidden
        windowActivePage = currentPage - floorHidden
        startAngle =
            if (isHorizontal) 90f.rf + 2.5f.rf * spacerAngleDegrees
            else 0f.rf - 2.5f.rf * spacerAngleDegrees

        val inactivePaint = RemotePaint { style = PaintingStyle.Fill }
        for (i in 0..6) {
            val pageIndex = floorHidden + i.toFloat().rf
            val isValidPage = pageIndex.isLessThan(totalPages.toFloat().rf)

            val slotAlpha =
                when (i) {
                    0 -> 1f.rf - shift
                    6 -> shift
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
                        (floorHidden.isLessThan((totalPages - 6).toFloat().rf)).select(
                            lerp(0.66f.rf, 1f.rf, shift),
                            1f.rf,
                        )
                    6 ->
                        (floorHidden.isLessThan((totalPages - 7).toFloat().rf)).select(
                            0.66f.rf * shift,
                            shift,
                        )
                    else -> 1f.rf
                }

            val distance = abs(windowActivePage - i.toFloat().rf)
            val fraction = distance.isLessThan(1f.rf).select(1f.rf - distance, 0f.rf)
            inactivePaint.color =
                unselectedColor.copy(
                    alpha = lerp(unselectedColor.alpha * slotAlpha, 0f.rf, fraction)
                )

            val dotRadius = radius * slotSizeRatio * (1f.rf - fraction)
            val angleRad =
                (startAngle + stepDirection.rf * (i.toFloat().rf - shift) * spacerAngleDegrees) *
                    (PI_FLOAT / 180f).rf
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
