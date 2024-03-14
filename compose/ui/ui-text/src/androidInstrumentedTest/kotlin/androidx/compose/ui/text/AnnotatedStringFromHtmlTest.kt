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

package androidx.compose.ui.text

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class AnnotatedStringFromHtmlTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    // pre-N block-level elements were separated with two new lines
    @SdkSuppress(minSdkVersion = 24)
    fun buildAnnotatedString_fromHtml() {
        rule.setContent {
            val expected = buildAnnotatedString {
                fun add(block: () -> Unit) {
                    block()
                    append("a")
                    pop()
                    append(" ")
                }
                fun addStyle(style: SpanStyle) {
                    add { pushStyle(style) }
                }

                add { pushLink(LinkAnnotation.Url("https://example.com")) }
                addStyle(SpanStyle(fontWeight = FontWeight.Bold))
                addStyle(SpanStyle(fontSize = 1.25.em))
                append("\na\n") // <div>
                addStyle(SpanStyle(fontFamily = FontFamily.Serif))
                addStyle(SpanStyle(color = Color.Green))
                addStyle(SpanStyle(fontStyle = FontStyle.Italic))
                append("\na\n") // <p>
                addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                addStyle(SpanStyle(fontSize = 0.8.em))
                addStyle(SpanStyle(background = Color.Red))
                addStyle(SpanStyle(baselineShift = BaselineShift.Subscript))
                addStyle(SpanStyle(baselineShift = BaselineShift.Superscript))
                addStyle(SpanStyle(fontFamily = FontFamily.Monospace))
                addStyle(SpanStyle(textDecoration = TextDecoration.Underline))
            }

            val actual = stringResource(androidx.compose.ui.text.test.R.string.html).parseAsHtml()

            assertThat(actual.text).isEqualTo(expected.text)
            assertThat(actual.spanStyles).containsExactlyElementsIn(expected.spanStyles).inOrder()
            assertThat(actual.paragraphStyles)
                .containsExactlyElementsIn(expected.paragraphStyles)
                .inOrder()
            assertThat(actual.getStringAnnotations(0, actual.length))
                .containsExactlyElementsIn(expected.getStringAnnotations(0, expected.length))
                .inOrder()
            assertThat(actual.getLinkAnnotations(0, actual.length))
                .containsExactlyElementsIn(expected.getLinkAnnotations(0, expected.length))
                .inOrder()
        }
    }

    @Test
    fun formattedString_withStyling() {
        rule.setContent {
            val actual = stringResource(
                androidx.compose.ui.text.test.R.string.formatting,
                "computer"
            ).parseAsHtml()
            assertThat(actual.text).isEqualTo("Hello, computer!")
            assertThat(actual.spanStyles).containsExactly(
                AnnotatedString.Range(SpanStyle(fontWeight = FontWeight.Bold), 7, 15)
            )
        }
    }
}
