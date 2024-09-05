/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.internal.toPath
import androidx.compose.material3.internal.transformed
import androidx.compose.material3.tokens.LoadingIndicatorTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.fastForEach
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// TODO Update the docs images to point to the loading indicator
/**
 * A Material Design loading indicator.
 *
 * This version of the loading indicator morphs between its [polygons] shapes by the value of its
 * [progress].
 *
 * ![Loading indicator
 * image](https://developer.android.com/images/reference/androidx/compose/material3/loading-indicator.png)
 *
 * It can be created like this:
 *
 * @sample androidx.compose.material3.samples.DeterminateLoadingIndicatorSample
 * @param progress the progress of this loading indicator, where 0.0 represents no progress and 1.0
 *   represents full progress. Values outside of this range are coerced into the range. The
 *   indicator will morph its shapes between the provided [polygons] according to the value of the
 *   progress.
 * @param modifier the [Modifier] to be applied to this loading indicator
 * @param color the loading indicator's color
 * @param polygons a list of [RoundedPolygon]s for the sequence of shapes this loading indicator
 *   will morph between as it progresses from 0.0 to 1.0. The loading indicator expects at least two
 *   items in that list.
 * @throws IllegalArgumentException if the [polygons] list holds less than two items
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun LoadingIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = LoadingIndicatorDefaults.indicatorColor,
    polygons: List<RoundedPolygon> = LoadingIndicatorDefaults.DeterminateIndicatorPolygons
) =
    LoadingIndicatorImpl(
        progress = progress,
        modifier = modifier,
        containerColor = Color.Unspecified,
        indicatorColor = color,
        containerShape = LoadingIndicatorDefaults.containerShape,
        indicatorPolygons = polygons,
    )

// TODO Update the docs images to point to the loading indicator.
/**
 * A Material Design loading indicator.
 *
 * This version of the loading indicator animates and morphs between various shapes as long as the
 * loading indicator is visible.
 *
 * ![Loading indicator
 * image](https://developer.android.com/images/reference/androidx/compose/material3/loading-indicator.png)
 *
 * It can be created like this:
 *
 * @sample androidx.compose.material3.samples.LoadingIndicatorSample
 * @param modifier the [Modifier] to be applied to this loading indicator
 * @param color the loading indicator's color
 * @param polygons a list of [RoundedPolygon]s for the sequence of shapes this loading indicator
 *   will morph between. The loading indicator expects at least two items in that list.
 * @throws IllegalArgumentException if the [polygons] list holds less than two items
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = LoadingIndicatorDefaults.indicatorColor,
    polygons: List<RoundedPolygon> = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons,
) =
    LoadingIndicatorImpl(
        modifier = modifier,
        containerColor = Color.Unspecified,
        indicatorColor = color,
        containerShape = LoadingIndicatorDefaults.containerShape,
        indicatorPolygons = polygons,
    )

// TODO Update the docs images to point to the loading indicator
/**
 * A Material Design contained loading indicator.
 *
 * This version of the loading indicator morphs between its [polygons] shapes by the value of its
 * [progress]. The shapes in this variation are contained within a colored [containerShape].
 *
 * ![Contained loading indicator
 * image](https://developer.android.com/images/reference/androidx/compose/material3/contained-loading-indicator.png)
 *
 * It can be created like this:
 *
 * @sample androidx.compose.material3.samples.DeterminateContainedLoadingIndicatorSample
 *
 * It can also be used as an indicator for a [PullToRefreshBox] like this:
 *
 * @sample androidx.compose.material3.samples.LoadingIndicatorPullToRefreshSample
 * @param progress the progress of this loading indicator, where 0.0 represents no progress and 1.0
 *   represents full progress. Values outside of this range are coerced into the range. The
 *   indicator will morph its shapes between the provided [polygons] according to the value of the
 *   progress.
 * @param modifier the [Modifier] to be applied to this loading indicator
 * @param containerColor the loading indicator's container color
 * @param indicatorColor the loading indicator's color
 * @param containerShape the loading indicator's container shape
 * @param polygons a list of [RoundedPolygon]s for the sequence of shapes this loading indicator
 *   will morph between as it progresses from 0.0 to 1.0. The loading indicator expects at least two
 *   items in that list.
 * @throws IllegalArgumentException if the [polygons] list holds less than two items
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun ContainedLoadingIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    containerColor: Color = LoadingIndicatorDefaults.containedContainerColor,
    indicatorColor: Color = LoadingIndicatorDefaults.containedIndicatorColor,
    containerShape: Shape = LoadingIndicatorDefaults.containerShape,
    polygons: List<RoundedPolygon> = LoadingIndicatorDefaults.DeterminateIndicatorPolygons
) =
    LoadingIndicatorImpl(
        progress = progress,
        modifier = modifier,
        containerColor = containerColor,
        indicatorColor = indicatorColor,
        containerShape = containerShape,
        indicatorPolygons = polygons,
    )

// TODO Update the docs images to point to the loading indicator.
/**
 * A Material Design contained loading indicator.
 *
 * This version of the loading indicator animates and morphs between various shapes as long as the
 * loading indicator is visible. The shapes in this variation are contained within a colored
 * [containerShape].
 *
 * ![Contained loading indicator
 * image](https://developer.android.com/images/reference/androidx/compose/material3/contained-loading-indicator.png)
 *
 * It can be created like this:
 *
 * @sample androidx.compose.material3.samples.ContainedLoadingIndicatorSample
 * @param modifier the [Modifier] to be applied to this loading indicator
 * @param containerColor the loading indicator's container color
 * @param indicatorColor the loading indicator's color
 * @param containerShape the loading indicator's container shape
 * @param polygons a list of [RoundedPolygon]s for the sequence of shapes this loading indicator
 *   will morph between. The loading indicator expects at least two items in that list.
 * @throws IllegalArgumentException if the [polygons] list holds less than two items
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun ContainedLoadingIndicator(
    modifier: Modifier = Modifier,
    containerColor: Color = LoadingIndicatorDefaults.containedContainerColor,
    indicatorColor: Color = LoadingIndicatorDefaults.containedIndicatorColor,
    containerShape: Shape = LoadingIndicatorDefaults.containerShape,
    polygons: List<RoundedPolygon> = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons,
) =
    LoadingIndicatorImpl(
        modifier = modifier,
        containerColor = containerColor,
        indicatorColor = indicatorColor,
        containerShape = containerShape,
        indicatorPolygons = polygons,
    )

/**
 * A determinate loading indicator implementation.
 *
 * @param progress the progress of this loading indicator, where 0.0 represents no progress and 1.0
 *   represents full progress. Values outside of this range are coerced into the range. The
 *   indicator will morph its shapes between the provided [indicatorPolygons] according to the value
 *   of the progress.
 * @param modifier the [Modifier] to be applied to this loading indicator
 * @param containerColor the loading indicator's container color
 * @param indicatorColor the loading indicator's color
 * @param containerShape the loading indicator's container shape
 * @param indicatorPolygons a list of [RoundedPolygon]s for the sequence of shapes this loading
 *   indicator will morph between as it progresses from 0.0 to 1.0. The loading indicator expects at
 *   least two items in that list.
 * @throws IllegalArgumentException if the [indicatorPolygons] list holds less than two items
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoadingIndicatorImpl(
    progress: () -> Float,
    modifier: Modifier,
    containerColor: Color,
    indicatorColor: Color,
    containerShape: Shape,
    indicatorPolygons: List<RoundedPolygon>
) {
    require(indicatorPolygons.size > 1) {
        "indicatorPolygons should have, at least, two RoundedPolygons"
    }
    val coercedProgress = { progress().coerceIn(0f, 1f) }
    val path = remember { Path() }
    val scaleMatrix = remember { Matrix() }
    val morphSequence =
        remember(indicatorPolygons) {
            morphSequence(polygons = indicatorPolygons, circularSequence = false)
        }
    val morphScaleFactor =
        remember(morphSequence) {
            // Calculate the shapes scale factor that will be applied to the morphed path as it's
            // scaled into the available size.
            // This overall scale factor ensures that the shapes are rendered without clipping and
            // at the correct ratio within the component by taking into account their occupied size
            // as they rotate, and taking into account the spec's ActiveIndicatorScale.
            calculateScaleFactor(indicatorPolygons) * LoadingIndicatorDefaults.ActiveIndicatorScale
        }
    Box(
        modifier =
            modifier
                .semantics(mergeDescendants = true) {
                    progressBarRangeInfo = ProgressBarRangeInfo(coercedProgress(), 0f..1f)
                }
                .size(
                    width = LoadingIndicatorDefaults.ContainerWidth,
                    height = LoadingIndicatorDefaults.ContainerHeight
                )
                .fillMaxSize()
                .clip(containerShape)
                .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        // Using a Spacer to render the indicator's shapes. Maintains a square aspect ratio (1:1)
        // to prevent shape distortion during rendering.
        Spacer(
            modifier =
                Modifier.aspectRatio(ratio = 1f, matchHeightConstraintsFirst = true)
                    .drawWithContent {
                        val progressValue = coercedProgress()
                        // Adjust the active morph index according to the progress.
                        val activeMorphIndex =
                            (morphSequence.size * progressValue)
                                .toInt()
                                .coerceAtMost(morphSequence.size - 1)
                        // Prepare the progress value that will be used for the active Morph.
                        val adjustedProgressValue =
                            if (progressValue == 1f && activeMorphIndex == morphSequence.size - 1) {
                                // Prevents a zero when the progress is one and we are at the last
                                // shape morph.
                                1f
                            } else {
                                (progressValue * morphSequence.size) % 1f
                            }

                        // Rotate counterclockwise.
                        val rotation = -progressValue * 180
                        rotate(rotation) {
                            drawPath(
                                path =
                                    processPath(
                                        path =
                                            morphSequence[activeMorphIndex].toPath(
                                                // Use the adjusted progress.
                                                progress = adjustedProgressValue,
                                                path = path,
                                                startAngle = 0
                                            ),
                                        size = size,
                                        scaleFactor = morphScaleFactor,
                                        scaleMatrix = scaleMatrix
                                    ),
                                color = indicatorColor,
                                style = Fill
                            )
                        }
                    }
        )
    }
}

/**
 * An indeterminate loading indicator implementation.
 *
 * @param modifier the [Modifier] to be applied to this loading indicator
 * @param containerColor the loading indicator's container color
 * @param indicatorColor the loading indicator's color
 * @param containerShape the loading indicator's container shape
 * @param indicatorPolygons a list of [RoundedPolygon]s for the sequence of shapes this loading
 *   indicator will morph between. The loading indicator expects at least two items in that list.
 * @throws IllegalArgumentException if the [indicatorPolygons] list holds less than two items
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
private fun LoadingIndicatorImpl(
    modifier: Modifier,
    containerColor: Color,
    indicatorColor: Color,
    containerShape: Shape,
    indicatorPolygons: List<RoundedPolygon>
) {
    require(indicatorPolygons.size > 1) {
        "indicatorPolygons should have, at least, two RoundedPolygons"
    }
    val morphSequence =
        remember(indicatorPolygons) {
            morphSequence(polygons = indicatorPolygons, circularSequence = true)
        }
    val shapesScaleFactor =
        remember(indicatorPolygons) {
            // Calculate the shapes scale factor that will be applied to the morphed path as it's
            // scaled into the available size.
            // This overall scale factor ensures that the shapes are rendered without clipping and
            // at the correct ratio within the component by taking into account their occupied size
            // as they rotate, and taking into account the spec's ActiveIndicatorScale.
            calculateScaleFactor(indicatorPolygons) * LoadingIndicatorDefaults.ActiveIndicatorScale
        }
    val morphProgress = remember { Animatable(0f) }
    var morphRotationTargetAngle by remember { mutableFloatStateOf(QuarterRotation) }
    val globalRotation = remember { Animatable(0f) }
    var currentMorphIndex by remember(indicatorPolygons) { mutableIntStateOf(0) }
    LaunchedEffect(indicatorPolygons) {
        launch {
            // Note that we up the visibilityThreshold here to 0.1, which is x10 than the default
            // threshold, and ends the low-damping spring in a shorter time.
            val morphAnimationSpec =
                spring(dampingRatio = 0.6f, stiffness = 200f, visibilityThreshold = 0.1f)
            while (true) {
                // Async launch of a spring that will finish in less than 650ms
                // (MorphIntervalMillis). We then delay the entire while loop by 650ms till the next
                // morph starts.
                val deferred = async {
                    val animationResult =
                        morphProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = morphAnimationSpec
                        )
                    if (animationResult.endReason == AnimationEndReason.Finished) {
                        currentMorphIndex = (currentMorphIndex + 1) % morphSequence.size
                        morphProgress.snapTo(0f)
                        morphRotationTargetAngle =
                            (morphRotationTargetAngle + QuarterRotation) % FullRotation
                    }
                }
                delay(MorphIntervalMillis)
                deferred.await()
            }
        }
        globalRotation.animateTo(
            targetValue = FullRotation,
            animationSpec =
                infiniteRepeatable(
                    tween(GlobalRotationDurationMillis, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
        )
    }

    val path = remember { Path() }
    val scaleMatrix = remember { Matrix() }
    Box(
        modifier =
            modifier
                .progressSemantics()
                .size(
                    width = LoadingIndicatorDefaults.ContainerWidth,
                    height = LoadingIndicatorDefaults.ContainerHeight
                )
                .fillMaxSize()
                .clip(containerShape)
                .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Spacer(
            modifier =
                Modifier.aspectRatio(1f, matchHeightConstraintsFirst = true).drawWithContent {
                    val progress = morphProgress.value
                    // Rotate clockwise.
                    rotate(progress * 90 + morphRotationTargetAngle + globalRotation.value) {
                        drawPath(
                            path =
                                processPath(
                                    path =
                                        morphSequence[currentMorphIndex].toPath(
                                            // Use the coerced progress value to eliminate any
                                            // bounciness from the morph. We scale the drawing
                                            // to simulate some bounciness instead.
                                            progress = progress,
                                            path = path,
                                            startAngle = 0
                                        ),
                                    size = size,
                                    scaleFactor = shapesScaleFactor,
                                    scaleMatrix = scaleMatrix
                                ),
                            color = indicatorColor,
                            style = Fill
                        )
                    }
                }
        )
    }
}

/** Contains default values by the [LoadingIndicator]. */
@ExperimentalMaterial3ExpressiveApi
object LoadingIndicatorDefaults {

