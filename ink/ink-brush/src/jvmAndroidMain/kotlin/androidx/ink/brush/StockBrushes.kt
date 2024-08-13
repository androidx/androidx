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
 * The list of available stock brushes includes:
 * - Marker: A simple, circular fixed-width brush.
 * - Pressure Pen: A pressure- and speed-sensitive brush that is optimized for handwriting with a
 *   stylus.
 * - Highlighter: A chisel-tip brush that is intended for highlighting text in a document (when used
 *   with a translucent brush color).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
@OptIn(ExperimentalInkCustomBrushApi::class)
public object StockBrushes {
    // Needed on both property and on getter for AndroidX build, but the Kotlin compiler doesn't
    // like it on the getter so suppress its complaint.
    @ExperimentalInkCustomBrushApi
    @get:ExperimentalInkCustomBrushApi
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @JvmStatic
    public val predictionFadeOutBehavior: BrushBehavior =
        BrushBehavior(
            source = BrushBehavior.Source.PREDICTED_TIME_ELAPSED_IN_MILLIS,
            target = BrushBehavior.Target.OPACITY_MULTIPLIER,
            sourceValueRangeLowerBound = 0F,
            sourceValueRangeUpperBound = 24F,
            targetModifierRangeLowerBound = 1F,
            targetModifierRangeUpperBound = 0.3F,
        )

    /**
     * Version 1 of the stock marker. This brush spec will not meaningfully change in future
     * releases, even as this property is marked [@Deprecated] and a new version is added.
     */
    @JvmStatic
    public val markerV1: BrushFamily =
        BrushFamily(tip = BrushTip(behaviors = listOf(predictionFadeOutBehavior)))

    /**
     * The latest version of the stock marker brush. This brush spec may change in future releases.
     */
    @JvmStatic public val marker: BrushFamily = markerV1

    /**
     * Version 1 of the stock pressure pen. This brush spec will not meaningfully change in future
     * releases, even as this property is marked [@Deprecated] and a new version is added.
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
     * The latest version of the stock pressure pen. This brush spec may change in future releases.
     */
    @JvmStatic public val pressurePen: BrushFamily = pressurePenV1

    /**
     * Version 1 of the stock highlighter. This brush spec will not meaningfully change in future
     * releases, even as this property is marked [@Deprecated] and a new version is added.
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
     * The latest version of the stock highlighter. This brush spec may change in future releases.
     */
    @JvmStatic public val highlighter: BrushFamily = highlighterV1
}
