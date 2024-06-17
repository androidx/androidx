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

import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AlignmentSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import androidx.compose.foundation.text.BasicText
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertDoesNotContainColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
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
                add { pushStringAnnotation("foo", "Bar") }
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

            val actual = AnnotatedString.fromHtml(
                stringResource(androidx.compose.ui.text.test.R.string.html)
            )

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
            val actual = AnnotatedString.fromHtml(stringResource(
                androidx.compose.ui.text.test.R.string.formatting,
                "computer"
            ))
            assertThat(actual.text).isEqualTo("Hello, computer!")
            assertThat(actual.spanStyles).containsExactly(
                AnnotatedString.Range(SpanStyle(fontWeight = FontWeight.Bold), 7, 15)
            )
        }
    }

    @Test
    fun annotationTag_withNoText_noStringAnnotation() {
        rule.setContent {
            val actual = AnnotatedString.fromHtml("a<annotation key1=value1></annotation>")

            assertThat(actual.text).isEqualTo("a")
            assertThat(actual.getStringAnnotations(0, actual.length)).isEmpty()
        }
    }

    @Test
    fun annotationTag_withNoAttributes_noStringAnnotation() {
        rule.setContent {
            val actual = AnnotatedString.fromHtml("<annotation>a</annotation>")

            assertThat(actual.text).isEqualTo("a")
            assertThat(actual.getStringAnnotations(0, actual.length)).isEmpty()
        }
    }

    @Test
    fun annotationTag_withOneAttribute_oneStringAnnotation() {
        rule.setContent {
            val actual = AnnotatedString.fromHtml("<annotation key1=value1>a</annotation>")

            assertThat(actual.text).isEqualTo("a")
            assertThat(actual.getStringAnnotations(0, actual.length)).containsExactly(
                AnnotatedString.Range("value1", 0, 1, "key1")
            )
        }
    }

    @Test
    fun annotationTag_withMultipleAttributes_multipleStringAnnotations() {
        rule.setContent {
            val actual = AnnotatedString.fromHtml("""
                <annotation key1="value1" key2=value2 keyThree="valueThree">a</annotation>
            """.trimIndent())

            assertThat(actual.text).isEqualTo("a")
            assertThat(actual.getStringAnnotations(0, actual.length)).containsExactly(
                AnnotatedString.Range("value1", 0, 1, "key1"),
                AnnotatedString.Range("value2", 0, 1, "key2"),
                AnnotatedString.Range("valueThree", 0, 1, "keythree")
            )
        }
    }

    @Test
    fun annotationTag_withMultipleAnnotations_multipleStringAnnotations() {
        rule.setContent {
            val actual = AnnotatedString.fromHtml("""
                <annotation key1=val1>a</annotation>a<annotation key2="val2">a</annotation>
                """.trimIndent())

            assertThat(actual.text).isEqualTo("aaa")
            assertThat(actual.getStringAnnotations(0, actual.length)).containsExactly(
                AnnotatedString.Range("val1", 0, 1, "key1"),
                AnnotatedString.Range("val2", 2, 3, "key2")
            )
        }
    }

    @Test
    fun annotationTag_withOtherTag() {
        rule.setContent {
            val actual = AnnotatedString.fromHtml(
                "<annotation key1=\"value1\">a</annotation><b>a</b>"
            )

            assertThat(actual.text).isEqualTo("aa")
            assertThat(actual.spanStyles).containsExactly(
                AnnotatedString.Range(SpanStyle(fontWeight = FontWeight.Bold), 1, 2),
            )
            assertThat(actual.getStringAnnotations(0, actual.length)).containsExactly(
                AnnotatedString.Range("value1", 0, 1, "key1")
            )
        }
    }

    @Test
    fun annotationTag_wrappedByOtherTag() {
        rule.setContent {
            val actual = AnnotatedString.fromHtml(
                "<b><annotation key1=\"value1\">a</annotation></b>"
            )

            assertThat(actual.text).isEqualTo("a")
            assertThat(actual.spanStyles).containsExactly(
                AnnotatedString.Range(SpanStyle(fontWeight = FontWeight.Bold), 0, 1)
            )
            assertThat(actual.getStringAnnotations(0, actual.length)).containsExactly(
                AnnotatedString.Range("value1", 0, 1, "key1")
            )
        }
    }

    fun verify_alignmentSpan() {
        val expected = buildAnnotatedString {
            withStyle(ParagraphStyle(textAlign = TextAlign.Center)) { append("a") }
        }
        val actual = buildSpannableString(
            AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER)
        ).toAnnotatedString()

        assertThat(actual.text).isEqualTo(expected.text)
        assertThat(actual.paragraphStyles).containsExactlyElementsIn(
            expected.paragraphStyles
        )
    }

    fun verify_backgroundColorSpan() {
        val expected = buildAnnotatedString {
            withStyle(SpanStyle(background = Color.Red)) { append("a") }
        }
        val actual =
            buildSpannableString(BackgroundColorSpan(Color.Red.toArgb())).toAnnotatedString()

        assertThat(actual.text).isEqualTo(expected.text)
        assertThat(actual.spanStyles).containsExactlyElementsIn(expected.spanStyles)
    }

    @Test
    fun verify_foregroundColorSpan() {
        val expected = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.Blue)) { append("a") }
        }
        val actual =
            buildSpannableString(ForegroundColorSpan(Color.Blue.toArgb())).toAnnotatedString()

        assertThat(actual.text).isEqualTo(expected.text)
        assertThat(actual.spanStyles).containsExactlyElementsIn(expected.spanStyles)
    }

    @Test
    fun verify_relativeSizeSpan() {
        val expected = buildAnnotatedString {
            withStyle(SpanStyle(fontSize = 0.6f.em)) { append("a") }
        }
        val actual = buildSpannableString(RelativeSizeSpan(0.6f)).toAnnotatedString()

        assertThat(actual.text).isEqualTo(expected.text)
        assertThat(actual.spanStyles).containsExactlyElementsIn(expected.spanStyles)
    }

    @Test
    fun verify_strikeThroughSpan() {
        val expected = buildAnnotatedString {
            withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append("a") }
        }
        val actual = buildSpannableString(StrikethroughSpan()).toAnnotatedString()

        assertThat(actual.text).isEqualTo(expected.text)
        assertThat(actual.spanStyles).containsExactlyElementsIn(expected.spanStyles)
    }

    @Test
    fun verify_styleSpan() {
        val expected = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                append("a")
            }
        }
        val actual = buildSpannableString(StyleSpan(Typeface.BOLD_ITALIC)).toAnnotatedString()

        assertThat(actual.text).isEqualTo(expected.text)
        assertThat(actual.spanStyles).containsExactlyElementsIn(expected.spanStyles)
    }

    fun verify_subscriptSpan() {
        val expected = buildAnnotatedString {
            withStyle(SpanStyle(baselineShift = BaselineShift.Subscript)) { append("a") }
        }
        val actual = buildSpannableString(SubscriptSpan()).toAnnotatedString()

        assertThat(actual.text).isEqualTo(expected.text)
        assertThat(actual.spanStyles).containsExactlyElementsIn(expected.spanStyles)
    }

    @Test
    fun verify_superScriptSpan() {
        val expected = buildAnnotatedString {
            withStyle(SpanStyle(baselineShift = BaselineShift.Superscript)) { append("a") }
        }
        val actual = buildSpannableString(SuperscriptSpan()).toAnnotatedString()

        assertThat(actual.text).isEqualTo(expected.text)
        assertThat(actual.spanStyles).containsExactlyElementsIn(expected.spanStyles)
    }

    @Test
    fun verify_typefaceSpan() {
        val expected = buildAnnotatedString {
            withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append("a") }
        }
        val actual = buildSpannableString(TypefaceSpan("monospace")).toAnnotatedString()

        assertThat(actual.text).isEqualTo(expected.text)
        assertThat(actual.spanStyles).containsExactlyElementsIn(expected.spanStyles)
    }

    @Test
    fun verify_underlineSpan() {
        val expected = buildAnnotatedString {
            withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { append("a") }
        }
        val actual = buildSpannableString(UnderlineSpan()).toAnnotatedString()

        assertThat(actual.text).isEqualTo(expected.text)
        assertThat(actual.spanStyles).containsExactlyElementsIn(expected.spanStyles)
    }

    @Test
    fun verify_urlSpan() {
        val spannable = SpannableStringBuilder()
        spannable.append("a", URLSpan("url"), Spanned.SPAN_INCLUSIVE_INCLUSIVE)

        val expected = buildAnnotatedString {
            withLink(LinkAnnotation.Url("url", null)) { append("a") }
        }
        assertThat(spannable.toAnnotatedString().text).isEqualTo(expected.text)
        assertThat(spannable.toAnnotatedString().getLinkAnnotations(0, 1))
            .containsExactlyElementsIn(expected.getLinkAnnotations(0, 1))
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun link_appliesColorFromHtmlTag() {
        val stringWithColoredLink = "<span style=\"color:blue\"><a href=\"url\">link</a></span>"
        val annotatedString = AnnotatedString.fromHtml(stringWithColoredLink)

        rule.setContent {
            BasicText(text = annotatedString)
        }

        rule.onNode(hasClickAction(), useUnmergedTree = true)
            .captureToImage()
            .assertContainsColor(Color.Blue)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun link_appliesColorFromMethod() {
        val stringWithColoredLink = "<span style=\"color:blue\"><a href=\"url\">link</a></span>"
        val annotatedString = AnnotatedString.fromHtml(
            stringWithColoredLink,
            TextLinkStyles(SpanStyle(color = Color.Green))
        )

        rule.setContent {
            BasicText(text = annotatedString)
        }

        rule.onNode(hasClickAction(), useUnmergedTree = true)
            .captureToImage()
            .assertContainsColor(Color.Green)
            .assertDoesNotContainColor(Color.Blue)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun link_mergesDecorationFromMethod() {
        val stringWithColoredLink = "<span style=\"color:blue\"><a href=\"url\">link</a></span>"
        val annotatedString = AnnotatedString.fromHtml(
            stringWithColoredLink,
            TextLinkStyles(SpanStyle(background = Color.Red))
        )

        rule.setContent {
            BasicText(text = annotatedString)
        }

        rule.onNode(hasClickAction(), useUnmergedTree = true)
            .captureToImage()
            .assertContainsColor(Color.Blue)
            .assertContainsColor(Color.Red)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun linkAnnotation_constructedFromMethodArguments() {
        val stringWithLink = "<a href=\"url\">link</a>"
        val annotatedString = AnnotatedString.fromHtml(
            stringWithLink,
            TextLinkStyles(
                style = SpanStyle(color = Color.Red),
                focusedStyle = SpanStyle(color = Color.Green),
                hoveredStyle = SpanStyle(color = Color.Blue),
                pressedStyle = SpanStyle(color = Color.Gray),
            ),
            linkInteractionListener = {}
        )

        val link = annotatedString.getLinkAnnotations(0, 4).first().item as LinkAnnotation.Url
        assertThat(link.url).isEqualTo("url")
        assertThat(link.styles?.style).isEqualTo(SpanStyle(color = Color.Red))
        assertThat(link.styles?.focusedStyle).isEqualTo(SpanStyle(color = Color.Green))
        assertThat(link.styles?.hoveredStyle).isEqualTo(SpanStyle(color = Color.Blue))
        assertThat(link.styles?.pressedStyle).isEqualTo(SpanStyle(color = Color.Gray))
        assertThat(link.linkInteractionListener).isNotNull()
    }

    private fun buildSpannableString(span: Any) = SpannableStringBuilder().also {
        it.append("a", span, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
    }
}
