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

package androidx.compose.ui.platform.accessibility

import androidx.collection.IntSet
import androidx.collection.mutableIntSetOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.Strings
import androidx.compose.ui.platform.getString
import androidx.compose.ui.semantics.AccessibilityAction
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsProperties.HideFromAccessibility
import androidx.compose.ui.semantics.SemanticsProperties.InvisibleToUser
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.findClosestParentNode
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.sortByGeometryGroupings
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import platform.UIKit.UIAccessibilityScrollDirection
import platform.UIKit.UIAccessibilityScrollDirectionDown
import platform.UIKit.UIAccessibilityScrollDirectionLeft
import platform.UIKit.UIAccessibilityScrollDirectionRight
import platform.UIKit.UIAccessibilityScrollDirectionUp


internal fun SemanticsNode.scrollIfPossible(
    direction: UIAccessibilityScrollDirection,
): AccessibilityScrollEventResult? {
    val config = this.config

    val deltaX: Int
    val deltaY: Int
    val isForward: Boolean
    val pageAction: SemanticsPropertyKey<AccessibilityAction<() -> Boolean>>
    val rangeProperty = if (direction.isHorizontal) {
        SemanticsProperties.HorizontalScrollAxisRange
    } else {
        SemanticsProperties.VerticalScrollAxisRange
    }

    val axisRange = config.getOrNull(rangeProperty)
    val isReverse = axisRange?.reverseScrolling == true
    val normalisedDirection = when (direction) {
        UIAccessibilityScrollDirectionUp -> if (isReverse) {
            UIAccessibilityScrollDirectionDown
        } else {
            UIAccessibilityScrollDirectionUp
        }

        UIAccessibilityScrollDirectionDown -> if (isReverse) {
            UIAccessibilityScrollDirectionUp
        } else {
            UIAccessibilityScrollDirectionDown
        }

        UIAccessibilityScrollDirectionRight -> if (isRTL xor isReverse) {
            UIAccessibilityScrollDirectionLeft
        } else {
            UIAccessibilityScrollDirectionRight
        }

        UIAccessibilityScrollDirectionLeft -> if (isRTL xor isReverse) {
            UIAccessibilityScrollDirectionRight
        } else {
            UIAccessibilityScrollDirectionLeft
        }

        else -> return null
    }

    when (normalisedDirection) {
        UIAccessibilityScrollDirectionUp -> {
            deltaX = 0
            deltaY = -size.height
            isForward = false
            pageAction = SemanticsActions.PageUp
        }

        UIAccessibilityScrollDirectionDown -> {
            deltaX = 0
            deltaY = size.height
            isForward = true
            pageAction = SemanticsActions.PageDown
        }

        UIAccessibilityScrollDirectionRight -> {
            deltaX = -size.width
            deltaY = 0
            isForward = false
            pageAction = SemanticsActions.PageLeft
        }

        UIAccessibilityScrollDirectionLeft -> {
            deltaX = size.width
            deltaY = 0
            isForward = true
            pageAction = SemanticsActions.PageRight
        }

        else -> return null
    }

    val succeeded = config.getOrNull(pageAction)?.action?.invoke()
        ?: config.getOrNull(SemanticsActions.ScrollBy)
            ?.action
            ?.invoke(deltaX.toFloat(), deltaY.toFloat())

    return when (succeeded) {
        true -> AccessibilityScrollEventResult(
            announceMessage = {
                announceMessage(isForward, config.getOrNull(rangeProperty))
            }
        )

        false -> null
        null -> parent?.scrollIfPossible(direction)
    }
}

private fun announceMessage(isForward: Boolean, range: ScrollAxisRange?): String? {
    range ?: return null
    return if (range.value() == 0f) {
        getString(Strings.FirstPage)
    } else if (range.value() == range.maxValue()) {
        getString(Strings.LastPage)
    } else if (isForward) {
        getString(Strings.NextPage)
    } else {
        getString(Strings.PreviousPage)
    }
}

internal data class AccessibilityScrollEventResult(
    val announceMessage: () -> String?,
)

/**
 * Try to perform a scroll on any ancestor of this element if the element is not fully visible.
 * @param targetRect to place in the center of scrollable area
 * @param safeAreaRectInWindow safe area rect to reduce focusable borders
 */
