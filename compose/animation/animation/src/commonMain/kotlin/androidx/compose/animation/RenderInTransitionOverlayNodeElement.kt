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

@file:OptIn(ExperimentalSharedTransitionApi::class)

package androidx.compose.animation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.modifier.ModifierLocalModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.InspectorInfo

internal class RenderInTransitionOverlayNodeElement(
    var sharedTransitionScope: SharedTransitionScopeImpl,
    var renderInOverlay: () -> Boolean,
    val zIndexInOverlay: Float,
) : ModifierNodeElement<RenderInTransitionOverlayNode>() {
    override fun create(): RenderInTransitionOverlayNode {
        return RenderInTransitionOverlayNode(
            sharedTransitionScope,
            renderInOverlay,
            zIndexInOverlay,
        )
    }

    override fun update(node: RenderInTransitionOverlayNode) {
        node.sharedScope = sharedTransitionScope
        node.renderInOverlay = renderInOverlay
        node.zIndexInOverlay = zIndexInOverlay
    }

    override fun hashCode(): Int =
        (sharedTransitionScope.hashCode() * 31 + renderInOverlay.hashCode()) * 31 +
            zIndexInOverlay.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other is RenderInTransitionOverlayNodeElement) {
            return sharedTransitionScope == other.sharedTransitionScope &&
                renderInOverlay === other.renderInOverlay &&
                zIndexInOverlay == other.zIndexInOverlay
        }
        return false
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "renderInSharedTransitionOverlay"
        properties["sharedTransitionScope"] = sharedTransitionScope
        properties["renderInOverlay"] = renderInOverlay
        properties["zIndexInOverlay"] = zIndexInOverlay
    }
}

internal class RenderInTransitionOverlayNode(
    var sharedScope: SharedTransitionScopeImpl,
    var renderInOverlay: () -> Boolean,
    zIndexInOverlay: Float,
) : Modifier.Node(), DrawModifierNode, ModifierLocalModifierNode {
    var zIndexInOverlay by mutableFloatStateOf(zIndexInOverlay)

    val parentState: SharedElementEntry?
        get() = ModifierLocalSharedElementInternalState.current

    private inner class LayerWithRenderer(val layer: GraphicsLayer) : LayerRenderer {
        override val parentState: SharedElementEntry?
            get() = this@RenderInTransitionOverlayNode.parentState

        override val zIndex: Float
            get() = this@RenderInTransitionOverlayNode.zIndexInOverlay

        override fun drawInOverlay(drawScope: DrawScope) {
            if (renderInOverlay()) {
                with(drawScope) {
                    val (x, y) =
                        sharedScope.root.localPositionOf(
                            this@RenderInTransitionOverlayNode.requireLayoutCoordinates(),
                            Offset.Zero,
                        )
                    translate(x, y) { drawLayer(layer) }
                }
            }
        }
    }

    // Render in-place logic. Depending on the result of `renderInOverlay()`, the content will
    // either render in-place or in the overlay, but never in both places.
    override fun ContentDrawScope.draw() {
        val layer = requireNotNull(layer) { "Error: layer never initialized" }
        layer.record { this@draw.drawContent() }
        if (!renderInOverlay()) {
            drawLayer(layer)
        }
    }

    val layer: GraphicsLayer?
        get() = layerWithRenderer?.layer

    private var layerWithRenderer: LayerWithRenderer? = null

    override fun onAttach() {
        LayerWithRenderer(requireGraphicsContext().createGraphicsLayer()).let {
            sharedScope.onLayerRendererCreated(it)
            layerWithRenderer = it
        }
    }

    override fun onDetach() {
        layerWithRenderer?.let {
            sharedScope.onLayerRendererRemoved(it)
            requireGraphicsContext().releaseGraphicsLayer(it.layer)
        }
    }
}
