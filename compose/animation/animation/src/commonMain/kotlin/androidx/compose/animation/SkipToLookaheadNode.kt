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

package androidx.compose.animation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrain
import androidx.compose.ui.unit.toSize
import kotlin.math.roundToInt

@OptIn(ExperimentalSharedTransitionApi::class)
internal class SkipToLookaheadNode(scaleToBounds: ScaleToBoundsImpl?, isEnabled: () -> Boolean) :
    LayoutModifierNode, Modifier.Node() {
    var scaleToBounds: ScaleToBoundsImpl? by mutableStateOf(scaleToBounds)
    var isEnabled: () -> Boolean by mutableStateOf(isEnabled)

    private var lookaheadConstraints: Constraints? = null
    private var lookaheadSize: IntSize = InvalidSize

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        if (isLookingAhead) {
            lookaheadConstraints = constraints
        }
        if (!isEnabled()) {
            return measurable.measure(constraints).run { layout(width, height) { place(0, 0) } }
        }
        val p: Placeable =
            if (isLookingAhead) {
                measurable.measure(constraints).also {
                    lookaheadSize = IntSize(it.width, it.height)
                }
            } else {
                measurable.measure(lookaheadConstraints!!)
            }
        val constrainedSize = constraints.constrain(lookaheadSize)
        return layout(constrainedSize.width, constrainedSize.height) {
            val scaleToBounds = scaleToBounds
            if (scaleToBounds == null) {
                p.place(0, 0)
            } else {
                val contentScale = scaleToBounds.contentScale
                val resolvedScale =
                    if (lookaheadSize.width == 0 || lookaheadSize.height == 0) {
                        ScaleFactor(1f, 1f)
                    } else
                        contentScale.computeScaleFactor(
                            lookaheadSize.toSize(),
                            constrainedSize.toSize(),
                        )

                val (x, y) =
                    scaleToBounds.alignment.align(
                        IntSize(
                            (lookaheadSize.width * resolvedScale.scaleX).roundToInt(),
                            (lookaheadSize.height * resolvedScale.scaleY).roundToInt(),
                        ),
                        constrainedSize,
                        layoutDirection,
                    )
                p.placeWithLayer(x, y) {
                    scaleX = resolvedScale.scaleX
                    scaleY = resolvedScale.scaleY
                    transformOrigin = TransformOrigin(0f, 0f)
                }
            }
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int,
    ): Int {
        // If lookahead has already occurred, return the lookahead width/height to skip propagating
        // the call further, and ensure convergence with lookahead.
        return if (!isLookingAhead && lookaheadSize.isValid) {
            lookaheadSize.width
        } else {
            measurable.maxIntrinsicWidth(height)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int,
    ): Int {
        // If lookahead has already occurred, return the lookahead width/height to skip propagating
        // the call further, and ensure convergence with lookahead.
        return if (!isLookingAhead && lookaheadSize.isValid) {
            lookaheadSize.width
        } else {
            measurable.minIntrinsicWidth(height)
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int,
    ): Int {
        // If lookahead has already occurred, return the lookahead width/height to skip propagating
        // the call further, and ensure convergence with lookahead.
        return if (!isLookingAhead && lookaheadSize.isValid) {
            lookaheadSize.height
        } else {
            measurable.maxIntrinsicHeight(width)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int,
    ): Int {
        // If lookahead has already occurred, return the lookahead width/height to skip propagating
        // the call further, and ensure convergence with lookahead.
        return if (!isLookingAhead && lookaheadSize.isValid) {
            lookaheadSize.height
        } else {
            measurable.minIntrinsicHeight(width)
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
internal data class SkipToLookaheadElement(
    val scaleToBounds: ScaleToBoundsImpl? = null,
    val isEnabled: () -> Boolean = DefaultEnabled,
) : ModifierNodeElement<SkipToLookaheadNode>() {
    override fun create(): SkipToLookaheadNode {
        return SkipToLookaheadNode(scaleToBounds, isEnabled)
    }

    override fun update(node: SkipToLookaheadNode) {
        node.scaleToBounds = scaleToBounds
        node.isEnabled = isEnabled
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "skipToLookahead"
        properties["scaleToBounds"] = scaleToBounds
        properties["isEnabled"] = isEnabled
    }
}

private val DefaultEnabled: () -> Boolean = { true }

@OptIn(ExperimentalSharedTransitionApi::class)
internal fun Modifier.createContentScaleModifier(
    scaleToBounds: ScaleToBoundsImpl,
    isEnabled: () -> Boolean,
): Modifier =
    this.then(
        if (scaleToBounds.contentScale == ContentScale.Crop) {
            Modifier.graphicsLayer { clip = isEnabled() }
        } else Modifier
    ) then SkipToLookaheadElement(scaleToBounds, isEnabled)
