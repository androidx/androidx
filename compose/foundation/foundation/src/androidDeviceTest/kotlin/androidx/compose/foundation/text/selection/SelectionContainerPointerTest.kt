/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.gestures.util.collapsed
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.InjectionScope
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.dragAndDrop
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTrackpadInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
internal class SelectionContainerPointerTest : AbstractSelectionContainerTest() {

    @Test
    fun mouseSelectionContinuesToBelowText() {
        createSelectionContainer {
            Column {
                BasicText(
                    AnnotatedString(textContent),
                    Modifier.fillMaxWidth().testTag(tag1),
                    style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                )
                BasicText(
                    AnnotatedString(textContent),
                    Modifier.fillMaxWidth().testTag(tag2),
                    style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                )
            }
        }

        val from = characterBox(tag1, offset = 0)
        val to = characterBox(tag2, offset = 3)
        rule.onRoot().performMouseInput {
            moveTo(from.centerLeft)
            press()
            moveTo(to.centerRight)
            release()
        }

        assertAnchorInfo(state.selection?.start, offset = 0, selectableId = 1)
        assertAnchorInfo(state.selection?.end, offset = 4, selectableId = 2)
    }

    @Test
    fun mouseSelectionContinuesToAboveText() {
        createSelectionContainer {
            Column {
                BasicText(
                    AnnotatedString(textContent),
                    Modifier.fillMaxWidth().testTag(tag1),
                    style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                )
                BasicText(
                    AnnotatedString(textContent),
                    Modifier.fillMaxWidth().testTag(tag2),
                    style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                )
            }
        }

        val from = characterBox(tag2, offset = 6) // second word should be selected
        val to = characterBox(tag1, offset = 5)
        rule.onRoot().performMouseInput {
            moveTo(from.centerRight)
            press()
            moveTo(to.centerLeft)
            release()
        }

        assertAnchorInfo(state.selection?.start, offset = 7, selectableId = 2)
        assertAnchorInfo(state.selection?.end, offset = 5, selectableId = 1)
    }

    @Test
    fun mouseDoubleClickSelectsAWord() =
        with(rule.density) {
            // Setup.
            createSelectionContainer()
            val characterSize = fontSize.toPx()

            // Act. Double click "m" in "Demo", and "Demo" should be selected.
            rule.onSelectionContainer().performMouseInput {
                doubleClick(Offset(textContent.indexOf('m') * characterSize, 0.5f * characterSize))
            }

            // Assert. Should select "Demo".
            assertThat(state.selection!!.start.offset).isEqualTo(textContent.indexOf('D'))
            assertThat(state.selection!!.end.offset).isEqualTo(textContent.indexOf('o') + 1)
        }

    @Test
    fun mousePrimaryClickOnSelectedTextClearsSelection() =
        with(rule.density) {
            // Setup.
            createSelectionContainer()
            val characterSize = fontSize.toPx()

            // Act. Double click "m" in "Demo", and "Demo" should be selected.
            rule.onSelectionContainer().performMouseInput {
                doubleClick(Offset(textContent.indexOf('m') * characterSize, 0.5f * characterSize))
            }
            rule.runOnIdle { assertThat(state.selection).isNotNull() }

            // Act. Click on the same place, and selection should be cleared.
            rule.onSelectionContainer().performMouseInput { click() }

            // Assert.
            // TODO(b/384750891) Cleared selection should be null
            rule.runOnIdle { assertThat(state.selection!!.toTextRange()).isEqualTo(14.collapsed) }
        }

    @Test
    fun mouseButtonWithTextClickInsideSelectionContainer() {
        var clickCounter = 0
        createSelectionContainer {
            Box(Modifier.clickable { clickCounter++ }) {
                BasicText(
                    text = "Button",
                    modifier = Modifier.align(Alignment.Center).testTag(tag1),
                )
            }
        }
        rule.onNodeWithTag(tag1, useUnmergedTree = true).performMouseInput { click() }
        rule.runOnIdle { assertThat(clickCounter).isEqualTo(1) }
    }

