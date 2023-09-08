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

package androidx.compose.ui.focus

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalComposeUiApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class RequestFocusExitTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun redirectingFocusExitFromChild1ToChild2_focusExitIsCalled() {
        // Arrange.
        val (destination, child1, child2) = FocusRequester.createRefs()
        var exitCount = 0
        rule.setFocusableContent {
            Box(Modifier.focusTarget()) {
                Box(
                    Modifier
                        .focusRequester(destination)
                        .focusTarget()
                )
                Box(
                    Modifier
                        .focusProperties {
                            exit = {
                                exitCount++
                                child2
                            }
                        }
                        .focusTarget()
                ) {
                    Box(
                        Modifier
                            .focusRequester(child1)
                            .focusTarget()
                    )
                    Box(
                        Modifier
                            .focusRequester(child2)
                            .focusTarget()
                    )
                }
            }
        }
        rule.runOnIdle { child1.requestFocus() }

        // Act.
        rule.runOnIdle { destination.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(exitCount).isEqualTo(1) }

        // Reset - To ensure that focus exit is called every time we exit.
        rule.runOnIdle {
            child1.requestFocus()
            exitCount = 0
        }

        // Act.
        rule.runOnIdle { destination.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(exitCount).isEqualTo(1) }
    }
}
