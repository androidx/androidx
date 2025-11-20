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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.internal.Strings
import androidx.compose.material3.adaptive.layout.internal.delegableSemantics
import androidx.compose.material3.adaptive.layout.internal.getString
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch

internal fun Modifier.paneExpansionDraggable(
    state: PaneExpansionState,
    minTouchTargetSize: Dp,
    interactionSource: MutableInteractionSource,
    semanticsProperties: (SemanticsPropertyReceiver.() -> Unit)?,
    animateFraction: () -> Float,
    lookaheadScope: LookaheadScope,
): Modifier =
    this.draggable(
            state = state.draggableState,
            orientation = Orientation.Horizontal,
            interactionSource = interactionSource,
            onDragStopped = { velocity -> state.settleToAnchorIfNeeded(velocity) },
        )
        .semanticsAction(semanticsProperties, interactionSource)
        .systemGestureExclusion()
        .animateWithFading(
            enabled = true,
            animateFraction = animateFraction,
            lookaheadScope = lookaheadScope,
        )
        .semanticsWithDefaults(state, semanticsProperties)
        .then(MinTouchTargetSizeElement(minTouchTargetSize))

/**
 * This function sets up the default semantics of pane expansion drag handles with the given
 * [PaneExpansionState]. It will provide suitable [contentDescription] as well as [onClick] function
 * to move the pane expansion among anchors that can be operated via accessibility services.
 *
 * It's supposed to be used with a [PaneScaffoldScope.paneExpansionDraggable] modifier, or a plain
 * [semantics] modifier associated with a drag handle composable.
 */
@Deprecated(
    message =
        "Just omit the semanticsProperties parameter when using " +
            "Modifier.paneExpansionDraggable to set up the default semantics",
    level = DeprecationLevel.WARNING,
)
@ExperimentalMaterial3AdaptiveApi
@Composable
fun PaneExpansionState.defaultDragHandleSemantics(): SemanticsPropertyReceiver.() -> Unit {
    val coroutineScope = rememberCoroutineScope()
    val contentDesc = getString(Strings.defaultPaneExpansionDragHandleContentDescription)
    val currentAnchor = currentAnchor
    val stateDesc =
        if (currentAnchor != null) {
            getString(
                Strings.defaultPaneExpansionDragHandleStateDescription,
                currentAnchor.description,
            )
        } else {
            null
        }
    val nextAnchor = nextAnchor
    val actionLabel =
        if (nextAnchor != null) {
            getString(
                Strings.defaultPaneExpansionDragHandleActionDescription,
                nextAnchor.description,
            )
        } else {
            null
        }
    return semantics@{
        contentDescription = contentDesc
        if (stateDesc != null) {
            stateDescription = stateDesc
        }
        if (nextAnchor == null) {
            // TODO(conrachen): handle this case
            return@semantics
        }
        onClick(label = actionLabel) {
            coroutineScope.launch { animateTo(nextAnchor) }
            return@onClick true
        }
    }
}

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
        ((parentData as? PaneScaffoldParentDataImpl) ?: PaneScaffoldParentDataImpl()).also {
            it.minTouchTargetSize = size
        }
}

private fun Modifier.semanticsAction(
    semanticsProperties: (SemanticsPropertyReceiver.() -> Unit)?,
    interactionSource: MutableInteractionSource,
): Modifier {
    return this.then(
        semanticsProperties?.onClickAction?.let {
            Modifier.clickable(interactionSource = interactionSource, indication = null) { it() }
        } ?: Modifier
    )
}

private val (SemanticsPropertyReceiver.() -> Unit).onClickAction: (() -> Boolean)?
    get() = SemanticsConfiguration().also { it.this() }.getOrNull(SemanticsActions.OnClick)?.action

private fun Modifier.semanticsWithDefaults(
    state: PaneExpansionState,
    semanticsProperties: (SemanticsPropertyReceiver.() -> Unit)?,
): Modifier =
    if (semanticsProperties != null) {
        semantics(mergeDescendants = true, properties = semanticsProperties)
    } else {
        delegableSemantics(mergeDescendants = true) {
            val contentDesc = getString(Strings.defaultPaneExpansionDragHandleContentDescription)
            val currentAnchor = state.currentAnchor
            val stateDesc =
                currentAnchor?.run {
                    getString(
                        Strings.defaultPaneExpansionDragHandleStateDescription,
                        this@delegableSemantics.description,
                    )
                }
            val nextAnchor = state.nextAnchor
            val actionLabel =
                nextAnchor?.run {
                    getString(
                        Strings.defaultPaneExpansionDragHandleActionDescription,
                        this@delegableSemantics.description,
                    )
                }

            contentDescription = contentDesc
            if (stateDesc != null) {
                stateDescription = stateDesc
            }
            if (nextAnchor == null) {
                // TODO(conrachen): handle this case
                return@delegableSemantics
            }
            onClick(label = actionLabel) {
                coroutineScope.launch { state.animateTo(nextAnchor) }
                return@onClick true
            }
        }
    }
