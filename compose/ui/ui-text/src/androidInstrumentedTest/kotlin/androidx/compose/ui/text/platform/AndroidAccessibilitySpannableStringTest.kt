/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.text.platform

import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.LocaleSpan
import android.text.style.RelativeSizeSpan
import android.text.style.ScaleXSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TtsSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.UncachedFontFamilyResolver
import androidx.compose.ui.text.UrlAnnotation
import androidx.compose.ui.text.VerbatimTtsAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.font.testutils.AsyncTestTypefaceLoader
import androidx.compose.ui.text.font.testutils.BlockingFauxFont
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.matchers.assertThat
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(InternalTextApi::class, ExperimentalTextApi::class)
@RunWith(AndroidJUnit4::class)
@SmallTest
class AndroidAccessibilitySpannableStringTest {
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val density = Density(context)
    private val fontFamilyResolver = UncachedFontFamilyResolver(context)
    private val urlSpanCache = URLSpanCache()

    @Test
    fun toAccessibilitySpannableString_with_locale() {
        val languageTag = "en-GB"
        val annotatedString = buildAnnotatedString {
            append("hello")
            withStyle(style = SpanStyle(localeList = LocaleList(languageTag))) {
                append("world")
            }
        }

        val spannableString = annotatedString.toAccessibilitySpannableString(
            density,
            fontFamilyResolver,
            urlSpanCache
        )

        assertThat(spannableString).isInstanceOf(SpannableString::class.java)
        assertThat(spannableString).hasSpan(
            LocaleSpan::class, 5, 10
        ) {
            it.locale?.language == languageTag
            true
        }
    }

    @Test
    fun toAccessibilitySpannableString_with_color() {
        val color = Color.Black
        val annotatedString = buildAnnotatedString {
            append("hello")
            withStyle(style = SpanStyle(color = color)) {
                append("world")
            }
        }

        val spannableString = annotatedString.toAccessibilitySpannableString(
            density,
            fontFamilyResolver,
            urlSpanCache
        )

        assertThat(spannableString).isInstanceOf(SpannableString::class.java)
        assertThat(spannableString).hasSpan(
            ForegroundColorSpan::class, 5, 10
        ) {
            it.foregroundColor == color.toArgb()
        }
    }

    @Test
    fun toAccessibilitySpannableString_with_fontSizeInSp() {
        val fontSize = 12.sp
        val annotatedString = buildAnnotatedString {
            append("hello")
            withStyle(style = SpanStyle(fontSize = fontSize)) {
                append("world")
            }
        }

        val spannableString = annotatedString.toAccessibilitySpannableString(
            density,
            fontFamilyResolver,
            urlSpanCache
        )

        assertThat(spannableString).isInstanceOf(SpannableString::class.java)
        assertThat(spannableString).hasSpan(
            AbsoluteSizeSpan::class, 5, 10
        ) {
            it.size == with(density) { fontSize.roundToPx() }
        }
    }

    @Test
    fun toAccessibilitySpannableString_with_fontSizeInEm() {
        val fontSize = 2.em
        val annotatedString = buildAnnotatedString {
            append("hello")
            withStyle(style = SpanStyle(fontSize = fontSize)) {
                append("world")
            }
        }

        val spannableString = annotatedString.toAccessibilitySpannableString(
            density,
            fontFamilyResolver,
            urlSpanCache
        )

        assertThat(spannableString).isInstanceOf(SpannableString::class.java)
        assertThat(spannableString).hasSpan(
            RelativeSizeSpan::class, 5, 10
        ) {
            it.sizeChange == fontSize.value
        }
    }

    @Test
    fun toAccessibilitySpannableString_with_fontWeightBold() {
        val annotatedString = buildAnnotatedString {
            append("hello")
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append("world")
            }
        }

        val spannableString = annotatedString.toAccessibilitySpannableString(
            density,
            fontFamilyResolver,
            urlSpanCache
        )

