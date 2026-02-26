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

package androidx.compose.animation

import androidx.collection.MutableScatterMap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.ArcAnimationSpec
import androidx.compose.animation.core.ArcMode
import androidx.compose.animation.core.ExperimentalAnimationSpecApi
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Tracks the index of the animation visual debugging color to be used next for local animation
 * debugging visualizations that do not have a custom color defined. Rotates through the predefined
 * color list in [LookaheadAnimationVisualDebugHelper.colors].
 */
private var colorIndex = 0

/**
 * A map to store a consistent color for each unique animation key, ensuring that the same key
 * always gets the same color in visual debugging.
 */
private val keyToColor: MutableScatterMap<Any, Color> = MutableScatterMap()

internal class LookaheadAnimationVisualDebugHelper() {
    private val reverseProgress = Animatable(0f)

    private val restartProgress = Animatable(0f)

    private var isProgressAnimationRunning = false

    var sharedTransitionScopeOffset: Offset = Offset.Zero
    var sharedTransitionScopeSize: IntSize = IntSize.Zero

    var debugOffset: Offset = Offset.Zero
    val debugPath: Path = Path()

    val centerPath: Path = Path()

    @Suppress("PrimitiveInCollection")
    val colors =
        listOf(
            Color(0xFFEA4335),
            Color(0xFFF29027),
            Color(0xFFD148DB),
            Color(0xFF4285F4),
            Color(0xFF3AB8BA),
        )

    internal fun onAttach(coroutineScope: CoroutineScope) {
        if (!isProgressAnimationRunning) {
            isProgressAnimationRunning = true

            // Start reverse animation
            coroutineScope.launch {
                reverseProgress.snapTo(0f)
                reverseProgress.animateTo(
                    targetValue = 1f,
                    animationSpec =
                        infiniteRepeatable(
                            animation = tween(500, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse,
                        ),
                )
            }

            // Start restart animation
            coroutineScope.launch {
                restartProgress.snapTo(0f)
                restartProgress.animateTo(
                    targetValue = 1f,
                    animationSpec =
                        infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart,
                        ),
                )
            }
        }
    }

    internal fun onDetach(coroutineScope: CoroutineScope) {
        if (isProgressAnimationRunning) {
            isProgressAnimationRunning = false
            coroutineScope.launch {
                // Stop reverse animation
                reverseProgress.stop()

                // Stop restart animation
                restartProgress.stop()
            }
        }
    }

    internal fun updateDrawingCoordinates(
        offsetInSharedTransitionScope: Offset,
        sizeOfSharedTransitionScope: IntSize,
    ) {
        sharedTransitionScopeOffset = offsetInSharedTransitionScope
        sharedTransitionScopeSize = sizeOfSharedTransitionScope
    }

    @Suppress("PrimitiveInCollection")
    @OptIn(ExperimentalLookaheadAnimationVisualDebugApi::class)
    internal fun ContentDrawScope.drawGlobalVisualizations() {
        val animatedDistance =
            (2 * sharedTransitionScopeSize.width + 2 * sharedTransitionScopeSize.height) *
                restartProgress.value
        val perimeterCenter = findPositionAlongPerimeter(animatedDistance)
        val gradientColors =
            listOf(
                Color(0xFFEA4335),
                Color(0xFF4285F4),
                Color(0xFF34A853),
                Color(0xFFFBBC04),
                Color(0xFFEA4335),
            )

        val perimeterBrush =
            Brush.radialGradient(colors = gradientColors, center = perimeterCenter, radius = 2000f)

        drawIntoCanvas { canvas ->
            val paint = androidx.compose.ui.graphics.Paint()
            perimeterBrush.applyTo(size = size, p = paint, alpha = 1.0f)
            paint.style = PaintingStyle.Stroke
            paint.strokeWidth = 8.dp.toPx() * reverseProgress.value + 4.dp.toPx()
            canvas.save()
            canvas.translate(-sharedTransitionScopeOffset.x, -sharedTransitionScopeOffset.y)
            canvas.drawRect(
                left = 0f,
                top = 0f,
                right = sharedTransitionScopeSize.width.toFloat(),
                bottom = sharedTransitionScopeSize.height.toFloat(),
                paint = paint,
            )
            canvas.restore()
        }
    }

    internal fun ContentDrawScope.drawOverlay(overlayColor: Color) {
        drawRect(overlayColor)
    }

