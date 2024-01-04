/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.glance.testing.unit

import androidx.glance.EmittableWithText
import androidx.glance.semantics.SemanticsModifier
import androidx.glance.semantics.SemanticsProperties
import androidx.glance.semantics.SemanticsPropertyKey
import androidx.glance.testing.GlanceNodeMatcher

/**
 * Returns a matcher that matches if a node is annotated by the given test tag.
 *
 * <p>This can be passed in "onNode" and "onNodeAll" functions on assertion providers to filter out
 * matching node(s) or in assertions to validate that node(s) satisfy the condition.
 *
 * @param testTag value to match against the free form string specified in {@code testTag} semantics
 *                modifier on Glance composable nodes.
 */
fun hasTestTag(testTag: String): GlanceNodeMatcher<MappedNode> =
    hasSemanticsPropertyValue(SemanticsProperties.TestTag, testTag)

private fun <T> hasSemanticsPropertyValue(
    key: SemanticsPropertyKey<T>,
    expectedValue: T
): GlanceNodeMatcher<MappedNode> {
    return GlanceNodeMatcher("${key.name} = '$expectedValue'") { node ->
        node.value.emittable.modifier.any {
            it is SemanticsModifier &&
                it.configuration.getOrElseNullable(key) { null } == expectedValue
        }
    }
}

/**
 * Returns a matcher that matches if text on node matches the provided text.
 *
 * <p>This can be passed in "onNode" and "onNodeAll" functions on assertion providers to filter out
 * matching node(s) or in assertions to validate that node(s) satisfy the condition.
 *
 * @param text value to match.
 * @param substring whether to perform substring matching
 * @param ignoreCase whether to perform case insensitive matching
 */
fun hasText(
    text: String,
    substring: Boolean = false,
    ignoreCase: Boolean = false
): GlanceNodeMatcher<MappedNode> = GlanceNodeMatcher(
    if (substring) {
        "contains '$text' (ignoreCase: $ignoreCase) as substring"
    } else {
        "has text = '$text' (ignoreCase: '$ignoreCase')"
    }
) { node ->
    val emittable = node.value.emittable
    if (emittable is EmittableWithText) {
        if (substring) {
            emittable.text.contains(text, ignoreCase)
        } else {
            emittable.text.equals(text, ignoreCase)
        }
    } else {
        false
    }
}
