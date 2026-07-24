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

package androidx.xr.glimmer.pager

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import androidx.xr.glimmer.currentContentColor
import androidx.xr.glimmer.pager.GlimmerHorizontalPagerDefaults.UnselectedIndicatorAlpha

@Suppress("ModifierNodeInspectableProperties")
internal class PageIndicatorElement(
    private val state: GlimmerPagerState,
    private val selectedIndicatorColor: Color,
    private val unselectedIndicatorColor: Color,
) : ModifierNodeElement<PageIndicatorNode>() {
    override fun create(): PageIndicatorNode =
        PageIndicatorNode(state, selectedIndicatorColor, unselectedIndicatorColor)

    override fun update(node: PageIndicatorNode) {
        node.state = state
        node.selectedIndicatorColor = selectedIndicatorColor
        node.unselectedIndicatorColor = unselectedIndicatorColor
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PageIndicatorElement) return false

        if (state != other.state) return false
        if (selectedIndicatorColor != other.selectedIndicatorColor) return false
        if (unselectedIndicatorColor != other.unselectedIndicatorColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + selectedIndicatorColor.hashCode()
        result = 31 * result + unselectedIndicatorColor.hashCode()
        return result
    }
}

internal class PageIndicatorNode(
    var state: GlimmerPagerState,
    var selectedIndicatorColor: Color,
    var unselectedIndicatorColor: Color,
) : Modifier.Node(), LayoutModifierNode, DrawModifierNode {

    private var hiddenDotsToTheLeft: Int = 0

    override fun onDetach() {
        super.onDetach()
        hiddenDotsToTheLeft = 0
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val visibleDots = minOf(MaxNumberOfIndicators, state.pageCount)
        val minWidth =
            when {
                visibleDots <= 0 -> return layout(0, 0) {}
                visibleDots > 1 ->
                    (IndicatorCenterToCenter * visibleDots + SelectedIndicatorSize).roundToPx()
                else -> SelectedIndicatorSize.roundToPx()
            }
        val minHeight = SelectedIndicatorSize.roundToPx()
        val placeable =
            measurable.measure(
                constraints.copy(
                    minWidth = minWidth.coerceIn(constraints.minWidth, constraints.maxWidth),
                    minHeight = minHeight.coerceIn(constraints.minHeight, constraints.maxHeight),
                )
            )
        return layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
    }

    override fun ContentDrawScope.draw() {
        val pageCount = state.pageCount
        val visibleDots = minOf(MaxNumberOfIndicators, pageCount)
        if (visibleDots <= 0) return

        val centerToCenterPx = if (visibleDots > 1) IndicatorCenterToCenter.toPx() else 0f
        val indicatorLength = centerToCenterPx * (visibleDots - 1)

        val continuousPosition = state.continuousPosition
        val closestPageFromStart = getClosestPageFromStart(continuousPosition, pageCount)
        val transitionProgress =
            getTransitionProgress(continuousPosition, closestPageFromStart, pageCount)

        // This value is the second to last visible page, e.g. o O O O O X o
        val rightPushThreshold = hiddenDotsToTheLeft + visibleDots - 2

        hiddenDotsToTheLeft =
            if (pageCount > MaxNumberOfIndicators) {
                val target =
                    if (!state.isScrollInProgress) {
                        // Programmatic jumps or initialization: snap immediately to target window
                        when {
                            closestPageFromStart > rightPushThreshold ->
                                closestPageFromStart - visibleDots + 2
                            continuousPosition <= hiddenDotsToTheLeft ->
                                continuousPosition.toInt() - 1
                            else -> hiddenDotsToTheLeft
                        }
                    } else {
                        // Active scrolling: shift by at most 1 page for smooth sliding
                        when {
                            closestPageFromStart > rightPushThreshold -> hiddenDotsToTheLeft + 1
                            continuousPosition <= hiddenDotsToTheLeft -> hiddenDotsToTheLeft - 1
                            else -> hiddenDotsToTheLeft
                        }
                    }
                target.coerceIn(0, pageCount - visibleDots)
            } else {
                0
            }

        val isPushingRight =
            continuousPosition > rightPushThreshold && continuousPosition < pageCount - 2

        val isPushingLeft = continuousPosition < hiddenDotsToTheLeft + 1 && continuousPosition > 1

        val windowSlideProgress = if (isPushingLeft || isPushingRight) transitionProgress else 0f

        val selectedDot =
            if (isPushingLeft) 1 else (closestPageFromStart - hiddenDotsToTheLeft).coerceAtLeast(0)

        val directionMultiplier = if (layoutDirection == LayoutDirection.Rtl) -1f else 1f

        // Calculate the starting draw position adjusting for direction
        val startPosition = center.x - (indicatorLength * directionMultiplier) / 2

        val resolvedSelectedColor =
            if (selectedIndicatorColor.isSpecified) selectedIndicatorColor
            else currentContentColor()
        val resolvedUnselectedColor =
            if (unselectedIndicatorColor.isSpecified) {
                unselectedIndicatorColor
            } else {
                resolvedSelectedColor.copy(
                    alpha = resolvedSelectedColor.alpha * UnselectedIndicatorAlpha
                )
            }

        drawContent()
        drawIndicators(
            startPosition = startPosition,
            directionMultiplier = directionMultiplier,
            centerToCenterPx = centerToCenterPx,
            selectedIndicatorColor = resolvedSelectedColor,
            unselectedIndicatorColor = resolvedUnselectedColor,
            visibleDots = visibleDots,
            hiddenDotsToTheLeft = hiddenDotsToTheLeft,
            totalPages = pageCount,
            windowSlideProgress = windowSlideProgress,
            isPushingLeft = isPushingLeft,
            isPushingRight = isPushingRight,
            selectedDot = selectedDot,
            transitionProgress = transitionProgress,
        )
    }
}

