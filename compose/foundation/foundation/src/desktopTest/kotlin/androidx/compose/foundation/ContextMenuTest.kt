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

package androidx.compose.foundation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalLocalization
import androidx.compose.ui.platform.NativeClipboard
import androidx.compose.ui.platform.PlatformLocalization
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import java.awt.datatransfer.StringSelection
import kotlin.test.assertEquals
import org.junit.Test


@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
class ContextMenuTest {

    // https://github.com/JetBrains/compose-multiplatform/issues/2729
    @Test
    fun `contextMenuArea emits one child when open`() = runContextMenuTest {
        var childrenCount = 0

        setContent {
            // We can't just look up the number of children via the semantic node tree because
            // the layout added for the context menu (the empty one in PopupLayout) is not a
            // semantic node
            Layout(
                content = {
                    val state = ContextMenuState()
                    state.status = ContextMenuState.Status.Open(
                        Rect(Offset(1f, 1f), 0f)
                    )

                    ContextMenuArea(
                        items = {
                            listOf(
                                ContextMenuItem(
                                    label = "Copy",
                                    onClick = {}
                                )
                            )
                        },
                        state = state
                    ) {
                        Box(content = {})
                    }
                },
                measurePolicy = { measurables, _ ->
                    childrenCount = measurables.size
                    layout(0, 0) {}
                }
            )
        }

        assertEquals(1, childrenCount)
    }

    @Test
    fun `different items for different selections in btf`() =
        `different items for different selections in textfield`(useBtf2 = false)

    @Test
    fun `different items for different selections in btf2`() =
        `different items for different selections in textfield`(useBtf2 = true)


    // https://youtrack.jetbrains.com/issue/CMP-7083/Context-menu-on-desktop-shows-incorrect-items-after-the-second-showing
    private fun `different items for different selections in textfield`(
        useBtf2: Boolean
    ) = runContextMenuTest {
        val localization = object : PlatformLocalization {
            override val copy = "copy"
            override val cut = "cut"
            override val paste = "paste"
            override val selectAll = "selectAll"
        }

        val clipboard = object : Clipboard {
            val text = StringSelection("text")
            val clipEntry = ClipEntry(text)

            val mockPlatformClipboard = java.awt.datatransfer.Clipboard("test").apply {
                this.setContents(text, null)
            }

            override suspend fun getClipEntry(): ClipEntry? {
                return clipEntry
            }

            override suspend fun setClipEntry(clipEntry: ClipEntry?) {
                TODO("Not yet implemented")
            }

            override val nativeClipboard: NativeClipboard
                get() = mockPlatformClipboard
        }
        val navEventInput = DirectNavigationEventInput()

        setContent {
            CompositionLocalProvider(
                LocalLocalization provides localization,
                LocalClipboard provides clipboard
            ) {
                val owner = LocalNavigationEventDispatcherOwner.current
                LaunchedEffect(owner) {
                    owner?.navigationEventDispatcher?.addInput(navEventInput)
                }
                if (useBtf2) {
                    BasicTextField(rememberTextFieldState("Text"), Modifier.testTag("textfield"))
                } else {
                    BasicTextField("Text", {}, Modifier.testTag("textfield"))
                }
            }
        }

        onNodeWithText(localization.copy).assertDoesNotExist()
        onNodeWithText(localization.cut).assertDoesNotExist()
        onNodeWithText(localization.paste).assertDoesNotExist()
        onNodeWithText(localization.selectAll).assertDoesNotExist()

        onNodeWithTag("textfield").performMouseInput { rightClick() }
        onNodeWithText(localization.copy).assertMenuItemDoesNotExistOrIsDisabled()
        onNodeWithText(localization.cut).assertMenuItemDoesNotExistOrIsDisabled()
        onNodeWithText(localization.paste).assertExists()
        onNodeWithText(localization.selectAll).assertExists()

        navEventInput.backCompleted()
        onNodeWithText(localization.copy).assertDoesNotExist()
        onNodeWithText(localization.cut).assertDoesNotExist()
        onNodeWithText(localization.paste).assertDoesNotExist()
        onNodeWithText(localization.selectAll).assertDoesNotExist()

        onNodeWithTag("textfield").performTextInputSelection(TextRange(0, "Text".length))
        onNodeWithTag("textfield").performMouseInput { rightClick() }
        onNodeWithText(localization.copy).assertExists()
        onNodeWithText(localization.cut).assertExists()
        onNodeWithText(localization.paste).assertExists()
        onNodeWithText(localization.selectAll).assertMenuItemDoesNotExistOrIsDisabled()

        navEventInput.backCompleted()
        onNodeWithText(localization.copy).assertDoesNotExist()
        onNodeWithText(localization.cut).assertDoesNotExist()
        onNodeWithText(localization.paste).assertDoesNotExist()
        onNodeWithText(localization.selectAll).assertDoesNotExist()
    }

    private fun SemanticsNodeInteraction.assertMenuItemDoesNotExistOrIsDisabled() {
        // New context menus disabled items; old context menus don't show disabled items.
        if (ComposeFoundationFlags.isNewContextMenuEnabled) {
            assertIsNotEnabled()
        } else {
            assertDoesNotExist()
        }
    }

    @Test
    fun contextMenuClosesAfterMenuItemSelection_btf1() =
        contextMenuClosesAfterMenuItemSelection(useBtf2 = false)

    @Test
    fun contextMenuClosesAfterMenuItemSelection_btf2() =
        contextMenuClosesAfterMenuItemSelection(useBtf2 = true)

    private fun contextMenuClosesAfterMenuItemSelection(useBtf2: Boolean) = runContextMenuTest {

        val localization = object : PlatformLocalization {
            override val copy = "copy"
            override val cut = "cut"
            override val paste = "paste"
            override val selectAll = "selectAll"
        }

        setContent {
            CompositionLocalProvider(LocalLocalization provides localization) {
                if (useBtf2) {
                    BasicTextField(
                        rememberTextFieldState("Text", initialSelection = TextRange(0, 4)),
                        Modifier.testTag("textfield")
                    )
                } else {
                    BasicTextField(
                        value = TextFieldValue("Text", selection = TextRange(0, 4)),
                        onValueChange = {},
                        modifier = Modifier.testTag("textfield")
                    )
                }
            }
        }

        onNodeWithTag("textfield").performMouseInput { rightClick(Offset(1f, height/2f)) }
        onNodeWithText(localization.copy).apply {
            assertExists()
            performClick()
            assertDoesNotExist()
        }
    }

    private fun runContextMenuTest(block: ComposeUiTest.() -> Unit) = runComposeUiTest {
        DesktopPlatform.withOverriddenCurrent(DesktopPlatform.Unknown) {
            block()
        }
    }
}