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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.MagnifierPositionInRoot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.google.common.truth.Truth.assertWithMessage
import kotlin.test.fail

internal fun getMagnifierCenterOffset(
    rule: ComposeTestRule,
    requireSpecified: Boolean = false
): Offset {
    val positions = getMagnifierPositions(rule)
    return if (requireSpecified) {
        val specifiedPositions = positions.filter { it.isSpecified }
        if (specifiedPositions.size != 1) {
            fail(
                "Expected one specified magnifier position, but found ${specifiedPositions.size}${
                    if (specifiedPositions.isEmpty()) "." else ": $specifiedPositions"
                }"
            )
        }
        specifiedPositions.single()
    } else {
        positions.firstOrNull() ?: fail("No magnifier position found")
    }
}

internal fun assertMagnifierExists(rule: ComposeTestRule) {
    assertWithMessage("Expected magnifier to exist and have specified coordinates.")
        .that(getMagnifierPositions(rule).any { it.isSpecified })
        .isTrue()
}

/**
 * Asserts that there is no magnifier being displayed. This may be because no
 * `Modifier.magnifier` modifiers are currently set on any nodes, or because all the magnifiers
 * that exist have an unspecified position.
 */
internal fun assertNoMagnifierExists(rule: ComposeTestRule) {
    // The magnifier semantics will be present whenever the modifier is, even if the modifier
    // isn't actually showing a magnifier because the position is unspecified. So instead of
    // just checking that no semantics property exists, we need to check that the value of each
    // property won't show a magnifier.
    assertWithMessage("Expected magnifier to not exist or exist with unspecified coordinates.")
        .that(getMagnifierPositions(rule).all { it.isUnspecified })
        .isTrue()
}

internal fun getMagnifierPositions(rule: ComposeTestRule) =
    rule.onAllNodes(SemanticsMatcher.keyIsDefined(MagnifierPositionInRoot))
        .fetchSemanticsNodes(atLeastOneRootRequired = false)
        .map { it.config[MagnifierPositionInRoot] }
        .let { positionFunctions ->
            rule.runOnIdle {
                positionFunctions.map { it.invoke() }
            }
        }
