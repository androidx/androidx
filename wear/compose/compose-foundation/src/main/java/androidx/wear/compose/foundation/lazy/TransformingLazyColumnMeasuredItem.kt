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

package androidx.wear.compose.foundation.lazy

import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScrollProgress.Companion.downwardMeasuredItemScrollProgress
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScrollProgress.Companion.upwardMeasuredItemScrollProgress
import androidx.wear.compose.foundation.lazy.layout.LazyLayoutItemAnimation
import androidx.wear.compose.foundation.lazy.layout.LazyLayoutMeasuredItem

/** Represents a placeable item in the [TransformingLazyColumn] layout. */
internal data class TransformingLazyColumnMeasuredItem(
    /** The index of the item in the list. */
    override val index: Int,
    /** The [Placeable] representing the content of the item, or null if no composable is inside. */
    val placeable: Placeable?,
    /** The constraints of the container holding the item. */
    val containerConstraints: Constraints,
    /** The vertical offset of the item from the top of the list after transformations applied. */
    override var offset: Int,
    /** The spacing after the item. */
    val spacing: Int,
    /**
     * The horizontal padding before the item. This doesn't affect vertical calculations, but needs
     * to be added to during final placement.
     */
    val leftPadding: Int,
    /**
     * The horizontal padding after the item. This doesn't affect vertical calculations, but needs
     * to be added to during final placement.
     */
    val rightPadding: Int,

    /** The scroll progress computed a the end of the measure pass. */
    private var measureScrollProgress: TransformingLazyColumnItemScrollProgress,
    override var measurementDirection: MeasurementDirection,
    /** The horizontal alignment to apply during placement. */
    val horizontalAlignment: Alignment.Horizontal,
    /** The [LayoutDirection] of the `Layout`. */
    private val layoutDirection: LayoutDirection,
    val animationProvider: () -> LazyLayoutItemAnimation? = { null },
    override val key: Any,
    override val contentType: Any?,
) : TransformingLazyColumnVisibleItemInfo, LazyLayoutMeasuredItem {

    // This is the value of the ScrollProgress, either the one set at the end of the measure pass
    // if there are no animations configured or the one computed by the animation, updated each
    // frame.
    override val scrollProgress: TransformingLazyColumnItemScrollProgress
        get() =
            // Ignore the animations during measure pass.
            if (isInMeasure) {
                measureScrollProgress
            } else {
                // We call animationProvider() directly to get the most current animation instance.
                // - During an active animation, this returns a stable instance.
                // - After a scroll, this returns the newly re-created instance, which is
                //   essential for subsequent animations to work correctly.
                animationProvider()?.animatedScrollProgress.let {
                    if (it == TransformingLazyColumnItemScrollProgress.Unspecified)
                        measureScrollProgress
                    else it
                } ?: measureScrollProgress
            }

    override val mainAxisSizeWithSpacings: Int
        get() = transformedHeight + spacing

    override val constraints = containerConstraints

    override val mainAxisOffset: Int
        get() = offset

    override val crossAxisOffset: Int
        get() = leftPadding

    override val parentData: Any? =
        placeable?.parentData?.let {
            if (it is TransformingLazyColumnParentData) {
                it.animationSpecs
            } else {
                it
            }
        }

    private var lastMeasuredTransformedHeight = placeable?.height ?: 0

    /** The height of the item after transformations applied. */
    override val transformedHeight: Int
        get() {
            if (isInMeasure) {
                lastMeasuredTransformedHeight =
                    // TODO: Save transformedHeight provider.
                    placeable?.let { p ->
                        (p.parentData as? TransformingLazyColumnParentData)?.let {
                            it.heightProvider?.invoke(p.height, measureScrollProgress)
                        } ?: p.height
                    } ?: 0
            }
            return lastMeasuredTransformedHeight
        }

    fun markMeasured() {
        // Force read the transformed height to update the lastMeasuredTransformedHeight.
        transformedHeight
        isInMeasure = false
    }

    fun moveBy(delta: Int, measurementDirection: MeasurementDirection) {
        when (measurementDirection) {
            MeasurementDirection.UPWARD -> {
                val bottomOffset = offset + transformedHeight + delta
                measureScrollProgress =
                    upwardMeasuredItemScrollProgress(
                        offset = bottomOffset,
                        height = measuredHeight,
                        containerHeight = containerConstraints.maxHeight,
                    )
                offset = bottomOffset - transformedHeight // transformed height is updated
            }
            MeasurementDirection.DOWNWARD -> {
                offset += delta
                measureScrollProgress =
                    downwardMeasuredItemScrollProgress(
                        offset = offset,
                        height = measuredHeight,
                        containerHeight = containerConstraints.maxHeight,
                    )
            }
        }
        this.measurementDirection = measurementDirection
    }

    fun moveAbove(offset: Int) {
        measureScrollProgress =
            upwardMeasuredItemScrollProgress(
                offset = offset,
                height = measuredHeight,
                containerHeight = containerConstraints.maxHeight,
            )
        measurementDirection = MeasurementDirection.UPWARD
        this.offset = offset - transformedHeight
    }

    fun moveBelow(offset: Int) {
        this.offset = offset
        measurementDirection = MeasurementDirection.DOWNWARD
        measureScrollProgress =
            downwardMeasuredItemScrollProgress(
                offset = offset,
                height = measuredHeight,
                containerHeight = containerConstraints.maxHeight,
            )
    }

    /**
     * Whether the item is currently being measured. Must be set to false before returning as a
     * measured result.
     */
    var isInMeasure: Boolean = true
        private set

    override val measuredHeight = placeable?.height ?: 0

    fun place(scope: Placeable.PlacementScope) =
        with(scope) {
            placeable?.let { placeable ->
                var intOffset =
                    IntOffset(
                        x =
                            leftPadding +
                                horizontalAlignment.align(
                                    space =
                                        containerConstraints.maxWidth - rightPadding - leftPadding,
                                    size = placeable.width,
                                    layoutDirection = layoutDirection,
                                ),
                        y = offset,
                    )
                val currentAnimation = animationProvider()
                if (currentAnimation == null) {
                    placeable.placeWithLayer(intOffset)
                } else {
                    intOffset += currentAnimation.placementDelta
                    currentAnimation.layer?.let { placeable.placeWithLayer(intOffset, it) }
                        ?: placeable.placeWithLayer(intOffset)
                    currentAnimation.finalOffset = intOffset
                }
            }
        }

    fun pinToCenter() {
        measureScrollProgress =
            downwardMeasuredItemScrollProgress(
                containerConstraints.maxHeight / 2 - measuredHeight / 2,
                measuredHeight,
                containerConstraints.maxHeight,
            )
        offset = containerConstraints.maxHeight / 2 - transformedHeight / 2
    }
}
