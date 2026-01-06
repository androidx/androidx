/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.mechanics.compose.modifier

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.layout.ApproachLayoutModifierNode
import androidx.compose.ui.layout.ApproachMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost
import com.android.mechanics.ManagedMotionValue
import com.android.mechanics.debug.DebugMotionValueNode
import com.android.mechanics.effects.VerticalTactileSurfaceRevealEffect
import com.android.mechanics.spec.Mapping
import com.android.mechanics.spec.MotionSpec
import com.android.mechanics.spec.builder.ComposeMotionBuilderContext
import com.android.mechanics.spec.builder.fixedSpatialValueSpec
import com.android.mechanics.spec.builder.motionBuilderContext
import com.android.mechanics.spec.builder.spatialMotionSpec
import kotlin.math.roundToInt

/**
 * This component remains hidden until its target height meets a minimum threshold. At that point,
 * it reveals itself by animating its height from 0 to the current target height.
 */
fun Modifier.verticalTactileSurfaceReveal(
    deltaY: Float = 0f,
    effectSpec: VerticalTactileSurfaceRevealEffect = VerticalTactileSurfaceRevealDefaults.spec,
    label: String? = null,
): Modifier =
    this then
        VerticalTactileSurfaceRevealElement(
            deltaY = deltaY,
            effectSpec = effectSpec,
            label = label,
            animatedValuesForTests = null,
        )

internal object VerticalTactileSurfaceRevealDefaults {
    val spec = VerticalTactileSurfaceRevealEffect()
}

@VisibleForTesting
internal fun Modifier.verticalTactileSurfaceReveal(
    deltaY: Float = 0f,
    effectSpec: VerticalTactileSurfaceRevealEffect = VerticalTactileSurfaceRevealDefaults.spec,
    label: String? = null,
    animatedValuesForTests: AnimatedValuesForTests,
): Modifier =
    this then
        VerticalTactileSurfaceRevealElement(
            deltaY = deltaY,
            effectSpec = effectSpec,
            label = label,
            animatedValuesForTests = animatedValuesForTests,
        )

@VisibleForTesting
internal class AnimatedValuesForTests {
    var offsetY = Float.NaN
    var height = Float.NaN
    var radius = Float.NaN
}

private data class VerticalTactileSurfaceRevealElement(
    val deltaY: Float,
    val effectSpec: VerticalTactileSurfaceRevealEffect,
    val label: String?,
    val animatedValuesForTests: AnimatedValuesForTests?,
) : ModifierNodeElement<VerticalTactileSurfaceRevealNode>() {
    override fun create(): VerticalTactileSurfaceRevealNode =
        VerticalTactileSurfaceRevealNode(
            deltaY = deltaY,
            effectSpec = effectSpec,
            label = label,
            animatedValuesForTests = animatedValuesForTests,
        )

    override fun update(node: VerticalTactileSurfaceRevealNode) {
        check(node.deltaY == deltaY) { "Cannot update deltaY from ${node.deltaY} to $deltaY" }
        node.update(effectSpec = effectSpec)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "tactileSurfaceReveal"
        properties["deltaY"] = deltaY
        properties["effectSpec"] = effectSpec
        properties["label"] = label
    }
}

