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

package androidx.glance.appwidget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Typeface
import android.os.Build
import android.text.SpannedString
import android.text.TextUtils
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TextAppearanceSpan
import android.text.style.UnderlineSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.RelativeLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.glance.GlanceInternalApi
import androidx.glance.Modifier
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.FontStyle
import androidx.glance.layout.FontWeight
import androidx.glance.layout.Text
import androidx.glance.layout.TextDecoration
import androidx.glance.layout.TextStyle
import androidx.glance.layout.absolutePadding
import androidx.glance.layout.padding
import androidx.glance.unit.Dp
import androidx.glance.unit.Sp
import androidx.glance.unit.dp
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
import java.util.Locale
import kotlin.math.floor
import kotlin.test.assertIs

@OptIn(GlanceInternalApi::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RemoteViewsTranslatorKtTest {

    private lateinit var fakeCoroutineScope: TestCoroutineScope
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val displayMetrics = context.resources.displayMetrics

    @Before
    fun setUp() {
        fakeCoroutineScope = TestCoroutineScope()
    }

    @Test
    fun canTranslateBox() = fakeCoroutineScope.runBlockingTest {
        val rv = runAndTranslate { Box {} }
        val view = context.applyRemoteViews(rv)

        assertIs<RelativeLayout>(view)
        assertThat(view.childCount).isEqualTo(0)
    }

    @Test
    fun canTranslateBoxWithAlignment() = fakeCoroutineScope.runBlockingTest {
        val rv = runAndTranslate {
            Box(contentAlignment = Alignment.BottomEnd) { }
        }
        val view = context.applyRemoteViews(rv)

        assertIs<RelativeLayout>(view)
        assertThat(view.gravity).isEqualTo(Gravity.BOTTOM or Gravity.END)
    }

    @Test
    fun canTranslateBoxWithChildren() = fakeCoroutineScope.runBlockingTest {
        val rv = runAndTranslate {
            Box {
                Box(contentAlignment = Alignment.Center) {}
                Box(contentAlignment = Alignment.BottomEnd) {}
            }
        }
        val view = context.applyRemoteViews(rv)

        assertIs<RelativeLayout>(view)
        assertThat(view.childCount).isEqualTo(2)
        assertThat(view.getChildAt(0)).isInstanceOf(RelativeLayout::class.java)
        assertThat(view.getChildAt(1)).isInstanceOf(RelativeLayout::class.java)
        val child1 = view.getChildAt(0) as RelativeLayout
        assertThat(child1.gravity).isEqualTo(Gravity.CENTER)
        val child2 = view.getChildAt(1) as RelativeLayout
        assertThat(child2.gravity).isEqualTo(Gravity.BOTTOM or Gravity.END)
    }

    @Test
    fun canTranslateMultipleNodes() = fakeCoroutineScope.runBlockingTest {
        val rv = runAndTranslate {
            Box(contentAlignment = Alignment.Center) {}
            Box(contentAlignment = Alignment.BottomEnd) {}
        }
        val view = context.applyRemoteViews(rv)

        assertIs<RelativeLayout>(view)
        assertThat(view.childCount).isEqualTo(2)
        assertThat(view.getChildAt(0)).isInstanceOf(RelativeLayout::class.java)
        assertThat(view.getChildAt(1)).isInstanceOf(RelativeLayout::class.java)
        val child1 = view.getChildAt(0) as RelativeLayout
        assertThat(child1.gravity).isEqualTo(Gravity.CENTER)
        val child2 = view.getChildAt(1) as RelativeLayout
        assertThat(child2.gravity).isEqualTo(Gravity.BOTTOM or Gravity.END)
    }

    @Test
    fun canTranslatePaddingModifier() = fakeCoroutineScope.runBlockingTest {
        val rv = runAndTranslate {
            Box(
                modifier = Modifier.padding(
                    start = 4.dp,
                    end = 5.dp,
                    top = 6.dp,
                    bottom = 7.dp,
                )
            ) { }
        }
        val view = context.applyRemoteViews(rv)

        assertIs<RelativeLayout>(view)
        assertThat(view.paddingLeft).isEqualTo(dpToPixel(4.dp))
        assertThat(view.paddingRight).isEqualTo(dpToPixel(5.dp))
        assertThat(view.paddingTop).isEqualTo(dpToPixel(6.dp))
        assertThat(view.paddingBottom).isEqualTo(dpToPixel(7.dp))
    }

    @Test
    fun canTranslatePaddingRTL() = fakeCoroutineScope.runBlockingTest {
        val rv = runAndTranslateInRtl {
            Box(
                modifier = Modifier.padding(
                    start = 4.dp,
                    end = 5.dp,
                    top = 6.dp,
                    bottom = 7.dp,
                )
            ) { }
        }
        val view = context.applyRemoteViews(rv)

        assertIs<RelativeLayout>(view)
        assertThat(view.paddingLeft).isEqualTo(dpToPixel(5.dp))
        assertThat(view.paddingRight).isEqualTo(dpToPixel(4.dp))
        assertThat(view.paddingTop).isEqualTo(dpToPixel(6.dp))
        assertThat(view.paddingBottom).isEqualTo(dpToPixel(7.dp))
    }

    @Test
    fun canTranslateAbsolutePaddingRTL() = fakeCoroutineScope.runBlockingTest {
        val rv = runAndTranslateInRtl {
            Box(
                modifier = Modifier.absolutePadding(
                    left = 4.dp,
                    right = 5.dp,
                    top = 6.dp,
                    bottom = 7.dp,
                )
            ) { }
        }
        val view = context.applyRemoteViews(rv)

        assertIs<RelativeLayout>(view)
        assertThat(view.paddingLeft).isEqualTo(dpToPixel(4.dp))
        assertThat(view.paddingRight).isEqualTo(dpToPixel(5.dp))
        assertThat(view.paddingTop).isEqualTo(dpToPixel(6.dp))
        assertThat(view.paddingBottom).isEqualTo(dpToPixel(7.dp))
    }

    @Test
    fun canTranslateText() = fakeCoroutineScope.runBlockingTest {
        val rv = runAndTranslate {
            Text("test")
        }
        val view = context.applyRemoteViews(rv)

        assertIs<TextView>(view)
        assertThat(view.text.toString()).isEqualTo("test")
    }

    @Test
    @Config(sdk = [23, 29])
    fun canTranslateText_withStyleWeightAndSize() = fakeCoroutineScope.runBlockingTest {
        val rv = runAndTranslate {
            Text(
                "test",
                style = TextStyle(fontWeight = FontWeight.Medium, size = 12.sp),
            )
        }
        val view = context.applyRemoteViews(rv)

        assertIs<TextView>(view)
        assertThat(view.textSize).isEqualTo(spToPixel(12.sp))
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
        val rv = runAndTranslate {
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
        val rv = runAndTranslate {
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
        val rv = runAndTranslate {
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
        val rv = runAndTranslate {
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

    private suspend fun runAndTranslate(
        context: Context = this.context,
        content: @Composable () -> Unit
    ): RemoteViews {
        val root = runTestingComposition(content)
        return translateComposition(context, root)
    }

    private suspend fun runAndTranslateInRtl(content: @Composable () -> Unit): RemoteViews {
        val rtlLocale = Locale.getAvailableLocales().first {
            TextUtils.getLayoutDirectionFromLocale(it) == View.LAYOUT_DIRECTION_RTL
        }
        val rtlContext = context.createConfigurationContext(
            Configuration(context.resources.configuration).also {
                it.setLayoutDirection(rtlLocale)
            }
        )
        return runAndTranslate(rtlContext, content)
    }

    private fun dpToPixel(dp: Dp) =
        floor(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.value, displayMetrics))
            .toInt()

    private fun spToPixel(sp: Sp) =
        floor(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp.value, displayMetrics))
            .toInt()
}