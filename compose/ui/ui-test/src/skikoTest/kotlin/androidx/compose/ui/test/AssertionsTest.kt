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

package androidx.compose.ui.test

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.RadioButton
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertFails


/**
 * Tests the assertion (e.g. [assertTextEquals]) functionality of the testing framework.
 */
@OptIn(ExperimentalTestApi::class)
class AssertionsTest {

    @Test
    fun testAssertExists() = runComposeUiTest {
        setContent {
            Box(Modifier.testTag("tag"))
        }

        onNodeWithTag("tag").assertExists()
        assertFails {
            onNodeWithTag("non-tag").assertExists()
        }
    }

    @Test
    fun testAssertDoesNotExist() = runComposeUiTest {
        setContent {
            Box(Modifier.testTag("tag"))
        }

        onNodeWithTag("text").assertDoesNotExist()
        assertFails {
            onNodeWithTag("tag").assertDoesNotExist()
        }
    }

    @Test
    fun testAssertIsDisplayed() = runComposeUiTest {
        setContent {
            Column(
                Modifier.size(100.dp)
            ) {
                Box(
                    Modifier
                        .testTag("tag1")
                        .size(100.dp)
                )
                Box(
                    Modifier
                        .testTag("tag2")
                        .size(100.dp)
                )
            }
        }

        onNodeWithTag("tag1").assertIsDisplayed()
        assertFails {
            onNodeWithTag("tag2").assertIsDisplayed()
        }
    }

    @Test
    fun testAssertIsNotDisplayed() = runComposeUiTest {
        setContent {
            Column(
                Modifier.size(100.dp)
            ) {
                Box(
                    Modifier
                        .testTag("tag1")
                        .size(100.dp)
                )
                Box(
                    Modifier
                        .testTag("tag2")
                        .size(100.dp)
                )
            }
        }

        onNodeWithTag("tag2").assertIsNotDisplayed()
        assertFails {
            onNodeWithTag("tag1").assertIsNotDisplayed()
        }
    }

    @Test
    fun testAssertIsEnabled() = runComposeUiTest {
        setContent {
            Button(
                onClick = {},
                enabled = true,
                modifier = Modifier.testTag("tag1")
            ) {}
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier.testTag("tag2")
            ) {}
        }