    @Suppress("PrimitiveInCollection")
    internal fun chooseColor(key: Any): Color {
        if (keyToColor.contains(key)) {
            return keyToColor[key]!!
        } else {
            if (colorIndex >= colors.size) {
                colorIndex = 0
            }
            val currentColor = colors[colorIndex]
            colorIndex += 1
            keyToColor[key] = currentColor
            return currentColor
        }
    }

    internal fun ContentDrawScope.drawInactiveVisualizations(
        animationColor: Color,
        isShowKeyLabelEnabled: Boolean,
        strokeWidth: Float,
        key: Any,
        textMeasurer: TextMeasurer? = null,
    ) {
        val highlightWidth = strokeWidth * 2f

        // If there is no specified color, choose a "random" color out of the default list
        val chosenColor: Color =
            if (animationColor != Color.Unspecified) {
                animationColor
            } else {
                // Draw animation border
                drawRect(color = Color.White, style = Stroke(width = highlightWidth))

                Color(0xFF9AA0A6)
            }

        // Draw animation border
        drawRect(color = chosenColor, style = Stroke(width = strokeWidth))

        // Print shared element key
        if (isShowKeyLabelEnabled) {
            if (textMeasurer != null) {
                val textLayoutResult =
                    textMeasurer.measure(
                        text = key.toString(),
                        style =
                            TextStyle(
                                color = chosenColor,
                                fontSize = 18.sp,
                                background = Color.White.copy(alpha = 0.6f),
                            ),
                    )

                drawText(textLayoutResult = textLayoutResult, topLeft = Offset(10f, 10f))
            }
        }
    }

    internal fun ContentDrawScope.drawLocalVisualizations(
        animationColor: Color,
        targetOffset: Offset,
        targetSize: Size,
        currentRect: Rect,
        center: Offset,
        isShowKeyLabelEnabled: Boolean,
        strokeWidth: Float,
        key: Any,
        textMeasurer: TextMeasurer? = null,
    ) {
        // Necessary for testing purposes
        if (animationColor == Color.Transparent) return

        val highlightWidth = strokeWidth * 2f

        // If there is no specified color, choose a "random" color out of the default list
        val chosenColor: Color =
            if (animationColor != Color.Unspecified) {
                animationColor
            } else {
                // Draw animation border
                drawRect(color = Color.White, style = Stroke(width = highlightWidth))

                // Draw target border
                translate(
                    left = targetOffset.x - currentRect.topLeft.x,
                    top = targetOffset.y - currentRect.topLeft.y,
                ) {
                    drawRect(
                        color = Color.White,
                        size = targetSize,
                        style = Stroke(width = highlightWidth),
                    )
                }

                // Draw animation path
                translate(
                    left = targetOffset.x - currentRect.topLeft.x - debugOffset.x,
                    top = targetOffset.y - currentRect.topLeft.y - debugOffset.y,
                ) {
                    translate(targetSize.width * 0.5f, targetSize.height * 0.5f) {
                        drawPath(
                            path = debugPath,
                            color = Color.White,
                            style =
                                Stroke(
                                    width = highlightWidth,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f)),
                                ),
                        )
                    }
                }

                // Draw position along path
                calculatePathCenter(strokeWidth * 3.5f)
                translate(left = center.x, top = center.y) { drawPath(centerPath, Color.White) }

