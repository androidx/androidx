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

package androidx.wear.compose.material3.lazy

import androidx.annotation.FloatRange
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.util.lerp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScrollProgress

/**
 * Defines visual transformations on the items of a [TransformingLazyColumn].
 *
 * When using this API, users need to make similar changes between all of the functions. For
 * example, if [getTransformedHeight] returns half of the size of the item, then transformation
 * functions should do the same, scaling or cropping the item.
 *
 * [getTransformedHeight] is called first, then the painter would be created and then container and
 * content transformations are applied.
 *
 * This shows how to create a custom transformation spec for the [TransformingLazyColumn].
 *
 * @sample androidx.wear.compose.material3.samples.CustomTransformationSpecSample
 *
 * This shows how to apply the [TransformationSpec] to custom component inside
 * [TransformingLazyColumn].
 *
 * @sample androidx.wear.compose.material3.samples.TransformationSpecButtonRowSample
 */
public interface TransformationSpec {
    /**
     * Calculates the transformed height to be passed into
     * [TransformingLazyColumnItemScope.transformedHeight] based on the parameters for the spec.
     *
     * @param measuredHeight The height in pixels of the item returned during measurement.
     * @param scrollProgress The scroll progress of the item.
     */
    public fun getTransformedHeight(
        measuredHeight: Int,
        scrollProgress: TransformingLazyColumnItemScrollProgress,
    ): Int

    /**
     * Visual transformations to be applied to the content of the item as it scrolls.
     *
     * @param scrollProgress The scroll progress of the item.
     */
    public fun GraphicsLayerScope.applyContentTransformation(
        scrollProgress: TransformingLazyColumnItemScrollProgress
    )

    /**
     * Visual transformations to be applied to the container of the item as it scrolls.
     *
     * @param scrollProgress The scroll progress of the item.
     */
    public fun GraphicsLayerScope.applyContainerTransformation(
        scrollProgress: TransformingLazyColumnItemScrollProgress
    )

    /**
     * Returns a new painter to be used instead of [painter] which should react on a transformation.
     *
     * @param painter The painter to be transformed. This is the original [Painter] the component
     *   was trying to use.
     * @param shape The shape of the item's container.
     * @param border The border of the item's container.
     */
    public fun TransformedContainerPainterScope.createTransformedContainerPainter(
        painter: Painter,
        shape: Shape,
        border: BorderStroke?,
    ): Painter
}

/** Convenience modifier to calculate transformed height using [TransformationSpec]. */
public fun Modifier.transformedHeight(
    scope: TransformingLazyColumnItemScope,
    transformationSpec: TransformationSpec,
): Modifier = with(scope) { transformedHeight(transformationSpec::getTransformedHeight) }

/** Provides additional information to the painter inside [TransformationSpec]. */
public interface TransformedContainerPainterScope {
    /** The progress of the scroll. */
    public val DrawScope.scrollProgress: TransformingLazyColumnItemScrollProgress

    /**
     * The height of the item in pixels. This might be different from
     * [TransformationSpec.getTransformedHeight] in cases graphical effects like clipping are
     * applied before scale.
     */
    public val DrawScope.itemHeight: Float
}

/**
 * This class represents the configuration parameters for one variable that changes as the item
 * moves on the screen and will be used to apply the corresponding transformation - for example:
 * container alpha. When an item enters from the top of the screen the value of this variable will
 * be [topValue]. As the item's bottom edge moves through the top transformation zone (inside the
 * top transition area), this variable will change from [topValue] to target value (target value is
 * the nominal value of this variable, with no transformation applied, such as 1f for alpha). When
 * the item's top edge moves through the bottom transformation zone (inside the bottom transition
 * area), this variable will change from the target value into [bottomValue], and keep that value
 * until it leaves the screen. The same process happens in reverse, entering from the bottom of the
 * screen and leaving at the top.
 */
