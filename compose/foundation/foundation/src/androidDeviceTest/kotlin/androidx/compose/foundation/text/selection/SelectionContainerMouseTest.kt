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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.gestures.util.collapsed
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
internal class SelectionContainerMouseTest : AbstractSelectionContainerTest() {

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

        assertAnchorInfo(selection.value?.start, offset = 0, selectableId = 1)
        assertAnchorInfo(selection.value?.end, offset = 4, selectableId = 2)
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

        assertAnchorInfo(selection.value?.start, offset = 7, selectableId = 2)
        assertAnchorInfo(selection.value?.end, offset = 5, selectableId = 1)
    }

    @Test
    fun doubleClickSelectsAWord() =
        with(rule.density) {
            // Setup.
            createSelectionContainer()
            val characterSize = fontSize.toPx()

            // Act. Double click "m" in "Demo", and "Demo" should be selected.
            rule.onSelectionContainer().performMouseInput {
                doubleClick(Offset(textContent.indexOf('m') * characterSize, 0.5f * characterSize))
            }

            // Assert. Should select "Demo".
            assertThat(selection.value!!.start.offset).isEqualTo(textContent.indexOf('D'))
            assertThat(selection.value!!.end.offset).isEqualTo(textContent.indexOf('o') + 1)
        }

    @Test
    fun primaryClickOnSelectedTextClearsSelection() =
        with(rule.density) {
            // Setup.
            createSelectionContainer()
            val characterSize = fontSize.toPx()

            // Act. Double click "m" in "Demo", and "Demo" should be selected.
            rule.onSelectionContainer().performMouseInput {
                doubleClick(Offset(textContent.indexOf('m') * characterSize, 0.5f * characterSize))
            }
            rule.runOnIdle { assertThat(selection.value).isNotNull() }

            // Act. Click on the same place, and selection should be cleared.
            rule.onSelectionContainer().performMouseInput { click() }

            // Assert.
            // TODO(b/384750891) Cleared selection should be null
            rule.runOnIdle { assertThat(selection.value!!.toTextRange()).isEqualTo(14.collapsed) }
        }

    @Test
    fun buttonWithTextClickInsideSelectionContainer() {
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
    fun buttonClickClearsSelection() =
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
            rule.runOnIdle { assertThat(selection.value!!.toTextRange()).isEqualTo(3.collapsed) }
            rule.runOnIdle { assertThat(clickCounter).isEqualTo(1) }
        }

    @Test
    fun buttonClickInsideDisableSelectionClearsSelection() =
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
            rule.runOnIdle { assertThat(selection.value).isNull() }
            rule.runOnIdle { assertThat(clickCounter).isEqualTo(1) }
        }

    @Test
    fun loseFocusClearsSelection() =
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
                            SelectionContainer(
                                selection = selection.value,
                                onSelectionChange = { selection.value = it },
                            ) {
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
            rule.runOnIdle { assertThat(selection.value).isNull() }
            rule.runOnIdle { assertThat(clickCounter).isEqualTo(1) }
        }

    @Test
    fun selectButtonTextInsideSelectionContainer() =
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
            rule.runOnIdle { assertThat(selection.value).isNotNull() }

            // Act. Click on the same place, and selection should be cleared.
            rule.onSelectionContainer().performMouseInput { click() }

            // Assert.
            // TODO(b/384750891) Cleared selection should be null
            rule.runOnIdle { assertThat(selection.value!!.toTextRange()).isEqualTo(14.collapsed) }
        }
}