internal suspend fun SemanticsNode.scrollToCenterRectIfNeeded(
    targetRect: Rect,
    safeAreaRectInWindow: Rect
) {
    val scrollByOffset = unmergedConfig.getOrNull(SemanticsActions.ScrollByOffset)
        ?: unmergedConfig.getOrNull(SemanticsActions.ScrollBy)?.action?.let { mapScrollBy(it) }
        ?: return parent?.scrollToCenterRectIfNeeded(targetRect, safeAreaRectInWindow) ?: Unit

    // Inverts offset in both axes when scrolling in these axes is inverted
    fun Offset.invertIfNeeded() = Offset(
        if (isInvertedHorizontally xor isRTL) -x else x,
        if (isInvertedVertically) -y else y,
    )

    val scrollableViewportRect = unclippedBoundsInWindow.intersect(safeAreaRectInWindow)
    val clippedTargetRect = scrollableViewportRect.intersect(targetRect)
    val geometryOffset = (targetRect.center - scrollableViewportRect.center).invertIfNeeded()

    // There is no need to scroll to the target rect if it is within visible boundaries.
    val consumedOffsetIfNodeInBounds = Offset(
        x = if (clippedTargetRect.width == targetRect.width) geometryOffset.x else 0f,
        y = if (clippedTargetRect.height == targetRect.height) geometryOffset.y else 0f,
    )
    val scrollOffset = geometryOffset - consumedOffsetIfNodeInBounds

    if (scrollOffset == Offset.Zero) {
        return
    }

    val consumedByScroll = scrollByOffset(scrollOffset)
    if (consumedByScroll + consumedOffsetIfNodeInBounds != geometryOffset) {
        val translatedRect = targetRect.translate(-consumedByScroll.invertIfNeeded())
        parent?.scrollToCenterRectIfNeeded(translatedRect, safeAreaRectInWindow)
    }
}

private fun SemanticsNode.mapScrollBy(
    scrollBy: (Float, Float) -> Boolean
): suspend (offset: Offset) -> Offset = { offset: Offset ->
    val consumed = Offset(
        x = unmergedConfig.getOrNull(SemanticsProperties.HorizontalScrollAxisRange)
            .consumeScrollDirection(offset.x),
        y = unmergedConfig.getOrNull(SemanticsProperties.VerticalScrollAxisRange)
            .consumeScrollDirection(offset.y)
    )

    scrollBy(offset.x, offset.y)

    consumed
}


// Due to implementation specifics of the lazy list, it's impossible to determine the exact
// amount of scroll offset that will be consumed after scrolling. Therefore, the method will consume
// the entire value if scrolling in the given direction is available.
private fun ScrollAxisRange?.consumeScrollDirection(value: Float): Float = when {
    this == null -> 0f
    value < 0f && this.value() > 0f -> value // Can scroll backward
    value > 0f && this.value() < this.maxValue() -> value // Can scroll forward
    else -> 0f
}

private val SemanticsNode.isInvertedHorizontally: Boolean
    get() = unmergedConfig.getOrNull(SemanticsProperties.HorizontalScrollAxisRange)
        ?.reverseScrolling ?: false
private val SemanticsNode.isInvertedVertically: Boolean
    get() = unmergedConfig.getOrNull(SemanticsProperties.VerticalScrollAxisRange)
        ?.reverseScrolling ?: false

internal val SemanticsNode.unclippedBoundsInWindow: Rect
    get() = Rect(positionInWindow, size.toSize())

internal val SemanticsNode.isRTL: Boolean
    get() = layoutInfo.layoutDirection == LayoutDirection.Rtl

// Simplified version of the isScreenReaderFocusable() from the
// AndroidComposeViewAccessibilityDelegateCompat.android.kt
internal fun SemanticsNode.isScreenReaderFocusable(): Boolean {
    return !isTransparent && canBeAccessibilityElement()
}

internal fun SemanticsNode.linkText(): String? {
    val (text, annotation) = this.findCorrespondingLinkAnnotations() ?: return null

    return text.substring(annotation.start, annotation.end).takeIf { it.isNotBlank() }
}

internal fun SemanticsNode.linkTag(): String? {
    val (_, annotation) = this.findCorrespondingLinkAnnotations() ?: return null

    return annotation.item.getTag()
}

private fun SemanticsNode.findCorrespondingLinkAnnotations():
    Pair<AnnotatedString, AnnotatedString.Range<LinkAnnotation>>? {
    if (!isLink()) {
        return null
    }
    val parentNode = parent ?: return null

    // See [SemanticsNodeInteraction.performFirstLinkClick] for the logic of finding the
    // corresponding link annotation to the child node
    var linkIndex = parentNode.children
        .asSequence()
        .filter { it.isLink() }
        .indexOfFirst { it.id == id }
        .takeIf { it >= 0 } ?: return null
    val texts = parentNode.config.getOrNull(SemanticsProperties.Text) ?: return null
    for (text in texts) {
        val annotations = text.getLinkAnnotations(0, text.length)
        if (linkIndex < annotations.count()) {
            return text to annotations[linkIndex]
        } else {
            linkIndex -= annotations.count()
        }
    }
    return null
}

private fun SemanticsNode.isLink(): Boolean =
    config.getOrNull(SemanticsProperties.LinkTestMarker) != null

private fun LinkAnnotation.getTag(): String? =
    when (this) {
        is LinkAnnotation.Clickable -> this.tag
        else -> null
    }