    /** A [LoadingIndicator] default container width. */
    val ContainerWidth: Dp = LoadingIndicatorTokens.ContainerWidth

    /** A [LoadingIndicator] default container height. */
    val ContainerHeight: Dp = LoadingIndicatorTokens.ContainerHeight

    /** A [LoadingIndicator] default active indicator size. */
    val IndicatorSize = LoadingIndicatorTokens.ActiveSize

    /** A [LoadingIndicator] default container [Shape]. */
    val containerShape: Shape
        @Composable get() = LoadingIndicatorTokens.ContainerShape.value

    /**
     * A [LoadingIndicator] default active indicator [Color] when using an uncontained
     * [LoadingIndicator].
     */
    val indicatorColor: Color
        @Composable get() = LoadingIndicatorTokens.ActiveIndicatorColor.value

    /**
     * A [LoadingIndicator] default active indicator [Color] when using a
     * [ContainedLoadingIndicator].
     */
    val containedIndicatorColor: Color
        @Composable get() = LoadingIndicatorTokens.ContainedActiveColor.value

    /** A [LoadingIndicator] default container [Color] when using a [ContainedLoadingIndicator]. */
    val containedContainerColor: Color
        @Composable get() = LoadingIndicatorTokens.ContainedContainerColor.value

    /**
     * The sequence of [RoundedPolygon]s that the indeterminate [LoadingIndicator] will morph
     * between when animating.
     *
     * By default, an indeterminate loading indicator will morph between seven shapes, but you may
     * provide your own shapes sequence when calling the API.
     */
    val IndeterminateIndicatorPolygons =
        listOf(
            MaterialShapes.SoftBurst,
            MaterialShapes.Cookie9Sided,
            MaterialShapes.Pentagon,
            MaterialShapes.Pill,
            MaterialShapes.Sunny,
            MaterialShapes.Cookie4Sided,
            MaterialShapes.Oval
        )