private fun DrawScope.drawIndicators(
    startPosition: Float,
    directionMultiplier: Float,
    centerToCenterPx: Float,
    selectedIndicatorColor: Color,
    unselectedIndicatorColor: Color,
    visibleDots: Int,
    hiddenDotsToTheLeft: Int,
    totalPages: Int,
    windowSlideProgress: Float,
    isPushingLeft: Boolean,
    isPushingRight: Boolean,
    selectedDot: Int,
    transitionProgress: Float,
) {
    var currentPos = startPosition - (centerToCenterPx * directionMultiplier)

    val currentDotProgress = inverseLerp(0.5f, 1.0f, transitionProgress)
    val currentDotScale = lerp(0.5f, 1.0f, currentDotProgress)
    val currentDotAlpha = lerp(0.0f, 1.0f, currentDotProgress)

    val nextDotProgress = inverseLerp(0.0f, 0.5f, transitionProgress)
    val nextDotScale = lerp(1.0f, 0.5f, nextDotProgress)
    val nextDotAlpha = lerp(1.0f, 0.0f, nextDotProgress)

    val dotsToDraw =
        if (totalPages > MaxNumberOfIndicators) {
            visibleDots + 1 // Include the extra fading dot for sliding
        } else {
            visibleDots // Just draw the actual dots
        }

    for (dot in 0 until dotsToDraw) {
        if (dot == selectedDot) {
            drawSelectedIndicator(
                basePos = currentPos,
                spacerStep = centerToCenterPx * directionMultiplier,
                transitionProgress = transitionProgress,
                color = selectedIndicatorColor,
            )
        }

        val indicatorSizeRatio =
            calculateIndicatorSizeRatio(
                dot = dot,
                visibleDots = visibleDots,
                hiddenDotsToTheLeft = hiddenDotsToTheLeft,
                totalPages = totalPages,
                selectedDot = selectedDot,
                windowSlideProgress = windowSlideProgress,
                isPushingLeft = isPushingLeft,
                isPushingRight = isPushingRight,
                currentDotScale = currentDotScale,
                nextDotScale = nextDotScale,
            )

        val indicatorAlpha =
            calculateIndicatorAlpha(
                dot = dot,
                visibleDots = visibleDots,
                selectedDot = selectedDot,
                windowSlideProgress = windowSlideProgress,
                currentDotAlpha = currentDotAlpha,
                nextDotAlpha = nextDotAlpha,
            )

        val spacerSizeRatio =
            if (dot == 0) {
                1 - windowSlideProgress
            } else {
                1f
            }
        val dynamicStep = centerToCenterPx * spacerSizeRatio * directionMultiplier
        currentPos += dynamicStep

        drawDot(
            pos = currentPos,
            radius = IndicatorItemRadius.toPx() * indicatorSizeRatio,
            color =
                unselectedIndicatorColor.copy(
                    alpha = unselectedIndicatorColor.alpha * indicatorAlpha
                ),
        )
    }
}

