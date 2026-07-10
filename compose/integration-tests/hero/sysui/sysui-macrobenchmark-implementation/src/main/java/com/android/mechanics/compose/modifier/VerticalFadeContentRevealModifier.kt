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

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
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
import androidx.compose.ui.util.fastCoerceAtLeast
import com.android.mechanics.ManagedMotionValue
import com.android.mechanics.debug.DebugMotionValueNode
import com.android.mechanics.effects.FixedValue
import com.android.mechanics.spec.Mapping
import com.android.mechanics.spec.MotionSpec
import com.android.mechanics.spec.builder.ComposeMotionBuilderContext
import com.android.mechanics.spec.builder.effectsMotionSpec
import com.android.mechanics.spec.builder.fixedEffectsValueSpec
import com.android.mechanics.spec.builder.motionBuilderContext

/** This component remains hidden until it reach its target height. */
fun Modifier.verticalFadeContentReveal(deltaY: Float = 0f, label: String? = null): Modifier =
    this then FadeContentRevealElement(deltaY = deltaY, label = label)

private data class FadeContentRevealElement(val deltaY: Float, val label: String?) :
    ModifierNodeElement<FadeContentRevealNode>() {
    override fun create(): FadeContentRevealNode =
        FadeContentRevealNode(deltaY = deltaY, label = label)

    override fun update(node: FadeContentRevealNode) {
        check(node.deltaY == deltaY) { "Cannot update deltaY from ${node.deltaY} to $deltaY" }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "fadeContentReveal"
        properties["deltaY"] = deltaY
        properties["label"] = label
    }
}

private class FadeContentRevealNode(val deltaY: Float, private val label: String?) :
    DelegatingNode(), ApproachLayoutModifierNode, CompositionLocalConsumerModifierNode {
    // These properties are calculated during the lookahead pass (`lookAheadMeasure`) to
    // orchestrate the reveal animation. They are guaranteed to be updated before `approachMeasure`
    // is called.
    private var lookAheadHeight by mutableFloatStateOf(Float.NaN)
    private var layoutOffsetY by mutableFloatStateOf(Float.NaN)
    // Created lazily upon first lookahead and disposed in `onDetach`.
    private var revealAlpha: ManagedMotionValue? = null

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

    override fun onDetach() {
        revealAlpha?.dispose()
        revealAlpha = null
    }

    private fun spec(): MotionSpec {
        return when (motionDriver.verticalState) {
            MotionDriver.State.MinValue -> {
                motionBuilderContext.fixedEffectsValueSpec(0f)
            }
            MotionDriver.State.Transition -> {
                motionBuilderContext.effectsMotionSpec(Mapping.Zero) {
                    after(layoutOffsetY + lookAheadHeight, FixedValue.One)
                }
            }
            MotionDriver.State.MaxValue -> {
                motionBuilderContext.fixedEffectsValueSpec(1f)
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

            if (revealAlpha == null) {
                val maxHeightDriven =
                    motionDriver.maxHeightDriven(
                        spec = derivedStateOf(::spec)::value,
                        label = "FadeContentReveal(${label.orEmpty()})",
                    )
                revealAlpha = maxHeightDriven
                delegate(DebugMotionValueNode(maxHeightDriven))
            }

            placeable.place(IntOffset.Zero)
        }
    }

    override fun isMeasurementApproachInProgress(lookaheadSize: IntSize): Boolean {
        val revealAlpha = revealAlpha
        return revealAlpha != null &&
            (motionDriver.verticalState == MotionDriver.State.Transition || !revealAlpha.isStable)
    }

    override fun ApproachMeasureScope.approachMeasure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        return measurable.measure(constraints).run {
            layout(width, height) {
                placeWithLayer(IntOffset.Zero) {
                    val revealAlpha = checkNotNull(revealAlpha).output.fastCoerceAtLeast(0f)
                    if (revealAlpha < 1f) {
                        alpha = revealAlpha
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                    }
                }
            }
        }
    }
}
