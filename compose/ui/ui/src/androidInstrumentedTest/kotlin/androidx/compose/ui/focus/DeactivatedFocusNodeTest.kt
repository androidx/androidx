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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class DeactivatedFocusNodeTest {
    @get:Rule val rule = createComposeRule()

    private lateinit var lazyListState: LazyListState
    private lateinit var coroutineScope: CoroutineScope
    private val focusStates = mutableMapOf<Int, FocusState>()
    private val initialFocusedItem = FocusRequester()

    @Test
    fun deactivatedActiveFocusNodeSendsFocusEvent() {
        // Arrange.
        rule.setTestContent {
            LazyRow(state = lazyListState, modifier = Modifier.size(10.dp)) {
                items(2) { index ->
                    Box(
                        Modifier.size(10.dp)
                            .testTag("$index")
                            .then(
                                if (index == 0) {
                                    Modifier.focusRequester(initialFocusedItem)
                                } else {
                                    Modifier
                                }
                            )
                            .onFocusChanged { focusStates[index] = it }
                            .focusTarget()
                    )
                }
            }
        }
        rule.runOnIdle {
            initialFocusedItem.requestFocus()
            focusStates.clear()
        }

        // Act.
        rule.runOnIdle { coroutineScope.launch { lazyListState.scrollToItem(1) } }

        // Assert.
        rule.runOnIdle { assertThat(focusStates[0]).isEqualTo(Inactive) }
    }

    @Test
    fun deactivatedActiveParentFocusNodeSendsFocusEvent() {
        // Arrange.
        rule.setTestContent {
            LazyRow(state = lazyListState, modifier = Modifier.size(10.dp)) {
                items(2) { index ->
                    Box(
                        Modifier.size(10.dp)
                            .onFocusChanged { focusStates[index] = it }
                            .focusTarget()
                    ) {
                        Box(
                            Modifier.size(5.dp)
                                .then(
                                    if (index == 0) {
                                        Modifier.focusRequester(initialFocusedItem)
                                    } else {
                                        Modifier
                                    }
                                )
                                .focusTarget()
                        )
                    }
                }
            }
        }
        rule.runOnIdle {
            initialFocusedItem.requestFocus()
            focusStates.clear()
        }

        // Act.
        rule.runOnIdle { coroutineScope.launch { lazyListState.scrollToItem(1) } }

        // Assert.
        rule.runOnIdle { assertThat(focusStates[0]).isEqualTo(Inactive) }
    }

    @Test
    fun deactivatedCapturedFocusNodeSendsFocusEvent() {
        // Arrange.
        rule.setTestContent {
            LazyRow(state = lazyListState, modifier = Modifier.size(10.dp)) {
                items(2) { index ->
                    Box(
                        Modifier.size(10.dp)
                            .testTag("$index")
                            .then(
                                if (index == 0) {
                                    Modifier.focusRequester(initialFocusedItem)
                                } else {
                                    Modifier
                                }
                            )
                            .onFocusChanged { focusStates[index] = it }
                            .focusTarget()
                    )
                }
            }
        }
        rule.runOnIdle {
            initialFocusedItem.requestFocus()
            initialFocusedItem.captureFocus()
            focusStates.clear()
        }

        // Act.
        rule.runOnIdle { coroutineScope.launch { lazyListState.scrollToItem(1) } }

        // Assert.
        rule.runOnIdle { assertThat(focusStates[0]).isEqualTo(Inactive) }
    }

    @Test
    fun deactivatedInactiveFocusNodeDoesNotSendFocusEvent() {
        // Arrange.
        rule.setTestContent {
            LazyRow(state = lazyListState, modifier = Modifier.size(10.dp)) {
                items(2) { index ->
                    Box(
                        Modifier.size(10.dp)
                            .testTag("$index")
                            .then(
                                if (index == 0) {
                                    Modifier.focusRequester(initialFocusedItem)
                                } else {
                                    Modifier
                                }
                            )
                            .onFocusChanged { focusStates[index] = it }
                            .focusTarget()
                    )
                }
            }
        }
        rule.runOnIdle { focusStates.clear() }

        // Act.
        rule.runOnIdle { coroutineScope.launch { lazyListState.scrollToItem(1) } }

        // Assert.
        rule.runOnIdle { assertThat(focusStates[0]).isNull() }
    }

    private fun ComposeContentTestRule.setTestContent(content: @Composable () -> Unit) {
        setContent {
            coroutineScope = rememberCoroutineScope()
            lazyListState = rememberLazyListState()
            Box { content() }
        }
    }
}
