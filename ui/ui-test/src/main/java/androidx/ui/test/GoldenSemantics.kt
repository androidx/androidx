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
import androidx.ui.foundation.selection.ToggleableState
import androidx.ui.foundation.semantics.inMutuallyExclusiveGroup
import androidx.ui.foundation.semantics.selected
import androidx.ui.foundation.semantics.toggleableState
import androidx.ui.semantics.enabled
import androidx.ui.semantics.accessibilityValue

/**
 * Ensures the created [SemanticsConfiguration] object doesn't have any default values set.
 * This intentionally enforces choosing every value in order to minimise possible unwanted
 * side effects. Should be used to create initial default semantics for composables and afterwards
 * [SemanticsConfiguration.copyWith] should be used to create a modified copy.
 */
// TODO(b/131309551): investigate the structure of this API
fun createFullSemantics(
    isEnabled: Boolean? = null,
    value: String? = null,
    toggleableState: ToggleableState? = null,
    inMutuallyExclusiveGroup: Boolean? = null,
    isSelected: Boolean? = null
): SemanticsConfiguration {
    return SemanticsConfiguration().also {
        isEnabled?.also { isEnabled -> it.enabled = isEnabled }
        value?.also { value -> it.accessibilityValue = value }
        toggleableState?.also { toggleableState -> it.toggleableState = toggleableState }
        inMutuallyExclusiveGroup?.also {
                inMutuallyExclusiveGroup -> it.inMutuallyExclusiveGroup = inMutuallyExclusiveGroup
        }
        isSelected?.also { selected -> it.selected = selected }
    }
}

/**
 * Returns a (mutated) copy of the original [SemanticsConfiguration] object.
 * Uses [SemanticsPropertiesBuilder] as an intermediate (mutable) representation of
 * [SemanticsConfiguration]
 */
fun SemanticsConfiguration.copyWith(diff: SemanticsConfiguration.() -> Unit):
        SemanticsConfiguration {
    return copy()
        .apply(diff)
}

fun SemanticsConfiguration.assertEquals(expected: SemanticsConfiguration) {
    val assertMessage = StringBuilder()

    for ((key, expectedValue) in expected) {
        if (this.contains(key)) {
            val thisValue = this[key]
            if (thisValue == expectedValue) {
                continue
            } else {
                assertMessage.append("\n- expected ${key.name}" +
                        " = '$expectedValue', but was $thisValue")
            }
        } else {
            assertMessage.append("\n- expected ${key.name} = '$expectedValue', but was missing")
        }
    }

    // TODO(pavlis/ryanmentley): Should we also check that there are no _extra_ semantics?
    //  I think the correct answer here is "no", but we should confirm

    if (assertMessage.isNotEmpty()) {
        throw AssertionError(
            "Expected semantics is not equal to the current one: " +
                    assertMessage.toString()
        )
    }
}
