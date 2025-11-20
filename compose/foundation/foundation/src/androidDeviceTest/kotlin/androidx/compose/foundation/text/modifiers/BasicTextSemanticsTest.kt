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

package androidx.compose.foundation.text.modifiers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.hasContentDescriptionExactly
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class BasicTextSemanticsTest {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @Test
    fun semanticsTextChanges_String() {
        var text by mutableStateOf("before")
        rule.setContent { BasicText(text) }
        rule.onNodeWithText("before").assertExists()
        text = "after"
        rule.onNodeWithText("after").assertExists()
    }

    @Test
    fun semanticsTextChanges_AnnotatedString() {
        var text by mutableStateOf("before")
        rule.setContent { BasicText(AnnotatedString(text)) }
        rule.onNodeWithText("before").assertExists()
        text = "after"
        rule.onNodeWithText("after").assertExists()
    }

    // regression test for b/376479686
    @Test
    fun inlineContentSemantics_matchesInMergedSemantics() {
        val testContentDescription = "Red Box"

        rule.setContent {
            val id = "inline"
            val inlineContent =
                InlineTextContent(Placeholder(16.sp, 16.sp, PlaceholderVerticalAlign.Center)) {
                    Box(
                        modifier =
                            Modifier.semantics {
                                    this.contentDescription = testContentDescription
                                    this.role = Role.Image
                                }
                                .background(Color.Red)
                                .fillMaxSize()
                    )
                }
            val inlineContentMap = mapOf(id to inlineContent)
            val text = buildAnnotatedString {
                append("before text - ")
                appendInlineContent(id)
                append(" - after text")
            }
            BasicText(text, inlineContent = inlineContentMap)
        }

        rule
            .onNode(
                matcher = hasContentDescriptionExactly(testContentDescription),
                // This test is specifically for talkback functionality,
                // so using the merged tree that is seen in prod is required.
                // Do not change this to true, even if the test failure recommends it.
                useUnmergedTree = false,
            )
            .assertExists()
    }
}