    /**
     * The sequence of [RoundedPolygon]s that the determinate [LoadingIndicator] will morph between
     * when animating. The loading indicator will morph between the shapes as its progress moves
     * between zero to one.
     *
     * By default, a determinate loading indicator will will morph between two shapes, but you may
     * provide your own shapes sequence when calling the API.
     */
    val DeterminateIndicatorPolygons =
        listOf(
            // Rotating the circle gets us a smoother morphing to the soft-burst shapes, which is
            // also being rotated at the same angle.
            MaterialShapes.Circle.transformed(Matrix().apply { rotateZ(360f / 20) }),
            MaterialShapes.SoftBurst
        )

    /**
     * A percentage value of the LoadingIndicator's active indicator scale when rendered within the
     * bounds of the container size.
     */
    internal val ActiveIndicatorScale =
        IndicatorSize.value / min(ContainerWidth.value, ContainerHeight.value)
}

/**
 * Returns a list of [Morph]s that describe the sequence of shapes that the [LoadingIndicator]
 * should animate in a loop.
 *
 * This function will create a morph between each consecutive [RoundedPolygon] shaped in the
 * provided array, and will create a final morph from the last shape to the first shape.
 *
 * Note that each [RoundedPolygon] within the returns [Morph]s is normalized here.
 *
 * @param polygons a [List] of [RoundedPolygon]s to create [Morph]s for
 * @param circularSequence indicate if an additional [Morph] should be created from the last item to
 *   the first item of the list in order to create a circular sequence of morphs
 * @see RoundedPolygon.normalized
 */
