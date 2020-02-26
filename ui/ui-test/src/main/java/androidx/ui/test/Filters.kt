/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.test

import androidx.ui.core.semantics.SemanticsNode
import androidx.ui.core.semantics.getOrNull
import androidx.ui.foundation.selection.ToggleableState
import androidx.ui.foundation.semantics.FoundationSemanticsProperties
import androidx.ui.semantics.SemanticsActions
import androidx.ui.semantics.SemanticsProperties

/**
 * Return whether the component is checkable.
 *
 * @see FoundationSemanticsProperties.ToggleableState
 */
fun isToggleable(): SemanticsPredicate =
    SemanticsPredicate.keyIsDefined(FoundationSemanticsProperties.ToggleableState)

/**
 * Returns whether the component is toggled.
 *
 * @see FoundationSemanticsProperties.ToggleableState
 */
fun isOn(): SemanticsPredicate = SemanticsPredicate.expectValue(
    FoundationSemanticsProperties.ToggleableState, ToggleableState.On)

/**
 * Returns whether the component is not toggled.
 *
 * @see FoundationSemanticsProperties.ToggleableState
 */
fun isOff(): SemanticsPredicate = SemanticsPredicate.expectValue(
    FoundationSemanticsProperties.ToggleableState, ToggleableState.Off)

/**
 * Return whether the component is selectable.
 *
 * @see FoundationSemanticsProperties.Selected
 */
fun isSelectable(): SemanticsPredicate =
    SemanticsPredicate.keyIsDefined(FoundationSemanticsProperties.Selected)

/**
 * Returns whether the component is selected.
 *
 * @see FoundationSemanticsProperties.Selected
 */
fun isSelected(): SemanticsPredicate =
    SemanticsPredicate.expectValue(FoundationSemanticsProperties.Selected, true)

/**
 * Returns whether the component is not selected.
 *
 * @see FoundationSemanticsProperties.Selected
 */
fun isUnselected(): SemanticsPredicate =
    SemanticsPredicate.expectValue(FoundationSemanticsProperties.Selected, false)

/**
 * Return whether the component has a semantics click action defined.
 *
 * @see SemanticsActions.OnClick
 */
fun hasClickAction(): SemanticsPredicate =
    SemanticsPredicate.keyIsDefined(SemanticsActions.OnClick)

/**
 * Return whether the component has no semantics click action defined.
 *
 * @see SemanticsActions.OnClick
 */
fun hasNoClickAction(): SemanticsPredicate =
    SemanticsPredicate.keyNotDefined(SemanticsActions.OnClick)

/**
 * Return whether the component has a semantics scrollable action defined.
 *
 * @see SemanticsActions.ScrollTo
 */
fun hasScrollAction(): SemanticsPredicate =
    SemanticsPredicate.keyIsDefined(SemanticsActions.ScrollTo)

/**
 * Return whether the component has no semantics scrollable action defined.
 *
 * @see SemanticsActions.ScrollTo
 */
fun hasNoScrollAction(): SemanticsPredicate =
    SemanticsPredicate.keyNotDefined(SemanticsActions.ScrollTo)

/**
 * Returns whether the component's label matches exactly to the given text.
 *
 * @param text Text to match.
 * @param ignoreCase Whether case should be ignored.
 *
 * @see hasSubstring
 * @see SemanticsProperties.AccessibilityLabel
 */
fun hasText(text: String, ignoreCase: Boolean = false): SemanticsPredicate {
    return SemanticsPredicate(
        "${SemanticsProperties.AccessibilityLabel.name} = '$text' (ignoreCase: $ignoreCase)"
    ) {
        config.getOrNull(SemanticsProperties.AccessibilityLabel).equals(text, ignoreCase)
    }
}

/**
 * Returns whether the component's label contains the given substring.
 *
 * @param substring Substring to check.
 * @param ignoreCase Whether case should be ignored.
 *
 * @see hasText
 * @see SemanticsProperties.AccessibilityLabel
 */
fun hasSubstring(substring: String, ignoreCase: Boolean = false):
        SemanticsPredicate {
    return SemanticsPredicate(
        "${SemanticsProperties.AccessibilityLabel.name}.contains($substring, $ignoreCase)"
    ) {
        config.getOrNull(SemanticsProperties.AccessibilityLabel)?.contains(substring, ignoreCase)
            ?: false
    }
}

/**
 * Returns whether the component's value matches exactly to the given accessibility value.
 *
 * @param value Value to match.
 *
 * @see SemanticsProperties.AccessibilityValue
 */
fun hasValue(value: String): SemanticsPredicate = SemanticsPredicate.expectValue(
    SemanticsProperties.AccessibilityValue, value)

/**
 * Returns whether the component is annotated by the given test tag.
 *
 * @param testTag Value to match.
 *
 * @see SemanticsProperties.TestTag
 */
