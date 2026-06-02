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

package androidx.xr.compose.testing

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.node.SubspaceLayoutNode
import androidx.xr.compose.subspace.semantics.semantics
import androidx.xr.compose.subspace.semantics.testTag
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SubspaceSemanticsMatcherTest {

    @Test
    fun expectValue_matchesSuccessfully_whenKeyMatches() {
        val node: SubspaceLayoutNode = SubspaceLayoutNode()
        node.modifier = SubspaceModifier.testTag(tag = "expectedTag")
        val matcher: SubspaceSemanticsMatcher =
            SubspaceSemanticsMatcher.expectValue(
                key = SemanticsProperties.TestTag,
                expectedValue = "expectedTag",
            )

        val result: Boolean = matcher.matches(node = node.measurableLayout)

        assertTrue(result)
    }

    @Test
    fun expectValue_returnsFalse_whenKeyDoesNotMatch() {
        val node: SubspaceLayoutNode = SubspaceLayoutNode()
        node.modifier = SubspaceModifier.testTag(tag = "differentTag")
        val matcher: SubspaceSemanticsMatcher =
            SubspaceSemanticsMatcher.expectValue(
                key = SemanticsProperties.TestTag,
                expectedValue = "expectedTag",
            )

        val result: Boolean = matcher.matches(node = node.measurableLayout)

        assertFalse(result)
    }

    @Test
    fun expectValue_matchesSuccessfully_whenExpectedValueIsNullAndKeyIsOmitted() {
        val node: SubspaceLayoutNode = SubspaceLayoutNode()
        // No testTag modifier attached, so TestTag key is omitted entirely.
        val customNullableKey: SemanticsPropertyKey<String?> =
            SemanticsPropertyKey(name = "customNullableKey")
        val matcher: SubspaceSemanticsMatcher =
            SubspaceSemanticsMatcher.expectValue(key = customNullableKey, expectedValue = null)

        val result: Boolean = matcher.matches(node = node.measurableLayout)

        assertTrue(result)
    }

    @Test
    fun expectValue_matchesSuccessfully_whenExpectedValueIsNullAndKeyIsPresentWithNullValue() {
        val customNullableKey: SemanticsPropertyKey<String?> =
            SemanticsPropertyKey(name = "customNullableKey")
        val node: SubspaceLayoutNode = SubspaceLayoutNode()
        node.modifier = SubspaceModifier.semantics { this[customNullableKey] = null }
        val matcher: SubspaceSemanticsMatcher =
            SubspaceSemanticsMatcher.expectValue(key = customNullableKey, expectedValue = null)

        val result: Boolean = matcher.matches(node = node.measurableLayout)

        assertTrue(result)
    }

    @Test
    fun keyIsDefined_returnsTrue_whenKeyIsPresent() {
        val node: SubspaceLayoutNode = SubspaceLayoutNode()
        node.modifier = SubspaceModifier.testTag(tag = "anyTag")
        val matcher: SubspaceSemanticsMatcher =
            SubspaceSemanticsMatcher.keyIsDefined(key = SemanticsProperties.TestTag)

        val result: Boolean = matcher.matches(node = node.measurableLayout)

        assertTrue(result)
    }

    @Test
    fun keyIsDefined_returnsFalse_whenKeyIsOmitted() {
        val node: SubspaceLayoutNode = SubspaceLayoutNode()
        val matcher: SubspaceSemanticsMatcher =
            SubspaceSemanticsMatcher.keyIsDefined(key = SemanticsProperties.TestTag)

        val result: Boolean = matcher.matches(node = node.measurableLayout)

        assertFalse(result)
    }

    @Test
    fun keyNotDefined_returnsTrue_whenKeyIsOmitted() {
        val node: SubspaceLayoutNode = SubspaceLayoutNode()
        val matcher: SubspaceSemanticsMatcher =
            SubspaceSemanticsMatcher.keyNotDefined(key = SemanticsProperties.TestTag)

        val result: Boolean = matcher.matches(node = node.measurableLayout)

        assertTrue(result)
    }

    @Test
    fun keyNotDefined_returnsFalse_whenKeyIsPresent() {
        val node: SubspaceLayoutNode = SubspaceLayoutNode()
        node.modifier = SubspaceModifier.testTag(tag = "anyTag")
        val matcher: SubspaceSemanticsMatcher =
            SubspaceSemanticsMatcher.keyNotDefined(key = SemanticsProperties.TestTag)

        val result: Boolean = matcher.matches(node = node.measurableLayout)

        assertFalse(result)
    }
}
