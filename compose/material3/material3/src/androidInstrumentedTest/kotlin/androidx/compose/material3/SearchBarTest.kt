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

package androidx.compose.material3

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.onConsumedWindowInsetsChanged
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3Api::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class SearchBarTest {
    @get:Rule val rule = createComposeRule()

    private val SearchBarTestTag = "SearchBar"
    private val IconTestTag = "Icon"
    private val BackTestTag = "Back"

    @Test
    fun searchBar_becomesExpandedAndFocusedOnClick_andNotExpandedAndUnfocusedOnBack() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.fillMaxSize()) {
                val dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
                var expanded by remember { mutableStateOf(false) }

                // Extra item for initial focus.
                Box(Modifier.size(10.dp).focusable())

                SearchBar(
                    modifier = Modifier.testTag(SearchBarTestTag),
                    inputField = {
                        SearchBarDefaults.InputField(
                            state = rememberTextFieldState("Query"),
                            onSearch = {},
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                        )
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    Button(
                        onClick = { dispatcher.onBackPressed() },
                        modifier = Modifier.testTag(BackTestTag),
                        content = { Text("Content") },
                    )
                }
            }
        }

        // For the purposes of this test, the content is the back button
        rule.onNodeWithTag(BackTestTag).assertDoesNotExist()

        rule.onNodeWithTag(SearchBarTestTag).performClick()
        rule.onNodeWithTag(BackTestTag).assertIsDisplayed()
        // onNodeWithText instead of onNodeWithTag to access the underlying text field
        rule.onNodeWithText("Query").assertIsFocused()

        rule.onNodeWithTag(BackTestTag).performClick()
        rule.onNodeWithTag(BackTestTag).assertDoesNotExist()
        rule.onNodeWithText("Query").assertIsNotFocused()
    }

    @Test
    fun searchBar_doesNotOverwriteFocusOfOtherComponents() {
        val focusRequester = FocusRequester()
        rule.setMaterialContent(lightColorScheme()) {
            Column(Modifier.fillMaxSize()) {
                SearchBar(
                    modifier = Modifier.testTag(SearchBarTestTag),
                    inputField = {
                        SearchBarDefaults.InputField(
                            state = rememberTextFieldState("Query"),
                            onSearch = {},
                            expanded = false,
                            onExpandedChange = {},
                        )
                    },
                    expanded = false,
                    onExpandedChange = {},
                    content = {},
                )
                TextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier.testTag("SIBLING").focusRequester(focusRequester)
                )
            }
        }

        rule.runOnIdle { focusRequester.requestFocus() }

        rule.onNodeWithTag("SIBLING").assertIsFocused()

        rule.onNodeWithTag(SearchBarTestTag).performClick()
        rule.onNodeWithText("Query").assertIsFocused()
    }

    @Test
    fun searchBar_onImeAction_executesSearchCallback() {
        var capturedSearchQuery = ""

        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.fillMaxSize()) {
                var expanded by remember { mutableStateOf(true) }

                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            state = rememberTextFieldState("Query"),
                            onSearch = { capturedSearchQuery = it },
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                        )
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    content = { Text("Content") },
                )
            }
        }
        // onNodeWithText instead of onNodeWithTag to access the underlying text field
        rule.onNodeWithText("Query").performImeAction()
        assertThat(capturedSearchQuery).isEqualTo("Query")
    }

    @Test
    fun searchBar_notExpandedSize() {
        rule
            .setMaterialContentForSizeAssertions {
                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            state = rememberTextFieldState(),
                            onSearch = {},
                            expanded = false,
                            onExpandedChange = {},
                            placeholder = { Text("Hint") },
                        )
                    },
                    expanded = false,
                    onExpandedChange = {},
                    content = {},
                )
            }
            .assertWidthIsEqualTo(SearchBarMinWidth)
            .assertHeightIsEqualTo(SearchBarDefaults.InputFieldHeight + SearchBarVerticalPadding)
    }

    @Test
    fun searchBar_expandedSize() {
        val totalHeight = 500.dp
        val totalWidth = 325.dp
        val searchBarSize = Ref<IntSize>()

        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.size(width = totalWidth, height = totalHeight)) {
                SearchBar(
                    modifier = Modifier.onGloballyPositioned { searchBarSize.value = it.size },
                    inputField = {
                        SearchBarDefaults.InputField(
                            state = rememberTextFieldState(),
                            onSearch = {},
                            expanded = true,
                            onExpandedChange = {},
                            placeholder = { Text("Hint") },
                        )
                    },
                    expanded = true,
                    onExpandedChange = {},
                    content = { Text("Content") },
                )
            }
        }

        rule.runOnIdleWithDensity {
            assertThat(searchBarSize.value?.width).isEqualTo(totalWidth.roundToPx())
            assertThat(searchBarSize.value?.height).isEqualTo(totalHeight.roundToPx())
        }
    }

    @Test
    fun searchBar_usesAndConsumesWindowInsets() {
        val parentTopInset = 10
        val searchBarTopInset = 25

        val position = Ref<Offset>()
        lateinit var density: Density
        lateinit var childConsumedInsets: WindowInsets

        rule.setMaterialContent(lightColorScheme()) {
            density = LocalDensity.current
            Box(Modifier.windowInsetsPadding(WindowInsets(top = parentTopInset))) {
                SearchBar(
                    modifier =
                        Modifier.onGloballyPositioned { position.value = it.positionInRoot() },
                    windowInsets = WindowInsets(top = searchBarTopInset),
                    inputField = {
                        SearchBarDefaults.InputField(
                            state = rememberTextFieldState(),
                            onSearch = {},
                            expanded = true,
                            onExpandedChange = {},
                            placeholder = { Text("Hint") },
                        )
                    },
                    expanded = true,
                    onExpandedChange = {},
                ) {
                    Box(Modifier.onConsumedWindowInsetsChanged { childConsumedInsets = it })
                }
            }
        }

        assertThat(position.value!!.y.roundToInt()).isEqualTo(parentTopInset)
        assertThat(childConsumedInsets.getTop(density)).isEqualTo(searchBarTopInset)
    }

    @Test
    fun searchBar_clickingIconButton_doesNotExpandSearchBarItself() {
        var iconClicked = false

        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.fillMaxSize()) {
                var expanded by remember { mutableStateOf(false) }

                SearchBar(
                    modifier = Modifier.testTag(SearchBarTestTag),
                    inputField = {
                        SearchBarDefaults.InputField(
                            state = rememberTextFieldState("Query"),
                            onSearch = {},
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            trailingIcon = {
                                IconButton(
                                    onClick = { iconClicked = true },
                                    modifier = Modifier.testTag(IconTestTag)
                                ) {
                                    Icon(Icons.Default.MoreVert, null)
                                }
                            }
                        )
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    Text("Content")
                }
            }
        }

        rule.onNodeWithText("Content").assertDoesNotExist()

        // Click icon, not search bar
        rule.onNodeWithTag(IconTestTag).performClick()
        assertThat(iconClicked).isTrue()
        rule.onNodeWithText("Content").assertDoesNotExist()

        // Click search bar
        rule.onNodeWithTag(SearchBarTestTag).performClick()
        rule.onNodeWithText("Content").assertIsDisplayed()
    }

    @Test
    fun dockedSearchBar_becomesExpandedAndFocusedOnClick_andNotExpandedAndUnfocusedOnBack() {
        rule.setMaterialContent(lightColorScheme()) {
            Column(Modifier.fillMaxSize()) {
                val dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
                var expanded by remember { mutableStateOf(false) }

                // Extra item for initial focus.
                Box(Modifier.size(10.dp).focusable())

                DockedSearchBar(
                    modifier = Modifier.testTag(SearchBarTestTag),
                    inputField = {
                        SearchBarDefaults.InputField(
                            state = rememberTextFieldState("Query"),
                            onSearch = {},
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                        )
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    Button(
                        onClick = { dispatcher.onBackPressed() },
                        modifier = Modifier.testTag(BackTestTag),
                        content = { Text("Content") },
                    )
                }
            }
        }

        // For the purposes of this test, the content is the back button
        rule.onNodeWithTag(BackTestTag).assertDoesNotExist()

        rule.onNodeWithTag(SearchBarTestTag).performClick()
        rule.onNodeWithTag(BackTestTag).assertIsDisplayed()
        // onNodeWithText instead of onNodeWithTag to access the underlying text field
        rule.onNodeWithText("Query").assertIsFocused()

        rule.onNodeWithTag(BackTestTag).performClick()
        rule.onNodeWithTag(BackTestTag).assertDoesNotExist()
        rule.onNodeWithText("Query").assertIsNotFocused()
    }

    @Test
    fun dockedSearchBar_doesNotOverwriteFocusOfOtherComponents() {
        val focusRequester = FocusRequester()
        rule.setMaterialContent(lightColorScheme()) {
            Column(Modifier.fillMaxSize()) {
                DockedSearchBar(
                    modifier = Modifier.testTag(SearchBarTestTag),
                    inputField = {
                        SearchBarDefaults.InputField(
                            state = rememberTextFieldState("Query"),
                            onSearch = {},
                            expanded = false,
                            onExpandedChange = {},
                        )
                    },
                    expanded = false,
                    onExpandedChange = {},
                    content = {},
                )
                TextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier.testTag("SIBLING").focusRequester(focusRequester)
                )
            }
        }

        rule.runOnIdle { focusRequester.requestFocus() }

        rule.onNodeWithTag("SIBLING").assertIsFocused()

        rule.onNodeWithTag(SearchBarTestTag).performClick()
        rule.onNodeWithText("Query").assertIsFocused()
    }

    @Test
    fun dockedSearchBar_onImeAction_executesSearchCallback() {
        var capturedSearchQuery = ""

        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.fillMaxSize()) {
                var expanded by remember { mutableStateOf(true) }

                DockedSearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            state = rememberTextFieldState("Query"),
                            onSearch = { capturedSearchQuery = it },
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                        )
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    content = { Text("Content") },
                )
            }
        }
        // onNodeWithText instead of onNodeWithTag to access the underlying text field
        rule.onNodeWithText("Query").performImeAction()
        assertThat(capturedSearchQuery).isEqualTo("Query")
    }

    @Test
    fun dockedSearchBar_notExpandedSize() {
        rule
            .setMaterialContentForSizeAssertions {
                DockedSearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            state = rememberTextFieldState(),
                            onSearch = {},
                            expanded = false,
                            onExpandedChange = {},
                            placeholder = { Text("Hint") },
                        )
                    },
                    expanded = false,
                    onExpandedChange = {},
                    content = {},
                )
            }
            .assertWidthIsEqualTo(SearchBarMinWidth)
            .assertHeightIsEqualTo(SearchBarDefaults.InputFieldHeight)
    }

    @Test
    fun dockedSearchBar_expandedSize() {
        rule
            .setMaterialContentForSizeAssertions {
                DockedSearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            state = rememberTextFieldState(),
                            onSearch = {},
                            expanded = true,
                            onExpandedChange = {},
                            placeholder = { Text("Hint") },
                        )
                    },
                    expanded = true,
                    onExpandedChange = {},
                    content = { Text("Content") },
                )
            }
            .assertWidthIsEqualTo(SearchBarMinWidth)
            .assertHeightIsEqualTo(
                SearchBarDefaults.InputFieldHeight + DockedExpandedTableMinHeight
            )
    }

    @Test
    fun dockedSearchBar_clickingIconButton_doesNotExpandSearchBarItself() {
        var iconClicked = false

        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.fillMaxSize()) {
                var expanded by remember { mutableStateOf(false) }

                DockedSearchBar(
                    modifier = Modifier.testTag(SearchBarTestTag),
                    inputField = {
                        SearchBarDefaults.InputField(
                            state = rememberTextFieldState("Query"),
                            onSearch = {},
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            trailingIcon = {
                                IconButton(
                                    onClick = { iconClicked = true },
                                    modifier = Modifier.testTag(IconTestTag)
                                ) {
                                    Icon(Icons.Default.MoreVert, null)
                                }
                            }
                        )
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    Text("Content")
                }
            }
        }

        rule.onNodeWithText("Content").assertDoesNotExist()

        // Click icon, not search bar
        rule.onNodeWithTag(IconTestTag).performClick()
        assertThat(iconClicked).isTrue()
        rule.onNodeWithText("Content").assertDoesNotExist()

        // Click search bar
        rule.onNodeWithTag(SearchBarTestTag).performClick()
        rule.onNodeWithText("Content").assertIsDisplayed()
    }
}
