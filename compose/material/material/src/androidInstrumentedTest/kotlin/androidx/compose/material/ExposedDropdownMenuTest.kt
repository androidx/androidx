/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.material

import android.view.KeyEvent
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertThat
import org.junit.Assume
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterialApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class ExposedDropdownMenuTest {

    @get:Rule val rule = createComposeRule()

    private val EDMBoxTag = "ExposedDropdownMenuBoxTag"
    private val TFTag = "TextFieldTag"
    private val TrailingIconTag = "TrailingIconTag"
    private val EDMTag = "ExposedDropdownMenuTag"
    private val MenuItemTag = "MenuItemTag"
    private val OptionName = "Option 1"

    @Test
    fun expandedBehaviour_expandsOnClickAndCollapsesOnOutside() {
        var textFieldBounds = Rect.Zero
        rule.setMaterialContent {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuForTest(
                expanded = expanded,
                onExpandChange = { expanded = it },
                onTextFieldBoundsChanged = { textFieldBounds = it }
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
    fun expandedBehaviour_dismissesOnBackPress() {
        rule.setMaterialContent {
            var expanded by remember { mutableStateOf(true) }
            ExposedDropdownMenuForTest(
                expanded = expanded,
                onExpandChange = { expanded = it },
            )
        }

        rule.onNodeWithTag(TFTag).assertIsDisplayed()
        rule.onNodeWithTag(EDMTag).assertIsDisplayed()
        rule.onNodeWithTag(MenuItemTag).assertIsDisplayed()

        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()

        rule.onNodeWithTag(TFTag).assertIsDisplayed()
        rule.onNodeWithTag(EDMTag).assertDoesNotExist()
        rule.onNodeWithTag(MenuItemTag).assertDoesNotExist()
    }

    @Test
    fun expandedBehaviour_dismissesOnEscapePress() {
        rule.setMaterialContent {
            var expanded by remember { mutableStateOf(true) }
            ExposedDropdownMenuForTest(
                expanded = expanded,
                onExpandChange = { expanded = it },
            )
        }

        rule.onNodeWithTag(TFTag).assertIsDisplayed()
        rule.onNodeWithTag(EDMTag).assertIsDisplayed()
        rule.onNodeWithTag(MenuItemTag).assertIsDisplayed()

        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            .pressKeyCode(KeyEvent.KEYCODE_ESCAPE)

        rule.onNodeWithTag(TFTag).assertIsDisplayed()
        rule.onNodeWithTag(EDMTag).assertDoesNotExist()
        rule.onNodeWithTag(MenuItemTag).assertDoesNotExist()
    }

    @Test
    fun expandedBehaviour_collapseOnTextFieldClick() {
        rule.setMaterialContent {
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
    fun edm_doesNotCollapse_whenTypingOnSoftKeyboard() {
        rule.setMaterialContent {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuForTest(expanded = expanded, onExpandChange = { expanded = it })
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
        Assume.assumeNotNull(zKey)

        repeat(3) {
            zKey.click()
            rule.waitForIdle()
        }

        val matcher = hasText("zzz")
        rule.waitUntil { matcher.matches(rule.onNodeWithTag(TFTag).fetchSemanticsNode()) }
        rule.onNodeWithTag(MenuItemTag).assertIsDisplayed()
    }

    @Test
    fun expandedBehaviour_expandsAndFocusesTextFieldOnTrailingIconClick() {
        rule.setMaterialContent {
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
    fun expandedBehaviour_doesNotExpandIfTouchEndsOutsideBounds() {
        var textFieldBounds = Rect.Zero
        rule.setMaterialContent {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuForTest(
                expanded = expanded,
                onExpandChange = { expanded = it },
                onTextFieldBoundsChanged = { textFieldBounds = it }
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
    fun expandedBehaviour_doesNotExpandIfTouchIsPartOfScroll() {
        val testIndex = 2
        var textFieldSize = IntSize.Zero
        rule.setMaterialContent() {
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
                                Modifier.then(
                                    if (index == testIndex)
                                        Modifier.testTag(TFTag).onSizeChanged { textFieldSize = it }
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
                                onClick = {
                                    textFieldState.setTextAndPlaceCursorAtEnd(OptionName)
                                    expanded = false
                                },
                                modifier =
                                    if (index == testIndex) {
                                        Modifier.testTag(MenuItemTag)
                                    } else {
                                        Modifier
                                    }
                            ) {
                                Text(OptionName)
                            }
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
    fun uiProperties_menuMatchesTextWidth() {
        var textFieldBounds by mutableStateOf(Rect.Zero)
        var menuBounds by mutableStateOf(Rect.Zero)
        rule.setMaterialContent {
            var expanded by remember { mutableStateOf(true) }
            ExposedDropdownMenuForTest(
                expanded = expanded,
                onExpandChange = { expanded = it },
                onTextFieldBoundsChanged = { textFieldBounds = it },
                onMenuBoundsChanged = { menuBounds = it }
            )
        }

        rule.onNodeWithTag(TFTag).assertIsDisplayed()
        rule.onNodeWithTag(MenuItemTag).assertIsDisplayed()

        rule.runOnIdle { assertThat(menuBounds.width).isEqualTo(textFieldBounds.width) }
    }

    @Test
    fun EDMBehaviour_rightOptionIsChosen() {
        rule.setMaterialContent {
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

    @Ignore("b/266109857")
    @Test
    fun doesNotCrashWhenAnchorDetachedFirst() {
        var parent: FrameLayout? = null
        rule.setContent {
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
                                            TextField(state = rememberTextFieldState("Text"))
                                            ExposedDropdownMenu(
                                                expanded = true,
                                                onDismissRequest = {},
                                            ) {
                                                DropdownMenuItem(
                                                    content = { Text(OptionName) },
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

    @Test
    fun withScrolledContent() {
        rule.setMaterialContent {
            Box(Modifier.fillMaxSize()) {
                ExposedDropdownMenuBox(
                    modifier = Modifier.align(Alignment.Center),
                    expanded = true,
                    onExpandedChange = {}
                ) {
                    val scrollState = rememberScrollState()
                    TextField(
                        state = rememberTextFieldState(),
                        label = { Text("Label") },
                    )
                    ExposedDropdownMenu(
                        expanded = true,
                        onDismissRequest = {},
                        scrollState = scrollState
                    ) {
                        repeat(100) {
                            Box(
                                Modifier.testTag("MenuContent ${it + 1}")
                                    .size(with(LocalDensity.current) { 70.toDp() })
                            )
                        }
                    }
                    LaunchedEffect(Unit) { scrollState.scrollTo(scrollState.maxValue) }
                }
            }
        }

        rule.waitForIdle()

        rule.onNodeWithTag("MenuContent 1").assertIsNotDisplayed()
        rule.onNodeWithTag("MenuContent 100").assertIsDisplayed()
    }

    @Composable
    fun ExposedDropdownMenuForTest(
        expanded: Boolean,
        onExpandChange: (Boolean) -> Unit,
        onTextFieldBoundsChanged: ((Rect) -> Unit)? = null,
        onMenuBoundsChanged: ((Rect) -> Unit)? = null
    ) {
        val textFieldState = rememberTextFieldState()
        Box(Modifier.fillMaxSize()) {
            ExposedDropdownMenuBox(
                modifier = Modifier.testTag(EDMBoxTag).align(Alignment.Center),
                expanded = expanded,
                onExpandedChange = onExpandChange,
            ) {
                TextField(
                    modifier =
                        Modifier.testTag(TFTag).onGloballyPositioned {
                            onTextFieldBoundsChanged?.invoke(it.boundsInRoot())
                        },
                    state = textFieldState,
                    label = { Text("Label") },
                    trailingIcon = {
                        Box(modifier = Modifier.testTag(TrailingIconTag)) {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        }
                    },
                    colors = ExposedDropdownMenuDefaults.textFieldColors()
                )
                ExposedDropdownMenu(
                    modifier =
                        Modifier.testTag(EDMTag).onGloballyPositioned {
                            onMenuBoundsChanged?.invoke(it.boundsInRoot())
                        },
                    expanded = expanded,
                    onDismissRequest = { onExpandChange(false) }
                ) {
                    DropdownMenuItem(
                        onClick = {
                            textFieldState.setTextAndPlaceCursorAtEnd(OptionName)
                            onExpandChange(false)
                        },
                        modifier = Modifier.testTag(MenuItemTag)
                    ) {
                        Text(text = OptionName)
                    }
                }
            }
        }
    }
}