                chooseColor(key)
            }

        // Draw animation border
        drawRect(color = chosenColor, style = Stroke(width = strokeWidth))

        // Draw target border
        translate(
            left = targetOffset.x - currentRect.topLeft.x,
            top = targetOffset.y - currentRect.topLeft.y,
        ) {
            drawRect(color = chosenColor, size = targetSize, style = Stroke(width = strokeWidth))
        }

        // Draw animation path
        translate(
            left = targetOffset.x - currentRect.topLeft.x - debugOffset.x,
            top = targetOffset.y - currentRect.topLeft.y - debugOffset.y,
        ) {
            translate(targetSize.width * 0.5f, targetSize.height * 0.5f) {
                drawPath(
                    path = debugPath,
                    color = chosenColor,
                    style =
                        Stroke(
                            width = strokeWidth,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f)),
                        ),
                )
            }
        }

        // Draw position along path
        calculatePathCenter(strokeWidth * 3)
        translate(left = center.x, top = center.y) { drawPath(centerPath, chosenColor) }

        // Print shared element key
        if (isShowKeyLabelEnabled) {
            if (textMeasurer != null) {
                val textLayoutResult =
                    textMeasurer.measure(
                        text = key.toString(),
                        style =
                            TextStyle(
                                color = chosenColor,
                                fontSize = 18.sp,
                                background = Color.White.copy(alpha = 0.6f),
                            ),
                    )

                drawText(textLayoutResult = textLayoutResult, topLeft = Offset(10f, 10f))

                translate(
                    left = targetOffset.x - currentRect.topLeft.x,
                    top = targetOffset.y - currentRect.topLeft.y,
                ) {
                    drawText(textLayoutResult = textLayoutResult, topLeft = Offset(10f, 10f))
                }
            }
        }
    }

    internal fun ContentDrawScope.drawMultipleMatchesElement(
        multipleMatchesColor: Color,
        isShowKeyLabelEnabled: Boolean,
        key: Any,
        numMatches: Int,
        textMeasurer: TextMeasurer,
        strokeWidth: Float,
    ) {
        val highlightWidth = strokeWidth * 2f
        drawRect(color = Color.White, style = Stroke(width = highlightWidth))
        drawRect(color = multipleMatchesColor, style = Stroke(width = strokeWidth))

        if (isShowKeyLabelEnabled) {
            val emoji =
                when (numMatches) {
                    2 -> "2\uFE0F⃣"
                    3 -> "3\uFE0F⃣"
                    4 -> "4\uFE0F⃣"
                    5 -> "5\uFE0F⃣"
                    6 -> "6\uFE0F⃣"
                    7 -> "7\uFE0F⃣"
                    8 -> "8\uFE0F⃣"
                    9 -> "9\uFE0F⃣"
                    else -> "> 9\uFE0F⃣"
                }
            val textLayoutResult =
                textMeasurer.measure(
                    text = "$key: $emoji matches",
                    style =
                        TextStyle(
                            color = Color.White,
                            fontSize = 22.sp,
                            background = multipleMatchesColor.copy(alpha = 0.8f),
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        ),
                )

            drawText(textLayoutResult = textLayoutResult, topLeft = Offset(10f, 10f))
        }
    }

    internal fun ContentDrawScope.drawUnmatchedElement(
        unmatchedColor: Color,
        isShowKeyLabelEnabled: Boolean,
        key: Any,
        textMeasurer: TextMeasurer,
        strokeWidth: Float,
    ) {
        val highlightWidth = strokeWidth * 2f
        drawRect(color = Color.White, style = Stroke(width = highlightWidth))
        drawRect(color = unmatchedColor, style = Stroke(width = strokeWidth))
        clipRect() {
            val w = size.width
            val h = size.height
            var x = -h // Start drawing from before the left edge.

            while (x < w) {
                drawLine(
                    color = unmatchedColor.copy(alpha = 0.3f),
                    start = Offset(x, h),
                    end = Offset(x + h, 0f),
                    strokeWidth = strokeWidth,
                )
                x += strokeWidth * 4
            }
        }

        if (isShowKeyLabelEnabled) {
            val textLayoutResult =
                textMeasurer.measure(
                    text = "$key: 0\uFE0F⃣ matches",
                    style =
                        TextStyle(
                            color = Color.White,
                            fontSize = 22.sp,
                            background = unmatchedColor.copy(alpha = 0.8f),
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        ),
                )

            drawText(textLayoutResult = textLayoutResult, topLeft = Offset(10f, 10f))
        }
    }

    private fun findPositionAlongPerimeter(distanceTraveled: Float): Offset {
        val width: Float = sharedTransitionScopeSize.width.toFloat()
        val height: Float = sharedTransitionScopeSize.height.toFloat()
        if (distanceTraveled <= width) {
            return Offset(distanceTraveled, 0f)
        }
        if (distanceTraveled <= (width + height)) {
            return Offset(width, distanceTraveled - width)
        }
        if (distanceTraveled <= (width * 2 + height)) {
            return Offset(width * 2 + height - distanceTraveled, height)
        }
        return Offset(0f, height * 2 + width * 2 - distanceTraveled)
    }

    private fun calculatePathCenter(diamondWidth: Float) {
        centerPath.rewind()

        centerPath.apply {
            moveTo(0f, -diamondWidth)
            lineTo(diamondWidth, 0f)
            lineTo(0f, diamondWidth)
            lineTo(-diamondWidth, 0f)
            close()
        }
    }

    @OptIn(ExperimentalAnimationSpecApi::class)
    internal fun calculatePath(
        spec: FiniteAnimationSpec<Rect>,
        current: Rect,
        target: Rect,
        initialVelocity: Rect = Rect(Offset(0f, 0f), 0f),
    ) {
        debugPath.rewind()

        // Linear
        if (
            spec is TweenSpec<Rect> ||
                spec is SnapSpec<Rect> ||
                (spec is ArcAnimationSpec<Rect> && spec.mode == ArcMode.ArcLinear)
        ) {
            debugPath.moveTo(current.center.x, current.center.y)
            debugPath.lineTo(target.center.x, target.center.y)
            debugPath.translate(-current.center)
            debugOffset = target.center - current.center
        }
        // Non-linear
        else {
            val pathAnim =
                TargetBasedAnimation(
                    animationSpec = spec,
                    typeConverter = Rect.VectorConverter,
                    initialValue = current,
                    targetValue = target,
                    initialVelocity = initialVelocity,
                )

            val durationNanos = pathAnim.durationNanos

            val startValue = pathAnim.getValueFromNanos(playTimeNanos = 0)

            val numSteps = 400
            for (i in 0..numSteps) {
                val playTime =
                    durationNanos - (durationNanos * (i.toFloat() / (numSteps - 1))).toLong()
                val rectAtTime = pathAnim.getValueFromNanos(playTime)
                val point = rectAtTime.center
                if (i == 0) {
                    debugPath.moveTo(point.x, point.y)
                } else {
                    debugPath.lineTo(point.x, point.y)
                }
            }
            debugPath.translate(-startValue.center)

            debugOffset = target.center - startValue.center
        }
    }
}

