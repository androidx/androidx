/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.brush

import androidx.annotation.RestrictTo
import androidx.ink.geometry.Angle
import kotlin.jvm.JvmStatic

/**
 * Provides a fixed set of stock [BrushFamily] objects that any app can use.
 *
 * All brush designs are versioned, so apps can safely store input points and brush specs instead of
 * the pixel result, but be able to regenerate strokes from stored input points that look like the
 * strokes originally drawn by the user. Brush designs are intended to evolve over time, and are
 * released as update packs to the stock library.
 *
 * Each successive brush version will keep to the spirit of the brush, but the actual effect can
 * change between versions. For example, a new version of the highlighter may introduce a variation
 * on how round the tip is, or what sort of curve maps color to pressure.
 *
 * We generally recommend that applications use the latest brush version available; but some use
 * cases, such as art, should be careful to track which version of a brush was used if the document
 * is regenerated, so that the user gets the same visual result.
 */
@OptIn(ExperimentalInkCustomBrushApi::class)
public object StockBrushes {

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview

    // Needed on both property and on getter for AndroidX build, but the Kotlin compiler doesn't
    // like it on the getter so suppress its complaint.
    @ExperimentalInkCustomBrushApi
    @get:ExperimentalInkCustomBrushApi
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @JvmStatic
    public val predictionFadeOutBehavior: BrushBehavior =
        BrushBehavior(
            targetNodes =
                listOf(
                    BrushBehavior.TargetNode(
                        target = BrushBehavior.Target.OPACITY_MULTIPLIER,
                        targetModifierRangeLowerBound = 1F,
                        targetModifierRangeUpperBound = 0.3F,
                        BrushBehavior.BinaryOpNode(
                            operation = BrushBehavior.BinaryOp.PRODUCT,
                            firstInput =
                                BrushBehavior.SourceNode(
                                    source = BrushBehavior.Source.PREDICTED_TIME_ELAPSED_IN_MILLIS,
                                    sourceValueRangeLowerBound = 0F,
                                    sourceValueRangeUpperBound = 24F,
                                ),
                            // The second branch of the binary op node keeps the opacity fade-out
                            // from starting
                            // until the predicted inputs have traveled at least 1.5x brush-size.
                            secondInput =
                                BrushBehavior.ResponseNode(
                                    responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                                    input =
                                        BrushBehavior.SourceNode(
                                            source =
                                                BrushBehavior.Source
                                                    .PREDICTED_DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE,
                                            sourceValueRangeLowerBound = 1.5F,
                                            sourceValueRangeUpperBound = 2F,
                                        ),
                                ),
                        ),
                    )
                )
        )

    /**
     * Version 1 of a simple, circular fixed-width brush.
     *
     * The behavior of this [BrushFamily] will not meaningfully change in future releases. More
     * significant updates would be contained in a [BrushFamily] with a different name specifying a
     * later version number.
     */
    @JvmStatic
    public val markerV1: BrushFamily =
        BrushFamily(tip = BrushTip(behaviors = listOf(predictionFadeOutBehavior)))

    /**
     * The latest version of a simple, circular fixed-width brush.
     *
     * The behavior of this [BrushFamily] may change in future releases, as it always points to the
     * latest version of the marker.
     */
    @JvmStatic public val markerLatest: BrushFamily = markerV1

    /**
     * Version 1 of a pressure- and speed-sensitive brush that is optimized for handwriting with a
     * stylus.
     *
     * The behavior of this [BrushFamily] will not meaningfully change in future releases. More
     * significant updates would be contained in a [BrushFamily] with a different name specifying a
     * later version number.
     */
    @JvmStatic
    public val pressurePenV1: BrushFamily =
        BrushFamily(
            tip =
                BrushTip(
                    behaviors =
                        listOf(
                            predictionFadeOutBehavior,
                            BrushBehavior(
                                source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                                target = BrushBehavior.Target.SIZE_MULTIPLIER,
                                sourceValueRangeLowerBound = 0f,
                                sourceValueRangeUpperBound = 1f,
                                targetModifierRangeLowerBound = 0.05f,
                                targetModifierRangeUpperBound = 1f,
                                sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.CLAMP,
                                responseCurve = EasingFunction.Predefined.LINEAR,
                                responseTimeMillis = 40L,
                                enabledToolTypes = setOf(InputToolType.STYLUS),
                            ),
                        )
                )
        )

    /**
     * The latest version of a pressure- and speed-sensitive brush that is optimized for handwriting
     * with a stylus.
     *
     * The behavior of this [BrushFamily] may change in future releases, as it always points to the
     * latest version of the pressure pen.
     */
    @JvmStatic public val pressurePenLatest: BrushFamily = pressurePenV1

    /**
     * Version 1 of a chisel-tip brush that is intended for highlighting text in a document (when
     * used with a translucent brush color).
     *
     * The behavior of this [BrushFamily] will not meaningfully change in future releases. More
     * significant updates would be contained in a [BrushFamily] with a different name specifying a
     * later version number.
     */
    @JvmStatic
    public val highlighterV1: BrushFamily =
        BrushFamily(
            tip =
                BrushTip(
                    scaleX = 0.05f,
                    scaleY = 1f,
                    cornerRounding = 0.11f,
                    rotation = Angle.degreesToRadians(150f),
                    behaviors = listOf(predictionFadeOutBehavior),
                )
        )

    /**
     * Version 1 of a chisel-tip brush that is intended for highlighting text in a document (when
     * used with a translucent brush color).
     *
     * The behavior of this [BrushFamily] may change in future releases, as it always points to the
     * latest version of the pressure pen.
     */
    @JvmStatic public val highlighterLatest: BrushFamily = highlighterV1
}
