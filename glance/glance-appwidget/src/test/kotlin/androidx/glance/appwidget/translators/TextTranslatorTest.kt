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
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.SpannedString
import android.text.style.AlignmentSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TextAppearanceSpan
import android.text.style.UnderlineSpan
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.glance.Modifier
import androidx.glance.appwidget.applyRemoteViews
import androidx.glance.appwidget.runAndTranslate
import androidx.glance.appwidget.runAndTranslateInRtl
import androidx.glance.appwidget.toPixels
import androidx.glance.layout.Column
import androidx.glance.layout.Text
import androidx.glance.layout.fillMaxWidth
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.sp
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TextTranslatorTest {

    private lateinit var fakeCoroutineScope: TestCoroutineScope
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val displayMetrics = context.resources.displayMetrics

    @Before
    fun setUp() {
        fakeCoroutineScope = TestCoroutineScope()
    }

    @Test
    fun canTranslateText() = fakeCoroutineScope.runBlockingTest {
        val rv = context.runAndTranslate {
            Text("test")
        }
        val view = context.applyRemoteViews(rv)

        assertIs<TextView>(view)
        assertThat(view.text.toString()).isEqualTo("test")
    }

    @Test
    @Config(sdk = [23, 29])
    fun canTranslateText_withStyleWeightAndSize() = fakeCoroutineScope.runBlockingTest {
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
    fun canTranslateText_withStyleStrikeThrough() = fakeCoroutineScope.runBlockingTest {
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
    fun canTranslateText_withStyleUnderline() = fakeCoroutineScope.runBlockingTest {
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
    fun canTranslateText_withStyleItalic() = fakeCoroutineScope.runBlockingTest {
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
    fun canTranslateText_withComplexStyle() = fakeCoroutineScope.runBlockingTest {
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
    fun canTranslateText_withAlignments() = fakeCoroutineScope.runBlockingTest {
        val rv = context.runAndTranslate {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Center", style = TextStyle(textAlign = TextAlign.Center))
                Text("Left", style = TextStyle(textAlign = TextAlign.Left))
                Text("Right", style = TextStyle(textAlign = TextAlign.Right))
                Text("Start", style = TextStyle(textAlign = TextAlign.Start))
                Text("End", style = TextStyle(textAlign = TextAlign.End))
            }
        }
        val view = context.applyRemoteViews(rv)

        assertIs<LinearLayout>(view)
        assertThat(view.childCount).isEqualTo(5)
        val center = assertIs<TextView>(view.getChildAt(0))
        val left = assertIs<TextView>(view.getChildAt(1))
        val right = assertIs<TextView>(view.getChildAt(2))
        val start = assertIs<TextView>(view.getChildAt(3))
        val end = assertIs<TextView>(view.getChildAt(4))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            assertThat(center.gravity).isEqualTo(Gravity.CENTER)
            assertThat(left.gravity).isEqualTo(Gravity.LEFT)
            assertThat(right.gravity).isEqualTo(Gravity.RIGHT)
            assertThat(start.gravity).isEqualTo(Gravity.START)
            assertThat(end.gravity).isEqualTo(Gravity.END)
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
    fun canTranslateText_withAlignmentsInRtl() = fakeCoroutineScope.runBlockingTest {
        val rv = context.runAndTranslateInRtl {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Center", style = TextStyle(textAlign = TextAlign.Center))
                Text("Left", style = TextStyle(textAlign = TextAlign.Left))
                Text("Right", style = TextStyle(textAlign = TextAlign.Right))
                Text("Start", style = TextStyle(textAlign = TextAlign.Start))
                Text("End", style = TextStyle(textAlign = TextAlign.End))
            }
        }
        val view = context.applyRemoteViews(rv)

        assertIs<LinearLayout>(view)
        assertThat(view.childCount).isEqualTo(5)
        val center = assertIs<TextView>(view.getChildAt(0))
        val left = assertIs<TextView>(view.getChildAt(1))
        val right = assertIs<TextView>(view.getChildAt(2))
        val start = assertIs<TextView>(view.getChildAt(3))
        val end = assertIs<TextView>(view.getChildAt(4))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            assertThat(center.gravity).isEqualTo(Gravity.CENTER)
            assertThat(left.gravity).isEqualTo(Gravity.LEFT)
            assertThat(right.gravity).isEqualTo(Gravity.RIGHT)
            assertThat(start.gravity).isEqualTo(Gravity.START)
            assertThat(end.gravity).isEqualTo(Gravity.END)
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