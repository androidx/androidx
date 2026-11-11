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

package androidx.wear.compose.material3

import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsModifier
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver

/**
 * Splits [SemanticsActions.CustomActions] out of any [SemanticsModifier] elements in this
 * [Modifier] chain without evaluating their [SemanticsModifier.semanticsConfiguration] during
 * composition, returning a pair of:
 * 1. The outer [Modifier] chain (preserving all non-[SemanticsActions.CustomActions] semantics on
 *    the outer container of a split button).
 * 2. A [Modifier] chain that applies only the extracted [SemanticsActions.CustomActions] to the
 *    focusable primary clickable container during the semantics phase.
 */
internal fun Modifier.extractCustomActions(): Pair<Modifier, Modifier> {
    if (!any { it is SemanticsModifier }) {
        return this to Modifier
    }

    return foldIn<Pair<Modifier, Modifier>>(Modifier to Modifier) { (outer, customActions), element
        ->
        if (element is SemanticsModifier) {
            (outer then FilteredSemanticsElement(element, includeCustomActions = false)) to
                (customActions then FilteredSemanticsElement(element, includeCustomActions = true))
        } else {
            (outer then element) to customActions
        }
    }
}

private class FilteredSemanticsElement(
    private val delegate: SemanticsModifier,
    private val includeCustomActions: Boolean,
) : ModifierNodeElement<FilteredSemanticsNode>() {

    override fun create(): FilteredSemanticsNode =
        FilteredSemanticsNode(delegate, includeCustomActions)

    override fun update(node: FilteredSemanticsNode) {
        node.update(delegate, includeCustomActions)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = if (includeCustomActions) "customActionsSemantics" else "outerSemantics"
        properties["delegate"] = delegate
        properties["includeCustomActions"] = includeCustomActions
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FilteredSemanticsElement) return false
        return includeCustomActions == other.includeCustomActions && delegate == other.delegate
    }

    override fun hashCode(): Int {
        var result = delegate.hashCode()
        result = 31 * result + includeCustomActions.hashCode()
        return result
    }
}

private class FilteredSemanticsNode(
    delegate: SemanticsModifier,
    private var includeCustomActions: Boolean,
) : Modifier.Node(), SemanticsModifierNode {

    private var delegate: SemanticsModifier = delegate
    private var delegateNode: SemanticsModifierNode? =
        delegate.createOrUpdateSemanticsNode(existingNode = null, previousDelegate = null)

    private val isClearingSemantics: Boolean
        get() =
            delegateNode?.shouldClearDescendantSemantics
                ?: delegate.semanticsConfiguration.isClearingSemantics

    private val filteringReceiver =
        object : SemanticsPropertyReceiver {
            var target: SemanticsPropertyReceiver? = null

            override fun <T> set(key: SemanticsPropertyKey<T>, value: T) {
                val routeToInner = key == SemanticsActions.CustomActions && !isClearingSemantics
                if (routeToInner == includeCustomActions) {
                    target?.set(key, value)
                }
            }
        }

    fun update(newDelegate: SemanticsModifier, newIncludeCustomActions: Boolean) {
        delegateNode =
            newDelegate.createOrUpdateSemanticsNode(
                existingNode = delegateNode,
                previousDelegate = delegate,
            )
        delegate = newDelegate
        includeCustomActions = newIncludeCustomActions
    }

    override val shouldClearDescendantSemantics: Boolean
        get() = !includeCustomActions && isClearingSemantics

    override val shouldMergeDescendantSemantics: Boolean
        get() =
            if (includeCustomActions) {
                false
            } else {
                delegateNode?.shouldMergeDescendantSemantics
                    ?: delegate.semanticsConfiguration.isMergingSemanticsOfDescendants
            }

    override fun SemanticsPropertyReceiver.applySemantics() {
        filteringReceiver.target = this
        try {
            val node = delegateNode
            if (node != null) {
                with(node) { filteringReceiver.applySemantics() }
            } else {
                for ((key, value) in delegate.semanticsConfiguration) {
                    @Suppress("UNCHECKED_CAST")
                    filteringReceiver[key as SemanticsPropertyKey<Any?>] = value
                }
            }
        } finally {
            filteringReceiver.target = null
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun SemanticsModifier.createOrUpdateSemanticsNode(
    existingNode: SemanticsModifierNode?,
    previousDelegate: SemanticsModifier?,
): SemanticsModifierNode? {
    val element = this as? ModifierNodeElement<Modifier.Node> ?: return null
    val modifierNode = existingNode as? Modifier.Node
    if (
        modifierNode != null && previousDelegate != null && previousDelegate::class == this::class
    ) {
        element.update(modifierNode)
        return existingNode
    }
    return element.create() as? SemanticsModifierNode
}
