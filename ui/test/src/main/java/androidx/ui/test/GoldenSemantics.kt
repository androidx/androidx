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

import androidx.ui.core.semantics.SemanticsProperties

// TODO(catalintudor): add remaining properties
class SemanticsPropertiesBuilder(
    var enabled: Boolean?,
    var checked: Boolean?,
    var inMutuallyExclusiveGroup: Boolean?,
    var selected: Boolean?
) {
    fun build(): SemanticsProperties {
        return SemanticsProperties(
            enabled,
            checked,
            inMutuallyExclusiveGroup = inMutuallyExclusiveGroup,
            selected = selected
        )
    }
}

/**
 * Ensures the created [SemanticsProperties] object doesn't have any default values set.
 * This intentionally enforces choosing every value in order to minimise possible unwanted
 * side effects. Should be used to create initial default semantics for widgets and afterwards
 * [SemanticsProperties.copyWith] should be used to create a modified copy.
 */
// TODO(catalintudor): add remaining properties
fun createFullSemantics(
    enabled: Boolean? = false,
    checked: Boolean? = false,
    selected: Boolean? = false,
    inMutuallyExclusiveGroup: Boolean? = false
): SemanticsProperties {
    return SemanticsProperties(
        enabled = enabled,
        checked = checked,
        inMutuallyExclusiveGroup = inMutuallyExclusiveGroup,
        selected = selected
    )
}

fun SemanticsProperties.toBuilder(): SemanticsPropertiesBuilder {
    return SemanticsPropertiesBuilder(
        enabled = enabled,
        checked = checked,
        inMutuallyExclusiveGroup = inMutuallyExclusiveGroup,
        selected = selected
    )
}

/**
 * Returns a (mutated) copy of the original [SemanticsProperties] object.
 * Uses [SemanticsPropertiesBuilder] as an intermediate (mutable) representation of
 * [SemanticsProperties]
 */
fun SemanticsProperties.copyWith(diff: SemanticsPropertiesBuilder.() -> Unit): SemanticsProperties {
    return toBuilder()
        .apply(diff)
        .build()
}

fun SemanticsProperties.assertEquals(expected: SemanticsProperties) {
    val assertMessage = StringBuilder()

    if (enabled != expected.enabled) {
        assertMessage.append("\n- expected 'enabled' = ${expected.enabled} but was $enabled")
    }

    if (checked != expected.checked) {
        assertMessage.append("\n- expected 'checked' = ${expected.checked} but was $checked")
    }
    if (inMutuallyExclusiveGroup != expected.inMutuallyExclusiveGroup) {
        assertMessage.append(
            "\n- expected 'inMutuallyExclusiveGroup' = ${expected.inMutuallyExclusiveGroup} " +
                    "but was $inMutuallyExclusiveGroup"
        )
    }
    if (selected != expected.selected) {
        assertMessage.append("\n- expected 'selected' = ${expected.selected} but was $selected")
    }

    if (assertMessage.isNotEmpty()) {
        throw AssertionError(
            "Expected semantics is not equal to the current one: " +
                    assertMessage.toString()
        )
    }
}