internal fun SemanticsNode.canBeAccessibilityElement(): Boolean {
    return !isHiddenFromAccessibility &&
        (unmergedConfig.isMergingSemanticsOfDescendants || isUnmergedSpeakingNode)
}

internal val SemanticsNode.isUnmergedSpeakingNode: Boolean get() {
    if (isFake) return false
    if (!unmergedConfig.isSpeakingNode) return false
    if (unmergedConfig.isActionableNode) return true

    val hasReplacedChildren = replacedChildren.isNotEmpty()

    var currentNode = layoutNode.parent
    while (currentNode != null) {
        if (currentNode.semanticsConfiguration?.isMergingSemanticsOfDescendants == true) {
            return false
        }
        if (hasReplacedChildren && currentNode.semanticsConfiguration?.isSpeakingNode == true) {
            return false
        }
        if (currentNode.semanticsConfiguration?.getOrNull(SemanticsProperties.IsTraversalGroup) == true) {
            return true
        }
        currentNode = currentNode.parent
    }

    return replacedChildren.isEmpty()
}

internal val SemanticsConfiguration.isActionableNode: Boolean
    get() = (contains(SemanticsActions.RequestFocus) ||
        contains(SemanticsActions.OnClick) ||
        contains(SemanticsActions.OnLongClick) ||
        contains(SemanticsActions.SetProgress) ||
        contains(SemanticsActions.SetText) ||
        contains(SemanticsActions.InsertTextAtCursor) ||
        contains(SemanticsActions.SetSelection) ||
        contains(SemanticsActions.OnImeAction) ||
        contains(SemanticsActions.CopyText) ||
        contains(SemanticsActions.CutText) ||
        contains(SemanticsActions.PasteText) ||
        contains(SemanticsActions.Expand) ||
        contains(SemanticsActions.Collapse) ||
        contains(SemanticsActions.Dismiss) ||
        contains(SemanticsActions.CustomActions))

internal val SemanticsConfiguration.isSpeakingNode: Boolean get() {
    return contains(SemanticsProperties.ContentDescription) ||
        contains(SemanticsProperties.EditableText) ||
        contains(SemanticsProperties.Text) ||
        contains(SemanticsProperties.StateDescription) ||
        contains(SemanticsProperties.ToggleableState) ||
        contains(SemanticsProperties.Selected) ||
        contains(SemanticsProperties.ProgressBarRangeInfo)
}

@Suppress("DEPRECATION")
private val SemanticsNode.isHiddenFromAccessibility: Boolean
    get() = unmergedConfig.contains(HideFromAccessibility) ||
        unmergedConfig.contains(InvisibleToUser)

internal val SemanticsNode.canScroll: Boolean
    get() = unmergedConfig.contains(SemanticsActions.ScrollBy) ||
        unmergedConfig.contains(SemanticsActions.ScrollByOffset)

private val UIAccessibilityScrollDirection.isHorizontal get() =
    this == UIAccessibilityScrollDirectionRight || this == UIAccessibilityScrollDirectionLeft

internal val SemanticsNode.allScrollableParentNodeIds: IntSet get() {
    var iterator: SemanticsNode? = this
    val result = mutableIntSetOf()

    while (iterator != null) {
        if (iterator.canScroll) {
            result.add(iterator.id)
        }
        iterator = iterator.parent
    }

    return result
}

internal val SemanticsNode.contentDescription: String? get() {
    val contentDescription = config.getOrNull(SemanticsProperties.ContentDescription)
        ?.joinToString(", ")
        ?.takeIf { it.isNotBlank() }

    return contentDescription ?: if (config.contains(SemanticsProperties.EditableText)) {
        null
    } else {
        config.getOrNull(SemanticsProperties.Text)?.joinToString(", ") { it.text }
    }
}

internal fun SemanticsNode.sortFlattenChildren(children: List<SemanticsNode>): List<SemanticsNode> {
    val sortedChildren = sortByGeometryGroupings(children) as MutableList<SemanticsNode>

    // Fix the specifics of nodes sorting where a parent node may go after a child in the sorted list.
    // Swapping them if the order is not specified by other criteria as TraversalIndex.
    // In case of other sort issues, consider copy and re-implementing the `sortByGeometryGroupings`
    // method to match TalkBack application traversal order.
    repeat(sortedChildren.count() - 1) { index ->
        val first = sortedChildren[index]
        val second = sortedChildren[index + 1]
        if (!first.unmergedConfig.contains(SemanticsProperties.TraversalIndex) &&
            !second.unmergedConfig.contains(SemanticsProperties.TraversalIndex) &&
            first.layoutNode.parent != second.layoutNode.parent &&
            first.layoutNode.findClosestParentNode({ it == second.layoutNode }) != null
        ) {
            sortedChildren[index] = second
            sortedChildren[index + 1] = first
        }
    }
    return sortedChildren
}
