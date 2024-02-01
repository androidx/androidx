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

import androidx.compose.ui.graphics.Color
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
import org.junit.Test

class TextAnnotatedStringContentCaptureInvalidationTest {

    private val context = InstrumentationRegistry.getInstrumentation().context
    private fun createSubject(text: AnnotatedString): TextAnnotatedStringNode {
        return TextAnnotatedStringNode(
            text,
            TextStyle.Default,
            createFontFamilyResolver(context),
            null
        )
    }

    @Test
    fun whenChangingText_invalidateTranslation() {
        val original = AnnotatedString("Ok")
        val after = AnnotatedString("kO")
        val subject = createSubject(original)
        subject.addTranslation("A translation goes here")

        subject.updateText(after)
        assertThat(subject.textSubstitution).isNull()
    }

    @Test
    fun whenChangingSpanStyle_noInvalidateTranslation() {
        val original = AnnotatedString("Ok")
        val after = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.Red)) {
                append(original.text)
            }
        }
        val subject = createSubject(original)
        val translation = "A translation goes here"
        subject.addTranslation(translation)
        subject.updateText(after)
        assertThat(subject.textSubstitution?.substitution?.text).isEqualTo(translation)
    }

    @Test
    fun whenChangingParagraphStyle_noInvalidateTranslation() {
        val original = AnnotatedString("Ok")
        val after = buildAnnotatedString {
            withStyle(ParagraphStyle(lineHeight = 1000.sp)) {
                append(original.text)
            }
        }
        val subject = createSubject(original)
        val translation = "A translation goes here"
        subject.addTranslation(translation)
        subject.updateText(after)
        assertThat(subject.textSubstitution?.substitution?.text).isEqualTo(translation)
    }

    @Test
    fun whenChangingAnnotation_noInvalidateTranslation() {
        val original = AnnotatedString("Ok")
        val after = buildAnnotatedString {
            append(original.text)
            addStringAnnotation("some annotation", "annotation", 0, 1)
        }
        val subject = createSubject(original)
        val translation = "A translation goes here"
        subject.addTranslation(translation)
        subject.updateText(after)
        assertThat(subject.textSubstitution?.substitution?.text).isEqualTo(translation)
    }
}

private fun TextAnnotatedStringNode.addTranslation(
    translation: String
): TextAnnotatedStringNode.TextSubstitutionValue? {
    setSubstitution(AnnotatedString(translation))
    return this.textSubstitution
}
