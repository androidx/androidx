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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.contextmenu.internal.ProvidePlatformTextContextMenuToolbar
import androidx.compose.foundation.text.contextmenu.test.ContextMenuFlagFlipperRunner
import androidx.compose.foundation.text.contextmenu.test.ContextMenuFlagSuppress
import androidx.compose.foundation.text.contextmenu.test.SpyTextActionModeCallback
import androidx.compose.foundation.text.contextmenu.test.assertShown
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@LargeTest
@RunWith(ContextMenuFlagFlipperRunner::class)
class SelectionContainerFocusTest {
    @get:Rule val rule = createComposeRule()

    private val textContent = "Text Demo Text"
    private val fontFamily = TEST_FONT_FAMILY

    private val selection1 = mutableStateOf<Selection?>(null)
    private val selection2 = mutableStateOf<Selection?>(null)
    private val fontSize = 20.sp
    private val boxSize = 40.dp

    private val hapticFeedback = mock<HapticFeedback>()

    @Test
    fun tap_to_cancel() {
        // Setup. Long press to create a selection.
        createSelectionContainer()
        // Touch position. In this test, each character's width and height equal to fontSize.
        // Position is computed so that (position, position) is the center of the first character.
        val positionInText = with(rule.density) { fontSize.toPx() / 2 }
        rule.onNodeWithTag("selectionContainer1").performTouchInput {
            longClick(Offset(x = positionInText, y = positionInText))
        }
        rule.runOnIdle { assertThat(selection1.value).isNotNull() }

        // Act.
        rule.onNodeWithTag("box").performTouchInput { click() }

        // Assert.
        rule.runOnIdle {
            assertThat(selection1.value).isNull()
            assertThat(selection2.value).isNull()
            verify(hapticFeedback, times(2))
                .performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    @Test
    fun select_anotherContainer_cancelOld() {
        // Setup. Long press to create a selection.
        createSelectionContainer()
        // Touch position. In this test, each character's width and height equal to fontSize.
        // Position is computed so that (position, position) is the center of the first character.
        val positionInText = with(rule.density) { fontSize.toPx() / 2 }
        rule.onNodeWithTag("selectionContainer1").performTouchInput {
            longClick(Offset(x = positionInText, y = positionInText))
        }
        rule.runOnIdle { assertThat(selection1.value).isNotNull() }

        // Act.
        rule.onNodeWithTag("selectionContainer2").performTouchInput {
            longClick(Offset(x = positionInText, y = positionInText))
        }

        // Assert.
        rule.runOnIdle {
            assertThat(selection1.value).isNull()
            assertThat(selection2.value).isNotNull()
            // There will be 2 times from the first SelectionContainer and 1 time from the second
            // SelectionContainer.
            verify(hapticFeedback, times(3))
                .performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    @ContextMenuFlagSuppress(suppressedFlagValue = true)
    @Test
    fun leavingComposition_hidesTextToolbar() {
        // null -> nothing got called, true -> show, false -> hide
        var lastShowCalled: Boolean? = null
        val fakeTextToolbar =
            FakeTextToolbar(
                onShowMenu = { _, _, _, _, _, _ -> lastShowCalled = true },
                onHideMenu = { lastShowCalled = false },
            )

        val tag = "SelectionContainer"

        var inComposition by mutableStateOf(true)

        rule.setContent {
            CompositionLocalProvider(LocalTextToolbar provides fakeTextToolbar) {
                if (inComposition) {
                    SelectionContainer(modifier = Modifier.testTag("SelectionContainer")) {
                        BasicText(
                            AnnotatedString(textContent),
                            Modifier.fillMaxWidth(),
                            style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                            softWrap = true,
                            overflow = TextOverflow.Clip,
                            maxLines = Int.MAX_VALUE,
                            inlineContent = mapOf(),
                            onTextLayout = {},
                        )
                    }
                }
            }
        }

        rule.onNodeWithTag(tag).performTouchInput { longClick() }
        rule.runOnIdle { assertThat(lastShowCalled).isTrue() }

        inComposition = false

        rule.runOnIdle { assertThat(lastShowCalled).isFalse() }
    }

    @ContextMenuFlagSuppress(suppressedFlagValue = false)
    @Test
    fun leavingComposition_hidesTextToolbar_newContextMenu() {
        val tag = "SelectionContainer"
        val spyTextActionModeCallback = SpyTextActionModeCallback()

        var inComposition by mutableStateOf(true)

        rule.setContent {
            ProvidePlatformTextContextMenuToolbar(
                callbackInjector = { spyTextActionModeCallback.apply { delegate = it } }
            ) {
                if (inComposition) {
                    SelectionContainer(modifier = Modifier.testTag("SelectionContainer")) {
                        BasicText(
                            AnnotatedString(textContent),
                            Modifier.fillMaxWidth(),
                            style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                            softWrap = true,
                            overflow = TextOverflow.Clip,
                            maxLines = Int.MAX_VALUE,
                            inlineContent = mapOf(),
                            onTextLayout = {},
                        )
                    }
                }
            }
        }

        rule.onNodeWithTag(tag).performTouchInput { longClick() }
        rule.waitForIdle()
        spyTextActionModeCallback.assertShown(shown = true)

        inComposition = false

        rule.waitForIdle()
        spyTextActionModeCallback.assertShown(shown = false)
    }

    private fun createSelectionContainer(isRtl: Boolean = false) {
        val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
        rule.setContent {
            CompositionLocalProvider(
                LocalHapticFeedback provides hapticFeedback,
                LocalLayoutDirection provides layoutDirection,
                LocalTextToolbar provides mock(),
            ) {
                Column {
                    SelectionContainer(
                        modifier = Modifier.testTag("selectionContainer1"),
                        selection = selection1.value,
                        onSelectionChange = { selection1.value = it },
                    ) {
                        Column {
                            BasicText(
                                text = AnnotatedString(textContent),
                                modifier = Modifier.fillMaxWidth(),
                                style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                            )
                            Box(Modifier.size(boxSize, boxSize).testTag("box"))
                        }
                    }

                    SelectionContainer(
                        modifier = Modifier.testTag("selectionContainer2"),
                        selection = selection2.value,
                        onSelectionChange = { selection2.value = it },
                    ) {
                        BasicText(
                            text = AnnotatedString(textContent),
                            modifier = Modifier.fillMaxWidth(),
                            style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                        )
                    }
                }
            }
        }
    }
}

internal fun FakeTextToolbar(
    onShowMenu:
        (
            rect: Rect,
            onCopyRequested: (() -> Unit)?,
            onPasteRequested: (() -> Unit)?,
            onCutRequested: (() -> Unit)?,
            onSelectAllRequested: (() -> Unit)?,
            onAutofillRequested: (() -> Unit)?,
        ) -> Unit,
    onHideMenu: () -> Unit,
): TextToolbar {
    return object : TextToolbar {
        private var _status: TextToolbarStatus = TextToolbarStatus.Hidden

        override fun showMenu(
            rect: Rect,
            onCopyRequested: (() -> Unit)?,
            onPasteRequested: (() -> Unit)?,
            onCutRequested: (() -> Unit)?,
            onSelectAllRequested: (() -> Unit)?,
            onAutofillRequested: (() -> Unit)?,
        ) {
            onShowMenu(
                rect,
                onCopyRequested,
                onPasteRequested,
                onCutRequested,
                onSelectAllRequested,
                onAutofillRequested,
            )
            _status = TextToolbarStatus.Shown
        }

        override fun showMenu(
            rect: Rect,
            onCopyRequested: (() -> Unit)?,
            onPasteRequested: (() -> Unit)?,
            onCutRequested: (() -> Unit)?,
            onSelectAllRequested: (() -> Unit)?,
        ) {
            _status = TextToolbarStatus.Shown
        }

        override fun hide() {
            onHideMenu()
            _status = TextToolbarStatus.Hidden
        }

        override val status: TextToolbarStatus
            get() = _status
    }
}
