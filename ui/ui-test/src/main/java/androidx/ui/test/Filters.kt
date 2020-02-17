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
        getOrNull(SemanticsProperties.AccessibilityLabel).equals(text, ignoreCase)
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
        getOrNull(SemanticsProperties.AccessibilityLabel)?.contains(substring, ignoreCase)
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