        onNodeWithTag("tag1").assertIsEnabled()
        assertFails {
            onNodeWithTag("tag2").assertIsEnabled()
        }
    }

    @Test
    fun testAssertIsNotEnabled() = runComposeUiTest {
        setContent {
            Button(
                onClick = {},
                enabled = true,
                modifier = Modifier.testTag("tag1")
            ) {}
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier.testTag("tag2")
            ) {}
        }

        onNodeWithTag("tag2").assertIsNotEnabled()
        assertFails {
            onNodeWithTag("tag1").assertIsNotEnabled()
        }
    }

    @Test
    fun testAssertIsOn() = runComposeUiTest {
        setContent {
            Checkbox(
                checked = true,
                onCheckedChange = { },
                modifier = Modifier.testTag("tag1")
            )
            Checkbox(
                checked = false,
                onCheckedChange = { },
                modifier = Modifier.testTag("tag2")
            )
        }

        onNodeWithTag("tag1").assertIsOn()
        assertFails {
            onNodeWithTag("tag2").assertIsOn()
        }
    }

    @Test
    fun testAssertIsOff() = runComposeUiTest {
        setContent {
            Checkbox(
                checked = true,
                onCheckedChange = { },
                modifier = Modifier.testTag("tag1")
            )
            Checkbox(
                checked = false,
                onCheckedChange = { },
                modifier = Modifier.testTag("tag2")
            )
        }

        onNodeWithTag("tag2").assertIsOff()
        assertFails {
            onNodeWithTag("tag1").assertIsOff()
        }
    }

    @Test
    fun testAssertIsSelected() = runComposeUiTest {
        setContent {
            RadioButton(
                selected = true,
                onClick = { },
                modifier = Modifier.testTag("tag1")
            )
            RadioButton(
                selected = false,
                onClick = { },
                modifier = Modifier.testTag("tag2")
            )
        }

        onNodeWithTag("tag1").assertIsSelected()
        assertFails {
            onNodeWithTag("tag2").assertIsSelected()
        }
    }

    @Test
    fun testAssertIsNotSelected() = runComposeUiTest {
        setContent {
            RadioButton(
                selected = true,
                onClick = { },
                modifier = Modifier.testTag("tag1")
            )
            RadioButton(
                selected = false,
                onClick = { },
                modifier = Modifier.testTag("tag2")
            )
        }

        onNodeWithTag("tag2").assertIsNotSelected()
        assertFails {
            onNodeWithTag("tag1").assertIsNotSelected()
        }
    }

    @Test
    fun testAssertIsToggleable() = runComposeUiTest {
        setContent {
            Checkbox(
                checked = false,
                onCheckedChange = { },
                modifier = Modifier.testTag("tag1")
            )
            Text(
                text = "Text",
                modifier = Modifier.testTag("tag2")
            )
        }

        onNodeWithTag("tag1").assertIsToggleable()
        assertFails {
            onNodeWithTag("tag2").assertIsToggleable()
        }
    }

    @Test
    fun testAssertIsSelectable() = runComposeUiTest {
        setContent {
            RadioButton(
                selected = false,
                onClick = { },
                modifier = Modifier.testTag("tag1")
            )
            Text(
                text = "Text",
                modifier = Modifier.testTag("tag2")
            )
        }

        onNodeWithTag("tag1").assertIsSelectable()
        assertFails {
            onNodeWithTag("tag2").assertIsSelectable()
        }
    }

    @Test
    fun testAssertIsFocused() = runComposeUiTest {
        setContent {
            val focusRequester = remember { FocusRequester() }
            Box(
                Modifier
                    .testTag("tag1")
                    .focusRequester(focusRequester)
                    .focusable()
            )
            Box(
                Modifier
                    .testTag("tag2")
                    .focusable()
            )
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }

        onNodeWithTag("tag1").assertIsFocused()
        assertFails {
            onNodeWithTag("tag2").assertIsFocused()
        }
    }

    @Test
    fun testAssertIsNotFocused() = runComposeUiTest {
        setContent {
            val focusRequester = remember { FocusRequester() }
            Box(
                Modifier
                    .testTag("tag1")
                    .focusRequester(focusRequester)
                    .focusable()
            )
            Box(
                Modifier
                    .testTag("tag2")
                    .focusable()
            )
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }

        onNodeWithTag("tag2").assertIsNotFocused()
        assertFails {
            onNodeWithTag("tag1").assertIsNotFocused()
        }
    }

    @Test
    fun testAssertContentDescriptionEquals() = runComposeUiTest {
        setContent {
            Button(
                onClick = {},
                modifier = Modifier.testTag("tag1")
            ) {
                Box(Modifier.semantics { contentDescription = "desc1" })
                Box(Modifier.semantics { contentDescription = "desc2" })
            }
            Button(
                onClick = {},
                modifier = Modifier.testTag("tag2")
            ) {
                Box(Modifier.semantics { contentDescription = "desc1" })
            }
        }

        onNodeWithTag("tag1").assertContentDescriptionEquals("desc1", "desc2")
        assertFails {
            onNodeWithTag("tag2").assertContentDescriptionEquals("desc1", "desc2")
        }
    }

    @Test
    fun testAssertContentDescriptionContains() = runComposeUiTest {
        setContent {
            Button(
                onClick = {},
                modifier = Modifier.testTag("tag1")
            ) {
                Box(Modifier.semantics { contentDescription = "desc1" })
                Box(Modifier.semantics { contentDescription = "desc2" })
            }
            Button(
                onClick = {},
                modifier = Modifier.testTag("tag2")
            ) {
                Box(Modifier.semantics { contentDescription = "desc" })
            }
        }

        onNodeWithTag("tag1").assertContentDescriptionContains("desc1")
        onNodeWithTag("tag1").assertContentDescriptionContains("desc2")
        assertFails {
            onNodeWithTag("tag2").assertContentDescriptionContains("desc1")
        }
    }

    @Test
    fun testAssertTextEquals() = runComposeUiTest {
        setContent {
            Text(
                text = "Hello, Compose",
                modifier = Modifier.testTag("tag1")
            )
            TextField(
                value = "Hello, TextField",
                onValueChange = {},
                modifier = Modifier.testTag("tag2")
            )
        }

        onNodeWithTag("tag1").assertTextEquals("Hello, Compose")
        onNodeWithTag("tag2").assertTextEquals("Hello, TextField")
        assertFails {
            onNodeWithTag("tag1").assertTextEquals("Hello")
        }
    }

    @Test
    fun testAssertTextContains() = runComposeUiTest {
        setContent {
            Button(
                onClick = {},
                modifier = Modifier.testTag("tag")
            ) {
                Text(text = "text1")
                Text(text = "text2")
            }
        }

        onNodeWithTag("tag").assertTextContains("text1")
        onNodeWithTag("tag").assertTextContains("text2")
        assertFails {
            onNodeWithTag("tag").assertTextContains("Hello")
        }
    }

    @Test
    fun testAssertValueEquals() = runComposeUiTest {
        setContent {
            Box(Modifier.semantics { stateDescription = "desc" }.testTag("tag"))
        }

        onNodeWithTag("tag").assertValueEquals("desc")
        assertFails {
            onNodeWithTag("tag").assertValueEquals("text")
        }
    }

    @Test
    fun testAssertRangeInfoEquals() = runComposeUiTest {
        setContent {
            Slider(
                valueRange = 0f..100f,
                value = 50f,
                onValueChange = {},
                modifier = Modifier.testTag("tag")
            )
        }

        onNodeWithTag("tag").assertRangeInfoEquals(
            ProgressBarRangeInfo(
                current = 50f,
                range = 0f..100f
            )
        )
        assertFails {
            onNodeWithTag("tag").assertRangeInfoEquals(
                ProgressBarRangeInfo(
                    current = 49f,
                    range = 0f..100f
                )
            )
        }
    }

    @Test
    fun testAssertHasClickAction() = runComposeUiTest {
        setContent {
            Button(
                onClick = {},
                modifier = Modifier.testTag("tag1")
            ) {}
            Text(
                text = "Hello",
                modifier = Modifier.testTag("tag2")
            )
        }

        onNodeWithTag("tag1").assertHasClickAction()
        assertFails {
            onNodeWithTag("tag2").assertHasClickAction()
        }
    }

    @Test
    fun testAssertHasNoClickAction() = runComposeUiTest {
        setContent {
            Button(
                onClick = {},
                modifier = Modifier.testTag("tag1")
            ) {}
            Text(
                text = "Hello",
                modifier = Modifier.testTag("tag2")
            )
        }

        onNodeWithTag("tag2").assertHasNoClickAction()
        assertFails {
            onNodeWithTag("tag1").assertHasNoClickAction()
        }
    }

    @Test
    fun testAssertAny() = runComposeUiTest {
        setContent {
            Text(
                text = "Hello",
                modifier = Modifier.testTag("tag")
            )
            Text(
                text = "Compose",
                modifier = Modifier.testTag("tag")
            )
        }

        onAllNodesWithTag("tag").assertAny(hasText("Hello"))
        onAllNodesWithTag("tag").assertAny(hasText("Compose"))
        assertFails {
            onAllNodesWithTag("tag").assertAny(hasText("Text"))
        }
    }

    @Test
    fun testAssertAll() = runComposeUiTest {
        setContent {
            Text(
                text = "Hello, World",
                modifier = Modifier.testTag("tag")
            )
            Text(
                text = "Hello, Compose",
                modifier = Modifier.testTag("tag")
            )
        }

        onAllNodesWithTag("tag").assertAll(hasText("Hello", substring = true))
        assertFails {
            onAllNodesWithTag("tag").assertAll(hasText("World", substring = true))
        }
    }
}