/**
 * Allows enabling and customizing shared element and animated bounds animation debugging. Note that
 * enabling LookaheadAnimationVisualDebugging affects the entire UI subtree generated by the content
 * lambda. It applies to all descendants, regardless of whether they are defined within the same
 * lexical scope.
 *
 * @param isEnabled Boolean specifying whether to enable animation debugging.
 * @param overlayColor The color of the translucent film covering everything underneath the lifted
 *   layer (where the shared elements and other elements rendered in overlay are rendered).
 * @param multipleMatchesColor The color to indicate a shared element key with multiple matches.
 * @param unmatchedElementColor The color to indicate a shared element key with no matches.
 * @param isShowKeyLabelEnabled Boolean specifying whether to print animated element keys.
 * @param content The composable content that debugging visualizations will apply to, although which
 *   visualizations appear depends on where the Modifiers are placed.
 *
 * An example of how to use it:
 *
 * @sample androidx.compose.animation.samples.LookaheadAnimationVisualDebuggingSample
 */
@Composable
@ExperimentalLookaheadAnimationVisualDebugApi
public fun LookaheadAnimationVisualDebugging(
    isEnabled: Boolean = true,
    overlayColor: Color = Color(0x8034A853),
    multipleMatchesColor: Color = Color(0xFFEA4335),
    unmatchedElementColor: Color = Color(0xFF9AA0A6),
    isShowKeyLabelEnabled: Boolean = false,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalLookaheadAnimationVisualDebugConfig provides
            LookaheadAnimationVisualDebugConfig(
                isEnabled,
                overlayColor,
                multipleMatchesColor,
                unmatchedElementColor,
                isShowKeyLabelEnabled,
            ),
        content = content,
    )
}

/**
 * Allows customizing a particular shared element or animated bounds animation for debugging. Note
 * that enabling CustomizedLookaheadAnimationVisualDebugging affects the entire UI subtree generated
 * by the content lambda. It applies to all descendants, regardless of whether they are defined
 * within the same lexical scope.
 *
 * @param debugColor The custom color specified for animation debugging visualizations.
 * @param content The composable content that debugging visualizations will apply to, although which
 *   visualizations appear depends on where the Modifiers are placed.
 *
 * An example of how to use it:
 *
 * @sample androidx.compose.animation.samples.CustomizedLookaheadAnimationVisualDebuggingSample
 */
@Composable
@ExperimentalLookaheadAnimationVisualDebugApi
public fun CustomizedLookaheadAnimationVisualDebugging(
    debugColor: Color,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalLookaheadAnimationVisualDebugColor provides (debugColor),
        content = content,
    )
}
