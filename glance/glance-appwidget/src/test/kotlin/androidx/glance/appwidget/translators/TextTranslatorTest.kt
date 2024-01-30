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

package androidx.glance.appwidget.translators

import android.content.Context
import android.content.res.Configuration
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.SpannedString
import android.text.style.AlignmentSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TextAppearanceSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.TextViewSubject.Companion.assertThat
import androidx.glance.appwidget.applyRemoteViews
import androidx.glance.appwidget.configurationContext
import androidx.glance.appwidget.nonGoneChildCount
import androidx.glance.appwidget.nonGoneChildren
import androidx.glance.appwidget.runAndTranslate
import androidx.glance.appwidget.runAndTranslateInRtl
import androidx.glance.appwidget.test.R
import androidx.glance.appwidget.toPixels
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxWidth
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontFamily
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TextTranslatorTest {

    private lateinit var fakeCoroutineScope: TestScope
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val lightContext = configurationContext { uiMode = Configuration.UI_MODE_NIGHT_NO }
    private val darkContext = configurationContext { uiMode = Configuration.UI_MODE_NIGHT_YES }
    private val displayMetrics = context.resources.displayMetrics

    @Before
    fun setUp() {
        fakeCoroutineScope = TestScope()
    }

    @Test
    fun canTranslateText() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Text("test")
        }
        val view = context.applyRemoteViews(rv)

        assertIs<TextView>(view)
        assertThat(view.text.toString()).isEqualTo("test")
    }

    @Test
    @Config(sdk = [23, 29])
    fun canTranslateText_withStyleWeightAndSize() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Text(
                "test",
                style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp),
            )
        }
        val view = context.applyRemoteViews(rv)

        assertIs<TextView>(view)
        assertThat(view.textSize).isEqualTo(12.sp.toPixels(displayMetrics))
        val content = view.text as SpannedString
        assertThat(content.toString()).isEqualTo("test")
        content.checkSingleSpan<TextAppearanceSpan> {
            if (Build.VERSION.SDK_INT >= 29) {
                assertThat(it.textFontWeight).isEqualTo(FontWeight.Medium.value)
                // Note: textStyle is always set, but to NORMAL if unspecified
                assertThat(it.textStyle).isEqualTo(Typeface.NORMAL)
            } else {
                assertThat(it.textStyle).isEqualTo(Typeface.BOLD)
            }
        }
    }

    @Test
    fun canTranslateText_withMonoFontFamily() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Text(
                "test",
                style = TextStyle(fontFamily = FontFamily.Monospace),
            )
        }
        val view = context.applyRemoteViews(rv)

        assertIs<TextView>(view)
        val content = view.text as SpannedString
        assertThat(content.toString()).isEqualTo("test")
        content.checkSingleSpan<TypefaceSpan> { span ->
            assertThat(span.family).isEqualTo("monospace")
        }
    }

    @Test
    fun canTranslateText_withMonoSerifFamily() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Text(
                "test",
                style = TextStyle(fontFamily = FontFamily.Serif),
            )
        }
        val view = context.applyRemoteViews(rv)

        assertIs<TextView>(view)
        val content = view.text as SpannedString
        assertThat(content.toString()).isEqualTo("test")
        content.checkSingleSpan<TypefaceSpan> { span ->
            assertThat(span.family).isEqualTo("serif")
        }
    }

    @Test
    fun canTranslateText_withSansFontFamily() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Text(
                "test",
                style = TextStyle(fontFamily = FontFamily.SansSerif),
            )
        }
        val view = context.applyRemoteViews(rv)

        assertIs<TextView>(view)
        val content = view.text as SpannedString
        assertThat(content.toString()).isEqualTo("test")
        content.checkSingleSpan<TypefaceSpan> { span ->
            assertThat(span.family).isEqualTo("sans-serif")
        }
    }

    @Test
    fun canTranslateText_withCursiveFontFamily() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Text(
                "test",
                style = TextStyle(fontFamily = FontFamily.Cursive),
            )
        }
        val view = context.applyRemoteViews(rv)

        assertIs<TextView>(view)
        val content = view.text as SpannedString
        assertThat(content.toString()).isEqualTo("test")
        content.checkSingleSpan<TypefaceSpan> { span ->
            assertThat(span.family).isEqualTo("cursive")
        }
    }

    @Test
    fun canTranslateText_withCustomFontFamily() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Text(
                "test",
                style = TextStyle(fontFamily = FontFamily("casual")),
            )
        }
        val view = context.applyRemoteViews(rv)

        assertIs<TextView>(view)
        val content = view.text as SpannedString
        assertThat(content.toString()).isEqualTo("test")
        content.checkSingleSpan<TypefaceSpan> { span ->
            assertThat(span.family).isEqualTo("casual")
        }
    }

    @Test
    fun canTranslateText_withStyleStrikeThrough() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Text("test", style = TextStyle(textDecoration = TextDecoration.LineThrough))
        }
        val view = context.applyRemoteViews(rv)

        assertIs<TextView>(view)
        val content = view.text as SpannedString
        assertThat(content.toString()).isEqualTo("test")
        content.checkSingleSpan<StrikethroughSpan> { }
    }

    @Test
    fun canTranslateText_withStyleUnderline() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Text("test", style = TextStyle(textDecoration = TextDecoration.Underline))
        }
        val view = context.applyRemoteViews(rv)

        assertIs<TextView>(view)
        val content = view.text as SpannedString
        assertThat(content.toString()).isEqualTo("test")
        content.checkSingleSpan<UnderlineSpan> { }
    }

    @Test
    fun canTranslateText_withStyleItalic() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Text("test", style = TextStyle(fontStyle = FontStyle.Italic))
        }
        val view = context.applyRemoteViews(rv)

        assertIs<TextView>(view)
        val content = view.text as SpannedString
        assertThat(content.toString()).isEqualTo("test")
        content.checkSingleSpan<StyleSpan> {
            assertThat(it.style).isEqualTo(Typeface.ITALIC)
        }
    }

    @Test
    @Config(sdk = [23, 29])
    fun canTranslateText_withComplexStyle() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Text(
                "test",
                style = TextStyle(
                    textDecoration = TextDecoration.Underline + TextDecoration.LineThrough,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        val view = context.applyRemoteViews(rv)

        assertIs<TextView>(view)
        val content = view.text as SpannedString
        assertThat(content.toString()).isEqualTo("test")
        assertThat(content.getSpans(0, content.length, Any::class.java)).hasLength(4)
        content.checkHasSingleTypedSpan<UnderlineSpan> { }
        content.checkHasSingleTypedSpan<StrikethroughSpan> { }
        content.checkHasSingleTypedSpan<StyleSpan> {
            assertThat(it.style).isEqualTo(Typeface.ITALIC)
        }
        content.checkHasSingleTypedSpan<TextAppearanceSpan> {
            if (Build.VERSION.SDK_INT >= 29) {
                assertThat(it.textFontWeight).isEqualTo(FontWeight.Bold.value)
                // Note: textStyle is always set, but to NORMAL if unspecified
                assertThat(it.textStyle).isEqualTo(Typeface.NORMAL)
            } else {
                assertThat(it.textStyle).isEqualTo(Typeface.BOLD)
            }
        }
    }

    @Test
    fun canTranslateText_withAlignments() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                Text("Center", style = TextStyle(textAlign = TextAlign.Center))
                Text("Left", style = TextStyle(textAlign = TextAlign.Left))
                Text("Right", style = TextStyle(textAlign = TextAlign.Right))
                Text("Start", style = TextStyle(textAlign = TextAlign.Start))
                Text("End", style = TextStyle(textAlign = TextAlign.End))
            }
        }
        val view = context.applyRemoteViews(rv)

        assertIs<LinearLayout>(view)
        assertThat(view.nonGoneChildCount).isEqualTo(5)
        val (center, left, right, start, end) = view.nonGoneChildren.toList()
        assertIs<TextView>(center)
        assertIs<TextView>(left)
        assertIs<TextView>(right)
        assertIs<TextView>(start)
        assertIs<TextView>(end)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            assertThat(center.horizontalGravity).isEqualTo(Gravity.CENTER_HORIZONTAL)
            assertThat(left.horizontalGravity).isEqualTo(Gravity.LEFT)
            assertThat(right.horizontalGravity).isEqualTo(Gravity.RIGHT)
            assertThat(start.horizontalGravity).isEqualTo(Gravity.START)
            assertThat(end.horizontalGravity).isEqualTo(Gravity.END)
        } else {
            assertIs<SpannedString>(center.text).checkSingleSpan<AlignmentSpan.Standard> {
                assertThat(it.alignment).isEqualTo(Layout.Alignment.ALIGN_CENTER)
            }
            assertIs<SpannedString>(left.text).checkSingleSpan<AlignmentSpan.Standard> {
                assertThat(it.alignment).isEqualTo(Layout.Alignment.ALIGN_NORMAL)
            }
            assertIs<SpannedString>(right.text).checkSingleSpan<AlignmentSpan.Standard> {
                assertThat(it.alignment).isEqualTo(Layout.Alignment.ALIGN_OPPOSITE)
            }
            assertIs<SpannedString>(start.text).checkSingleSpan<AlignmentSpan.Standard> {
                assertThat(it.alignment).isEqualTo(Layout.Alignment.ALIGN_NORMAL)
            }
            assertIs<SpannedString>(end.text).checkSingleSpan<AlignmentSpan.Standard> {
                assertThat(it.alignment).isEqualTo(Layout.Alignment.ALIGN_OPPOSITE)
            }
        }
    }

    @Test
    fun canTranslateText_withAlignmentsInRtl() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslateInRtl {
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                Text("Center", style = TextStyle(textAlign = TextAlign.Center))
                Text("Left", style = TextStyle(textAlign = TextAlign.Left))
                Text("Right", style = TextStyle(textAlign = TextAlign.Right))
                Text("Start", style = TextStyle(textAlign = TextAlign.Start))
                Text("End", style = TextStyle(textAlign = TextAlign.End))
            }
        }
        val view = context.applyRemoteViews(rv)

        assertIs<LinearLayout>(view)
        assertThat(view.nonGoneChildCount).isEqualTo(5)
        val (center, left, right, start, end) = view.nonGoneChildren.toList()
        assertIs<TextView>(center)
        assertIs<TextView>(left)
        assertIs<TextView>(right)
        assertIs<TextView>(start)
        assertIs<TextView>(end)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            assertThat(center.horizontalGravity).isEqualTo(Gravity.CENTER_HORIZONTAL)
            assertThat(left.horizontalGravity).isEqualTo(Gravity.LEFT)
            assertThat(right.horizontalGravity).isEqualTo(Gravity.RIGHT)
            assertThat(start.horizontalGravity).isEqualTo(Gravity.START)
            assertThat(end.horizontalGravity).isEqualTo(Gravity.END)
        } else {
            assertIs<SpannedString>(center.text).checkSingleSpan<AlignmentSpan.Standard> {
                assertThat(it.alignment).isEqualTo(Layout.Alignment.ALIGN_CENTER)
            }
            assertIs<SpannedString>(left.text).checkSingleSpan<AlignmentSpan.Standard> {
                assertThat(it.alignment).isEqualTo(Layout.Alignment.ALIGN_OPPOSITE)
            }
            assertIs<SpannedString>(right.text).checkSingleSpan<AlignmentSpan.Standard> {
                assertThat(it.alignment).isEqualTo(Layout.Alignment.ALIGN_NORMAL)
            }
            assertIs<SpannedString>(start.text).checkSingleSpan<AlignmentSpan.Standard> {
                assertThat(it.alignment).isEqualTo(Layout.Alignment.ALIGN_NORMAL)
            }
            assertIs<SpannedString>(end.text).checkSingleSpan<AlignmentSpan.Standard> {
                assertThat(it.alignment).isEqualTo(Layout.Alignment.ALIGN_OPPOSITE)
            }
        }
    }

    @Test
    fun canTranslateText_withColor_fixed() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Column {
                Text("Blue", style = TextStyle(color = ColorProvider(Color.Blue)))
                Text("Red", style = TextStyle(color = ColorProvider(Color.Red)))
            }
        }
        val view = context.applyRemoteViews(rv)

        assertIs<LinearLayout>(view)
        assertThat(view.nonGoneChildCount).isEqualTo(2)

        val (blue, red) = view.nonGoneChildren.toList()
        assertIs<TextView>(blue)
        assertIs<TextView>(red)
        assertThat(blue).hasTextColor(android.graphics.Color.BLUE)
        assertThat(red).hasTextColor(android.graphics.Color.RED)
    }

    @Config(minSdk = 29)
    @Test
    fun canTranslateText_withColor_resource_light() = fakeCoroutineScope.runTest {
        val rv = lightContext.runAndTranslate {
            Text("GrayResource", style = TextStyle(color = ColorProvider(R.color.my_color)))
        }
        val view = lightContext.applyRemoteViews(rv)

        assertIs<TextView>(view)
        assertThat(view).hasTextColor("#EEEEEE")
    }

    @Config(minSdk = 29)
    @Test
    fun canTranslateText_withColor_resource_dark() = fakeCoroutineScope.runTest {
        val rv = darkContext.runAndTranslate {
            Text("GrayResource", style = TextStyle(color = ColorProvider(R.color.my_color)))
        }
        val view = darkContext.applyRemoteViews(rv)

        assertIs<TextView>(view)
        assertThat(view).hasTextColor("#111111")
    }

    @Config(minSdk = 29)
    @Test
    fun canTranslateText_withColor_dayNight_light() = fakeCoroutineScope.runTest {
        val rv = lightContext.runAndTranslate {
            Text(
                "Green day / Magenta night",
                style = TextStyle(color = ColorProvider(day = Color.Green, night = Color.Magenta))
            )
        }
        val view = lightContext.applyRemoteViews(rv)

        assertIs<TextView>(view)
        assertThat(view).hasTextColor(android.graphics.Color.GREEN)
    }

    @Config(minSdk = 29)
    @Test
    fun canTranslateText_withColor_dayNight_dark() = fakeCoroutineScope.runTest {
        val rv = darkContext.runAndTranslate {
            Text(
                "Green day / Magenta night",
                style = TextStyle(color = ColorProvider(day = Color.Green, night = Color.Magenta))
            )
        }
        val view = darkContext.applyRemoteViews(rv)

        assertIs<TextView>(view)
        assertThat(view).hasTextColor(android.graphics.Color.MAGENTA)
    }

    @Test
    fun canTranslateText_withMaxLines() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Text("Max line is set", maxLines = 5)
        }
        val view = context.applyRemoteViews(rv)

        assertIs<TextView>(view)
        assertThat(view.maxLines).isEqualTo(5)
    }

    @Test
    fun canTranslateTextWithSemanticsModifier_contentDescription() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Text(
                text = "Max line is set",
                maxLines = 5,
                modifier = GlanceModifier.semantics {
                    contentDescription = "Custom text description"
                },
            )
        }
        val view = context.applyRemoteViews(rv)

        assertIs<TextView>(view)
        assertThat(view.contentDescription).isEqualTo("Custom text description")
    }

    private val TextView.horizontalGravity
        get() = this.gravity and Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK

    // Check there is a single span, that it's of the correct type and passes the [check].
    private inline fun <reified T> SpannedString.checkSingleSpan(check: (T) -> Unit) {
        val spans = getSpans(0, length, Any::class.java)
        assertThat(spans).hasLength(1)
        checkInstance(spans[0], check)
    }

    // Check there is a single span of the given type and that it passes the [check].
    private inline fun <reified T> SpannedString.checkHasSingleTypedSpan(check: (T) -> Unit) {
        val spans = getSpans(0, length, T::class.java)
        assertThat(spans).hasLength(1)
        check(spans[0])
    }

    private inline fun <reified T> checkInstance(obj: Any, check: (T) -> Unit) {
        assertIs<T>(obj)
        check(obj)
    }
}