public class TransformationVariableSpec(
    /**
     * The value this variable will have when the item's bottom edge is above the top transformation
     * zone, usually this happens when it is (or is about to be) partially outside of the screen on
     * the top side.
     */
    @FloatRange(from = 0.0, to = 1.0) public val topValue: Float,

    /**
     * The value this variable will have when the item is not in either transformation zone, and is
     * in the "center" of the screen, i.e. the top edge is above the bottom transformation zone, and
     * the bottom edge is below the top transformation zone.
     */
    @FloatRange(from = 0.0, to = 1.0) public val targetValue: Float = 1f,

    /**
     * The value this variable will have when the item's top edge is below the bottom transformation
     * zone, usually this happens when it is (or is about to be) partially outside of the screen on
     * the bottom side.
     */
    @FloatRange(from = 0.0, to = 1.0) public val bottomValue: Float = topValue,

    /**
     * Defines how far into the transition area the transformation zone starts. For example, a value
     * of 0.5f means that when the item enters the screen from the top, this variable will not start
     * to transform until the bottom of the item reaches the middle point of the transition area.
     * Should be less than [transformationZoneExitFraction].
     *
     * Visually, the top transition area and top transformation zone can be visualized as:
     * ```
     *    ˅  --------------------- <-- Top of the screen
     *    |  |                   |
     *    |  |-------------------| <- Enter fraction, item moving down
     * TA |  |        TZ         | <- Transformation Zone
     *    |  |-------------------| <- Exit fraction, item moving down
     *    |  |                   |
     *    ^  |-------------------|
     * ```
     *
     * On the bottom, it is the same mirrored vertically (exit fraction is above enter fraction). It
     * is also worth noting that in the bottom, it is the top of the item that triggers starting and
     * ending transformations.
     */
    @FloatRange(from = 0.0, to = 1.0) public val transformationZoneEnterFraction: Float = 0f,

    /**
     * Defines how far into the transition area the transformation zone ends. For example, a value
     * of 0.5f means that when the item is moving down, its bottom edge needs to reach the middle
     * point of the transition area for this variable to reach it's maximum/target value. Should be
     * greater than [transformationZoneEnterFraction].
     *
     * See also [transformationZoneEnterFraction]
     */
    @FloatRange(from = 0.0, to = 1.0) public val transformationZoneExitFraction: Float = 1f,
) {
    init {
        require(transformationZoneEnterFraction in 0f..1f) {
            "transformationZoneEnterFraction must be between 0 and 1, inclusive"
        }
        require(transformationZoneExitFraction in 0f..1f) {
            "transformationZoneExitFraction must be between 0 and 1, inclusive"
        }
        require(transformationZoneEnterFraction < transformationZoneExitFraction) {
            "transformationZoneEnterFraction must be less than transformationZoneExitFraction"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TransformationVariableSpec

        if (topValue != other.topValue) return false
        if (targetValue != other.targetValue) return false
        if (bottomValue != other.bottomValue) return false
        if (transformationZoneEnterFraction != other.transformationZoneEnterFraction) return false
        if (transformationZoneExitFraction != other.transformationZoneExitFraction) return false

        return true
    }

    override fun hashCode(): Int {
        var result = topValue.hashCode()
        result = 31 * result + targetValue.hashCode()
        result = 31 * result + bottomValue.hashCode()
        result = 31 * result + transformationZoneEnterFraction.hashCode()
        result = 31 * result + transformationZoneExitFraction.hashCode()
        return result
    }

    override fun toString(): String {
        return "TransformationVariableSpec(topValue=$topValue, targetValue=$targetValue, bottomValue=$bottomValue, transformationZoneEnterFraction=$transformationZoneEnterFraction, transformationZoneExitFraction=$transformationZoneExitFraction)"
    }

    /** Returns a copy of the TransformationVariableSpec. */
    public fun copy(
        topValue: Float = this.topValue,
        targetValue: Float = this.targetValue,
        bottomValue: Float = this.bottomValue,
        transformationZoneEnterFraction: Float = this.transformationZoneEnterFraction,
        transformationZoneExitFraction: Float = this.transformationZoneExitFraction,
    ): TransformationVariableSpec {
        return TransformationVariableSpec(
            topValue,
            targetValue,
            bottomValue,
            transformationZoneEnterFraction,
            transformationZoneExitFraction,
        )
    }
}

/** Helper function to lerp between the variables for different screen sizes. */
public fun lerp(
    start: TransformationVariableSpec,
    stop: TransformationVariableSpec,
    progress: Float,
): TransformationVariableSpec =
    TransformationVariableSpec(
        topValue = lerp(start.topValue, stop.topValue, progress),
        targetValue = lerp(start.targetValue, stop.targetValue, progress),
        bottomValue = lerp(start.bottomValue, stop.bottomValue, progress),
        transformationZoneEnterFraction =
            lerp(
                start.transformationZoneEnterFraction,
                stop.transformationZoneEnterFraction,
                progress,
            ),
        transformationZoneExitFraction =
            lerp(
                start.transformationZoneExitFraction,
                stop.transformationZoneExitFraction,
                progress,
            ),
    )
