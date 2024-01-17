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

package androidx.compose.ui.platform

import android.view.View
import androidx.collection.IntObjectMap
import androidx.collection.MutableIntSet
import androidx.collection.mutableIntSetOf
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.OwnerScope
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.util.fastForEach

/**
 * A snapshot of the semantics node. The children here is fixed and are taken from the time
 * this node is constructed. While a SemanticsNode always contains the up-to-date children.
 */
internal class SemanticsNodeCopy(
    semanticsNode: SemanticsNode,
    currentSemanticsNodes: IntObjectMap<SemanticsNodeWithAdjustedBounds>
) {
    val unmergedConfig = semanticsNode.unmergedConfig
    val children: MutableIntSet = mutableIntSetOf()

    init {
        semanticsNode.replacedChildren.fastForEach { child ->
            if (currentSemanticsNodes.contains(child.id)) {
                children.add(child.id)
            }
        }
    }
}

internal fun getTextLayoutResult(configuration: SemanticsConfiguration): TextLayoutResult? {
    val textLayoutResults = mutableListOf<TextLayoutResult>()
    val getLayoutResult = configuration.getOrNull(SemanticsActions.GetTextLayoutResult)
        ?.action?.invoke(textLayoutResults) ?: return null
    return if (getLayoutResult) {
        textLayoutResults[0]
    } else {
        null
    }
}

/**
 * These objects are used as snapshot observation scopes for the purpose of sending accessibility
 * scroll events whenever the scroll offset changes.  There is one per scroller and their lifecycle
 * is the same as the scroller's lifecycle in the semantics tree.
 */
internal class ScrollObservationScope(
    val semanticsNodeId: Int,
    val allScopes: List<ScrollObservationScope>,
    var oldXValue: Float?,
    var oldYValue: Float?,
    var horizontalScrollAxisRange: ScrollAxisRange?,
    var verticalScrollAxisRange: ScrollAxisRange?
) : OwnerScope {
    override val isValidOwnerScope get() = allScopes.contains(this)
}

internal fun List<ScrollObservationScope>.findById(id: Int): ScrollObservationScope? {
    for (index in indices) {
        if (this[index].semanticsNodeId == id) {
            return this[index]
        }
    }
    return null
}

internal fun Role.toLegacyClassName(): String? =
    when (this) {
        Role.Button -> "android.widget.Button"
        Role.Checkbox -> "android.widget.CheckBox"
        Role.RadioButton -> "android.widget.RadioButton"
        Role.Image -> "android.widget.ImageView"
        Role.DropdownList -> "android.widget.Spinner"
        else -> null
    }

/**
 * This function retrieves the View corresponding to a semanticsId, if it exists.
 */
internal fun AndroidViewsHandler.semanticsIdToView(id: Int): View? =
    layoutNodeToHolder.entries.firstOrNull { it.key.semanticsId == id }?.value

internal fun LayoutNode.isAncestorOf(node: LayoutNode): Boolean {
    val p = node.parent ?: return false
    return (p == this) || isAncestorOf(p)
}
