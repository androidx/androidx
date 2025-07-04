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

package androidx.compose.foundation.text.modifiers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.sp
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test

class TextStringContentCaptureTest {

    @get:Rule val rule = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().context

    private fun createSubject(text: String): TextStringSimpleElement {
        return TextStringSimpleElement(text, TextStyle.Default, createFontFamilyResolver(context))
    }

    @Test
    fun whenChangingText_invalidateTranslation() {
        val original = "Ok"
        val current = mutableStateOf(createSubject(original))
        rule.setContent { Box(current.value) }

        val semantics = rule.onNodeWithText(original).fetchSemanticsNode()
        rule.runOnIdle { semantics.translateTo("Foo") }
        val after = "After"
        current.value = createSubject(after)

        val newSemantics = rule.onNodeWithText(after).fetchSemanticsNode()
        rule.runOnIdle { Truth.assertThat(newSemantics.fetchTranslation()).isNull() }
    }

    @Test
    fun text_withInlineContent_doesNotCrash_invalidatesPlaceholders_whenApplyingTranslation() {
        var layoutResult: TextLayoutResult? = null

        rule.setContent {
            BasicText(
                buildAnnotatedString {
                    append("hey")
                    appendInlineContent("inline", alternateText = "00")
                },
                modifier = Modifier.testTag("TAG"),
                onTextLayout = { layoutResult = it },
                inlineContent =
                    mapOf(
                        "inline" to
                            InlineTextContent(
                                placeholder = Placeholder(1.sp, 1.sp, PlaceholderVerticalAlign.Top)
                            ) {}
                    ),
            )
        }

        // show the translated text
        val node = rule.onNodeWithTag("TAG").fetchSemanticsNode()
        rule.runOnIdle {
            val translatedText = buildAnnotatedString { append("hola") }
            node.config[SemanticsActions.SetTextSubstitution].action?.invoke(translatedText)
            node.config[SemanticsActions.ShowTextSubstitution].action?.invoke(true)
        }

        rule.runOnIdle {
            Truth.assertThat(layoutResult).isNotNull()
            Truth.assertThat(layoutResult!!.placeholderRects).isEqualTo(emptyList<Rect?>())
        }
    }
}

private fun SemanticsNode.fetchTranslation(): String? {
    return config.getOrNull(SemanticsProperties.TextSubstitution)?.text
}

private fun SemanticsNode.translateTo(translation: String) {
    config[SemanticsActions.SetTextSubstitution].action?.invoke(AnnotatedString(translation))
}
