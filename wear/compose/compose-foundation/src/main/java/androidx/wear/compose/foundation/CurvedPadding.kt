/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.compose.foundation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Apply additional space along the edges of the content.
 *
 * @param paddingValues The [ArcPaddingValues] to use. See that class and factory methods to see how
 *   paddings can be specified.
 */
public fun CurvedModifier.padding(paddingValues: ArcPaddingValues) =
    this.then { child -> PaddingWrapper(child, paddingValues) }

/**
 * Apply additional space along the edges of the content. Dimmensions are in dp. For before and
 * after they will be considered as if they are at the midpoint of the content (for conversion
 * between dimension and angle).
 *
 * @param outer The space to add to the outer edge of the content (away from the center of the
 *   containing CurvedLayout)
 * @param inner The space to add to the inner edge of the content (towards the center of the
 *   containing CurvedLayout)
 * @param before The space added before the component, if it was draw clockwise. This is the edge of
 *   the component with the "smallest" angle.
 * @param after The space added after the component, if it was draw clockwise. This is the edge of
 *   the component with the "biggest" angle.
 */
public fun CurvedModifier.padding(outer: Dp, inner: Dp, before: Dp, after: Dp) =
    padding(ArcPaddingValuesImpl(outer, inner, before, after))

/**
 * Apply [angular] dp space before and after the component, and [radial] dp space to the outer and
 * inner edges.
 *
 * @param radial The space added to the outer and inner edges of the content, in dp.
 * @param angular The space added before and after the content, in dp.
 */
public fun CurvedModifier.padding(radial: Dp = 0.dp, angular: Dp = 0.dp) =
    padding(radial, radial, angular, angular)

/**
 * Apply [all] dp space around the component.
 *
 * @param all The space added to all edges.
 */
public fun CurvedModifier.padding(all: Dp = 0.dp) = padding(all, all, all, all)

/**
 * Apply additional space along each edge of the content in [Dp]. See the [ArcPaddingValues]
 * factories for convenient ways to build [ArcPaddingValues].
 */
@Stable
public interface ArcPaddingValues {
    /** Padding in the outward direction from the center of the [CurvedLayout] */
    fun calculateOuterPadding(radialDirection: CurvedDirection.Radial): Dp

    /** Padding in the inwards direction towards the center of the [CurvedLayout] */
    fun calculateInnerPadding(radialDirection: CurvedDirection.Radial): Dp

    /**
     * Padding added before the component, if it was draw clockwise. This is the edge of the
     * component with the "smallest" angle.
     */
    fun calculateAfterPadding(
        layoutDirection: LayoutDirection,
        angularDirection: CurvedDirection.Angular
    ): Dp

    /**
     * Padding added after the component, if it was draw clockwise. This is the edge of the
     * component with the "biggest" angle.
     */
    fun calculateBeforePadding(
        layoutDirection: LayoutDirection,
        angularDirection: CurvedDirection.Angular
    ): Dp
}

/**
 * Apply additional space along each edge of the content in [Dp]. Note that that all dimensions are
 * applied to a concrete edge, indepenend on layout direction and curved layout direction.
 *
 * @param outer Padding in the outward direction from the center of the [CurvedLayout]
 * @param inner Padding in the inwards direction towards the center of the [CurvedLayout]
 * @param before Padding added before the component, if it was draw clockwise.
 * @param after Padding added after the component, if it was draw clockwise.
 */
public fun ArcPaddingValues(
    outer: Dp = 0.dp,
    inner: Dp = 0.dp,
    before: Dp = 0.dp,
    after: Dp = 0.dp
): ArcPaddingValues = ArcPaddingValuesImpl(outer, inner, before, after)

/** Apply [all] dp of additional space along each edge of the content. */
public fun ArcPaddingValues(all: Dp): ArcPaddingValues = ArcPaddingValuesImpl(all, all, all, all)

/**
 * Apply [radial] dp of additional space on the edges towards and away from the center, and
 * [angular] dp before and after the component.
 */
public fun ArcPaddingValues(radial: Dp = 0.dp, angular: Dp = 0.dp): ArcPaddingValues =
    ArcPaddingValuesImpl(radial, radial, angular, angular)

@Immutable
internal class ArcPaddingValuesImpl(val outer: Dp, val inner: Dp, val before: Dp, val after: Dp) :
    ArcPaddingValues {
    override fun equals(other: Any?): Boolean {
        return other is ArcPaddingValuesImpl &&
            outer == other.outer &&
            inner == other.inner &&
            before == other.before &&
            after == other.after
    }

    override fun hashCode() =
        ((outer.hashCode() * 31 + inner.hashCode()) * 31 + before.hashCode()) * 31 +
            after.hashCode()

    override fun toString(): String {
        return "ArcPaddingValuesImpl(outer=$outer, inner=$inner, before=$before, after=$after)"
    }

    override fun calculateOuterPadding(radialDirection: CurvedDirection.Radial) = outer

    override fun calculateInnerPadding(radialDirection: CurvedDirection.Radial) = inner

    override fun calculateBeforePadding(
        layoutDirection: LayoutDirection,
        angularDirection: CurvedDirection.Angular
    ) = before

    override fun calculateAfterPadding(
        layoutDirection: LayoutDirection,
        angularDirection: CurvedDirection.Angular
    ) = after
}

internal class PaddingWrapper(child: CurvedChild, val paddingValues: ArcPaddingValues) :
    BaseCurvedChildWrapper(child) {
    private var outerPx = 0f
    private var innerPx = 0f
    private var beforePx = 0f
    private var afterPx = 0f

    override fun CurvedMeasureScope.initializeMeasure(measurables: Iterator<Measurable>) {
        outerPx = paddingValues.calculateOuterPadding(curvedLayoutDirection.radial).toPx()
        innerPx = paddingValues.calculateInnerPadding(curvedLayoutDirection.radial).toPx()
        beforePx =
            paddingValues
                .calculateBeforePadding(
                    curvedLayoutDirection.layoutDirection,
                    curvedLayoutDirection.angular
                )
                .toPx()
        afterPx =
            paddingValues
                .calculateAfterPadding(
                    curvedLayoutDirection.layoutDirection,
                    curvedLayoutDirection.angular
                )
                .toPx()
        with(wrapped) { initializeMeasure(measurables) }
    }

    override fun doEstimateThickness(maxRadius: Float) =
        wrapped.estimateThickness(maxRadius) + outerPx + innerPx

    override fun doRadialPosition(
        parentOuterRadius: Float,
        parentThickness: Float
    ): PartialLayoutInfo {
        val partialLayoutInfo =
            wrapped.radialPosition(parentOuterRadius - outerPx, parentThickness - outerPx - innerPx)
        val angularPadding = (beforePx + afterPx) / partialLayoutInfo.measureRadius
        return PartialLayoutInfo(
            partialLayoutInfo.sweepRadians + angularPadding,
            partialLayoutInfo.outerRadius + outerPx,
            partialLayoutInfo.thickness + innerPx + outerPx,
            partialLayoutInfo.measureRadius
        )
    }

    override fun doAngularPosition(
        parentStartAngleRadians: Float,
        parentSweepRadians: Float,
        centerOffset: Offset
    ): Float {
        val startAngularPadding = beforePx / measureRadius
        val angularPadding = (beforePx + afterPx) / measureRadius
        return wrapped.angularPosition(
            parentStartAngleRadians + startAngularPadding,
            parentSweepRadians - angularPadding,
            centerOffset
        ) - startAngularPadding
    }
}
