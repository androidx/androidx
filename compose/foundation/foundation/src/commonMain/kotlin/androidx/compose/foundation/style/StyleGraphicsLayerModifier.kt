/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.foundation.style

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.updateLayerBlock
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints

/**
 * A [Modifier.Node] that makes content draw into a draw layer. The draw layer can be invalidated
 * separately from parents. A [graphicsLayer] should be used when the content updates independently
 * from anything above it to minimize the invalidated content.
 *
 * [graphicsLayer] can be used to apply effects to content, such as scaling, rotation, opacity,
 * shadow, and clipping. Prefer this version when you have layer properties backed by a
 * [androidx.compose.runtime.State] or an animated value as reading a state inside [block] will only
 * cause the layer properties update without triggering recomposition and relayout.
 *
 * NOTE: [block] can be invoked multiple times, which is why it's important for performance to
 * minimize work done inside of it. [block] may also be invoked before effects.
 *
 * [layerRequired] can be used to avoid creating a layer that will do nothing. The avoids the
 * overhead of the layer if no properties will be modified from their default value.
 *
 * @param layerRequired return whether the layer is required. If the parameter is `null` or
 *   [layerRequired] returns `true`, a layer is created. If [layerRequired] returns `false` then the
 *   layer is not created and [block] is not invoked. This can be used to determine if a layer is
 *   necessary based on the properties that will be set by [block].
 * @param block block on [GraphicsLayerScope] where you define the layer properties.
 */
@ExperimentalFoundationStyleApi
public fun Modifier.graphicsLayer(
    layerRequired: (() -> Boolean)? = null,
    block: GraphicsLayerScope.() -> Unit,
): Modifier = this then StyleGraphicsLayerElement(layerRequired, block)

@ExperimentalFoundationStyleApi
internal class StyleGraphicsLayerElement(
    val layerRequired: (() -> Boolean)?,
    val block: GraphicsLayerScope.() -> Unit,
) : ModifierNodeElement<StyleGraphicsLayerNode>() {
    override fun create(): StyleGraphicsLayerNode = StyleGraphicsLayerNode(layerRequired, block)

    override fun update(node: StyleGraphicsLayerNode) {
        node.update(layerRequired, block)
    }

    override fun hashCode(): Int {
        var hash = block.hashCode()
        hash *= 31
        layerRequired?.let { hash += it.hashCode() }
        return hash
    }

    override fun equals(other: Any?): Boolean =
        other is StyleGraphicsLayerElement &&
            layerRequired === other.layerRequired &&
            block === other.block

    override fun InspectorInfo.inspectableProperties() {
        name = "graphicsLayer"
        properties["layerRequired"] = layerRequired
        properties["block"] = block
    }
}

@ExperimentalFoundationStyleApi
internal class StyleGraphicsLayerNode(
    var layerRequired: (() -> Boolean)?,
    block: GraphicsLayerScope.() -> Unit,
) : DelegatingNode(), LayoutModifierNode, ObserverModifierNode {
    private var _block = block
    private var lastBlock: (GraphicsLayerScope.() -> Unit)? = null
    private var _layerBlockCache: (GraphicsLayerScope.() -> Unit)? = null
    private val layerBlock: GraphicsLayerScope.() -> Unit
        get() {
            if (lastBlock !== _block || _layerBlockCache == null) {
                val block = _block
                lastBlock = block
                _layerBlockCache = { observeReads { block() } }
            }
            return _layerBlockCache!!
        }

    fun update(layerRequired: (() -> Boolean)?, block: GraphicsLayerScope.() -> Unit) {
        val invalidateLayout = this.layerRequired !== layerRequired
        val invalidateLayer = _block !== block
        if (invalidateLayout || invalidateLayer) {
            this.layerRequired = layerRequired
            this._block = block
            if (invalidateLayer) updateLayerBlock(layerBlock)
            if (invalidateLayout) invalidateMeasurement()
        }
    }

    override fun onObservedReadsChanged() {
        updateLayerBlock(layerBlock)
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            val useLayer = layerRequired?.let { it() } ?: true
            if (useLayer) {
                placeable.placeWithLayer(0, 0, layerBlock = layerBlock)
            } else {
                placeable.place(0, 0)
            }
        }
    }
}

@OptIn(ExperimentalFoundationStyleApi::class)
private val graphicsLayerPropertySet =
    setOf(
        alphaProperty,
        translationXProperty,
        translationYProperty,
        transformOriginXProperty,
        transformOriginYProperty,
        rotationXProperty,
        rotationYProperty,
        rotationZProperty,
        scaleXProperty,
        scaleYProperty,
        cameraDistanceProperty,
        colorFilterProperty,
    )

/**
 * Transitionary modifier. Do not use. This will be removed before the Styles API is stable.
 *
 * This modifier is to help enable transition between the old [Style] based API and the new
 * [CustomStyle] based API. This duplicates the graphics layer behavior of [LayerStyleScope] in a
 * [Style].
 */
@ExperimentalFoundationStyleApi
public fun Modifier.styleGraphicsLayer(styleResolver: StyleResolver): Modifier =
    graphicsLayer(
        layerRequired = {
            styleResolver.resolve { anySet(graphicsLayerPropertySet) || clipProperty.value }
        }
    ) {
        styleResolver.resolve {
            clip = clipProperty.value
            shape = shapeProperty.value
            alpha = alphaProperty.value
            translationX = translationXProperty.value
            translationY = translationYProperty.value
            transformOrigin =
                TransformOrigin(transformOriginXProperty.value, transformOriginYProperty.value)
            rotationX = rotationXProperty.value
            rotationY = rotationYProperty.value
            rotationZ = rotationZProperty.value
            scaleX = scaleXProperty.value
            scaleY = scaleYProperty.value
            cameraDistance = cameraDistanceProperty.value
            colorFilter = colorFilterProperty.value
        }
    }
