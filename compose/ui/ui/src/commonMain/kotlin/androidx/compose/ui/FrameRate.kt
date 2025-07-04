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

package androidx.compose.ui

import androidx.annotation.FloatRange
import androidx.compose.ui.ComposeUiFlags.isAdaptiveRefreshRateEnabled
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.NodeCoordinator
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.findNearestAncestor
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.util.fastForEach

/**
 * Set a requested frame rate on Composable
 *
 * You can set the preferred frame rate (frames per second) for a Composable using a non-negative
 * number. This API should only be used when a specific frame rate is needed for your Composable.
 * For example, 24 or 30 for video play.
 *
 * If multiple frame rates are requested, they will be aggregated to determine a feasible frame
 * rate.
 *
 * If you no longer want this modifier to influence the frame rate, clear the preference by setting
 * it to 0.
 *
 * Keep in mind that the preferred frame rate affects the frame rate for the next frame, so use this
 * method carefully. It's important to note that the preference is valid as long as the Composable
 * is drawn.
 *
 * @param frameRate The preferred frame rate the content should be rendered at. Default value is 0.
 * @sample androidx.compose.ui.samples.SetFrameRateSample
 * @see graphicsLayer
 */
fun Modifier.preferredFrameRate(@FloatRange(from = 0.0, to = 360.0) frameRate: Float) =
    if (@OptIn(ExperimentalComposeUiApi::class) isAdaptiveRefreshRateEnabled) {
        this.graphicsLayer().frameRate(frameRate)
    } else {
        this
    }

/**
 * Set a requested frame rate on Composable
 *
 * You can set the preferred frame rate (frames per second) for a Composable using a frame rate
 * category see: [FrameRateCategory].
 *
 * For increased frame rates, please consider using FrameRateCategory.High.
 *
 * Keep in mind that the preferred frame rate affects the frame rate for the next frame, so use this
 * method carefully. It's important to note that the preference is valid as long as the Composable
 * is drawn.
 *
 * @param frameRateCategory The preferred frame rate category the content should be rendered at.
 * @sample androidx.compose.ui.samples.SetFrameRateCategorySample
 * @see graphicsLayer
 */
fun Modifier.preferredFrameRate(frameRateCategory: FrameRateCategory) =
    if (@OptIn(ExperimentalComposeUiApi::class) isAdaptiveRefreshRateEnabled) {
        this.graphicsLayer().frameRate(frameRateCategory.value)
    } else {
        this
    }

private fun Modifier.frameRate(frameRate: Float) = this then FrameRateElement(frameRate)

private data class FrameRateElement(val frameRate: Float) :
    ModifierNodeElement<FrameRateModifierNode>() {

    override fun create(): FrameRateModifierNode = FrameRateModifierNode(frameRate)

    override fun update(node: FrameRateModifierNode) {
        if (node.frameRate != frameRate) {
            node.shouldUpdateFrameRates = true
            node.frameRate = frameRate
            // Invalidates this modifier's draw layer to handle frame rate override
            node.invalidateDraw()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "FrameRateModifierNode"
        properties["frameRate"] = frameRate
    }
}

internal class FrameRateModifierNode(var frameRate: Float) :
    Modifier.Node(), TraversableNode, DrawModifierNode {

    var shouldUpdateFrameRates = true
    override val traverseKey = "TRAVERSAL_NODE_KEY_FRAME_RATE_MODIFIER_NODE"

    override fun onAttach() {
        // for the use case that the node is attached again after detached
        shouldUpdateFrameRates = true
    }

    override fun onDetach() {
        // This modifier overrides the frame rates of child nodes,
        // so we need to revert its effect when the modifier is removed.
        findNearestAncestor()?.let { ancestor ->
            setChildrenLayerFrameRate(coordinator?.wrapped, ancestor.frameRate)
        } ?: setChildrenLayerFrameRate(coordinator?.wrapped, 0f)
    }

    override fun ContentDrawScope.draw() {
        // Handle the frame rate override
        if (shouldUpdateFrameRates) {
            coordinator?.layer?.frameRate = frameRate
            setChildrenLayerFrameRate(coordinator?.wrapped, frameRate)
            shouldUpdateFrameRates = false
        }
        drawContent()
    }

    fun setChildrenLayerFrameRate(nodeCoordinator: NodeCoordinator?, frameRate: Float) {
        var node = nodeCoordinator
        while (node != null) {
            nodeCoordinator?.layer?.let { layer ->
                if (layer.frameRate == 0f || layer.isFrameRateFromParent) {
                    layer.frameRate = frameRate
                    layer.isFrameRateFromParent = frameRate != 0f
                } else {
                    // A developer has set a frame rate for this layer. To avoid overriding
                    // this setting with inherited frame rates in this layer and its child layers,
                    // return early to stop further processing.
                    return
                }
            }
            node = node.wrapped
        }
        val coordinatorToUse = nodeCoordinator ?: coordinator
        coordinatorToUse?.layoutNode?.children?.fastForEach {
            setChildrenLayerFrameRate(it.outerCoordinator, frameRate)
        }
    }
}
