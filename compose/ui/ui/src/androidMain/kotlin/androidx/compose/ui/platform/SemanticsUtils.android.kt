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

import android.annotation.SuppressLint
import android.graphics.Region
import android.view.View
import androidx.collection.IntObjectMap
import androidx.collection.MutableIntObjectMap
import androidx.collection.MutableIntSet
import androidx.collection.emptyIntObjectMap
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.node.OwnerScope
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastRoundToInt
import androidx.core.view.accessibility.AccessibilityNodeProviderCompat

/**
 * A snapshot of the semantics node. The children here is fixed and are taken from the time this
 * node is constructed. While a SemanticsNode always contains the up-to-date children.
 */
internal class SemanticsNodeCopy(
    semanticsNode: SemanticsNode,
    currentSemanticsNodes: IntObjectMap<SemanticsNodeWithAdjustedBounds>
) {
    val unmergedConfig = semanticsNode.unmergedConfig
    // Root node must always be considered visible and sent to listening services
    val isTransparent = if (semanticsNode.isRoot) false else semanticsNode.isTransparent
    val children: MutableIntSet = MutableIntSet(semanticsNode.replacedChildren.size)

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
    val getLayoutResult =
        configuration
            .getOrNull(SemanticsActions.GetTextLayoutResult)
            ?.action
            ?.invoke(textLayoutResults) ?: return null
    return if (getLayoutResult) {
        textLayoutResults[0]
    } else {
        null
    }
}

@SuppressLint("PrimitiveInCollection")
internal fun getScrollViewportLength(configuration: SemanticsConfiguration): Float? {
    val viewPortCalculationsResult = mutableListOf<Float>()
    val actionResult =
        configuration
            .getOrNull(SemanticsActions.GetScrollViewportLength)
            ?.action
            ?.invoke(viewPortCalculationsResult) ?: return null
    return if (actionResult) {
        viewPortCalculationsResult[0]
    } else {
        null
    }
}

/**
 * These objects are used as snapshot observation scopes for the purpose of sending accessibility
 * scroll events whenever the scroll offset changes. There is one per scroller and their lifecycle
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
    override val isValidOwnerScope
        get() = allScopes.contains(this)
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
        Role.ValuePicker -> "android.widget.NumberPicker"
        else -> null
    }

internal fun SemanticsNode.isImportantForAccessibility() =
    isVisible &&
        (unmergedConfig.isMergingSemanticsOfDescendants ||
            unmergedConfig.containsImportantForAccessibility())

// TODO(347749977): go through and remove experimental tag on `invisible` properties
@OptIn(ExperimentalComposeUiApi::class)
internal val SemanticsNode.isVisible: Boolean
    get() = !isTransparent && !unmergedConfig.contains(SemanticsProperties.InvisibleToUser)

internal val DefaultFakeNodeBounds = Rect(0f, 0f, 10f, 10f)

/** Semantics node with adjusted bounds for the uncovered(by siblings) part. */
internal class SemanticsNodeWithAdjustedBounds(
    val semanticsNode: SemanticsNode,
    val adjustedBounds: android.graphics.Rect
)

/** This function retrieves the View corresponding to a semanticsId, if it exists. */
internal fun AndroidViewsHandler.semanticsIdToView(id: Int): View? =
    layoutNodeToHolder.entries.firstOrNull { it.key.semanticsId == id }?.value

/**
 * Finds pruned [SemanticsNode]s in the tree owned by this [SemanticsOwner]. A semantics node
 * completely covered by siblings drawn on top of it will be pruned. Return the results in a map.
 */
internal fun SemanticsOwner.getAllUncoveredSemanticsNodesToIntObjectMap():
    IntObjectMap<SemanticsNodeWithAdjustedBounds> {
    val root = unmergedRootSemanticsNode
    if (!root.layoutNode.isPlaced || !root.layoutNode.isAttached) {
        return emptyIntObjectMap()
    }

    // Default capacity chosen to accommodate common scenarios
    val nodes = MutableIntObjectMap<SemanticsNodeWithAdjustedBounds>(48)

    val unaccountedSpace =
        with(root.boundsInRoot) {
            Region(
                left.fastRoundToInt(),
                top.fastRoundToInt(),
                right.fastRoundToInt(),
                bottom.fastRoundToInt()
            )
        }

    fun findAllSemanticNodesRecursive(currentNode: SemanticsNode, region: Region) {
        val notAttachedOrPlaced =
            !currentNode.layoutNode.isPlaced || !currentNode.layoutNode.isAttached
        if (
            (unaccountedSpace.isEmpty && currentNode.id != root.id) ||
                (notAttachedOrPlaced && !currentNode.isFake)
        ) {
            return
        }
        val touchBoundsInRoot = currentNode.touchBoundsInRoot
        val left = touchBoundsInRoot.left.fastRoundToInt()
        val top = touchBoundsInRoot.top.fastRoundToInt()
        val right = touchBoundsInRoot.right.fastRoundToInt()
        val bottom = touchBoundsInRoot.bottom.fastRoundToInt()

        region.set(left, top, right, bottom)

        val virtualViewId =
            if (currentNode.id == root.id) {
                AccessibilityNodeProviderCompat.HOST_VIEW_ID
            } else {
                currentNode.id
            }
        if (region.op(unaccountedSpace, Region.Op.INTERSECT)) {
            nodes[virtualViewId] = SemanticsNodeWithAdjustedBounds(currentNode, region.bounds)
            // Children could be drawn outside of parent, but we are using clipped bounds for
            // accessibility now, so let's put the children recursion inside of this if. If later
            // we decide to support children drawn outside of parent, we can move it out of the
            // if block.
            val children = currentNode.replacedChildren
            for (i in children.size - 1 downTo 0) {
                // Links in text nodes are semantics children. But for Android accessibility support
                // we don't publish them to the accessibility services because they are exposed
                // as UrlSpan/ClickableSpan spans instead
                if (children[i].config.contains(SemanticsProperties.LinkTestMarker)) {
                    continue
                }
                findAllSemanticNodesRecursive(children[i], region)
            }
            if (currentNode.isImportantForAccessibility()) {
                unaccountedSpace.op(left, top, right, bottom, Region.Op.DIFFERENCE)
            }
        } else {
            if (currentNode.isFake) {
                val parentNode = currentNode.parent
                // use parent bounds for fake node
                val boundsForFakeNode =
                    if (parentNode?.layoutInfo?.isPlaced == true) {
                        parentNode.boundsInRoot
                    } else {
                        DefaultFakeNodeBounds
                    }
                nodes[virtualViewId] =
                    SemanticsNodeWithAdjustedBounds(
                        currentNode,
                        android.graphics.Rect(
                            boundsForFakeNode.left.fastRoundToInt(),
                            boundsForFakeNode.top.fastRoundToInt(),
                            boundsForFakeNode.right.fastRoundToInt(),
                            boundsForFakeNode.bottom.fastRoundToInt(),
                        )
                    )
            } else if (virtualViewId == AccessibilityNodeProviderCompat.HOST_VIEW_ID) {
                // Root view might have WRAP_CONTENT layout params in which case it will have zero
                // bounds if there is no other content with semantics. But we need to always send
                // the
                // root view info as there are some other apps (e.g. Google Assistant) that depend
                // on accessibility info
                nodes[virtualViewId] = SemanticsNodeWithAdjustedBounds(currentNode, region.bounds)
            }
        }
    }

    findAllSemanticNodesRecursive(root, Region())
    return nodes
}
