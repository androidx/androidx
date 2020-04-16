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
fun isToggleable(): SemanticsMatcher =
    SemanticsMatcher.keyIsDefined(FoundationSemanticsProperties.ToggleableState)

/**
 * Returns whether the component is toggled.
 *
 * @see FoundationSemanticsProperties.ToggleableState
 */
fun isOn(): SemanticsMatcher = SemanticsMatcher.expectValue(
    FoundationSemanticsProperties.ToggleableState, ToggleableState.On)

/**
 * Returns whether the component is not toggled.
 *
 * @see FoundationSemanticsProperties.ToggleableState
 */
fun isOff(): SemanticsMatcher = SemanticsMatcher.expectValue(
    FoundationSemanticsProperties.ToggleableState, ToggleableState.Off)

/**
 * Return whether the component is selectable.
 *
 * @see FoundationSemanticsProperties.Selected
 */
fun isSelectable(): SemanticsMatcher =
    SemanticsMatcher.keyIsDefined(FoundationSemanticsProperties.Selected)

/**
 * Returns whether the component is selected.
 *
 * @see FoundationSemanticsProperties.Selected
 */
fun isSelected(): SemanticsMatcher =
    SemanticsMatcher.expectValue(FoundationSemanticsProperties.Selected, true)

/**
 * Returns whether the component is not selected.
 *
 * @see FoundationSemanticsProperties.Selected
 */
fun isUnselected(): SemanticsMatcher =
    SemanticsMatcher.expectValue(FoundationSemanticsProperties.Selected, false)

/**
 * Return whether the component has a semantics click action defined.
 *
 * @see SemanticsActions.OnClick
 */
fun hasClickAction(): SemanticsMatcher =
    SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick)

/**
 * Return whether the component has no semantics click action defined.
 *
 * @see SemanticsActions.OnClick
 */
fun hasNoClickAction(): SemanticsMatcher =
    SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick)

/**
 * Return whether the component has a semantics scrollable action defined.
 *
 * @see SemanticsActions.ScrollTo
 */
fun hasScrollAction(): SemanticsMatcher =
    SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollTo)

/**
 * Return whether the component has no semantics scrollable action defined.
 *
 * @see SemanticsActions.ScrollTo
 */
fun hasNoScrollAction(): SemanticsMatcher =
    SemanticsMatcher.keyNotDefined(SemanticsActions.ScrollTo)

/**
 * Returns whether the component's label matches exactly to the given text.
 *
 * @param text Text to match.
 * @param ignoreCase Whether case should be ignored.
 *
 * @see hasSubstring
 * @see SemanticsProperties.AccessibilityLabel
 */
fun hasText(text: String, ignoreCase: Boolean = false): SemanticsMatcher {
    return SemanticsMatcher.fromCondition(
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
        SemanticsMatcher {
    return SemanticsMatcher.fromCondition(
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
fun hasValue(value: String): SemanticsMatcher = SemanticsMatcher.expectValue(
    SemanticsProperties.AccessibilityValue, value)

/**
 * Returns whether the component is annotated by the given test tag.
 *
 * @param testTag Value to match.
 *
 * @see SemanticsProperties.TestTag
 */
fun hasTestTag(testTag: String): SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.TestTag, testTag)

// TODO(ryanmentley/pavlis): Do we want these convenience functions?
/**
 * Verifies that the component is in a mutually exclusive group - that is,
 * that [FoundationSemanticsProperties.InMutuallyExclusiveGroup] is set to true
 *
 */
fun isInMutuallyExclusiveGroup(): SemanticsMatcher =
    SemanticsMatcher.expectValue(FoundationSemanticsProperties.InMutuallyExclusiveGroup, true)

/**
 * Returns whether the component is hidden.
 *
 * This checks only the property of the component itself. Ignoring parents visibility.
 *
 * @see SemanticsProperties.Hidden
 */
fun isHidden(): SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.Hidden, true)

/**
 * Returns whether the component is not hidden.
 *
 * This checks only the property of the component itself. Ignoring parents visibility.
 *
 * @see SemanticsProperties.Hidden
 */
fun isNotHidden(): SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.Hidden, false)

/**
 * Returns whether the component's parent satisfies the given matcher.
 *
 * Returns false if no parent exists.
 */
fun hasParentThat(matcher: SemanticsMatcher): SemanticsMatcher {
    // TODO(b/150292800): If this is used in assert we should print the parent's node semantics
    //  in the error message or say that no parent was found.
    return SemanticsMatcher.fromCondition("hasParentThat(${matcher.description})") {
        parent?.run { matcher.matches(this) } ?: false
    }
}

/**
 * Returns whether the component has at least one child that satisfies the given matcher.
 */
fun hasAnyChildThat(matcher: SemanticsMatcher): SemanticsMatcher {
    // TODO(b/150292800): If this is used in assert we should print the children nodes semantics
    //  in the error message or say that no children were found.
    return SemanticsMatcher.fromCondition("hasAnyChildThat(${matcher.description})") {
        matcher.matchesAny(this.children)
    }
}

/**
 * Returns whether the component has at least one sibling that satisfies the given matcher.
 *
 * Sibling is defined as a any other node that shares the same parent.
 */
fun hasAnySiblingThat(matcher: SemanticsMatcher): SemanticsMatcher {
    // TODO(b/150292800): If this is used in assert we should print the sibling nodes semantics
    //  in the error message or say that no siblings were found.
    return SemanticsMatcher.fromCondition("hasAnySiblingThat(${matcher.description})"
    ) {
        val node = this
        parent?.run { matcher.match(this.children).any { it.id != node.id } } ?: false
    }
}

/**
 * Returns whether the component has at least one ancestor that satisfies the given matcher.
 *
 * Example: For the following tree
 * |-X
 * |-A
 *   |-B
 *     |-C1
 *     |-C2
 * In case of C1, we would check the matcher against A and B
 */
fun hasAnyAncestorThat(matcher: SemanticsMatcher): SemanticsMatcher {
    // TODO(b/150292800): If this is used in assert we should print the ancestor nodes semantics
    //  in the error message or say that no ancestors were found.
    return SemanticsMatcher.fromCondition("hasAnyAncestorThat(${matcher.description})") {
        matcher.matchesAny(ancestors)
    }
}

/**
 * Returns whether the component has at least one descendant that satisfies the given matcher.
 *
 * Example: For the following tree
 * |-X
 * |-A
 *   |-B
 *     |-C1
 *     |-C2
 * In case of A, we would check the matcher against B,C1 and C2
 */
fun hasAnyDescendantThat(matcher: SemanticsMatcher): SemanticsMatcher {
    // TODO(b/150292800): If this is used in assert we could consider printing the whole subtree but
    //  it might be too much to show. But we could at least warn if there were no ancestors found.
    fun checkIfSubtreeMatches(matcher: SemanticsMatcher, node: SemanticsNode): Boolean {
        if (matcher.matchesAny(node.children)) {
            return true
        }

        return node.children.any { checkIfSubtreeMatches(matcher, it) }
    }

    return SemanticsMatcher.fromCondition("hasAnyDescendantThat(${matcher.description})") {
        checkIfSubtreeMatches(matcher, this)
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