        assertThat(spannableString).isInstanceOf(SpannableString::class.java)
        assertThat(spannableString).hasSpan(
            StyleSpan::class, 5, 10
        ) {
            it.style == Typeface.BOLD
        }
    }

    @Test
    fun toAccessibilitySpannableString_with_italic() {
        val annotatedString = buildAnnotatedString {
            append("hello")
            withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                append("world")
            }
        }

        val spannableString = annotatedString.toAccessibilitySpannableString(
            density,
            fontFamilyResolver,
            urlSpanCache
        )

        assertThat(spannableString).isInstanceOf(SpannableString::class.java)
        assertThat(spannableString).hasSpan(
            StyleSpan::class, 5, 10
        ) {
            it.style == Typeface.ITALIC
        }
    }

    @Test
    fun toAccessibilitySpannableString_with_fontFamily() {
        val fontFamily = FontFamily.Monospace
        val annotatedString = buildAnnotatedString {
            append("hello")
            withStyle(style = SpanStyle(fontFamily = fontFamily)) {
                append("world")
            }
        }

        val spannableString = annotatedString.toAccessibilitySpannableString(
            density,
            fontFamilyResolver,
            urlSpanCache
        )

        assertThat(spannableString).isInstanceOf(SpannableString::class.java)
        assertThat(
            spannableString.getSpans(
                0,
                spannableString.length,
                TypefaceSpan::class.java
            )
        ).isEmpty()
    }

    @Test
    fun toAccessibilitySpannableString_with_underline() {
        val annotatedString = buildAnnotatedString {
            append("hello")
            withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                append("world")
            }
        }

        val spannableString = annotatedString.toAccessibilitySpannableString(
            density,
            fontFamilyResolver,
            urlSpanCache
        )

        assertThat(spannableString).isInstanceOf(SpannableString::class.java)
        assertThat(spannableString).hasSpan(
            UnderlineSpan::class, 5, 10
        )
    }

    @Test
    fun toAccessibilitySpannableString_with_lineThrough() {
        val annotatedString = buildAnnotatedString {
            append("hello")
            withStyle(style = SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                append("world")
            }
        }

        val spannableString = annotatedString.toAccessibilitySpannableString(
            density,
            fontFamilyResolver,
            urlSpanCache
        )

        assertThat(spannableString).isInstanceOf(SpannableString::class.java)
        assertThat(spannableString).hasSpan(
            StrikethroughSpan::class, 5, 10
        )
    }

    @Test
    fun toAccessibilitySpannableString_with_scaleX() {
        val scaleX = 1.2f
        val annotatedString = buildAnnotatedString {
            append("hello")
            withStyle(
                style = SpanStyle(textGeometricTransform = TextGeometricTransform(scaleX = scaleX))
            ) {
                append("world")
            }
        }

        val spannableString = annotatedString.toAccessibilitySpannableString(
            density,
            fontFamilyResolver,
            urlSpanCache
        )

        assertThat(spannableString).isInstanceOf(SpannableString::class.java)
        assertThat(spannableString).hasSpan(
            ScaleXSpan::class, 5, 10
        ) {
            it.scaleX == scaleX
        }
    }

    @Test
    fun toAccessibilitySpannableString_with_background() {
        val backgroundColor = Color.Red
        val annotatedString = buildAnnotatedString {
            append("hello")
            withStyle(style = SpanStyle(background = backgroundColor)) {
                append("world")
            }
        }

        val spannableString = annotatedString.toAccessibilitySpannableString(
            density,
            fontFamilyResolver,
            urlSpanCache
        )

        assertThat(spannableString).isInstanceOf(SpannableString::class.java)
        assertThat(spannableString).hasSpan(
            BackgroundColorSpan::class, 5, 10
        ) {
            it.backgroundColor == backgroundColor.toArgb()
        }
    }

    @Test
    fun toAccessibilitySpannableString_with_verbatimTtsAnnotation() {
        val annotatedString = buildAnnotatedString {
            append("hello")
            withAnnotation(VerbatimTtsAnnotation("verbatim")) {
                append("world")
            }
        }

        val spannableString = annotatedString.toAccessibilitySpannableString(
            density,
            fontFamilyResolver,
            urlSpanCache
        )

        assertThat(spannableString).isInstanceOf(SpannableString::class.java)
        assertThat(spannableString).hasSpan(
            TtsSpan::class, 5, 10
        ) {
            it.type == TtsSpan.TYPE_VERBATIM &&
                it.args.getString(TtsSpan.ARG_VERBATIM) == "verbatim"
        }
    }

    @Test
    fun toAccessibilitySpannableString_with_urlAnnotation() {
        val annotatedString = buildAnnotatedString {
            append("hello")
            withAnnotation(UrlAnnotation("http://url.com")) {
                append("world")
            }
        }

        val spannableString = annotatedString.toAccessibilitySpannableString(
            density,
            fontFamilyResolver,
            urlSpanCache
        )

        assertThat(spannableString).isInstanceOf(SpannableString::class.java)
        assertThat(spannableString).hasSpan(
            URLSpan::class, 5, 10
        ) {
            it.url == "http://url.com"
        }
    }

    @Test
    fun fontsInSpanStyles_areIgnored() {
        // b/232238615
        val loader = AsyncTestTypefaceLoader()
        val font = BlockingFauxFont(loader, Typeface.MONOSPACE)
        val string = buildAnnotatedString {
            pushStyle(SpanStyle(fontFamily = font.toFontFamily()))
            append("crashes if b/232238615 not fixed")
        }
        val density = Density(1f, 1f)
        val context = InstrumentationRegistry.getInstrumentation().context
        val fontFamilyResolver = createFontFamilyResolver(context)

        // see if font span is added
        string.toAccessibilitySpannableString(density, fontFamilyResolver, URLSpanCache())

        // toAccessibilitySpannableString should _not_ make any font requests
        assertThat(loader.blockingRequests).isEmpty()
    }

    /**
     * A11y services rely on ClickableSpans being the same instance for the same source spans across
     * multiple calls of [toAccessibilitySpannableString] for the same source string. If this is not
     * the case, their onClick handler will not be invoked. See b/253292081.
     */
    @Test
    fun urlSpansAreSameInstanceForSameAnnotatedString() {
        val string = buildAnnotatedString {
            pushUrlAnnotation(UrlAnnotation("google.com"))
            append("link")
        }

        val spannable1 = string.toAccessibilitySpannableString(
            density, fontFamilyResolver, urlSpanCache
        )
        val spannable2 = string.toAccessibilitySpannableString(
            density, fontFamilyResolver, urlSpanCache
        )
        val urlSpan1 = spannable1.getSpans(0, string.length, URLSpan::class.java).single()
        val urlSpan2 = spannable2.getSpans(0, string.length, URLSpan::class.java).single()

        assertThat(spannable1).isNotSameInstanceAs(spannable2)
        assertThat(urlSpan1).isSameInstanceAs(urlSpan2)
    }

    @Test
    fun urlSpansSame_forSameAnnotationAndRange() {
        val link = LinkAnnotation.Url("url")

        val string = buildAnnotatedString {
            pushLink(link)
            append("link")
        }

        val spannable1 = string.toAccessibilitySpannableString(
            density, fontFamilyResolver, urlSpanCache
        )
        val spannable2 = string.toAccessibilitySpannableString(
            density, fontFamilyResolver, urlSpanCache
        )
        val urlSpan1 = spannable1.getSpans(0, string.length, URLSpan::class.java).single()
        val urlSpan2 = spannable2.getSpans(0, string.length, URLSpan::class.java).single()

        assertThat(spannable1).isNotSameInstanceAs(spannable2)
        assertThat(urlSpan1).isSameInstanceAs(urlSpan2)
    }

    @Test
    fun clickableSpansSame_forSameAnnotationAndRange_forUrlsWithCallback() {
        val link = LinkAnnotation.Url("url", linkInteractionListener = {})

        val string = buildAnnotatedString {
            pushLink(link)
            append("link")
        }

        val spannable1 = string.toAccessibilitySpannableString(
            density, fontFamilyResolver, urlSpanCache
        )
        val spannable2 = string.toAccessibilitySpannableString(
            density, fontFamilyResolver, urlSpanCache
        )
        val span1 = spannable1.getSpans(0, string.length, ClickableSpan::class.java).single()
        val span2 = spannable2.getSpans(0, string.length, ClickableSpan::class.java).single()

        assertThat(spannable1).isNotSameInstanceAs(spannable2)
        assertThat(span1).isSameInstanceAs(span2)
    }

    @Test
    fun clickableSpansSame_forSameAnnotationAndRange_forClickables() {
        val link = LinkAnnotation.Clickable("Tag", linkInteractionListener = { })

        val string = buildAnnotatedString {
            pushLink(link)
            append("clickable")
        }

        val spannable1 = string.toAccessibilitySpannableString(
            density, fontFamilyResolver, urlSpanCache
        )
        val spannable2 = string.toAccessibilitySpannableString(
            density, fontFamilyResolver, urlSpanCache
        )
        val span1 = spannable1.getSpans(0, string.length, ClickableSpan::class.java).single()
        val span2 = spannable2.getSpans(0, string.length, ClickableSpan::class.java).single()

        assertThat(spannable1).isNotSameInstanceAs(spannable2)
        assertThat(span1).isSameInstanceAs(span2)
    }

    @Test
    fun clickableSpansDifferent_forSameAnnotationButDifferentRange_forUrlsWithCallback() {
        val link = LinkAnnotation.Url("url", linkInteractionListener = {})

        val string = buildAnnotatedString {
            withLink(link) { append("link") }
            withLink(link) { append("link") }
        }

        val spannable = string.toAccessibilitySpannableString(
            density, fontFamilyResolver, urlSpanCache
        )
        val span1 = spannable.getSpans(0, string.length, ClickableSpan::class.java)[0]
        val span2 = spannable.getSpans(0, string.length, ClickableSpan::class.java)[1]

        assertThat(span1).isNotSameInstanceAs(span2)
    }

    @Test
    fun clickableSpansDifferent_forSameAnnotatedStringWithDifferentRange_forClickables() {
        val link = LinkAnnotation.Clickable("tag", linkInteractionListener = {})

        val string = buildAnnotatedString {
            withLink(link) { append("link") }
            withLink(link) { append("link") }
        }

        val spannable = string.toAccessibilitySpannableString(
            density, fontFamilyResolver, urlSpanCache
        )
        val span1 = spannable.getSpans(0, string.length, ClickableSpan::class.java)[0]
        val span2 = spannable.getSpans(0, string.length, ClickableSpan::class.java)[1]

        assertThat(span1).isNotSameInstanceAs(span2)
    }
}
