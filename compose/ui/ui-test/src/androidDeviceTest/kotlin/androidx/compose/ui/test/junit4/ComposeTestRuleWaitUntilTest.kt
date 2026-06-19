/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.test.junit4

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.testutils.expectError
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class ComposeTestRuleWaitUntilTest {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    companion object {
        private const val TestTag = "TestTag"
        private const val Timeout = 500L
    }

    @Composable private fun TaggedBox() = Box(Modifier.size(10.dp, 10.dp).testTag(TestTag))

    @Test
    fun waitUntil_includesConditionDescription_whenSpecified() {
        rule.setContent { TaggedBox() }

        expectError<ComposeTimeoutException>(
            // This is actually regex, so special characters need to be escaped.
            expectedMessage = "Condition \\(foo\\) still not satisfied after $Timeout ms"
        ) {
            rule.waitUntil("foo", timeoutMillis = Timeout) { false }
        }
    }

    @Test
    fun waitUntilNodeCount_succeedsWhen_nodeCountCorrect() {
        rule.setContent {
            TaggedBox()
            TaggedBox()
            TaggedBox()
        }

        rule.waitUntilNodeCount(hasTestTag(TestTag), 3, Timeout)
    }

    @Test
    fun waitUntilNodeCount_throwsWhen_nodeCountIncorrect() {
        rule.setContent {
            TaggedBox()
            TaggedBox()
            TaggedBox()
        }

        expectError<ComposeTimeoutException>(
            expectedMessage =
                "Condition \\(exactly 2 nodes match \\(TestTag = 'TestTag'\\)\\) " +
                    "still not satisfied after $Timeout ms"
        ) {
            rule.waitUntilNodeCount(hasTestTag(TestTag), 2, Timeout)
        }
    }

    @Test
    fun waitUntilAtLeastOneExists_succeedsWhen_nodesExist() {
        rule.setContent {
            TaggedBox()
            TaggedBox()
        }

        rule.waitUntilAtLeastOneExists(hasTestTag(TestTag))
    }

    @Test
    fun waitUntilAtLeastOneExists_throwsWhen_nodesDoNotExist() {
        rule.setContent { Box(Modifier.size(10.dp)) }

        expectError<ComposeTimeoutException>(
            expectedMessage =
                "Condition \\(at least one node matches " +
                    "\\(TestTag = 'TestTag'\\)\\) still not satisfied after $Timeout ms"
        ) {
            rule.waitUntilAtLeastOneExists(hasTestTag(TestTag), Timeout)
        }
    }

    @Test
    fun waitUntilExactlyOneExists_succeedsWhen_oneNodeExists() {
        rule.setContent { TaggedBox() }

        rule.waitUntilExactlyOneExists(hasTestTag(TestTag))
    }

    @Test
    fun waitUntilExactlyOneExists_throwsWhen_twoNodesExist() {
        rule.setContent {
            TaggedBox()
            TaggedBox()
        }

        expectError<ComposeTimeoutException>(
            expectedMessage =
                "Condition \\(exactly 1 nodes match \\(TestTag = 'TestTag'\\)\\) " +
                    "still not satisfied after $Timeout ms"
        ) {
            rule.waitUntilExactlyOneExists(hasTestTag(TestTag), Timeout)
        }
    }

    @Test
    fun waitUntilDoesNotExists_succeedsWhen_nodeDoesNotExist() {
        rule.setContent { Box(Modifier.size(10.dp)) }

        rule.waitUntilDoesNotExist(hasTestTag(TestTag), timeoutMillis = Timeout)
    }

    @Test
    fun waitUntilDoesNotExists_throwsWhen_nodeExistsUntilTimeout() {
        rule.setContent { TaggedBox() }

        expectError<ComposeTimeoutException>(
            expectedMessage =
                "Condition \\(exactly 0 nodes match \\(TestTag = 'TestTag'\\)\\) " +
                    "still not satisfied after $Timeout ms"
        ) {
            rule.waitUntilDoesNotExist(hasTestTag(TestTag), timeoutMillis = Timeout)
        }
    }

    @Test
    fun waitUntilAtLeastOneExists_withUnmergedTree() {
        rule.setContent {
            Box(Modifier.semantics(mergeDescendants = true) {}.testTag("parent")) {
                Box(Modifier.testTag("child"))
            }
        }

        // This should time out because by default it uses merged tree and "child" is merged.
        expectError<ComposeTimeoutException> {
            rule.waitUntilAtLeastOneExists(hasTestTag("child"), timeoutMillis = Timeout)
        }

        // This should succeed because we explicitly use unmerged tree.
        rule.waitUntilAtLeastOneExists(
            hasTestTag("child"),
            timeoutMillis = Timeout,
            useUnmergedTree = true,
        )
    }

    @Test
    fun waitUntilDoesNotExist_withUnmergedTree() {
        rule.setContent {
            Box(Modifier.semantics(mergeDescendants = true) {}.testTag("parent")) {
                Box(Modifier.testTag("child"))
            }
        }

        // This should succeed by default because "child" is not in merged tree.
        rule.waitUntilDoesNotExist(hasTestTag("child"), timeoutMillis = Timeout)

        // This should throw because "child" IS in unmerged tree.
        expectError<ComposeTimeoutException> {
            rule.waitUntilDoesNotExist(
                hasTestTag("child"),
                timeoutMillis = Timeout,
                useUnmergedTree = true,
            )
        }
    }

    @Test
    fun waitUntilNodeCount_withUnmergedTree() {
        rule.setContent {
            Box(Modifier.semantics(mergeDescendants = true) {}.testTag("parent")) {
                Box(Modifier.testTag("child"))
                Box(Modifier.testTag("child"))
            }
        }

        // This should time out because by default it uses merged tree and "child" is merged.
        expectError<ComposeTimeoutException> {
            rule.waitUntilNodeCount(hasTestTag("child"), count = 2, timeoutMillis = Timeout)
        }

        // This should succeed because we explicitly use unmerged tree.
        rule.waitUntilNodeCount(
            hasTestTag("child"),
            count = 2,
            timeoutMillis = Timeout,
            useUnmergedTree = true,
        )
    }

    @Test
    fun waitUntilExactlyOneExists_withUnmergedTree() {
        rule.setContent {
            Box(Modifier.semantics(mergeDescendants = true) {}.testTag("parent")) {
                Box(Modifier.testTag("child"))
            }
        }

        // This should time out because by default it uses merged tree and "child" is merged.
        expectError<ComposeTimeoutException> {
            rule.waitUntilExactlyOneExists(hasTestTag("child"), timeoutMillis = Timeout)
        }

        // This should succeed because we explicitly use unmerged tree.
        rule.waitUntilExactlyOneExists(
            hasTestTag("child"),
            timeoutMillis = Timeout,
            useUnmergedTree = true,
        )
    }
}
