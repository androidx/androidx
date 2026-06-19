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

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTestApi::class)
class ExposedDropdownMenuTest {

    private val TFTag = "TextFieldTag"
    private val TrailingIconTag = "TrailingIconTag"
    private val EDMTag = "ExposedDropdownMenuTag"
    private val MenuItemTag = "MenuItemTag"
    private val OptionName = "Option 1"

    @Test
    fun edm_expandsOnClick_andCollapsesOnClickOutside() = runComposeUiTest {
        var textFieldBounds = Rect.Zero
        setMaterialContent(lightColorScheme()) {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuForTest(
                expanded = expanded,
                onExpandChange = { expanded = it },
                textFieldModifier =
                    Modifier.onGloballyPositioned { textFieldBounds = it.boundsInRoot() },
            )
        }

        onNodeWithTag(TFTag).assertIsDisplayed()
        onNodeWithTag(EDMTag).assertDoesNotExist()

        // Click on the TextField
        onNodeWithTag(TFTag).performClick()

        onNodeWithTag(MenuItemTag).assertIsDisplayed()

        // Click outside EDM
        onAllNodes(isRoot()).onFirst().apply {
            performMouseInput {
                click(Offset(textFieldBounds.right + 1f, textFieldBounds.bottom + 1f))
            }
        }

        onNodeWithTag(MenuItemTag).assertDoesNotExist()
    }

    @Test
    fun edm_collapsesOnTextFieldClick() = runComposeUiTest {
        setMaterialContent(lightColorScheme()) {
            var expanded by remember { mutableStateOf(true) }
            ExposedDropdownMenuForTest(expanded = expanded, onExpandChange = { expanded = it })
        }

        onNodeWithTag(TFTag).assertIsDisplayed()
        onNodeWithTag(EDMTag).assertIsDisplayed()
        onNodeWithTag(MenuItemTag).assertIsDisplayed()

        // Click on the TextField
        onNodeWithTag(TFTag).performClick()

        onNodeWithTag(MenuItemTag).assertDoesNotExist()
    }

    @Test
    fun edm_notEditable_doesNotExpand_whenDisabled() = runComposeUiTest {
        setMaterialContent(lightColorScheme()) {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuForTest(
                expanded = expanded,
                onExpandChange = { expanded = it },
                editable = false,
                enabled = false,
            )
        }

        onNodeWithTag(TFTag).assertIsDisplayed()
        onNodeWithTag(EDMTag).assertDoesNotExist()

        // Click on the TextField
        onNodeWithTag(TFTag).performClick()

        // Menu still is not displayed
        onNodeWithTag(EDMTag).assertDoesNotExist()
    }

    @Test
    fun edm_editable_doesNotExpand_whenDisabled() = runComposeUiTest {
        setMaterialContent(lightColorScheme()) {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuForTest(
                expanded = expanded,
                onExpandChange = { expanded = it },
                editable = true,
                enabled = false,
            )
        }

        onNodeWithTag(TFTag).assertIsDisplayed()
        onNodeWithTag(EDMTag).assertDoesNotExist()

        // Click on the TextField
        onNodeWithTag(TFTag).performClick()

        // Menu still is not displayed
        onNodeWithTag(EDMTag).assertDoesNotExist()
    }

    @Test
    fun edm_expandsAndFocusesTextField_whenTrailingIconClicked() = runComposeUiTest {
        setMaterialContent(lightColorScheme()) {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuForTest(expanded = expanded, onExpandChange = { expanded = it })
        }

        onNodeWithTag(TFTag).assertIsDisplayed()
        onNodeWithTag(TrailingIconTag, useUnmergedTree = true).assertIsDisplayed()

        // Click on the Trailing Icon
        onNodeWithTag(TrailingIconTag, useUnmergedTree = true).performClick()

        onNodeWithTag(TFTag).assertIsFocused()
        onNodeWithTag(MenuItemTag).assertIsDisplayed()
    }

    @Test
    fun edm_doesNotExpand_ifTouchEndsOutsideBounds() = runComposeUiTest {
        var textFieldBounds = Rect.Zero
        setMaterialContent(lightColorScheme()) {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuForTest(
                expanded = expanded,
                onExpandChange = { expanded = it },
                textFieldModifier =
                    Modifier.onGloballyPositioned { textFieldBounds = it.boundsInRoot() },
            )
        }

        onNodeWithTag(TFTag).assertIsDisplayed()
        onNodeWithTag(EDMTag).assertDoesNotExist()

        // A swipe that ends outside the bounds of the anchor should not expand the menu.
        onNodeWithTag(TFTag).performTouchInput {
            swipe(
                start = this.center,
                end = Offset(this.centerX, this.centerY + (textFieldBounds.height / 2) + 1),
                durationMillis = 100,
            )
        }
        onNodeWithTag(MenuItemTag).assertDoesNotExist()
    }

