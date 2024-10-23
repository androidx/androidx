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

import android.view.KeyEvent
import android.widget.FrameLayout
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeNotNull
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3Api::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class ExposedDropdownMenuTest {

    @get:Rule val rule = createComposeRule()

    private val TFTag = "TextFieldTag"
    private val TrailingIconTag = "TrailingIconTag"
    private val EDMTag = "ExposedDropdownMenuTag"
    private val MenuItemTag = "MenuItemTag"
    private val OptionName = "Option 1"

    @Test
    fun edm_expandsOnClick_andCollapsesOnClickOutside() {
        var textFieldBounds = Rect.Zero
        rule.setMaterialContent(lightColorScheme()) {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuForTest(
                expanded = expanded,
                onExpandChange = { expanded = it },
                textFieldModifier =
                    Modifier.onGloballyPositioned { textFieldBounds = it.boundsInRoot() }
            )
        }

        rule.onNodeWithTag(TFTag).assertIsDisplayed()
        rule.onNodeWithTag(EDMTag).assertDoesNotExist()

        // Click on the TextField
        rule.onNodeWithTag(TFTag).performClick()

        rule.onNodeWithTag(MenuItemTag).assertIsDisplayed()

        // Click outside EDM
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            .click(
                (textFieldBounds.right + 1).toInt(),
                (textFieldBounds.bottom + 1).toInt(),
            )

        rule.onNodeWithTag(MenuItemTag).assertDoesNotExist()
    }

    @Test
    fun edm_collapsesOnTextFieldClick() {
        rule.setMaterialContent(lightColorScheme()) {
            var expanded by remember { mutableStateOf(true) }
            ExposedDropdownMenuForTest(expanded = expanded, onExpandChange = { expanded = it })
        }

        rule.onNodeWithTag(TFTag).assertIsDisplayed()
        rule.onNodeWithTag(EDMTag).assertIsDisplayed()
        rule.onNodeWithTag(MenuItemTag).assertIsDisplayed()

        // Click on the TextField
        rule.onNodeWithTag(TFTag).performClick()

        rule.onNodeWithTag(MenuItemTag).assertDoesNotExist()
    }

    @Test
    fun edm_notEditable_collapsesOnBackPress() {
        rule.setMaterialContent(lightColorScheme()) {
            var expanded by remember { mutableStateOf(true) }
            ExposedDropdownMenuForTest(
                expanded = expanded,
                onExpandChange = { expanded = it },
                editable = false,
            )
        }

        rule.onNodeWithTag(TFTag).assertIsDisplayed()
        rule.onNodeWithTag(EDMTag).assertIsDisplayed()
        rule.onNodeWithTag(MenuItemTag).assertIsDisplayed()

        rule.waitForIdle()
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.pressBack()

        rule.onNodeWithTag(TFTag).assertIsDisplayed()
        rule.onNodeWithTag(MenuItemTag).assertDoesNotExist()
    }

    @Test
    fun edm_editable_collapsesOnBackPress() {
        rule.setMaterialContent(lightColorScheme()) {
            var expanded by remember { mutableStateOf(true) }
            ExposedDropdownMenuForTest(
                expanded = expanded,
                onExpandChange = { expanded = it },
                editable = true,
            )
        }

        rule.onNodeWithTag(TFTag).assertIsDisplayed()
        rule.onNodeWithTag(EDMTag).assertIsDisplayed()
        rule.onNodeWithTag(MenuItemTag).assertIsDisplayed()

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        // First back closes keyboard
        device.pressBack()
        // Second back closes menu
        device.pressBack()

        rule.onNodeWithTag(TFTag).assertIsDisplayed()
        rule.onNodeWithTag(MenuItemTag).assertDoesNotExist()
    }

    @Test
    fun edm_notEditable_collapsesOnBackDispatch() {
        lateinit var backDispatcher: OnBackPressedDispatcher
        rule.setMaterialContent(lightColorScheme()) {
            backDispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            var expanded by remember { mutableStateOf(true) }
            ExposedDropdownMenuForTest(
                expanded = expanded,
                onExpandChange = { expanded = it },
                editable = false,
            )
        }

        rule.onNodeWithTag(TFTag).assertIsDisplayed()
        rule.onNodeWithTag(EDMTag).assertIsDisplayed()
        rule.onNodeWithTag(MenuItemTag).assertIsDisplayed()

        rule.runOnIdle { backDispatcher.onBackPressed() }

        rule.onNodeWithTag(TFTag).assertIsDisplayed()
        rule.onNodeWithTag(MenuItemTag).assertDoesNotExist()
    }

    @Test
    fun edm_editable_collapsesOnBackDispatch() {
        lateinit var backDispatcher: OnBackPressedDispatcher
        rule.setMaterialContent(lightColorScheme()) {
            backDispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            var expanded by remember { mutableStateOf(true) }
            ExposedDropdownMenuForTest(
                expanded = expanded,
                onExpandChange = { expanded = it },
                editable = true,
            )
        }

        rule.onNodeWithTag(TFTag).assertIsDisplayed()
        rule.onNodeWithTag(EDMTag).assertIsDisplayed()
        rule.onNodeWithTag(MenuItemTag).assertIsDisplayed()

        rule.runOnIdle { backDispatcher.onBackPressed() }

        rule.onNodeWithTag(TFTag).assertIsDisplayed()
        rule.onNodeWithTag(MenuItemTag).assertDoesNotExist()
    }

    @Test
    fun edm_notEditable_collapsesOnEscapePress() {
        rule.setMaterialContent(lightColorScheme()) {
            var expanded by remember { mutableStateOf(true) }
            ExposedDropdownMenuForTest(
                expanded = expanded,
                onExpandChange = { expanded = it },
                editable = false,
            )
        }

        rule.onNodeWithTag(TFTag).assertIsDisplayed()
        rule.onNodeWithTag(EDMTag).assertIsDisplayed()
        rule.onNodeWithTag(MenuItemTag).assertIsDisplayed()

        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            .pressKeyCode(KeyEvent.KEYCODE_ESCAPE)

        rule.onNodeWithTag(TFTag).assertIsDisplayed()
        rule.onNodeWithTag(MenuItemTag).assertDoesNotExist()
    }

    @Ignore("b/374850853")
    @Test
    fun edm_editable_collapsesOnEscapePress() {
        rule.setMaterialContent(lightColorScheme()) {
            var expanded by remember { mutableStateOf(true) }
            ExposedDropdownMenuForTest(
                expanded = expanded,
                onExpandChange = { expanded = it },
                editable = true,
            )
        }

        rule.onNodeWithTag(TFTag).assertIsDisplayed()
        rule.onNodeWithTag(EDMTag).assertIsDisplayed()
        rule.onNodeWithTag(MenuItemTag).assertIsDisplayed()

        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            .pressKeyCode(KeyEvent.KEYCODE_ESCAPE)

        rule.onNodeWithTag(TFTag).assertIsDisplayed()
        rule.onNodeWithTag(MenuItemTag).assertDoesNotExist()
    }

    @Test
    fun edm_notEditable_doesNotExpand_whenDisabled() {
        rule.setMaterialContent(lightColorScheme()) {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuForTest(
                expanded = expanded,
                onExpandChange = { expanded = it },
                editable = false,
                enabled = false,
            )
        }

        rule.onNodeWithTag(TFTag).assertIsDisplayed()
        rule.onNodeWithTag(EDMTag).assertDoesNotExist()

        // Click on the TextField
        rule.onNodeWithTag(TFTag).performClick()

        // Menu still is not displayed
        rule.onNodeWithTag(EDMTag).assertDoesNotExist()
    }

    @Test
    fun edm_editable_doesNotExpand_whenDisabled() {
        rule.setMaterialContent(lightColorScheme()) {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuForTest(
                expanded = expanded,
                onExpandChange = { expanded = it },
                editable = true,
                enabled = false,
            )
        }

        rule.onNodeWithTag(TFTag).assertIsDisplayed()
        rule.onNodeWithTag(EDMTag).assertDoesNotExist()

        // Click on the TextField
        rule.onNodeWithTag(TFTag).performClick()

        // Menu still is not displayed
        rule.onNodeWithTag(EDMTag).assertDoesNotExist()
    }

    @Test
    fun edm_doesNotCollapse_whenTypingOnSoftKeyboard() {
        rule.setMaterialContent(lightColorScheme()) {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuForTest(
                expanded = expanded,
                onExpandChange = { expanded = it },
                editable = true,
            )
        }

        rule.onNodeWithTag(TFTag).performClick()

        rule.onNodeWithTag(TFTag).assertIsDisplayed()
        rule.onNodeWithTag(TFTag).assertIsFocused()
        rule.onNodeWithTag(EDMTag).assertIsDisplayed()
        rule.onNodeWithTag(MenuItemTag).assertIsDisplayed()

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val zKey = device.findObject(By.desc("z")) ?: device.findObject(By.text("z"))
        // Only run the test if we can find a key to type, which might fail for any number of
        // reasons (keyboard doesn't appear, unexpected locale, etc.)
        assumeNotNull(zKey)

        repeat(3) {
            zKey.click()
            rule.waitForIdle()
        }

        val matcher = hasText("zzz")
        rule.waitUntil { matcher.matches(rule.onNodeWithTag(TFTag).fetchSemanticsNode()) }
        rule.onNodeWithTag(MenuItemTag).assertIsDisplayed()
    }

    @Test
    fun edm_expandsAndFocusesTextField_whenTrailingIconClicked() {
        rule.setMaterialContent(lightColorScheme()) {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuForTest(
                expanded = expanded,
                onExpandChange = { expanded = it },
            )
        }

        rule.onNodeWithTag(TFTag).assertIsDisplayed()
        rule.onNodeWithTag(TrailingIconTag, useUnmergedTree = true).assertIsDisplayed()

        // Click on the Trailing Icon
        rule.onNodeWithTag(TrailingIconTag, useUnmergedTree = true).performClick()

        rule.onNodeWithTag(TFTag).assertIsFocused()
        rule.onNodeWithTag(MenuItemTag).assertIsDisplayed()
    }

    @Test
    fun edm_doesNotExpand_ifTouchEndsOutsideBounds() {
        var textFieldBounds = Rect.Zero
        rule.setMaterialContent(lightColorScheme()) {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuForTest(
                expanded = expanded,
                onExpandChange = { expanded = it },
                textFieldModifier =
                    Modifier.onGloballyPositioned { textFieldBounds = it.boundsInRoot() }
            )
        }

        rule.onNodeWithTag(TFTag).assertIsDisplayed()
        rule.onNodeWithTag(EDMTag).assertDoesNotExist()

        // A swipe that ends outside the bounds of the anchor should not expand the menu.
        rule.onNodeWithTag(TFTag).performTouchInput {
            swipe(
                start = this.center,
                end = Offset(this.centerX, this.centerY + (textFieldBounds.height / 2) + 1),
                durationMillis = 100
            )
        }
        rule.onNodeWithTag(MenuItemTag).assertDoesNotExist()
    }

    @Test
    fun edm_doesNotExpand_ifTouchIsPartOfScroll() {
        val testIndex = 2
        var textFieldSize = IntSize.Zero
        rule.setMaterialContent(lightColorScheme()) {
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
                            colors = ExposedDropdownMenuDefaults.textFieldColors()
                        )
                        ExposedDropdownMenu(
                            modifier =
                                if (index == testIndex) {
                                    Modifier.testTag(EDMTag)
                                } else {
                                    Modifier
                                },
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
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

        rule.onNodeWithTag(TFTag).assertIsDisplayed()
        rule.onNodeWithTag(EDMTag).assertDoesNotExist()

        // A swipe that causes a scroll should not expand the menu, even if it remains within the
        // bounds of the anchor.
        rule.onNodeWithTag(TFTag).performTouchInput {
            swipe(
                start = this.center,
                end = Offset(this.centerX, this.centerY - (textFieldSize.height / 2) + 1),
                durationMillis = 100
            )
        }
        rule.onNodeWithTag(MenuItemTag).assertDoesNotExist()
    }

    @Test
    fun edm_doesNotRecomposeOnScroll() {
        var compositionCount = 0
        lateinit var scrollState: ScrollState
        lateinit var scope: CoroutineScope
        rule.setMaterialContent(lightColorScheme()) {
            scrollState = rememberScrollState()
            scope = rememberCoroutineScope()
            Column(Modifier.verticalScroll(scrollState)) {
                Spacer(Modifier.height(300.dp))

                val expanded = false
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = {},
                ) {
                    TextField(
                        modifier =
                            Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        readOnly = true,
                        state = rememberTextFieldState(),
                        label = { Text("Label") },
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = {},
                        content = {},
                    )
                    SideEffect { compositionCount++ }
                }

                Spacer(Modifier.height(300.dp))
            }
        }

        assertThat(compositionCount).isEqualTo(1)

        rule.runOnIdle { scope.launch { scrollState.animateScrollBy(500f) } }
        rule.waitForIdle()

        assertThat(compositionCount).isEqualTo(1)
    }

    @Test
    fun edm_anchorTypeIsUpdated_evenIfTextFieldIsNotClicked() {
        var expanded by mutableStateOf(false)
        var type: ExposedDropdownMenuAnchorType? = null
        rule.setMaterialContent(lightColorScheme()) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
            ) {
                TextField(
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                    state = rememberTextFieldState(),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    label = { Text("Label") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = expanded,
                            modifier =
                                Modifier.menuAnchor(
                                    ExposedDropdownMenuAnchorType.SecondaryEditable
                                ),
                        )
                    }
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(OptionName) },
                        onClick = {},
                    )
                }
                SideEffect { type = anchorType }
            }
        }
        rule.runOnIdle { expanded = true }
        assertThat(type).isEqualTo(ExposedDropdownMenuAnchorType.PrimaryEditable)
    }

    @Test
    fun edm_widthMatchesTextFieldWidth() {
        var textFieldBounds by mutableStateOf(Rect.Zero)
        var menuBounds by mutableStateOf(Rect.Zero)
        rule.setMaterialContent(lightColorScheme()) {
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
                menuModifier = Modifier.onGloballyPositioned { menuBounds = it.boundsInRoot() }
            )
        }

        rule.onNodeWithTag(TFTag).assertIsDisplayed()
        rule.onNodeWithTag(MenuItemTag).assertIsDisplayed()

        rule.runOnIdle { assertThat(menuBounds.width).isEqualTo(textFieldBounds.width) }
    }

    @Test
    fun edm_collapsesWithSelection_whenMenuItemClicked() {
        rule.setMaterialContent(lightColorScheme()) {
            var expanded by remember { mutableStateOf(true) }
            ExposedDropdownMenuForTest(expanded = expanded, onExpandChange = { expanded = it })
        }

        rule.onNodeWithTag(TFTag).assertIsDisplayed()
        rule.onNodeWithTag(MenuItemTag).assertIsDisplayed()

        // Choose the option
        rule.onNodeWithTag(MenuItemTag).performClick()

        // Menu should collapse
        rule.onNodeWithTag(MenuItemTag).assertDoesNotExist()
        rule.onNodeWithTag(TFTag).assertTextContains(OptionName)
    }

    @Test
    fun edm_resizesWithinWindowBounds_uponImeAppearance() {
        var actualMenuSize: IntSize? = null
        var density: Density? = null
        val itemSize = 50.dp
        val itemCount = 10

        rule.setMaterialContent(lightColorScheme()) {
            density = LocalDensity.current
            Column(Modifier.fillMaxSize()) {
                // Push the EDM down so opening the keyboard causes a pan/scroll
                Spacer(Modifier.weight(1f))

                ExposedDropdownMenuBox(expanded = true, onExpandedChange = {}) {
                    TextField(
                        modifier =
                            Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                        state = rememberTextFieldState(),
                        label = { Text("Label") },
                    )
                    ExposedDropdownMenu(
                        expanded = true,
                        onDismissRequest = {},
                        modifier = Modifier.onGloballyPositioned { actualMenuSize = it.size }
                    ) {
                        repeat(itemCount) { Box(Modifier.size(itemSize)) }
                    }
                }
            }
        }

        // This would fit on screen if the keyboard wasn't displayed.
        val menuPreferredHeight =
            with(density!!) { (itemSize * itemCount + DropdownMenuVerticalPadding * 2).roundToPx() }
        // But the keyboard *is* displayed, forcing the actual size to be smaller.
        assertThat(actualMenuSize!!.height).isLessThan(menuPreferredHeight)
    }

    @Test
    fun edm_doesNotCrash_whenAnchorDetachedFirst() {
        var parent: FrameLayout? = null
        rule.setMaterialContent(lightColorScheme()) {
            AndroidView(
                factory = { context ->
                    FrameLayout(context)
                        .apply {
                            addView(
                                ComposeView(context).apply {
                                    setContent {
                                        ExposedDropdownMenuBox(
                                            expanded = true,
                                            onExpandedChange = {}
                                        ) {
                                            TextField(
                                                state = rememberTextFieldState("Text"),
                                                modifier =
                                                    Modifier.menuAnchor(
                                                        ExposedDropdownMenuAnchorType
                                                            .PrimaryEditable
                                                    ),
                                            )
                                            ExposedDropdownMenu(
                                                expanded = true,
                                                onDismissRequest = {},
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text(OptionName) },
                                                    onClick = {},
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        .also { parent = it }
                }
            )
        }

        rule.runOnIdle { parent!!.removeAllViews() }

        rule.waitForIdle()

        // Should not have crashed.
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun edm_withScrolledContent() {
        lateinit var scrollState: ScrollState
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.fillMaxSize()) {
                ExposedDropdownMenuBox(
                    modifier = Modifier.align(Alignment.Center),
                    expanded = true,
                    onExpandedChange = {}
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
                        scrollState = scrollState
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

        rule.runOnIdle { runBlocking { scrollState.scrollTo(scrollState.maxValue) } }

        rule.waitForIdle()

        rule.onNodeWithTag("MenuContent 1").assertIsNotDisplayed()
        rule.onNodeWithTag("MenuContent 100").assertIsDisplayed()
    }

    @Test
    fun edm_hasDropdownSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            ExposedDropdownMenuForTest(
                expanded = false,
                onExpandChange = {},
            )
        }

        rule
            .onNodeWithTag(TFTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.DropdownList))
    }

    @Test
    fun edm_positionProvider() {
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
                    anchorBounds =
                        IntRect(
                            size = anchorSize,
                            offset = IntOffset(0, 0),
                        ),
                    windowSize = windowSize,
                    popupContentSize = popupSize,
                    layoutDirection = layoutDirection,
                )
            )
            .isEqualTo(IntOffset(0, anchorSize.height))

        // off-screen (above)
        assertThat(
                edmPositionProvider.calculatePosition(
                    anchorBounds =
                        IntRect(
                            size = anchorSize,
                            offset = IntOffset(0, -150),
                        ),
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
                        IntRect(
                            size = anchorSize,
                            offset = IntOffset(0, windowSize.height + 100),
                        ),
                    windowSize = windowSize,
                    popupContentSize = popupSize,
                    layoutDirection = layoutDirection,
                )
            )
            .isEqualTo(
                IntOffset(
                    0,
                    windowSize.height + topWindowInsets - verticalMargin - popupSize.height
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
                    colors = ExposedDropdownMenuDefaults.textFieldColors()
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
                        modifier = Modifier.testTag(MenuItemTag)
                    )
                }
            }
        }
    }
}
