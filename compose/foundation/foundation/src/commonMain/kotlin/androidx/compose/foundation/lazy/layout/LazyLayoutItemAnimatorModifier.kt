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

package androidx.compose.foundation.lazy.layout

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

internal fun Modifier.lazyLayoutItemAnimator(animator: LazyLayoutItemAnimator<*>): Modifier =
    this then DisplayingDisappearingItemsElement(animator)

private data class DisplayingDisappearingItemsElement(
    private val animator: LazyLayoutItemAnimator<*>
) : ModifierNodeElement<DisplayingDisappearingItemsNode>() {
    override fun create() = DisplayingDisappearingItemsNode(animator)

    override fun update(node: DisplayingDisappearingItemsNode) {
        node.setAnimator(animator)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "DisplayingDisappearingItemsElement"
    }
}

private data class DisplayingDisappearingItemsNode(
    private var animator: LazyLayoutItemAnimator<*>
) : Modifier.Node(), DrawModifierNode {
    override fun ContentDrawScope.draw() {
        with(animator) { onDrawDisappearingItems() }
        drawContent()
    }

    override fun onAttach() {
        animator.displayingNode = this
    }

    override fun onDetach() {
        animator.reset()
    }

    fun setAnimator(animator: LazyLayoutItemAnimator<*>) {
        if (this.animator != animator) {
            if (node.isAttached) {
                this.animator.reset()
                animator.displayingNode = this
                this.animator = animator
            }
        }
    }
}
