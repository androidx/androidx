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
import androidx.ui.input.ImeAction
import androidx.ui.semantics.AccessibilityRangeInfo
import androidx.ui.semantics.SemanticsActions
import androidx.ui.semantics.SemanticsProperties
import androidx.ui.text.TextSemanticsProperties
import androidx.ui.util.fastAny

/**
 * Returns whether the component is enabled.
 *
 * @see SemanticsProperties.Enabled
 */
fun isEnabled(): SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.Enabled, true)

/**
 * Returns whether the component is not enabled.
 *
 * @see SemanticsProperties.Enabled
 */
fun isNotEnabled(): SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.Enabled, false)

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
    return SemanticsMatcher(
        "${SemanticsProperties.AccessibilityLabel.name} = '$text' (ignoreCase: $ignoreCase)"
    ) {
        it.config.getOrNull(SemanticsProperties.AccessibilityLabel).equals(text, ignoreCase)
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
    return SemanticsMatcher(
        "${SemanticsProperties.AccessibilityLabel.name}.contains($substring, $ignoreCase)"
    ) {
        it.config.getOrNull(SemanticsProperties.AccessibilityLabel)?.contains(substring, ignoreCase)
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
 * Returns whether the component's range info matches exactly to the given accessibility range info.
 *
 * @param rangeInfo range info to match.
 *
 * @see SemanticsProperties.AccessibilityRangeInfo
 */
fun hasRangeInfo(rangeInfo: AccessibilityRangeInfo): SemanticsMatcher = SemanticsMatcher
    .expectValue(SemanticsProperties.AccessibilityRangeInfo, rangeInfo)

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
 * Returns whether the component is a dialog.
 *
 * This only checks if the component itself is a dialog, not if it is _part of_ a dialog. Use
 * `hasAnyAncestorThat(isDialog())` for that.
 *
 * @see FoundationSemanticsProperties.IsDialog
 */
fun isDialog(): SemanticsMatcher =
    SemanticsMatcher.keyIsDefined(FoundationSemanticsProperties.IsDialog)

/** Returns whether the component is a popup.
 *
 * This only checks if the component itself is a popup, not if it is _part of_ a popup. Use
 * `hasAnyAncestorThat(isPopup())` for that.
 *
 * @see SemanticsProperties.IsPopup
 */
fun isPopup(): SemanticsMatcher =
    SemanticsMatcher.keyIsDefined(SemanticsProperties.IsPopup)

/**
 * Returns whether the component defines the given IME action.
 *
 * @param actionType the action to match.
 */
fun hasImeAction(actionType: ImeAction) =
    SemanticsMatcher.expectValue(TextSemanticsProperties.ImeAction, actionType)

/**
 * Return whether the component supports input methods.
 *
 * Supporting input methods means that the component provides a connection to IME (keyboard) and is
 * able to accept input from it. This is however not enforced and relies on the components to
 * properly add this to semantics when they provide input. Note that this is not related to
 * gestures input but only to IME. This can be used to for instance filter out all text fields.
 */
fun hasInputMethodsSupport() =
    SemanticsMatcher.expectValue(TextSemanticsProperties.SupportsInputMethods, true)

/*
 * Return whether the component is the root semantics node.
 *
 * There is always one root in every component tree, added implicitly by Compose.
 */
fun isRoot() =
    SemanticsMatcher("isRoot") { it.isRoot }

/**
 * Returns whether the component's parent satisfies the given matcher.
 *
 * Returns false if no parent exists.
 */
fun hasParentThat(matcher: SemanticsMatcher): SemanticsMatcher {
    // TODO(b/150292800): If this is used in assert we should print the parent's node semantics
    //  in the error message or say that no parent was found.
    return SemanticsMatcher("hasParentThat(${matcher.description})") {
        it.parent?.run { matcher.matches(this) } ?: false
    }
}

/**
 * Returns whether the component has at least one child that satisfies the given matcher.
 */
fun hasAnyChildThat(matcher: SemanticsMatcher): SemanticsMatcher {
    // TODO(b/150292800): If this is used in assert we should print the children nodes semantics
    //  in the error message or say that no children were found.
    return SemanticsMatcher("hasAnyChildThat(${matcher.description})") {
        matcher.matchesAny(it.children)
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
    return SemanticsMatcher("hasAnySiblingThat(${matcher.description})"
    ) {
        val node = it
        it.parent?.run { matcher.matchesAny(this.children.filter { child -> child.id != node.id }) }
            ?: false
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
    return SemanticsMatcher("hasAnyAncestorThat(${matcher.description})") {
        matcher.matchesAny(it.ancestors)
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

        return node.children.fastAny { checkIfSubtreeMatches(matcher, it) }
    }

    return SemanticsMatcher("hasAnyDescendantThat(${matcher.description})") {
        checkIfSubtreeMatches(matcher, it)
    }
}

internal val SemanticsNode.ancestors: Iterable<SemanticsNode>
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
