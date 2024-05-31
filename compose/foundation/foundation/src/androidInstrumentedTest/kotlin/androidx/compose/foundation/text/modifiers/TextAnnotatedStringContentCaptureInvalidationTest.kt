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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class TextAnnotatedStringContentCaptureInvalidationTest {

    @get:Rule val rule = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().context

    private fun createSubject(text: AnnotatedString): TextAnnotatedStringElement {
        return TextAnnotatedStringElement(
            text,
            TextStyle.Default,
            createFontFamilyResolver(context),
            null
        )
    }

    @Test
    fun whenChangingText_invalidateTranslation() {
        val original = AnnotatedString("Ok")
        val current = mutableStateOf(createSubject(original))
        rule.setContent { Box(current.value) }

        val semantics = rule.onNodeWithText("Ok").fetchSemanticsNode()
        rule.runOnIdle { semantics.translateTo("Foo") }
        val after = AnnotatedString("After")
        current.value = createSubject(after)

        val newSemantics = rule.onNodeWithText(after.text).fetchSemanticsNode()
        rule.runOnIdle { assertThat(newSemantics.fetchTranslation()).isNull() }
    }

    @Test
    fun whenChangingSpanStyle_noInvalidateTranslation() {
        val original = AnnotatedString("Ok")
        val after = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.Red)) { append(original.text) }
        }

        val current = mutableStateOf(createSubject(original))

        rule.setContent { Box(current.value) }
        val translation = "A translation goes here"
        val node = rule.onNodeWithText(original.text).fetchSemanticsNode()
        rule.runOnIdle { node.translateTo(translation) }
        current.value = createSubject(after)

        val node2 = rule.onNodeWithText(after.text).fetchSemanticsNode()
        rule.runOnIdle { assertThat(node2.fetchTranslation()).isEqualTo(translation) }
    }

    @Test
    fun whenChangingParagraphStyle_noInvalidateTranslation() {
        val original = AnnotatedString("Ok")
        val after = buildAnnotatedString {
            withStyle(ParagraphStyle(lineHeight = 1000.sp)) { append(original.text) }
        }
        val current = mutableStateOf(createSubject(original))

        rule.setContent { Box(current.value) }
        val translation = "A translation goes here"
        val node = rule.onNodeWithText(original.text).fetchSemanticsNode()
        rule.runOnIdle { node.translateTo(translation) }
        current.value = createSubject(after)

        val node2 = rule.onNodeWithText(after.text).fetchSemanticsNode()
        rule.runOnIdle { assertThat(node2.fetchTranslation()).isEqualTo(translation) }
    }

    @Test
    fun whenChangingAnnotation_noInvalidateTranslation() {
        val original = AnnotatedString("Ok")
        val after = buildAnnotatedString {
            append(original.text)
            addStringAnnotation("some annotation", "annotation", 0, 1)
        }
        val current = mutableStateOf(createSubject(original))

        rule.setContent { Box(current.value) }
        val translation = "A translation goes here"
        val node = rule.onNodeWithText(original.text).fetchSemanticsNode()
        rule.runOnIdle { node.translateTo(translation) }
        current.value = createSubject(after)

        val node2 = rule.onNodeWithText(after.text).fetchSemanticsNode()
        rule.runOnIdle { assertThat(node2.fetchTranslation()).isEqualTo(translation) }
    }
}

private fun SemanticsNode.fetchTranslation(): String? {
    return config.getOrNull(SemanticsProperties.TextSubstitution)?.text
}

private fun SemanticsNode.translateTo(translation: String) {
    config[SemanticsActions.SetTextSubstitution].action?.invoke(AnnotatedString(translation))
}