private fun morphSequence(polygons: List<RoundedPolygon>, circularSequence: Boolean): List<Morph> {
    return buildList {
        for (i in polygons.indices) {
            if (i + 1 < polygons.size) {
                add(Morph(polygons[i].normalized(), polygons[i + 1].normalized()))
            } else if (circularSequence) {
                // Create a morph from the last shape to the first shape
                add(Morph(polygons[i].normalized(), polygons[0].normalized()))
            }
        }
    }
}

/**
 * Calculates a scale factor that will be used when scaling the provided [RoundedPolygon]s into a
 * specified sized container.
 *
 * Since the polygons may rotate, a simple [RoundedPolygon.calculateBounds] is not enough to
 * determine the size the polygon will occupy as it rotates. Using the simple bounds calculation may
 * result in a clipped shape.
 *
 * This function calculates and returns a scale factor by utilizing the
 * [RoundedPolygon.calculateMaxBounds] and comparing its result to the
 * [RoundedPolygon.calculateBounds]. The scale factor can later be used when calling [processPath].
 */
private fun calculateScaleFactor(indicatorPolygons: List<RoundedPolygon>): Float {
    var scaleFactor = 1f
    // Axis-aligned max bounding box for this object, where the rectangles left, top, right, and
    // bottom values will be stored in entries 0, 1, 2, and 3, in that order.
    val bounds = FloatArray(size = 4)
    val maxBounds = FloatArray(size = 4)
    indicatorPolygons.fastForEach { polygon ->
        polygon.calculateBounds(bounds)
        polygon.calculateMaxBounds(maxBounds)
        val scaleX = bounds.width() / maxBounds.width()
        val scaleY = bounds.height() / maxBounds.height()
        // We use max(scaleX, scaleY) to handle cases like a pill-shape that can throw off the
        // entire calculation.
        scaleFactor = min(scaleFactor, max(scaleX, scaleY))
    }
    return scaleFactor
}

