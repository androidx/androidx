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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.onConsumedWindowInsetsChanged
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.tokens.SearchBarTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.WindowInsets
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3Api::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class SearchBarTest {
    @get:Rule val rule = createComposeRule()

    private val SearchBarTestTag = "SearchBar"
    private val ScrollableContentTestTag = "Scrollable"
    private val CollapsedInputFieldTestTag = "CollapsedInputField"
    private val ExpandedInputFieldTestTag = "ExpandedInputField"
    private val IconTestTag = "Icon"
    private val BackTestTag = "Back"
    private val ContentTestTag = "Content"
    private val BoxTestTag = "BoxTestTag"

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
                    modifier = Modifier.testTag("SIBLING").focusRequester(focusRequester),
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
                                    modifier = Modifier.testTag(IconTestTag),
                                ) {
                                    Icon(Icons.Default.MoreVert, null)
                                }
                            },
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
                    modifier = Modifier.testTag("SIBLING").focusRequester(focusRequester),
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
                                    modifier = Modifier.testTag(IconTestTag),
                                ) {
                                    Icon(Icons.Default.MoreVert, null)
                                }
                            },
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
    fun searchBarColors_containerColor_becomesContainerColorOfTextField() {
        lateinit var colors: SearchBarColors

        rule.setMaterialContent(lightColorScheme()) {
            colors = SearchBarDefaults.colors(containerColor = Color.Red)
        }

        assertThat(colors.inputFieldColors.focusedContainerColor).isEqualTo(Color.Red)
        assertThat(colors.inputFieldColors.unfocusedContainerColor).isEqualTo(Color.Red)
        assertThat(colors.inputFieldColors.disabledContainerColor).isEqualTo(Color.Red)
    }

    // Tests for new search bar APIs below this section

    @Test
    fun newSearchBar_becomesExpandedAndFocusedOnClick_andCollapsedAndUnfocusedOnBack() {
        var softwareKeyboardController: SoftwareKeyboardController? = null
        var isInTouchMode = false
        rule.setMaterialContent(lightColorScheme()) {
            val inputModeManager = LocalInputModeManager.current

            SideEffect { isInTouchMode = inputModeManager.requestInputMode(InputMode.Touch) }

            Box(Modifier.fillMaxSize()) {
                val searchBarState = rememberSearchBarState()
                val textFieldState = rememberTextFieldState("Query")

                // Extra item for initial focus.
                Box(Modifier.size(10.dp).focusable())

                SearchBar(
                    modifier = Modifier.testTag(SearchBarTestTag),
                    state = searchBarState,
                    inputField = {
                        InputField(
                            searchBarState = searchBarState,
                            textFieldState = textFieldState,
                            modifier = Modifier.testTag(CollapsedInputFieldTestTag),
                        )
                    },
                )
                ExpandedFullScreenSearchBar(
                    state = searchBarState,
                    inputField = {
                        softwareKeyboardController = LocalSoftwareKeyboardController.current
                        InputField(
                            searchBarState = searchBarState,
                            textFieldState = textFieldState,
                            modifier = Modifier.testTag(ExpandedInputFieldTestTag),
                        )
                    },
                ) {
                    Text("Content", modifier = Modifier.testTag(ContentTestTag))
                }
            }
        }
        assumeTrue(isInTouchMode)

        rule.onNodeWithTag(ContentTestTag).assertDoesNotExist()

        rule.onNodeWithTag(SearchBarTestTag).performClick()
        rule.waitForIdle()

        rule.onNodeWithTag(ContentTestTag).assertIsDisplayed()
        rule.onNodeWithTag(ExpandedInputFieldTestTag).assertIsFocused()

        softwareKeyboardController?.hide()
        rule.waitForIdle()

        // Dismiss search bar
        Espresso.pressBack()
        rule.waitForIdle()

        rule.onNodeWithTag(ContentTestTag).assertDoesNotExist()
        rule.onNodeWithTag(ExpandedInputFieldTestTag).assertDoesNotExist()
        rule.onNodeWithTag(CollapsedInputFieldTestTag).assertIsNotFocused()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun newSearchBar_expansionBehavior_inNonTouchMode() {
        val focusRequester = FocusRequester()
        var softwareKeyboardController: SoftwareKeyboardController? = null
        lateinit var searchBarState: SearchBarState
        var isInKeyboardMode = false
        rule.setMaterialContent(lightColorScheme()) {
            val textFieldState = rememberTextFieldState()
            searchBarState = rememberSearchBarState()
            val inputModeManager = LocalInputModeManager.current

            SideEffect { isInKeyboardMode = inputModeManager.requestInputMode(InputMode.Keyboard) }

            Box(Modifier.fillMaxSize()) {
                // Extra item for initial focus.
                Box(Modifier.size(10.dp).focusable())

                SearchBar(
                    modifier = Modifier.testTag(SearchBarTestTag),
                    state = searchBarState,
                    inputField = {
                        InputField(
                            searchBarState = searchBarState,
                            textFieldState = textFieldState,
                            modifier =
                                Modifier.testTag(CollapsedInputFieldTestTag)
                                    .focusRequester(focusRequester),
                        )
                    },
                )
                ExpandedFullScreenSearchBar(
                    state = searchBarState,
                    inputField = {
                        softwareKeyboardController = LocalSoftwareKeyboardController.current
                        InputField(
                            searchBarState = searchBarState,
                            textFieldState = textFieldState,
                            modifier = Modifier.testTag(ExpandedInputFieldTestTag),
                        )
                    },
                ) {
                    Text("Content", modifier = Modifier.testTag(ContentTestTag))
                }
            }
        }
        assumeTrue(isInKeyboardMode)
        rule.runOnIdle { focusRequester.requestFocus() }

        // Focused and collapsed
        rule.onNodeWithTag(CollapsedInputFieldTestTag).assertIsFocused()
        rule.onNodeWithTag(ContentTestTag).assertDoesNotExist()

        // start typing
        rule.onNodeWithTag(CollapsedInputFieldTestTag).performKeyInput { pressKey(Key.A) }
        rule.waitForIdle()

        // Focused and expanded
        rule.onNodeWithTag(ContentTestTag).assertIsDisplayed()
        rule.onNodeWithTag(ExpandedInputFieldTestTag).assertIsFocused()

        softwareKeyboardController?.hide()
        rule.waitForIdle()

        // Dismiss search bar
        Espresso.pressBack()
        rule.waitUntil { searchBarState.currentValue == SearchBarValue.Collapsed }

        // Focused and collapsed
        rule.onNodeWithTag(CollapsedInputFieldTestTag).assertIsFocused()
        rule.onNodeWithTag(ContentTestTag).assertDoesNotExist()

        // press down
        rule.onNodeWithTag(CollapsedInputFieldTestTag).performKeyInput {
            pressKey(Key.DirectionDown)
        }
        rule.waitForIdle()

        // Focused and expanded
        rule.onNodeWithTag(ContentTestTag).assertIsDisplayed()
        rule.onNodeWithTag(ExpandedInputFieldTestTag).assertIsFocused()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun newSearchBar_expanded_isReachableViaDownKey() {
        val focusRequester = FocusRequester()
        var focused by mutableStateOf(false)
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.fillMaxSize()) {
                val state = rememberSearchBarState(initialValue = SearchBarValue.Expanded)
                ExpandedFullScreenSearchBar(
                    state = state,
                    inputField = {
                        InputField(
                            searchBarState = state,
                            textFieldState = rememberTextFieldState(),
                            modifier =
                                Modifier.testTag(ExpandedInputFieldTestTag)
                                    .focusRequester(focusRequester),
                        )
                    },
                ) {
                    Text(
                        "Content",
                        modifier =
                            Modifier.onFocusChanged {
                                    if (it.isFocused) {
                                        focused = true
                                    }
                                }
                                .focusTarget(),
                    )
                }
            }
        }

        rule.onNodeWithTag(ExpandedInputFieldTestTag).performKeyInput {
            pressKey(Key.DirectionDown)
        }

        rule.runOnIdle { assertThat(focused).isTrue() }
    }

    @Test
    fun newSearchBar_doesNotOverwriteFocusOfOtherComponents() {
        val focusRequester = FocusRequester()
        rule.setMaterialContent(lightColorScheme()) {
            Column(Modifier.fillMaxSize()) {
                val searchBarState = rememberSearchBarState()
                val textFieldState = rememberTextFieldState("Query")
                SearchBar(
                    modifier = Modifier.testTag(SearchBarTestTag),
                    state = searchBarState,
                    inputField = {
                        InputField(
                            searchBarState = searchBarState,
                            textFieldState = textFieldState,
                            modifier = Modifier.testTag(CollapsedInputFieldTestTag),
                        )
                    },
                )
                ExpandedFullScreenSearchBar(
                    state = searchBarState,
                    inputField = {
                        InputField(
                            searchBarState = searchBarState,
                            textFieldState = textFieldState,
                            modifier = Modifier.testTag(ExpandedInputFieldTestTag),
                        )
                    },
                    content = {},
                )

                TextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier.testTag("SIBLING").focusRequester(focusRequester),
                )
            }
        }

        rule.runOnIdle { focusRequester.requestFocus() }

        rule.onNodeWithTag("SIBLING").assertIsFocused()

        rule.onNodeWithTag(SearchBarTestTag).performClick()
        rule.onNodeWithTag(ExpandedInputFieldTestTag).assertIsFocused()
    }

    @Test
    fun newSearchBar_onImeAction_executesSearchCallback() {
        var capturedSearchQuery = ""

        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.fillMaxSize()) {
                val searchBarState = rememberSearchBarState()
                val textFieldState = rememberTextFieldState("Query")
                SearchBar(
                    modifier = Modifier.testTag(SearchBarTestTag),
                    state = searchBarState,
                    inputField = {
                        InputField(
                            searchBarState = searchBarState,
                            textFieldState = textFieldState,
                            onSearch = { capturedSearchQuery = it },
                            modifier = Modifier.testTag(CollapsedInputFieldTestTag),
                        )
                    },
                )
                ExpandedFullScreenSearchBar(
                    state = searchBarState,
                    inputField = {
                        InputField(
                            searchBarState = searchBarState,
                            textFieldState = textFieldState,
                            onSearch = { capturedSearchQuery = it },
                            modifier = Modifier.testTag(ExpandedInputFieldTestTag),
                        )
                    },
                    content = { Text("Content") },
                )
            }
        }
        rule.onNodeWithTag(CollapsedInputFieldTestTag).performImeAction()
        assertThat(capturedSearchQuery).isEqualTo("Query")

        capturedSearchQuery = ""

        rule.onNodeWithTag(SearchBarTestTag).performClick()
        rule.onNodeWithTag(ExpandedInputFieldTestTag).performImeAction()
        assertThat(capturedSearchQuery).isEqualTo("Query")
    }

    @Test
    fun newSearchBar_collapsedSize() {
        rule
            .setMaterialContentForSizeAssertions {
                val searchBarState = rememberSearchBarState()
                val textFieldState = rememberTextFieldState("Query")
                SearchBar(
                    modifier = Modifier.testTag(SearchBarTestTag),
                    state = searchBarState,
                    inputField = {
                        InputField(
                            searchBarState = searchBarState,
                            textFieldState = textFieldState,
                            modifier = Modifier.testTag(CollapsedInputFieldTestTag),
                        )
                    },
                )
            }
            .assertWidthIsEqualTo(SearchBarMinWidth)
            .assertHeightIsEqualTo(SearchBarDefaults.InputFieldHeight)
    }

    @Test
    fun newSearchBar_clickingIconButton_doesNotExpandSearchBarItself() {
        var iconClicked = false

        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.fillMaxSize()) {
                val searchBarState = rememberSearchBarState()
                val textFieldState = rememberTextFieldState("Query")
                SearchBar(
                    modifier = Modifier.testTag(SearchBarTestTag),
                    state = searchBarState,
                    inputField = {
                        InputField(
                            searchBarState = searchBarState,
                            textFieldState = textFieldState,
                            modifier = Modifier.testTag(CollapsedInputFieldTestTag),
                            trailingIcon = {
                                IconButton(
                                    onClick = { iconClicked = true },
                                    modifier = Modifier.testTag(IconTestTag),
                                ) {
                                    Icon(Icons.Default.MoreVert, null)
                                }
                            },
                        )
                    },
                )
                ExpandedFullScreenSearchBar(
                    state = searchBarState,
                    inputField = {
                        InputField(
                            searchBarState = searchBarState,
                            textFieldState = textFieldState,
                            modifier = Modifier.testTag(ExpandedInputFieldTestTag),
                            // don't need a trailing icon since the search bar should never expand
                        )
                    },
                    content = { Text("Content", modifier = Modifier.testTag(ContentTestTag)) },
                )
            }
        }

        rule.onNodeWithTag(ContentTestTag).assertDoesNotExist()

        // Click icon, not search bar
        rule.onNodeWithTag(IconTestTag).performClick()
        assertThat(iconClicked).isTrue()
        rule.onNodeWithTag(ContentTestTag).assertDoesNotExist()
    }

    @Test
    fun appBarWithSearch_usesAndConsumesWindowInsets() {
        val parentTopInset = 10
        val searchBarTopInset = 25

        val searchBarPosition = Ref<Offset>()
        val inputFieldPosition = Ref<Offset>()
        lateinit var density: Density
        lateinit var childConsumedInsets: WindowInsets

        rule.setMaterialContent(lightColorScheme()) {
            density = LocalDensity.current
            val searchBarState = rememberSearchBarState()
            Box(Modifier.windowInsetsPadding(WindowInsets(top = parentTopInset))) {
                AppBarWithSearch(
                    state = searchBarState,
                    modifier =
                        Modifier.onGloballyPositioned {
                            searchBarPosition.value = it.positionInRoot()
                        },
                    windowInsets = WindowInsets(top = searchBarTopInset),
                    inputField = {
                        InputField(
                            searchBarState = searchBarState,
                            textFieldState = rememberTextFieldState(),
                            modifier =
                                Modifier.onGloballyPositioned {
                                    inputFieldPosition.value = it.positionInRoot()
                                },
                        )

                        Box(Modifier.onConsumedWindowInsetsChanged { childConsumedInsets = it })
                    },
                )
            }
        }

        assertThat(searchBarPosition.value!!.y.roundToInt()).isEqualTo(parentTopInset)
        assertThat(inputFieldPosition.value!!.y.roundToInt())
            .isEqualTo(
                searchBarTopInset + with(density) { AppBarWithSearchVerticalPadding.roundToPx() }
            )
        assertThat(childConsumedInsets.getTop(density)).isEqualTo(searchBarTopInset)
    }

    @Test
    fun appBarWithSearch_scrollBehavior_showsAndHidesWithVerticalScroll() {
        rule.setMaterialContent(lightColorScheme()) { SearchBarWithScrollableContent() }

        rule.onNodeWithTag(SearchBarTestTag).assertIsDisplayed()

        // swipe up/scroll down -> search bar hides
        rule.onNodeWithTag(ScrollableContentTestTag).performTouchInput { swipeUp() }
        rule.waitForIdle()
        rule.onNodeWithTag(SearchBarTestTag).assertIsNotDisplayed()

        // swipe down/scroll up -> search bar reappears
        rule.onNodeWithTag(ScrollableContentTestTag).performTouchInput { swipeDown() }
        rule.waitForIdle()
        rule.onNodeWithTag(SearchBarTestTag).assertIsDisplayed()
    }

    @Test
    fun appBarWithSearch_scrollBehavior_showsAndHidesWithVerticalScroll_reverseLayout() {
        rule.setMaterialContent(lightColorScheme()) {
            val reverseLayout = true
            val scrollBehavior =
                SearchBarDefaults.enterAlwaysSearchBarScrollBehavior(reverseLayout = reverseLayout)
            SearchBarWithScrollableContent(
                searchBarScrollBehavior = scrollBehavior,
                reverseLayout = reverseLayout,
            )
        }

        rule.onNodeWithTag(SearchBarTestTag).assertIsDisplayed()

        // swipe down/scroll up -> search bar stays on screen
        rule.onNodeWithTag(ScrollableContentTestTag).performTouchInput { swipeDown() }
        rule.waitForIdle()
        rule.onNodeWithTag(SearchBarTestTag).assertIsDisplayed()

        // swipe up/scroll down -> search bar hides
        rule.onNodeWithTag(ScrollableContentTestTag).performTouchInput { swipeUp() }
        rule.waitForIdle()
        rule.onNodeWithTag(SearchBarTestTag).assertIsNotDisplayed()

        // swipe down/scroll up -> search bar reappears
        rule.onNodeWithTag(ScrollableContentTestTag).performTouchInput { swipeDown() }
        rule.waitForIdle()
        rule.onNodeWithTag(SearchBarTestTag).assertIsDisplayed()
    }

    @Test
    fun appBarWithSearch_scrollBehavior_scrollDisabled() {
        var canScroll by mutableStateOf(true)
        rule.setMaterialContent(lightColorScheme()) {
            val scrollBehavior =
                SearchBarDefaults.enterAlwaysSearchBarScrollBehavior(canScroll = { canScroll })
            SearchBarWithScrollableContent(searchBarScrollBehavior = scrollBehavior)
        }

        // search bar is initially displayed
        rule.onNodeWithTag(SearchBarTestTag).assertIsDisplayed()

        rule.runOnIdle { canScroll = false }

        // swipe up/scroll down -> search bar does NOT hide
        rule.onNodeWithTag(ScrollableContentTestTag).performTouchInput { swipeUp() }
        rule.waitForIdle()
        rule.onNodeWithTag(SearchBarTestTag).assertIsDisplayed()

        rule.runOnIdle { canScroll = true }

        // swipe up/scroll down -> search bar hides
        rule.onNodeWithTag(ScrollableContentTestTag).performTouchInput { swipeUp() }
        rule.waitForIdle()
        rule.onNodeWithTag(SearchBarTestTag).assertIsNotDisplayed()

        rule.runOnIdle { canScroll = false }

        // swipe down/scroll up -> search bar does NOT reappear
        rule.onNodeWithTag(ScrollableContentTestTag).performTouchInput { swipeDown() }
        rule.waitForIdle()
        rule.onNodeWithTag(SearchBarTestTag).assertIsNotDisplayed()

        rule.runOnIdle { canScroll = true }

        // swipe down/scroll up -> search bar reappears
        rule.onNodeWithTag(ScrollableContentTestTag).performTouchInput { swipeDown() }
        rule.waitForIdle()
        rule.onNodeWithTag(SearchBarTestTag).assertIsDisplayed()
    }

    @Test
    fun appBarWithSearch_scrollBehavior_restoresOffsetState() {
        val restorationTester = StateRestorationTester(rule)
        var scrollBehavior: SearchBarScrollBehavior? = null
        restorationTester.setContent {
            scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
        }

        rule.runOnIdle {
            scrollBehavior!!.scrollOffsetLimit = -350f
            scrollBehavior!!.scrollOffset = -300f
        }

        scrollBehavior = null

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertThat(scrollBehavior!!.scrollOffsetLimit).isEqualTo(-350f)
            assertThat(scrollBehavior!!.scrollOffset).isEqualTo(-300f)
        }
    }

    @Test
    fun appBarWithSearch_correctlyPadsWhenParentHandlesInsetsAndContentPaddingIsUsed() {
        val appBarHeightDp = SearchBarTokens.ContainerHeight + AppBarWithSearchVerticalPadding * 2
        val siblingBoxHeightDp = 40.dp
        val appBarContentPaddingTopDp = 20.dp
        val statusBarHeightDp = 10.dp

        // The test illustrates "sibling problem": inset consumption isn't shared between sibling
        // components. If one uses an inset, its siblings aren't aware. In the example the parent
        // consumes insets.
        rule.setMaterialContent(lightColorScheme()) {
            val statusBarInsets =
                Insets.of(0, with(LocalDensity.current) { statusBarHeightDp.roundToPx() }, 0, 0)
            val windowInsets =
                WindowInsetsCompat.Builder()
                    .setInsets(WindowInsetsCompat.Type.statusBars(), statusBarInsets)
                    .build()
            val searchBarState = rememberSearchBarState()
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowInsets(windowInsets)) {
                Column(
                    modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    AppBarWithSearch(
                        modifier = Modifier.testTag(SearchBarTestTag),
                        state = searchBarState,
                        inputField = {
                            InputField(
                                searchBarState = searchBarState,
                                textFieldState = rememberTextFieldState(),
                            )
                        },
                        contentPadding = PaddingValues(top = appBarContentPaddingTopDp),
                    )
                    Box(
                        modifier =
                            Modifier.testTag(BoxTestTag)
                                .fillMaxWidth()
                                .height(siblingBoxHeightDp)
                                .windowInsetsPadding(WindowInsets.statusBars),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Sibling Box")
                    }
                }
            }
        }

        rule.waitForIdle()

        rule
            .onNodeWithTag(SearchBarTestTag)
            .assertTopPositionInRootIsEqualTo(statusBarHeightDp)
            .assertHeightIsAtLeast(appBarHeightDp + appBarContentPaddingTopDp)
        rule
            .onNodeWithTag(BoxTestTag)
            .assertHeightIsEqualTo(siblingBoxHeightDp)
            .assertTopPositionInRootIsEqualTo(
                appBarContentPaddingTopDp + appBarHeightDp + statusBarHeightDp
            )
    }

    @Composable
    private fun InputField(
        searchBarState: SearchBarState,
        textFieldState: TextFieldState,
        modifier: Modifier = Modifier,
        onSearch: (String) -> Unit = {},
        trailingIcon: (@Composable () -> Unit)? = null,
    ) {
        SearchBarDefaults.InputField(
            searchBarState = searchBarState,
            textFieldState = textFieldState,
            onSearch = onSearch,
            modifier = modifier,
            placeholder = { Text("Search") },
            trailingIcon = trailingIcon,
        )
    }

    @Composable
    private fun SearchBarWithScrollableContent(
        searchBarScrollBehavior: SearchBarScrollBehavior =
            SearchBarDefaults.enterAlwaysSearchBarScrollBehavior(),
        reverseLayout: Boolean = false,
    ) {
        val textFieldState = rememberTextFieldState()
        val searchBarState = rememberSearchBarState()
        val scope = rememberCoroutineScope()
        Scaffold(
            modifier = Modifier.nestedScroll(searchBarScrollBehavior.nestedScrollConnection),
            topBar = {
                AppBarWithSearch(
                    modifier = Modifier.testTag(SearchBarTestTag),
                    scrollBehavior = searchBarScrollBehavior,
                    state = searchBarState,
                    inputField = {
                        InputField(
                            searchBarState = searchBarState,
                            textFieldState = textFieldState,
                            onSearch = { scope.launch { searchBarState.animateToCollapsed() } },
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Account",
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.testTag(ScrollableContentTestTag),
                contentPadding = innerPadding,
                reverseLayout = reverseLayout,
            ) {
                items(100) {
                    Box(modifier = Modifier.fillMaxWidth().height(60.dp)) {
                        Text(text = it.toString())
                    }
                }
            }
        }
    }
}