    @Test
    fun mouseButtonClickClearsSelection() =
        with(rule.density) {
            var clickCounter = 0
            createSelectionContainer {
                Column {
                    TestText(textContent)
                    TestButton(Modifier.size(50.dp).testTag(tag1), onClick = { clickCounter++ }) {
                        TestText("Button")
                    }
                }
            }
            val characterSize = fontSize.toPx()

            // Act. Double click "m" in "Demo", and "Demo" should be selected.
            rule.onSelectionContainer().performMouseInput {
                doubleClick(Offset(textContent.indexOf('m') * characterSize, 0.5f * characterSize))
            }
            rule.onNodeWithTag(tag1, useUnmergedTree = true).performMouseInput { click() }

            // Assert.
            // TODO(b/384750891) Cleared selection should be null
            rule.runOnIdle { assertThat(state.selection!!.toTextRange()).isEqualTo(3.collapsed) }
            rule.runOnIdle { assertThat(clickCounter).isEqualTo(1) }
        }

    @Test
    fun mouseButtonClickInsideDisableSelectionClearsSelection() =
        with(rule.density) {
            var clickCounter = 0
            createSelectionContainer {
                Column {
                    TestText(textContent)
                    DisableSelection {
                        TestButton(
                            Modifier.size(50.dp).testTag(tag1),
                            onClick = { clickCounter++ },
                        ) {
                            TestText("Button")
                        }
                    }
                }
            }
            val characterSize = fontSize.toPx()

            // Act. Double click "m" in "Demo", and "Demo" should be selected.
            rule.onSelectionContainer().performMouseInput {
                doubleClick(Offset(textContent.indexOf('m') * characterSize, 0.5f * characterSize))
            }
            rule.onNodeWithTag(tag1, useUnmergedTree = true).performMouseInput { click() }

            // Assert.
            rule.runOnIdle { assertThat(state.selection).isNull() }
            rule.runOnIdle { assertThat(clickCounter).isEqualTo(1) }
        }

