/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.Composable
import androidx.test.filters.MediumTest
import androidx.ui.core.Modifier
import androidx.ui.core.semantics.semantics
import androidx.ui.layout.Column
import androidx.ui.semantics.SemanticsPropertyReceiver
import androidx.ui.semantics.accessibilityLabel
import androidx.ui.semantics.testTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class FindersTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    @Test
    fun findAll_zeroOutOfOne_findsNone() {
        composeTestRule.setContent {
            BoundaryNode { testTag = "not_myTestTag" }
        }

        findAll(hasTestTag("myTestTag")).assertCountEquals(0)
    }

    @Test
    fun findAll_oneOutOfTwo_findsOne() {
        composeTestRule.setContent {
            BoundaryNode { testTag = "myTestTag" }
            BoundaryNode { testTag = "myTestTag2" }
        }

        findAll(hasTestTag("myTestTag"))
            .assertCountEquals(1)
            .first()
            .assert(hasTestTag("myTestTag"))
    }

    @Test
    fun findAll_twoOutOfTwo_findsTwo() {
        composeTestRule.setContent {
            BoundaryNode { testTag = "myTestTag" }
            BoundaryNode { testTag = "myTestTag" }
        }

        findAll(hasTestTag("myTestTag"))
            .assertCountEquals(2)
            .apply {
                get(0).assert(hasTestTag("myTestTag"))
                get(1).assert(hasTestTag("myTestTag"))
            }
    }

    @Test
    fun findByText_matches() {
        composeTestRule.setContent {
            BoundaryNode { accessibilityLabel = "Hello World" }
        }

        findByText("Hello World")
    }

    @Test(expected = AssertionError::class)
    fun findByText_fails() {
        composeTestRule.setContent {
            BoundaryNode { accessibilityLabel = "Hello World" }
        }

        // Need to assert exists or it won't fail
        findByText("World").assertExists()
    }

    @Test
    fun findBySubstring_matches() {
        composeTestRule.setContent {
            BoundaryNode { accessibilityLabel = "Hello World" }
        }

        findBySubstring("World")
    }

    @Test
    fun findBySubstring_ignoreCase_matches() {
        composeTestRule.setContent {
            BoundaryNode { accessibilityLabel = "Hello World" }
        }

        findBySubstring("world", ignoreCase = true)
    }

    @Test(expected = AssertionError::class)
    fun findBySubstring_wrongCase_fails() {
        composeTestRule.setContent {
            BoundaryNode { accessibilityLabel = "Hello World" }
        }

        // Need to assert exists or it won't fail
        findBySubstring("world").assertExists()
    }

    @Composable
    fun BoundaryNode(props: (SemanticsPropertyReceiver.() -> Unit)? = null) {
        Column(Modifier.semantics(properties = props)) {}
    }
}