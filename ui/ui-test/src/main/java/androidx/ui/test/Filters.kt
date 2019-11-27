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

import androidx.ui.core.semantics.SemanticsConfiguration
import androidx.ui.core.semantics.getOrNull
import androidx.ui.foundation.semantics.FoundationSemanticsProperties
import androidx.ui.semantics.SemanticsActions
import androidx.ui.semantics.SemanticsProperties

/**
 * Return whether the component is checkable.
 */
val SemanticsConfiguration.isToggleable: Boolean
    get() = contains(FoundationSemanticsProperties.ToggleableState)

/**
 * Return whether the component has a semantics click action defined.
 */
val SemanticsConfiguration.hasClickAction: Boolean
    get() = SemanticsActions.OnClick in this

/**
 * Return whether the component has a semantics scrollable action defined.
 */
val SemanticsConfiguration.hasScrollAction: Boolean
    get() = SemanticsActions.ScrollTo in this

/**
 * Returns whether the component's label matches exactly to the given text.
 *
 * @param text Text to match.
 * @param ignoreCase Whether case should be ignored.
 * @see hasSubstring
 */
fun SemanticsConfiguration.hasText(text: String, ignoreCase: Boolean = false): Boolean {
    return getOrNull(SemanticsProperties.AccessibilityLabel).equals(text, ignoreCase)
}

/**
 * Returns whether the component's label contains the given substring.
 *
 * @param substring Substring to check.
 * @param ignoreCase Whether case should be ignored.
 * @see hasText
 */
fun SemanticsConfiguration.hasSubstring(substring: String, ignoreCase: Boolean = false): Boolean {
    return getOrNull(SemanticsProperties.AccessibilityLabel)?.contains(substring, ignoreCase)
        ?: false
}

// TODO(ryanmentley/pavlis): Do we want these convenience functions?
/**
 * Verifies that the component is in a mutually exclusive group - that is,
 * that [FoundationSemanticsProperties.InMutuallyExclusiveGroup] is set to true
 *
 */
val SemanticsConfiguration.isInMutuallyExclusiveGroup: Boolean
    get() = getOrNull(FoundationSemanticsProperties.InMutuallyExclusiveGroup) == true