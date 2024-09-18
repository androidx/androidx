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

package androidx.compose.material3.adaptive.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A default, basic non-customizable implementation of pane expansion drag handle. Note that this
 * implementation will be deprecated in favor of the corresponding Material3 implementation when
 * it's available.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
// TODO(b/327637983): Implement this as a customizable component as a Material3 component.
fun ThreePaneScaffoldScope.PaneExpansionDragHandle(
    state: PaneExpansionState,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val animationProgress = { motionProgress }
    // The outer Box to account for the actual touch target area
    Box(
        modifier =
            modifier
                // TODO(b/327637983): Moves to use LocalMinimumInteractiveComponentSize when moving
                //   to Material3
                .paneExpansionDragHandle(state, DragHandleMinTouchTargetSize)
                .animateWithFading(
                    enabled = true,
                    animateFraction = animationProgress,
                    lookaheadScope = this
                ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier =
                Modifier.size(4.dp, 48.dp)
                    .graphicsLayer(shape = CircleShape, clip = true)
                    .background(color)
        )
    }
}

@ExperimentalMaterial3AdaptiveApi
internal fun Modifier.paneExpansionDragHandle(
    state: PaneExpansionState,
    minTouchTargetSize: Dp
): Modifier =
    this.draggable(
            state = state,
            orientation = Orientation.Horizontal,
            onDragStopped = { velocity -> state.settleToAnchorIfNeeded(velocity) }
        )
        .systemGestureExclusion()
        .requiredWidthIn(min = minTouchTargetSize)
        .requiredHeightIn(min = minTouchTargetSize)
        .then(MinTouchTargetSizeElement(DragHandleMinTouchTargetSize))

internal expect fun Modifier.systemGestureExclusion(): Modifier

private data class MinTouchTargetSizeElement(val size: Dp) :
    ModifierNodeElement<MinTouchTargetSizeNode>() {
    private val inspectorInfo = debugInspectorInfo {
        name = "minTouchTargetSize"
        properties["size"] = size
    }

    override fun create(): MinTouchTargetSizeNode {
        return MinTouchTargetSizeNode(size)
    }

    override fun update(node: MinTouchTargetSizeNode) {
        node.size = size
    }

    override fun InspectorInfo.inspectableProperties() {
        inspectorInfo()
    }
}

private class MinTouchTargetSizeNode(var size: Dp) : ParentDataModifierNode, Modifier.Node() {
    override fun Density.modifyParentData(parentData: Any?) =
        ((parentData as? PaneScaffoldParentData) ?: PaneScaffoldParentData()).also {
            it.minTouchTargetSize = size
        }
}

private val DragHandleMinTouchTargetSize = 48.dp