private fun DrawScope.drawSelectedIndicator(
    basePos: Float,
    spacerStep: Float,
    transitionProgress: Float,
    color: Color,
) {
    val startWeight = (1 - transitionProgress * 2).coerceAtLeast(0f)
    val endWeight = (transitionProgress * 2 - 1).coerceAtLeast(0f)
    val blurbWeight = (1 - startWeight - endWeight).coerceAtLeast(0.01f)

    val startOffset = spacerStep + (spacerStep * endWeight)
    val length = spacerStep * blurbWeight

    val lineStart = basePos + startOffset
    val lineEnd = lineStart + length

    drawLine(
        color = color,
        start = Offset(lineStart, center.y),
        end = Offset(lineEnd, center.y),
        strokeWidth = SelectedIndicatorSize.toPx(),
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawDot(pos: Float, radius: Float, color: Color) {
    val location = Offset(pos, center.y)
    drawCircle(color, radius, location)
}

private fun calculateIndicatorSizeRatio(
    dot: Int,
    visibleDots: Int,
    hiddenDotsToTheLeft: Int,
    totalPages: Int,
    selectedDot: Int,
    windowSlideProgress: Float,
    isPushingLeft: Boolean,
    isPushingRight: Boolean,
    currentDotScale: Float,
    nextDotScale: Float,
): Float {
    val maxHiddenDotsToTheLeft = totalPages - visibleDots
    val atLeftLimit = hiddenDotsToTheLeft == 0
    val nearLeftLimit = hiddenDotsToTheLeft == 1
    val atRightLimit = hiddenDotsToTheLeft == maxHiddenDotsToTheLeft
    val nearRightLimit = hiddenDotsToTheLeft == maxHiddenDotsToTheLeft - 1

    val baseRatio =
        when (dot) {
            0 -> {
                val isFullSize = atLeftLimit || (nearLeftLimit && isPushingLeft)
                if (isFullSize) 1f else EdgeIndicatorSizeFraction * (1f - windowSlideProgress)
            }
            1 -> lerp(1f, EdgeIndicatorSizeFraction, windowSlideProgress)
            visibleDots - 1 -> {
                if (isPushingRight || isPushingLeft) {
                    lerp(EdgeIndicatorSizeFraction, 1f, windowSlideProgress)
                } else if (hiddenDotsToTheLeft < maxHiddenDotsToTheLeft) {
                    EdgeIndicatorSizeFraction
                } else {
                    1f
                }
            }
            visibleDots -> {
                val isFullSize =
                    (nearRightLimit && isPushingRight) || (atRightLimit && isPushingLeft)
                if (isFullSize) 1f else EdgeIndicatorSizeFraction * windowSlideProgress
            }
            else -> 1f
        }

    val transitionScale =
        when (dot) {
            selectedDot -> currentDotScale
            selectedDot + 1 -> nextDotScale
            else -> 1f
        }

    return baseRatio * transitionScale
}

private fun calculateIndicatorAlpha(
    dot: Int,
    visibleDots: Int,
    selectedDot: Int,
    windowSlideProgress: Float,
    currentDotAlpha: Float,
    nextDotAlpha: Float,
): Float {
    val baseAlpha =
        when (dot) {
            0 -> 1 - windowSlideProgress
            visibleDots -> windowSlideProgress
            else -> 1f
        }

    val transitionAlpha =
        when (dot) {
            selectedDot -> currentDotAlpha
            selectedDot + 1 -> nextDotAlpha
            else -> 1f
        }

    return baseAlpha * transitionAlpha
}

private fun inverseLerp(start: Float, stop: Float, value: Float): Float {
    return ((value - start) / (stop - start)).fastCoerceIn(0f, 1f)
}

private const val EdgeIndicatorSizeFraction = 8f / 12f
private const val MaxNumberOfIndicators = 7

private val IndicatorItemRadius = 6.dp
private val IndicatorCenterToCenter = 27.dp
private val SelectedIndicatorSize = 18.dp