    @Test
    fun mouseLoseFocusClearsSelection() =
        with(rule.density) {
            var clickCounter = 0
            rule.setContent {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    TestParent(Modifier.testTag("selectionContainer")) {
                        Column {
                            TestButton(
                                Modifier.size(50.dp).testTag(tag1),
                                onClick = { clickCounter++ },
                            ) {
                                TestText("Button")
                            }
                            SelectionContainer(state = state) {
                                TestText(textContent, Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
            rule.waitForIdle()
            val characterSize = fontSize.toPx()

            // Act. Double click "m" in "Demo", and "Demo" should be selected.
            rule.onSelectionContainer().performMouseInput {
                doubleClick(Offset(textContent.indexOf('m') * characterSize, 0.5f * characterSize))
            }
            rule.onNodeWithTag(tag1, useUnmergedTree = true).performMouseInput { click() }

            // Assert.
            rule.runOnIdle { assertThat(state.selection).isNull() }
            rule.runOnIdle { assertThat(clickCounter).isEqualTo(1) }
        }

    @Test
    fun mouseSelectButtonTextInsideSelectionContainer() =
        with(rule.density) {
            var clickCounter = 0

            // Setup.
            createSelectionContainer {
                TestButton(onClick = { clickCounter++ }) {
                    TestText(textContent, Modifier.fillMaxSize())
                }
            }
            val characterSize = fontSize.toPx()

            // Act. Double click "m" in "Demo", and "Demo" should be selected.
            rule.onSelectionContainer().performMouseInput {
                doubleClick(Offset(textContent.indexOf('m') * characterSize, 0.5f * characterSize))
            }
            rule.runOnIdle { assertThat(state.selection).isNotNull() }

            // Act. Click on the same place, and selection should be cleared.
            rule.onSelectionContainer().performMouseInput { click() }

            // Assert.
            // TODO(b/384750891) Cleared selection should be null
            rule.runOnIdle { assertThat(state.selection!!.toTextRange()).isEqualTo(14.collapsed) }
        }

    @Test
    fun trackpadSelectionContinuesToBelowText() {
        createSelectionContainer {
            Column {
                BasicText(
                    AnnotatedString(textContent),
                    Modifier.fillMaxWidth().testTag(tag1),
                    style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                )
                BasicText(
                    AnnotatedString(textContent),
                    Modifier.fillMaxWidth().testTag(tag2),
                    style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                )
            }
        }

        val from = characterBox(tag1, offset = 0)
        val to = characterBox(tag2, offset = 3)
        rule.onRoot().performTrackpadInput {
            moveTo(from.centerLeft)
            press()
            moveTo(to.centerRight)
            release()
        }

        assertAnchorInfo(state.selection?.start, offset = 0, selectableId = 1)
        assertAnchorInfo(state.selection?.end, offset = 4, selectableId = 2)
    }

    @Test
    fun trackpadSelectionContinuesToAboveText() {
        createSelectionContainer {
            Column {
                BasicText(
                    AnnotatedString(textContent),
                    Modifier.fillMaxWidth().testTag(tag1),
                    style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                )
                BasicText(
                    AnnotatedString(textContent),
                    Modifier.fillMaxWidth().testTag(tag2),
                    style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                )
            }
        }

        val from = characterBox(tag2, offset = 6) // second word should be selected
        val to = characterBox(tag1, offset = 5)
        rule.onRoot().performTrackpadInput {
            moveTo(from.centerRight)
            press()
            moveTo(to.centerLeft)
            release()
        }

        assertAnchorInfo(state.selection?.start, offset = 7, selectableId = 2)
        assertAnchorInfo(state.selection?.end, offset = 5, selectableId = 1)
    }

    @Test
    fun trackpadDoubleClickSelectsAWord() =
        with(rule.density) {
            // Setup.
            createSelectionContainer()
            val characterSize = fontSize.toPx()

            // Act. Double click "m" in "Demo", and "Demo" should be selected.
            rule.onSelectionContainer().performTrackpadInput {
                doubleClick(Offset(textContent.indexOf('m') * characterSize, 0.5f * characterSize))
            }

            // Assert. Should select "Demo".
            assertThat(state.selection!!.start.offset).isEqualTo(textContent.indexOf('D'))
            assertThat(state.selection!!.end.offset).isEqualTo(textContent.indexOf('o') + 1)
        }

    @Test
    fun trackpadPrimaryClickOnSelectedTextClearsSelection() =
        with(rule.density) {
            // Setup.
            createSelectionContainer()
            val characterSize = fontSize.toPx()

            // Act. Double click "m" in "Demo", and "Demo" should be selected.
            rule.onSelectionContainer().performTrackpadInput {
                doubleClick(Offset(textContent.indexOf('m') * characterSize, 0.5f * characterSize))
            }
            rule.runOnIdle { assertThat(state.selection).isNotNull() }

            // Act. Click on the same place, and selection should be cleared.
            rule.onSelectionContainer().performTrackpadInput { click() }

            // Assert.
            // TODO(b/384750891) Cleared selection should be null
            rule.runOnIdle { assertThat(state.selection!!.toTextRange()).isEqualTo(14.collapsed) }
        }

    @Test
    fun trackpadButtonWithTextClickInsideSelectionContainer() {
        var clickCounter = 0
        createSelectionContainer {
            Box(Modifier.clickable { clickCounter++ }) {
                BasicText(
                    text = "Button",
                    modifier = Modifier.align(Alignment.Center).testTag(tag1),
                )
            }
        }
        rule.onNodeWithTag(tag1, useUnmergedTree = true).performTrackpadInput { click() }
        rule.runOnIdle { assertThat(clickCounter).isEqualTo(1) }
    }

    @Test
    fun trackpadButtonClickClearsSelection() =
        with(rule.density) {
            var clickCounter = 0
            createSelectionContainer {
                Column {
                    TestText(textContent)
                    TestButton(Modifier.size(50.dp).testTag(tag1), onClick = { clickCounter++ }) {
                        TestText("Button")
                    }
                }
            }
            val characterSize = fontSize.toPx()

            // Act. Double click "m" in "Demo", and "Demo" should be selected.
            rule.onSelectionContainer().performTrackpadInput {
                doubleClick(Offset(textContent.indexOf('m') * characterSize, 0.5f * characterSize))
            }
            rule.onNodeWithTag(tag1, useUnmergedTree = true).performTrackpadInput { click() }

            // Assert.
            // TODO(b/384750891) Cleared selection should be null
            rule.runOnIdle { assertThat(state.selection!!.toTextRange()).isEqualTo(3.collapsed) }
            rule.runOnIdle { assertThat(clickCounter).isEqualTo(1) }
        }

    @Test
    fun trackpadButtonClickInsideDisableSelectionClearsSelection() =
        with(rule.density) {
            var clickCounter = 0
            createSelectionContainer {
                Column {
                    TestText(textContent)
                    DisableSelection {
                        TestButton(
                            Modifier.size(50.dp).testTag(tag1),
                            onClick = { clickCounter++ },
                        ) {
                            TestText("Button")
                        }
                    }
                }
            }
            val characterSize = fontSize.toPx()

            // Act. Double click "m" in "Demo", and "Demo" should be selected.
            rule.onSelectionContainer().performTrackpadInput {
                doubleClick(Offset(textContent.indexOf('m') * characterSize, 0.5f * characterSize))
            }
            rule.onNodeWithTag(tag1, useUnmergedTree = true).performTrackpadInput { click() }

            // Assert.
            rule.runOnIdle { assertThat(state.selection).isNull() }
            rule.runOnIdle { assertThat(clickCounter).isEqualTo(1) }
        }

    @Test
    fun trackpadLoseFocusClearsSelection() =
        with(rule.density) {
            var clickCounter = 0
            rule.setContent {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    TestParent(Modifier.testTag("selectionContainer")) {
                        Column {
                            TestButton(
                                Modifier.size(50.dp).testTag(tag1),
                                onClick = { clickCounter++ },
                            ) {
                                TestText("Button")
                            }
                            SelectionContainer(state = state) {
                                TestText(textContent, Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
            rule.waitForIdle()
            val characterSize = fontSize.toPx()

            // Act. Double click "m" in "Demo", and "Demo" should be selected.
            rule.onSelectionContainer().performTrackpadInput {
                doubleClick(Offset(textContent.indexOf('m') * characterSize, 0.5f * characterSize))
            }
            rule.onNodeWithTag(tag1, useUnmergedTree = true).performTrackpadInput { click() }

            // Assert.
            rule.runOnIdle { assertThat(state.selection).isNull() }
            rule.runOnIdle { assertThat(clickCounter).isEqualTo(1) }
        }

    @Test
    fun trackpadSelectButtonTextInsideSelectionContainer() =
        with(rule.density) {
            var clickCounter = 0

            // Setup.
            createSelectionContainer {
                TestButton(onClick = { clickCounter++ }) {
                    TestText(textContent, Modifier.fillMaxSize())
                }
            }
            val characterSize = fontSize.toPx()

            // Act. Double click "m" in "Demo", and "Demo" should be selected.
            rule.onSelectionContainer().performTrackpadInput {
                doubleClick(Offset(textContent.indexOf('m') * characterSize, 0.5f * characterSize))
            }
            rule.runOnIdle { assertThat(state.selection).isNotNull() }

            // Act. Click on the same place, and selection should be cleared.
            rule.onSelectionContainer().performTrackpadInput { click() }

            // Assert.
            // TODO(b/384750891) Cleared selection should be null
            rule.runOnIdle { assertThat(state.selection!!.toTextRange()).isEqualTo(14.collapsed) }
        }

    private fun testMouseSelection(
        start: InjectionScope.() -> Offset,
        end: InjectionScope.() -> Offset,
        expectStartSelectableId: Int,
        expectStartOffset: Int,
        expectEndSelectableId: Int,
        expectEndOffset: Int,
        expectJoinedSelectedText: String,
    ) {
        rule.onSelectionContainer().performMouseInput { dragAndDrop(start = start(), end = end()) }
        rule.runOnIdle {
            val selection = state.selection
            assertNotNull(selection)
            assertThat(selection.start.selectableId).isEqualTo(expectStartSelectableId)
            assertThat(selection.start.offset).isEqualTo(expectStartOffset)
            assertThat(selection.end.selectableId).isEqualTo(expectEndSelectableId)
            assertThat(selection.end.offset).isEqualTo(expectEndOffset)
            assertThat(state.joinedSelectedText).isEqualTo(expectJoinedSelectedText)
        }
    }

    @Test
    fun mouseSelectionStartBetweenSelectablesVertically() = withMouseSelectionBetweenTextEnabled {
        val topText = "Top Text"
        val bottomText = "Bottom Text"

        // Setup
        createSelectionContainer {
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                TestText(topText)
                TestText(bottomText)
            }
        }

        // Select from middle of container to start.
        testMouseSelection(
            start = { center },
            end = { topLeft },
            expectStartSelectableId = 1,
            expectStartOffset = topText.length,
            expectEndSelectableId = 1,
            expectEndOffset = 0,
            expectJoinedSelectedText = topText,
        )

        // Select from middle of container to end.
        testMouseSelection(
            start = { center },
            end = { bottomCenter },
            expectStartSelectableId = 2,
            expectStartOffset = 0,
            expectEndSelectableId = 2,
            expectEndOffset = bottomText.length,
            expectJoinedSelectedText = bottomText,
        )

        // Select from start of container to end.
        testMouseSelection(
            start = { topCenter },
            end = { bottomCenter },
            expectStartSelectableId = 1,
            expectStartOffset = 0,
            expectEndSelectableId = 2,
            expectEndOffset = bottomText.length,
            expectJoinedSelectedText = topText + bottomText,
        )

        // Act. Select from end of container to start.
        testMouseSelection(
            start = { bottomCenter },
            end = { topCenter },
            expectStartSelectableId = 2,
            expectStartOffset = bottomText.length,
            expectEndSelectableId = 1,
            expectEndOffset = 0,
            expectJoinedSelectedText = topText + bottomText,
        )
    }

    @Test
    fun mouseSelectionStartBetweenSelectablesHorizontally() = withMouseSelectionBetweenTextEnabled {
        val leftText = "Left" // Shorter text to make it fit horizontally
        val rightText = "Right"

        // Setup
        createSelectionContainer {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TestText(leftText)
                TestText(rightText)
            }
        }

        // Select from middle of container to start.
        testMouseSelection(
            start = { center },
            end = { centerLeft },
            expectStartSelectableId = 1,
            expectStartOffset = leftText.length,
            expectEndSelectableId = 1,
            expectEndOffset = 0,
            expectJoinedSelectedText = leftText,
        )

        // Select from middle of container to end.
        testMouseSelection(
            start = { center },
            end = { centerRight },
            expectStartSelectableId = 2,
            expectStartOffset = 0,
            expectEndSelectableId = 2,
            expectEndOffset = rightText.length,
            expectJoinedSelectedText = rightText,
        )

        // Select from start of container to end.
        testMouseSelection(
            start = { centerLeft },
            end = { centerRight },
            expectStartSelectableId = 1,
            expectStartOffset = 0,
            expectEndSelectableId = 2,
            expectEndOffset = rightText.length,
            expectJoinedSelectedText = leftText + rightText,
        )

        // Act. Select from end of container to start.
        testMouseSelection(
            start = { centerRight },
            end = { centerLeft },
            expectStartSelectableId = 2,
            expectStartOffset = rightText.length,
            expectEndSelectableId = 1,
            expectEndOffset = 0,
            expectJoinedSelectedText = leftText + rightText,
        )
    }

    @Test
    fun selectWithMouseInsideAndDragRightUpAndOutside() = withMouseSelectionBetweenTextEnabled {
        // Set up
        createSelectionContainer {
            Column(Modifier.testTag("column").padding(50.dp)) {
                BasicText("Lorem\nipsum\ndolor", modifier = Modifier.testTag("text1"))
                // The 2nd text is needed to reproduce the issue
                BasicText("Hello")
            }
        }

        rule.onNodeWithTag("text1").performMouseInput {
            updatePointerTo(center)
            press()
            moveTo(topRight + Offset(10f, 0f))
        }

        rule.runOnIdle {
            // Verify it doesn't crash and selects something
            assertThat(state.joinedSelectedText).isNotEmpty()
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    private inline fun withMouseSelectionBetweenTextEnabled(block: () -> Unit) {
        val savedValue = ComposeFoundationFlags.isMouseSelectionBetweenTextEnabled
        ComposeFoundationFlags.isMouseSelectionBetweenTextEnabled = true
        try {
            block()
        } finally {
            ComposeFoundationFlags.isMouseSelectionBetweenTextEnabled = savedValue
        }
    }
}

private val SelectionState.joinedSelectedText
    get() = selectedTexts.joinToString(separator = "")