    @Test
    fun edm_doesNotExpand_ifTouchIsPartOfScroll() = runComposeUiTest {
        val testIndex = 2
        var textFieldSize = IntSize.Zero
        setMaterialContent(lightColorScheme()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(50) { index ->
                    var expanded by remember { mutableStateOf(false) }
                    val textFieldState = rememberTextFieldState()

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        TextField(
                            modifier =
                                Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                                    .then(
                                        if (index == testIndex)
                                            Modifier.testTag(TFTag).onSizeChanged {
                                                textFieldSize = it
                                            }
                                        else {
                                            Modifier
                                        }
                                    ),
                            state = textFieldState,
                            label = { Text("Label") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        )
                        ExposedDropdownMenu(
                            modifier =
                                if (index == testIndex) {
                                    Modifier.testTag(EDMTag)
                                } else {
                                    Modifier
                                },
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(OptionName) },
                                onClick = {
                                    textFieldState.setTextAndPlaceCursorAtEnd(OptionName)
                                    expanded = false
                                },
                                modifier =
                                    if (index == testIndex) {
                                        Modifier.testTag(MenuItemTag)
                                    } else {
                                        Modifier
                                    },
                            )
                        }
                    }
                }
            }
        }

        onNodeWithTag(TFTag).assertIsDisplayed()
        onNodeWithTag(EDMTag).assertDoesNotExist()

        // A swipe that causes a scroll should not expand the menu, even if it remains within the
        // bounds of the anchor.
        onNodeWithTag(TFTag).performTouchInput {
            swipe(
                start = this.center,
                end = Offset(this.centerX, this.centerY - (textFieldSize.height / 2) + 1),
                durationMillis = 100,
            )
        }
        onNodeWithTag(MenuItemTag).assertDoesNotExist()
    }

    @Test
    fun edm_expands_onEnterKey() = runComposeUiTest {
        var expanded by mutableStateOf(false)

        setMaterialContent(lightColorScheme()) {
            ExposedDropdownMenuForTest(
                expanded = expanded,
                onExpandChange = { expanded = it },
                textFieldModifier = Modifier.testTag("TextField"),
            )
        }

        onNodeWithTag("TextField").requestFocus().performKeyInput { pressKey(Key.Enter) }

        runOnIdle { assertTrue(expanded) }
    }

    @Test
    @Ignore // TODO Fails on Desktop
    fun edm_collapses_onEnterKey() = runComposeUiTest {
        var expanded by mutableStateOf(true)

        setMaterialContent(lightColorScheme()) {
            ExposedDropdownMenuForTest(
                expanded = expanded,
                onExpandChange = { expanded = it },
                textFieldModifier = Modifier.testTag("TextField"),
            )
        }

        onNodeWithTag("TextField").requestFocus().performKeyInput { pressKey(Key.Enter) }

        runOnIdle { assertFalse(expanded) }
    }

    @Test
    fun edm_editable_expands_onEnterKey() = runComposeUiTest {
        var expanded by mutableStateOf(false)

        setMaterialContent(lightColorScheme()) {
            ExposedDropdownMenuForTest(
                editable = true,
                expanded = expanded,
                onExpandChange = { expanded = it },
                textFieldModifier = Modifier.testTag("TextField"),
            )
        }

        onNodeWithTag("TextField").requestFocus().performKeyInput { pressKey(Key.Enter) }

        runOnIdle { assertTrue(expanded) }
    }

    @Test
    fun edm_editable_collapses_onEnterKey() = runComposeUiTest {
        var expanded by mutableStateOf(true)

        setMaterialContent(lightColorScheme()) {
            ExposedDropdownMenuForTest(
                editable = true,
                expanded = expanded,
                onExpandChange = { expanded = it },
                textFieldModifier = Modifier.testTag("TextField"),
            )
        }

        onNodeWithTag("TextField").requestFocus().performKeyInput { pressKey(Key.Enter) }

        runOnIdle { assertFalse(expanded) }
    }

    @Test
    fun edm_editable_doesNotExpand_onSpacebarKey() = runComposeUiTest {
        var expanded by mutableStateOf(false)

        setMaterialContent(lightColorScheme()) {
            ExposedDropdownMenuForTest(
                editable = true,
                expanded = expanded,
                onExpandChange = { expanded = it },
                textFieldModifier = Modifier.testTag("TextField"),
            )
        }

        onNodeWithTag("TextField").requestFocus().performKeyInput { pressKey(Key.Spacebar) }

        runOnIdle { assertFalse(expanded) }
    }

    @Test
    fun edm_editable_doesNotCollapse_onSpacebarKey() = runComposeUiTest {
        var expanded by mutableStateOf(true)

        setMaterialContent(lightColorScheme()) {
            ExposedDropdownMenuForTest(
                editable = true,
                expanded = expanded,
                onExpandChange = { expanded = it },
                textFieldModifier = Modifier.testTag("TextField"),
            )
        }

        onNodeWithTag("TextField").requestFocus().performKeyInput { pressKey(Key.Spacebar) }

        runOnIdle { assertTrue(expanded) }
    }

    @Test
    fun edm_doesNotRecomposeOnScroll() = runComposeUiTest {
        var compositionCount = 0
        lateinit var scrollState: ScrollState
        lateinit var scope: CoroutineScope
        setMaterialContent(lightColorScheme()) {
            scrollState = rememberScrollState()
            scope = rememberCoroutineScope()
            Column(Modifier.verticalScroll(scrollState)) {
                Spacer(Modifier.height(300.dp))

                val expanded = false
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = {}) {
                    TextField(
                        modifier =
                            Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        readOnly = true,
                        state = rememberTextFieldState(),
                        label = { Text("Label") },
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = {}, content = {})
                    SideEffect { compositionCount++ }
                }

                Spacer(Modifier.height(300.dp))
            }
        }

        assertThat(compositionCount).isEqualTo(1)

        runOnIdle { scope.launch { scrollState.animateScrollBy(500f) } }
        waitForIdle()

        assertThat(compositionCount).isEqualTo(1)
    }

    @Test
    fun edm_anchorTypeIsUpdated_evenIfTextFieldIsNotClicked() = runComposeUiTest {
        var expanded by mutableStateOf(false)
        var type: ExposedDropdownMenuAnchorType? = null
        setMaterialContent(lightColorScheme()) {
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                TextField(
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                    state = rememberTextFieldState(),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    label = { Text("Label") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = expanded,
                            modifier =
                                Modifier.menuAnchor(ExposedDropdownMenuAnchorType.SecondaryEditable),
                        )
                    },
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text(OptionName) }, onClick = {})
                }
                SideEffect { type = anchorType }
            }
        }
        runOnIdle { expanded = true }
        assertThat(type).isEqualTo(ExposedDropdownMenuAnchorType.PrimaryEditable)
    }

    @Test
    fun edm_widthMatchesTextFieldWidth() = runComposeUiTest {
        var textFieldBounds by mutableStateOf(Rect.Zero)
        var menuBounds by mutableStateOf(Rect.Zero)
        setMaterialContent(lightColorScheme()) {
            var expanded by remember { mutableStateOf(true) }
            ExposedDropdownMenuForTest(
                expanded = expanded,
                onExpandChange = { expanded = it },
                textFieldModifier =
                    Modifier
                        // Make text field nearly full screen width to test that
                        // menu is not limited by the default system popup width
                        .fillMaxWidth(fraction = 0.98f)
                        .onGloballyPositioned { textFieldBounds = it.boundsInRoot() },
                menuModifier = Modifier.onGloballyPositioned { menuBounds = it.boundsInRoot() },
            )
        }

        onNodeWithTag(TFTag).assertIsDisplayed()
        onNodeWithTag(MenuItemTag).assertIsDisplayed()

        runOnIdle { assertThat(menuBounds.width).isEqualTo(textFieldBounds.width) }
    }

    @Test
    fun edm_collapsesWithSelection_whenMenuItemClicked() = runComposeUiTest {
        setMaterialContent(lightColorScheme()) {
            var expanded by remember { mutableStateOf(true) }
            ExposedDropdownMenuForTest(expanded = expanded, onExpandChange = { expanded = it })
        }

        onNodeWithTag(TFTag).assertIsDisplayed()
        onNodeWithTag(MenuItemTag).assertIsDisplayed()

        // Choose the option
        onNodeWithTag(MenuItemTag).performClick()

        // Menu should collapse
        onNodeWithTag(MenuItemTag).assertDoesNotExist()
        onNodeWithTag(TFTag).assertTextContains(OptionName)
    }

    @Test
    fun edm_withScrolledContent() = runComposeUiTest {
        lateinit var scrollState: ScrollState
        setMaterialContent(lightColorScheme()) {
            Box(Modifier.fillMaxSize()) {
                ExposedDropdownMenuBox(
                    modifier = Modifier.align(Alignment.Center),
                    expanded = true,
                    onExpandedChange = {},
                ) {
                    scrollState = rememberScrollState()
                    TextField(
                        modifier =
                            Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                        state = rememberTextFieldState(),
                        label = { Text("Label") },
                    )
                    ExposedDropdownMenu(
                        expanded = true,
                        onDismissRequest = {},
                        scrollState = scrollState,
                    ) {
                        repeat(100) {
                            Text(
                                text = "Text ${it + 1}",
                                modifier = Modifier.testTag("MenuContent ${it + 1}"),
                            )
                        }
                    }
                }
            }
        }

        CoroutineScope(Dispatchers.Unconfined).launch {
            scrollState.scrollTo(scrollState.maxValue)
        }

        onNodeWithTag("MenuContent 1").assertIsNotDisplayed()
        onNodeWithTag("MenuContent 100").assertIsDisplayed()
    }

    @Test
    fun edm_hasDropdownSemantics() = runComposeUiTest {
        setMaterialContent(lightColorScheme()) {
            ExposedDropdownMenuForTest(expanded = false, onExpandChange = {})
        }

        onNodeWithTag(TFTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.DropdownList))
    }

    @Test
    fun edm_positionProvider() = runComposeUiTest {
        val topWindowInsets = 50
        val density = Density(1f)
        val anchorSize = IntSize(width = 200, height = 100)
        val popupSize = IntSize(width = 200, height = 340)
        val windowSize = IntSize(width = 500, height = 500)
        val verticalMargin = with(density) { MenuVerticalMargin.roundToPx() }
        val layoutDirection = LayoutDirection.Ltr

        val edmPositionProvider =
            ExposedDropdownMenuPositionProvider(
                density = density,
                topWindowInsets = topWindowInsets,
            )

        // typical case
        assertThat(
            edmPositionProvider.calculatePosition(
                anchorBounds = IntRect(size = anchorSize, offset = IntOffset(0, 0)),
                windowSize = windowSize,
                popupContentSize = popupSize,
                layoutDirection = layoutDirection,
            )
        )
            .isEqualTo(IntOffset(0, anchorSize.height))

        // off-screen (above)
        assertThat(
            edmPositionProvider.calculatePosition(
                anchorBounds = IntRect(size = anchorSize, offset = IntOffset(0, -150)),
                windowSize = windowSize,
                popupContentSize = popupSize,
                layoutDirection = layoutDirection,
            )
        )
            .isEqualTo(IntOffset(0, verticalMargin))

        // interacting with window insets
        assertThat(
            edmPositionProvider.calculatePosition(
                anchorBounds =
                    IntRect(
                        size = anchorSize,
                        // If it weren't for topWindowInsets allowance,
                        // the menu would be considered "off-screen"
                        offset = IntOffset(0, 100),
                    ),
                windowSize = windowSize,
                popupContentSize = popupSize,
                layoutDirection = layoutDirection,
            )
        )
            .isEqualTo(IntOffset(0, 100 + anchorSize.height))

        // off-screen (below)
        assertThat(
            edmPositionProvider.calculatePosition(
                anchorBounds =
                    IntRect(size = anchorSize, offset = IntOffset(0, windowSize.height + 100)),
                windowSize = windowSize,
                popupContentSize = popupSize,
                layoutDirection = layoutDirection,
            )
        )
            .isEqualTo(
                IntOffset(
                    0,
                    windowSize.height + topWindowInsets - verticalMargin - popupSize.height,
                )
            )
    }

    @Composable
    fun ExposedDropdownMenuForTest(
        expanded: Boolean,
        onExpandChange: (Boolean) -> Unit,
        editable: Boolean = false,
        enabled: Boolean = true,
        textFieldModifier: Modifier = Modifier,
        menuModifier: Modifier = Modifier,
    ) {
        val textFieldState = rememberTextFieldState()
        Box(Modifier.fillMaxSize()) {
            ExposedDropdownMenuBox(
                modifier = Modifier.align(Alignment.Center),
                expanded = expanded,
                onExpandedChange = onExpandChange,
            ) {
                TextField(
                    modifier =
                        textFieldModifier
                            .menuAnchor(
                                type =
                                    if (editable) {
                                        ExposedDropdownMenuAnchorType.PrimaryEditable
                                    } else {
                                        ExposedDropdownMenuAnchorType.PrimaryNotEditable
                                    },
                                enabled = enabled,
                            )
                            .testTag(TFTag),
                    state = textFieldState,
                    lineLimits = TextFieldLineLimits.SingleLine,
                    readOnly = !editable,
                    label = { Text("Label") },
                    trailingIcon = {
                        Box(Modifier.testTag(TrailingIconTag)) {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        }
                    },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                )
                ExposedDropdownMenu(
                    modifier = menuModifier.testTag(EDMTag),
                    expanded = expanded,
                    onDismissRequest = { onExpandChange(false) },
                ) {
                    DropdownMenuItem(
                        text = { Text(OptionName) },
                        onClick = {
                            textFieldState.setTextAndPlaceCursorAtEnd(OptionName)
                            onExpandChange(false)
                        },
                        modifier = Modifier.testTag(MenuItemTag),
                    )
                }
            }
        }
    }
}
