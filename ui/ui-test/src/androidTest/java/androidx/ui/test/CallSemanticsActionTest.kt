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
import androidx.compose.state
import androidx.test.filters.MediumTest
import androidx.ui.core.Modifier
import androidx.ui.core.semantics.semantics
import androidx.ui.layout.Column
import androidx.ui.semantics.AccessibilityAction
import androidx.ui.semantics.SemanticsPropertyKey
import androidx.ui.semantics.SemanticsPropertyReceiver
import androidx.ui.semantics.accessibilityLabel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class CallSemanticsActionTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    @Test
    fun performSemanticsAction() {
        composeTestRule.setContent {
            val state = state { "Nothing" }
            BoundaryNode {
                setString("SetString") { state.value = it; return@setString true }
                accessibilityLabel = state.value
            }
        }

        findByText("Nothing")
            .assertExists()
            .callSemanticsAction(MyActions.SetString) { it("Hello") }

        findByText("Nothing")
            .assertDoesNotExist()

        findByText("Hello")
            .assertExists()
    }

    object MyActions {
        val SetString = SemanticsPropertyKey<AccessibilityAction<(String) -> Boolean>>("SetString")
    }

    fun SemanticsPropertyReceiver.setString(label: String? = null, action: (String) -> Boolean) {
        this[MyActions.SetString] = AccessibilityAction(label, action)
    }

    @Composable
    fun BoundaryNode(props: (SemanticsPropertyReceiver.() -> Unit)? = null) {
        Column(Modifier.semantics(properties = props)) {}
    }
}