/**
 * Returns the width value from the [FloatArray] that was calculated by a
 * [RoundedPolygon.calculateBounds] or [[RoundedPolygon.calculateMaxBounds]].
 */
private fun FloatArray.width(): Float {
    // Expecting a FloatArray of [left, top, right, bottom]
    return this[2] - this[0]
}

/**
 * Returns the height value from the [FloatArray] that was calculated by a
 * [RoundedPolygon.calculateBounds] or [RoundedPolygon.calculateMaxBounds].
 */
private fun FloatArray.height(): Float {
    // Expecting a FloatArray of [left, top, right, bottom]
    return this[3] - this[1]
}

/**
 * Process a given path to scale it and center it inside the given size.
 *
 * @param path a [Path] that was generated by a _normalized_ [Morph] or [RoundedPolygon]
 * @param size a [Size] that the provided [path] is going to be scaled and centered into
 * @param scaleFactor a scale factor that will be taken into account uniformly when the [path] is
 *   scaled (i.e. the scaleX would be the [size] width x the scale factor, and the scaleY would be
 *   the [size] height x the scale factor)
 * @param scaleMatrix a [Matrix] that would be used to apply the scaling. Note that any provided
 *   matrix will be reset in this function.
 */
private fun processPath(
    path: Path,
    size: Size,
    scaleFactor: Float,
    scaleMatrix: Matrix = Matrix(),
): Path {
    scaleMatrix.reset()

    scaleMatrix.apply { scale(x = size.width * scaleFactor, y = size.height * scaleFactor) }

    // Scale to the desired size.
    path.transform(scaleMatrix)

    // Translate the path to align its center with the available size center.
    path.translate(size.center - path.getBounds().center)
    return path
}

private const val GlobalRotationDurationMillis = 4666
private const val MorphIntervalMillis = 650L

private const val FullRotation = 360f
private const val QuarterRotation = FullRotation / 4f