private class VerticalTactileSurfaceRevealNode(
    val deltaY: Float,
    private var effectSpec: VerticalTactileSurfaceRevealEffect,
    private val label: String?,
    private val animatedValuesForTests: AnimatedValuesForTests?,
) : DelegatingNode(), ApproachLayoutModifierNode, CompositionLocalConsumerModifierNode {
    // These properties are calculated during the lookahead pass (`lookAheadMeasure`) to
    // orchestrate the reveal animation. They are guaranteed to be updated before `approachMeasure`
    // is called.
    private var lookAheadHeight by mutableFloatStateOf(Float.NaN)
    private var layoutOffsetY by mutableFloatStateOf(Float.NaN)

    // Created lazily upon first lookahead and disposed in `onDetach`.
    private var revealHeight: ManagedMotionValue? = null

    /**
     * The [MotionDriver] that controls the parent's motion, used to determine the reveal
     * animation's progress.
     *
     * It is initialized in `onAttach` and is safe to use in all subsequent measure passes.
     */
    private lateinit var motionDriver: MotionDriver

    private lateinit var motionBuilderContext: ComposeMotionBuilderContext

    override fun onAttach() {
        motionDriver = findMotionDriver()
        motionBuilderContext = motionBuilderContext()
    }

    fun update(effectSpec: VerticalTactileSurfaceRevealEffect) {
        this.effectSpec = effectSpec
    }

    override fun onDetach() {
        revealHeight?.dispose()
        revealHeight = null
    }

    private fun spec(): MotionSpec {
        return when (motionDriver.verticalState) {
            MotionDriver.State.MinValue -> {
                motionBuilderContext.fixedSpatialValueSpec(0f)
            }

            MotionDriver.State.Transition -> {
                // Cache the state read to avoid the performance cost of accessing it twice.
                val start = layoutOffsetY
                motionBuilderContext.spatialMotionSpec(Mapping.Zero) {
                    between(start = start, end = start + lookAheadHeight, effect = effectSpec)
                }
            }

            MotionDriver.State.MaxValue -> {
                motionBuilderContext.fixedSpatialValueSpec(lookAheadHeight)
            }
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        return if (isLookingAhead) {
            lookAheadMeasure(measurable, constraints)
        } else {
            measurable.measure(constraints).run { layout(width, height) { place(IntOffset.Zero) } }
        }
    }

    private fun MeasureScope.lookAheadMeasure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        val targetHeight = placeable.height.toFloat()
        lookAheadHeight = targetHeight
        return layout(placeable.width, placeable.height) {
            layoutOffsetY = with(motionDriver) { driverOffset() }.y + deltaY

            if (revealHeight == null) {
                val maxHeightDriven =
                    motionDriver.maxHeightDriven(
                        spec = derivedStateOf(::spec)::value,
                        label = "TactileSurfaceReveal(${label.orEmpty()})",
                    )
                revealHeight = maxHeightDriven
                delegate(DebugMotionValueNode(maxHeightDriven))
            }

            placeable.place(IntOffset.Zero)
        }
    }

    override fun isMeasurementApproachInProgress(lookaheadSize: IntSize): Boolean {
        val revealHeight = revealHeight
        return revealHeight != null &&
            (motionDriver.verticalState == MotionDriver.State.Transition || !revealHeight.isStable)
    }

    override fun ApproachMeasureScope.approachMeasure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        return measurable.measure(constraints).run {
            layout(width, height) {
                placeWithLayer(IntOffset.Zero) {
                    val heightRevealed =
                        constraints
                            .constrainHeight(checkNotNull(revealHeight).output.roundToInt())
                            .toFloat()

                    if (heightRevealed != lookAheadHeight) {
                        approachGraphicsLayer(heightRevealed)
                    }
                }
            }
        }
    }

    private fun GraphicsLayerScope.approachGraphicsLayer(heightRevealed: Float) {
        val maxHeight = lookAheadHeight

        // Center the element vertically (translationY + shape) and clip
        clip = true
        val heightLeft = heightRevealed - maxHeight
        translationY = heightLeft / 2f
        shape = GenericShape { placeableSize, _ ->
            val phase2HeightStart = maxHeight * effectSpec.phase2HeightPercentStart

            val phase1MarginXMax =
                effectSpec.phase1MarginX.toPx().fastCoerceAtMost(phase2HeightStart)
            val phase1Progress =
                (phase2HeightStart - heightRevealed).fastCoerceAtLeast(0f) / phase2HeightStart
            val offsetX = phase1MarginXMax * phase1Progress
            val offsetY = -translationY

            val rect =
                Rect(
                    Offset(offsetX, offsetY),
                    Size(placeableSize.width - (offsetX * 2f), heightRevealed),
                )

            val radiusMax = effectSpec.maxCornerSize().toPx().fastCoerceAtMost(maxHeight / 2f)
            val radius = (heightRevealed / 2f).fastCoerceAtMost(radiusMax)

            animatedValuesForTests?.let {
                animatedValuesForTests.offsetY = offsetY
                animatedValuesForTests.height = heightRevealed
                animatedValuesForTests.radius = radius
            }

            if (radius != 0f) {
                addRoundRect(RoundRect(rect, CornerRadius(radius)))
            } else {
                addRect(rect)
            }
        }

        val fullyVisibleMinHeight = effectSpec.phase1HeightMin.toPx()
        if (fullyVisibleMinHeight != 0f) {
            val revealAlpha = (heightRevealed / fullyVisibleMinHeight).fastCoerceAtLeast(0f)
            if (revealAlpha < 1f) {
                alpha = revealAlpha
                compositingStrategy = CompositingStrategy.ModulateAlpha
            }
        }
    }
}
