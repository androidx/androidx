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

import android.annotation.TargetApi
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
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.glance.GlanceInternalApi
import androidx.glance.Modifier
import androidx.glance.appwidget.layout.LazyColumn
import androidx.glance.appwidget.layout.ReservedItemIdRangeEnd
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.FontStyle
import androidx.glance.layout.FontWeight
import androidx.glance.layout.Row
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
import java.lang.IllegalArgumentException
import java.util.Locale
import kotlin.test.assertFailsWith
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
        val child1 = view.getChildAt(0)
        val child2 = view.getChildAt(1)
        assertIs<RelativeLayout>(child1)
        assertIs<RelativeLayout>(child2)
        assertThat(child1.gravity).isEqualTo(Gravity.CENTER)
        assertThat(child2.gravity).isEqualTo(Gravity.BOTTOM or Gravity.END)
    }

    @Test
    fun canReapplyTranslateBox() = fakeCoroutineScope.runBlockingTest {
        val rv = runAndTranslate {
            Box {
                Box(contentAlignment = Alignment.Center) {}
                Box(contentAlignment = Alignment.BottomEnd) {}
            }
        }
        val view = context.applyRemoteViews(rv)
        assertIs<RelativeLayout>(view)
        assertThat(view.childCount).isEqualTo(2)
        val child1 = view.getChildAt(0)
        val child2 = view.getChildAt(1)
        assertIs<RelativeLayout>(child1)
        assertIs<RelativeLayout>(child2)

        rv.reapply(context, view)

        assertIs<RelativeLayout>(view)
        assertThat(view.childCount).isEqualTo(2)
        val newChild1 = view.getChildAt(0)
        val newChild2 = view.getChildAt(1)
        assertIs<RelativeLayout>(newChild1)
        assertIs<RelativeLayout>(newChild2)
        assertThat(newChild1.gravity).isEqualTo(Gravity.CENTER)
        assertThat(newChild2.gravity).isEqualTo(Gravity.BOTTOM or Gravity.END)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            assertThat(newChild1).isSameInstanceAs(child1)
            assertThat(newChild2).isSameInstanceAs(child2)
        }
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
        val child1 = view.getChildAt(0)
        val child2 = view.getChildAt(1)
        assertIs<RelativeLayout>(child1)
        assertIs<RelativeLayout>(child2)
        assertThat(child1.gravity).isEqualTo(Gravity.CENTER)
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
        assertThat(view.paddingLeft).isEqualTo(4.dp.toPixels())
        assertThat(view.paddingRight).isEqualTo(5.dp.toPixels())
        assertThat(view.paddingTop).isEqualTo(6.dp.toPixels())
        assertThat(view.paddingBottom).isEqualTo(7.dp.toPixels())
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
        assertThat(view.paddingLeft).isEqualTo(5.dp.toPixels())
        assertThat(view.paddingRight).isEqualTo(4.dp.toPixels())
        assertThat(view.paddingTop).isEqualTo(6.dp.toPixels())
        assertThat(view.paddingBottom).isEqualTo(7.dp.toPixels())
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
        assertThat(view.paddingLeft).isEqualTo(4.dp.toPixels())
        assertThat(view.paddingRight).isEqualTo(5.dp.toPixels())
        assertThat(view.paddingTop).isEqualTo(6.dp.toPixels())
        assertThat(view.paddingBottom).isEqualTo(7.dp.toPixels())
    }

    @Test
    fun canTranslateRow() = fakeCoroutineScope.runBlockingTest {
        val rv = runAndTranslate { Row { } }
        val view = context.applyRemoteViews(rv)

        assertIs<LinearLayout>(view)
        assertThat(view.childCount).isEqualTo(0)
    }

    @Test
    fun canTranslateColumn() = fakeCoroutineScope.runBlockingTest {
        val rv = runAndTranslate { Column { } }
        val view = context.applyRemoteViews(rv)

        assertIs<LinearLayout>(view)
        assertThat(view.childCount).isEqualTo(0)
    }

    @Test
    @TargetApi(24)
    fun canTranslateRowWithAlignment() = fakeCoroutineScope.runBlockingTest {
        val rv = runAndTranslate {
            Row(
                horizontalAlignment = Alignment.End,
                verticalAlignment = Alignment.Bottom
            ) { }
        }
        val view = context.applyRemoteViews(rv)

        assertIs<LinearLayout>(view)
        assertThat(view.gravity).isEqualTo(Gravity.BOTTOM or Gravity.END)
    }

    @Test
    @TargetApi(24)
    fun canTranslateColumnWithAlignment() = fakeCoroutineScope.runBlockingTest {
        val rv = runAndTranslate {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalAlignment = Alignment.Bottom
            ) { }
        }
        val view = context.applyRemoteViews(rv)

        assertIs<LinearLayout>(view)
        assertThat(view.gravity).isEqualTo(Gravity.BOTTOM or Gravity.START)
    }

    @Test
    @TargetApi(24)
    fun canTranslateRowWithChildren() = fakeCoroutineScope.runBlockingTest {
        val rv = runAndTranslate {
            Row {
                Row(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) { }
                Row(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.Top
                ) { }
                Row(
                    horizontalAlignment = Alignment.End,
                    verticalAlignment = Alignment.Bottom
                ) { }
            }
        }
        val view = context.applyRemoteViews(rv)

        assertIs<LinearLayout>(view)
        assertThat(view.childCount).isEqualTo(3)
        val child1 = view.getChildAt(0)
        assertIs<LinearLayout>(child1)
        assertThat(child1.gravity).isEqualTo(Gravity.CENTER)
        val child2 = view.getChildAt(1)
        assertIs<LinearLayout>(child2)
        assertThat(child2.gravity).isEqualTo(Gravity.CENTER or Gravity.TOP)
        val child3 = view.getChildAt(2)
        assertIs<LinearLayout>(child3)
        assertThat(child3.gravity).isEqualTo(Gravity.BOTTOM or Gravity.END)
    }

    @Test
    @TargetApi(24)
    fun canTranslateColumnWithChildren() = fakeCoroutineScope.runBlockingTest {
        val rv = runAndTranslate {
            Column {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) { }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.Top
                ) { }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalAlignment = Alignment.Bottom
                ) { }
            }
        }
        val view = context.applyRemoteViews(rv)

        assertIs<LinearLayout>(view)
        assertThat(view.childCount).isEqualTo(3)
        val child1 = view.getChildAt(0)
        assertIs<LinearLayout>(child1)
        assertThat(child1.gravity).isEqualTo(Gravity.CENTER)
        val child2 = view.getChildAt(1)
        assertIs<LinearLayout>(child2)
        assertThat(child2.gravity).isEqualTo(Gravity.CENTER or Gravity.TOP)
        val child3 = view.getChildAt(2)
        assertIs<LinearLayout>(child3)
        assertThat(child3.gravity).isEqualTo(Gravity.BOTTOM or Gravity.END)
    }

    @Test
    fun canTranslateRowPaddingModifier() = fakeCoroutineScope.runBlockingTest {
        val rv = runAndTranslate {
            Row(
                modifier = Modifier.padding(
                    start = 17.dp,
                    end = 16.dp,
                    top = 15.dp,
                    bottom = 14.dp,
                )
            ) { }
        }
        val view = context.applyRemoteViews(rv)

        assertIs<LinearLayout>(view)
        assertThat(view.paddingLeft).isEqualTo(17.dp.toPixels())
        assertThat(view.paddingRight).isEqualTo(16.dp.toPixels())
        assertThat(view.paddingTop).isEqualTo(15.dp.toPixels())
        assertThat(view.paddingBottom).isEqualTo(14.dp.toPixels())
    }

    @Test
    fun canTranslateColumnPaddingModifier() = fakeCoroutineScope.runBlockingTest {
        val rv = runAndTranslate {
            Column(
                modifier = Modifier.padding(
                    start = 13.dp,
                    end = 12.dp,
                    top = 11.dp,
                    bottom = 10.dp,
                )
            ) { }
        }
        val view = context.applyRemoteViews(rv)

        assertIs<LinearLayout>(view)
        assertThat(view.paddingLeft).isEqualTo(13.dp.toPixels())
        assertThat(view.paddingRight).isEqualTo(12.dp.toPixels())
        assertThat(view.paddingTop).isEqualTo(11.dp.toPixels())
        assertThat(view.paddingBottom).isEqualTo(10.dp.toPixels())
    }

    @Test
    fun canTranslateRowPaddingRTL() = fakeCoroutineScope.runBlockingTest {
        val rv = runAndTranslateInRtl {
            Row(
                modifier = Modifier.padding(
                    start = 4.dp,
                    end = 5.dp,
                    top = 6.dp,
                    bottom = 7.dp,
                )
            ) { }
        }
        val view = context.applyRemoteViews(rv)

        assertIs<LinearLayout>(view)
        assertThat(view.paddingLeft).isEqualTo(5.dp.toPixels())
        assertThat(view.paddingRight).isEqualTo(4.dp.toPixels())
        assertThat(view.paddingTop).isEqualTo(6.dp.toPixels())
        assertThat(view.paddingBottom).isEqualTo(7.dp.toPixels())
    }

    @Test
    fun canTranslateColumnPaddingRTL() = fakeCoroutineScope.runBlockingTest {
        val rv = runAndTranslateInRtl {
            Column(
                modifier = Modifier.padding(
                    start = 8.dp,
                    end = 9.dp,
                    top = 10.dp,
                    bottom = 11.dp,
                )
            ) { }
        }
        val view = context.applyRemoteViews(rv)

        assertIs<LinearLayout>(view)
        assertThat(view.paddingLeft).isEqualTo(9.dp.toPixels())
        assertThat(view.paddingRight).isEqualTo(8.dp.toPixels())
        assertThat(view.paddingTop).isEqualTo(10.dp.toPixels())
        assertThat(view.paddingBottom).isEqualTo(11.dp.toPixels())
    }

    @Test
    fun canTranslateRowAbsolutePaddingRTL() = fakeCoroutineScope.runBlockingTest {
        val rv = runAndTranslateInRtl {
            Row(
                modifier = Modifier.absolutePadding(
                    left = 12.dp,
                    right = 13.dp,
                    top = 14.dp,
                    bottom = 15.dp,
                )
            ) { }
        }
        val view = context.applyRemoteViews(rv)

        assertIs<LinearLayout>(view)
        assertThat(view.paddingLeft).isEqualTo(12.dp.toPixels())
        assertThat(view.paddingRight).isEqualTo(13.dp.toPixels())
        assertThat(view.paddingTop).isEqualTo(14.dp.toPixels())
        assertThat(view.paddingBottom).isEqualTo(15.dp.toPixels())
    }

    @Test
    fun canTranslateColumnAbsolutePaddingRTL() = fakeCoroutineScope.runBlockingTest {
        val rv = runAndTranslateInRtl {
            Column(
                modifier = Modifier.absolutePadding(
                    left = 16.dp,
                    right = 17.dp,
                    top = 18.dp,
                    bottom = 19.dp,
                )
            ) { }
        }
        val view = context.applyRemoteViews(rv)

        assertIs<LinearLayout>(view)
        assertThat(view.paddingLeft).isEqualTo(16.dp.toPixels())
        assertThat(view.paddingRight).isEqualTo(17.dp.toPixels())
        assertThat(view.paddingTop).isEqualTo(18.dp.toPixels())
        assertThat(view.paddingBottom).isEqualTo(19.dp.toPixels())
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
        assertThat(view.textSize).isEqualTo(12.sp.toPixels())
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

    @Test
    fun canTranslateLazyColumn_emptyList() = fakeCoroutineScope.runBlockingTest {
        val rv = runAndTranslate {
            LazyColumn { }
        }

        assertIs<ListView>(context.applyRemoteViews(rv))
    }

    @Test
    fun canTranslateLazyColumn_withItem() = fakeCoroutineScope.runBlockingTest {
        val rv = runAndTranslate {
            LazyColumn {
                item { Text("First") }
                item { Row { Text("Second") } }
            }
        }

        assertIs<ListView>(context.applyRemoteViews(rv))
    }

    @Test
    fun canTranslateLazyColumn_withMultiChildItem() = fakeCoroutineScope.runBlockingTest {
        val rv = runAndTranslate {
            LazyColumn {
                item {
                    Text("First")
                    Text("Second")
                }
            }
        }

        assertIs<ListView>(context.applyRemoteViews(rv))
    }

    @Test
    fun canTranslateLazyColumn_withMaximumUnreservedItemId() = fakeCoroutineScope.runBlockingTest {
        runAndTranslate {
            LazyColumn {
                item(ReservedItemIdRangeEnd + 1) { Text("First") }
            }
        }
    }

    @Test
    fun cannotTranslateLazyColumn_failsWithReservedItemId() = fakeCoroutineScope.runBlockingTest {
        assertFailsWith<IllegalArgumentException> {
            runAndTranslate {
                LazyColumn {
                    item(ReservedItemIdRangeEnd) { Text("First") }
                }
            }
        }
    }

    @Test
    fun canTranslateLazyColumn_maximumLists() = fakeCoroutineScope.runBlockingTest {
        val rv = runAndTranslate {
            LazyColumn { }
            LazyColumn { }
            LazyColumn { }
        }

        val rootLayout = assertIs<ViewGroup>(context.applyRemoteViews(rv))
        assertThat(rootLayout.childCount).isEqualTo(3)
        assertThat(rootLayout.getChildAt(0).id).isEqualTo(R.id.glanceListView1)
        assertThat(rootLayout.getChildAt(1).id).isEqualTo(R.id.glanceListView2)
        assertThat(rootLayout.getChildAt(2).id).isEqualTo(R.id.glanceListView3)
    }

    @Test
    fun cannotTranslateLazyColumn_tooManyLists() = fakeCoroutineScope.runBlockingTest {
        assertFailsWith<IllegalArgumentException> {
            runAndTranslate {
                LazyColumn { }
                LazyColumn { }
                LazyColumn { }
                LazyColumn { }
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
        appWidgetId: Int = 0,
        content: @Composable () -> Unit
    ): RemoteViews {
        val root = runTestingComposition(content)
        return translateComposition(context, appWidgetId, root)
    }

    private suspend fun runAndTranslateInRtl(
        appWidgetId: Int = 0,
        content: @Composable () -> Unit
    ): RemoteViews {
        val rtlLocale = Locale.getAvailableLocales().first {
            TextUtils.getLayoutDirectionFromLocale(it) == View.LAYOUT_DIRECTION_RTL
        }
        val rtlContext = context.createConfigurationContext(
            Configuration(context.resources.configuration).also {
                it.setLayoutDirection(rtlLocale)
            }
        )
        return runAndTranslate(rtlContext, appWidgetId, content = content)
    }

    private fun Dp.toPixels() = toPixels(displayMetrics)
    private fun Sp.toPixels() =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, displayMetrics).toInt()
}
