/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.test.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.semantics.inputText
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.text
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.AnnotatedString

@Sampled
fun assertContentDescriptionContainsSample() {
    composeTestRule.setContent {
        // Explicitly merging descendants to demonstrate list semantics
        Row(Modifier.semantics(mergeDescendants = true) { testTag = "iconRow" }) {
            Icon(ColorPainter(Color.Red), contentDescription = "Navigate Up")
            Icon(ColorPainter(Color.Yellow), contentDescription = "Go Home")
        }
    }

    // The merged content description list is: ["Navigate Up", "Go Home"]

    // "Navigate Up" is an exact match for one of the items in the list.
    composeTestRule.onNodeWithTag("iconRow").assertContentDescriptionContains("Navigate Up")

    // "Navigate" is a substring of an item in the list, and we explicitly enable substring
    // matching.
    composeTestRule
        .onNodeWithTag("iconRow")
        .assertContentDescriptionContains("Navigate", substring = true)
}

@Sampled
fun assertContentDescriptionEqualsSample() {
    composeTestRule.setContent {
        // Explicitly merging descendants to demonstrate list semantics
        Row(Modifier.semantics(mergeDescendants = true) { testTag = "iconRow" }) {
            Icon(ColorPainter(Color.Red), contentDescription = "Navigate Up")
            Icon(ColorPainter(Color.Yellow), contentDescription = "Go Home")
        }
    }

    // The merged content description list is: ["Navigate Up", "Go Home"]

    // We provide all items exactly as they appear in the merged list.
    // Order does not matter.
    composeTestRule
        .onNodeWithTag("iconRow")
        .assertContentDescriptionEquals("Go Home", "Navigate Up")
}

@Sampled
fun assertTextContainsSample() {
    composeTestRule.setContent {
        // Explicitly merging descendants to demonstrate list semantics
        Row(Modifier.semantics(mergeDescendants = true) { testTag = "textRow" }) {
            Text("Hello")
            Text("World")
        }
    }

    // The merged text list is: ["Hello", "World"]

    // "Hello" is an exact match for one of the items in the list.
    composeTestRule.onNodeWithTag("textRow").assertTextContains("Hello")

    // "Hel" is a substring of an item in the list, and we explicitly enable substring matching.
    composeTestRule.onNodeWithTag("textRow").assertTextContains("Hel", substring = true)
}

@Sampled
fun assertTextEqualsSample() {
    composeTestRule.setContent {
        // Explicitly merging descendants to demonstrate list semantics
        Row(Modifier.semantics(mergeDescendants = true) { testTag = "textRow" }) {
            Text("Hello")
            Text("World")
        }
    }

    // The merged text list is: ["Hello", "World"]

    // We provide all items exactly as they appear in the merged list.
    // Order does not matter.
    composeTestRule.onNodeWithTag("textRow").assertTextEquals("World", "Hello")
}

@Sampled
fun assertTextEqualsWithInputTextSample() {
    composeTestRule.setContent {
        // Explicitly merging descendants to demonstrate list semantics
        Row(Modifier.semantics(mergeDescendants = true) { testTag = "textRow" }) {
            Text("First Name:")
            Text("Compose")
        }

        // Simulating a password field where the visual text is masked,
        // but the raw user input is stored in InputText.
        Box(
            Modifier.semantics {
                text = AnnotatedString("(000) 123-4567")
                inputText = AnnotatedString("0001234567")
                testTag = "phoneField"
            }
        )
    }

    // The merged text list is: ["First Name:", "Compose"]

    // We provide all items exactly as they appear in the merged list.
    composeTestRule.onNodeWithTag("textRow").assertTextEquals("Compose", "First Name:")

    // By default, assertTextEquals evaluates Text (and EditableText),
    // but ignores InputText. The evaluated list is simply ["(000) 123-4567"].
    composeTestRule.onNodeWithTag("phoneField").assertTextEquals("(000) 123-4567")

    // We explicitly opt in to evaluating the InputText property.
    composeTestRule
        .onNodeWithTag("phoneField")
        .assertTextEquals("0001234567", "(000) 123-4567", includeInputText = true)
}