fun hasTestTag(testTag: String): SemanticsPredicate =
    SemanticsPredicate.expectValue(SemanticsProperties.TestTag, testTag)

// TODO(ryanmentley/pavlis): Do we want these convenience functions?
/**
 * Verifies that the component is in a mutually exclusive group - that is,
 * that [FoundationSemanticsProperties.InMutuallyExclusiveGroup] is set to true
 *
 */
fun isInMutuallyExclusiveGroup(): SemanticsPredicate =
    SemanticsPredicate.expectValue(FoundationSemanticsProperties.InMutuallyExclusiveGroup, true)

/**
 * Returns whether the component is hidden.
 *
 * This checks only the property of the component itself. Ignoring parents visibility.
 *
 * @see SemanticsProperties.Hidden
 */
fun isHidden(): SemanticsPredicate =
    SemanticsPredicate.expectValue(SemanticsProperties.Hidden, true)

/**
 * Returns whether the component is not hidden.
 *
 * This checks only the property of the component itself. Ignoring parents visibility.
 *
 * @see SemanticsProperties.Hidden
 */
fun isNotHidden(): SemanticsPredicate =
    SemanticsPredicate.expectValue(SemanticsProperties.Hidden, false)

/**
 * Returns whether the component's parent satisfies the given predicate.
 *
 * Returns false if no parent exists.
 */
fun hasParentThat(predicate: SemanticsPredicate): SemanticsPredicate {
    // TODO(b/150292800): If this is used in assert we should print the parent's node semantics
    //  in the error message or say that no parent was found.
    return SemanticsPredicate("hasAnyParentThat(${predicate.description})") {
        parent?.run { predicate.condition(this) } ?: false
    }
}

/**
 * Returns whether the component has at least one child that satisfies the given predicate.
 */
fun hasAnyChildThat(predicate: SemanticsPredicate): SemanticsPredicate {
    // TODO(b/150292800): If this is used in assert we should print the children nodes semantics
    //  in the error message or say that no children were found.
    return SemanticsPredicate("hasAnyChildThat(${predicate.description})") {
        this.children.any { predicate.condition(it) }
    }
}

/**
 * Returns whether the component has at least one sibling that satisfies the given predicate.
 *
 * Sibling is defined as a any other node that shares the same parent.
 */
fun hasAnySiblingThat(predicate: SemanticsPredicate): SemanticsPredicate {
    // TODO(b/150292800): If this is used in assert we should print the sibling nodes semantics
    //  in the error message or say that no siblings were found.
    return SemanticsPredicate("hasAnySiblingThat(${predicate.description})") {
        val node = this
        parent?.run { this.children.any { it.id != node.id && predicate.condition(it) } } ?: false
    }
}

/**
 * Returns whether the component has at least one ancestor that satisfies the given predicate.
 *
 * Example: For the following tree
 * |-X
 * |-A
 *   |-B
 *     |-C1
 *     |-C2
 * In case of C1, we would check the predicate against A and B
 */
fun hasAnyAncestorThat(predicate: SemanticsPredicate): SemanticsPredicate {
    // TODO(b/150292800): If this is used in assert we should print the ancestor nodes semantics
    //  in the error message or say that no ancestors were found.
    return SemanticsPredicate("hasAnyAncestorThat(${predicate.description})") {
        ancestors.any { predicate.condition(it) }
    }
}

/**
 * Returns whether the component has at least one descendant that satisfies the given predicate.
 *
 * Example: For the following tree
 * |-X
 * |-A
 *   |-B
 *     |-C1
 *     |-C2
 * In case of A, we would check the predicate against B,C1 and C2
 */
fun hasAnyDescendantThat(predicate: SemanticsPredicate): SemanticsPredicate {
    // TODO(b/150292800): If this is used in assert we could consider printing the whole subtree but
    //  it might be too much to show. But we could at least warn if there were no ancestors found.
    fun checkIfSubtreeMatches(predicate: SemanticsPredicate, node: SemanticsNode): Boolean {
        if (predicate.condition(node)) {
            return true
        }

        return node.children.any {
            checkIfSubtreeMatches(predicate, it)
        }
    }

    return SemanticsPredicate("hasAnyDescendantThat(${predicate.description})") {
        this.children.any { checkIfSubtreeMatches(predicate, it) }
    }
}

private val SemanticsNode.ancestors: Iterable<SemanticsNode>
    get() = object : Iterable<SemanticsNode> {
        override fun iterator(): Iterator<SemanticsNode> {
            return object : Iterator<SemanticsNode> {
                var next = parent
                override fun hasNext(): Boolean {
                    return next != null
                }
                override fun next(): SemanticsNode {
                    return next!!.also { next = it.parent }
                }
            }
        }
    }
