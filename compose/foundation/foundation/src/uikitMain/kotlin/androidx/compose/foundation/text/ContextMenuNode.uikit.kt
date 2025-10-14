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

package androidx.compose.foundation.text

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.uikit.LocalUIView
import androidx.compose.ui.uikit.utils.CMPEditMenuView
import androidx.compose.ui.unit.Density
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIView

internal class ContextMenuLayoutElement(
    private val editMenuView: CMPEditMenuView
) : ModifierNodeElement<ContextMenuLayoutNode>() {

    override fun create(): ContextMenuLayoutNode {
        return ContextMenuLayoutNode(editMenuView = editMenuView)
    }

    override fun update(node: ContextMenuLayoutNode) {
        node.update(editMenuView = editMenuView)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ContextMenuLayoutElement) return false

        if (editMenuView != other.editMenuView) return false

        return true
    }

    override fun hashCode(): Int {
        return editMenuView.hashCode()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "contextMenu"
        properties["editMenuView"] = editMenuView
    }
}

internal class ContextMenuLayoutNode(
    var editMenuView: CMPEditMenuView
) : Modifier.Node(),
    CompositionLocalConsumerModifierNode,
    GlobalPositionAwareModifierNode,
    ObserverModifierNode {

    /**
     * Current density provided by [LocalDensity]. Used as a receiver to callback functions that
     * are expected return pixel targeted offsets.
     */
    private var density: Density? = null

    private var containerView: UIView? = null

    fun update(editMenuView: CMPEditMenuView) {
        if (this.editMenuView != editMenuView) {
            this.editMenuView.removeFromSuperview()
            this.editMenuView = editMenuView

            if (isAttached) {
                containerView?.addSubview(editMenuView)
            }
        }
    }

    override fun onAttach() {
        onObservedReadsChanged()
    }

    override fun onDetach() {
        editMenuView.removeFromSuperview()
        containerView = null
        density = null
    }

    override fun onObservedReadsChanged() {
        observeReads {
            val previousContainerView = containerView
            val currentContainerView = try {
                currentValueOf(LocalUIView)
            } catch (_: IllegalStateException) {
                null
            }
            density = currentValueOf(LocalDensity)

            if (previousContainerView != currentContainerView) {
                containerView = currentContainerView
                editMenuView.removeFromSuperview()
                containerView?.addSubview(editMenuView)
            }
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val density = density ?: return
        editMenuView.setFrame(coordinates.boundsInRoot().toCGRect(density))
    }
}

internal fun Rect.toCGRect(density: Density) = with(density) {
    CGRectMake(
        x = topLeft.x.toDp().value.toDouble(),
        y = topLeft.y.toDp().value.toDouble(),
        width = width.toDp().value.toDouble(),
        height = height.toDp().value.toDouble()
    )
}