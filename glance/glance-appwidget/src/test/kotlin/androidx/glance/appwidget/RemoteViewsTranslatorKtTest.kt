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
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import android.text.SpannedString
import android.text.style.StrikethroughSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RadioGroup
import android.widget.RemoteViews
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.children
import androidx.glance.Button
import androidx.glance.GlanceModifier
import androidx.glance.Visibility
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.FrameLayoutSubject.Companion.assertThat
import androidx.glance.appwidget.LinearLayoutSubject.Companion.assertThat
import androidx.glance.appwidget.TextViewSubject.Companion.assertThat
import androidx.glance.appwidget.ViewSubject.Companion.assertThat
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.LazyVerticalGrid
import androidx.glance.appwidget.lazy.GridCells
import androidx.glance.appwidget.lazy.ReservedItemIdRangeEnd
import androidx.glance.appwidget.test.R
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.absolutePadding
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.visibility
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RemoteViewsTranslatorKtTest {

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
    fun checkRootAliasCorrectness() {
        assertThat(LastRootAlias - FirstRootAlias + 1).isEqualTo(RootAliasCount)
    }

    @Test
    fun canTranslateBox() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate { Box {} }
        val view = context.applyRemoteViews(rv)

        assertIs<FrameLayout>(view)
        assertThat(view.childCount).isEqualTo(0)
    }

    @Test
    fun canTranslateBoxWithAlignment() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Box(contentAlignment = Alignment.BottomEnd) { Text("text") }
        }
        val view = context.applyRemoteViews(rv)

        assertIs<FrameLayout>(view)
        assertThat(view).hasContentAlignment(Alignment.BottomEnd)
    }

    @Test
    fun canTranslateBoxWithChildren() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Box {
                Box(contentAlignment = Alignment.Center) { Text("text1") }
                Box(contentAlignment = Alignment.BottomEnd) { Text("text2") }
            }
        }
        val view = context.applyRemoteViews(rv)

        assertIs<FrameLayout>(view)
        assertThat(view.nonGoneChildCount).isEqualTo(2)
        val (child1, child2) = view.nonGoneChildren.toList()
        assertIs<FrameLayout>(child1)
        assertIs<FrameLayout>(child2)
        assertThat(child1).hasContentAlignment(Alignment.Center)
        assertThat(child2).hasContentAlignment(Alignment.BottomEnd)
    }

    @Test
    fun canReapplyTranslateBox() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Box {
                Box(contentAlignment = Alignment.Center) { Text("tex1") }
                Box(contentAlignment = Alignment.BottomEnd) { Text("text2") }
            }
        }
        val view = context.applyRemoteViews(rv)
        assertIs<FrameLayout>(view)
        assertThat(view.nonGoneChildCount).isEqualTo(2)
        val (child1, child2) = view.nonGoneChildren.toList()
        assertIs<FrameLayout>(child1)
        assertIs<FrameLayout>(child2)

        rv.reapply(context, view)

        assertIs<FrameLayout>(view)
        assertThat(view.nonGoneChildCount).isEqualTo(2)
        val (newChild1, newChild2) = view.nonGoneChildren.toList()
        assertIs<FrameLayout>(newChild1)
        assertIs<FrameLayout>(newChild2)
        assertThat(newChild1).hasContentAlignment(Alignment.Center)
        assertThat(newChild2).hasContentAlignment(Alignment.BottomEnd)
        assertThat(newChild1).isSameInstanceAs(child1)
        assertThat(newChild2).isSameInstanceAs(child2)
    }

    @Test
    fun canTranslateMultipleNodes() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Box(contentAlignment = Alignment.Center) { Text("text1") }
            Box(contentAlignment = Alignment.BottomEnd) { Text("text2") }
        }
        val view = context.applyRemoteViews(rv)

        assertIs<FrameLayout>(view)
        assertThat(view.nonGoneChildCount).isEqualTo(2)
        val (child1, child2) = view.nonGoneChildren.toList()
        assertIs<FrameLayout>(child1)
        assertIs<FrameLayout>(child2)
        assertThat(child1).hasContentAlignment(Alignment.Center)
        assertThat(child2).hasContentAlignment(Alignment.BottomEnd)
    }

    @Test
    fun canTranslatePaddingModifier() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Box(
                modifier = GlanceModifier.padding(
                    start = 4.dp,
                    end = 5.dp,
                    top = 6.dp,
                    bottom = 7.dp,
                )
            ) { }
        }
        val view = context.applyRemoteViews(rv)

        assertIs<FrameLayout>(view)
        assertThat(view.paddingLeft).isEqualTo(4.dp.toPixels())
        assertThat(view.paddingRight).isEqualTo(5.dp.toPixels())
        assertThat(view.paddingTop).isEqualTo(6.dp.toPixels())
        assertThat(view.paddingBottom).isEqualTo(7.dp.toPixels())
    }

    @Test
    fun canTranslatePaddingRTL() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslateInRtl {
            Box(
                modifier = GlanceModifier.padding(
                    start = 4.dp,
                    end = 5.dp,
                    top = 6.dp,
                    bottom = 7.dp,
                )
            ) { }
        }
        val view = context.applyRemoteViews(rv)

        assertIs<FrameLayout>(view)
        assertThat(view.paddingLeft).isEqualTo(5.dp.toPixels())
        assertThat(view.paddingRight).isEqualTo(4.dp.toPixels())
        assertThat(view.paddingTop).isEqualTo(6.dp.toPixels())
        assertThat(view.paddingBottom).isEqualTo(7.dp.toPixels())
    }

    @Test
    fun canTranslateAbsolutePaddingRTL() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslateInRtl {
            Box(
                modifier = GlanceModifier.absolutePadding(
                    left = 4.dp,
                    right = 5.dp,
                    top = 6.dp,
                    bottom = 7.dp,
                )
            ) { }
        }
        val view = context.applyRemoteViews(rv)

        assertIs<FrameLayout>(view)
        assertThat(view.paddingLeft).isEqualTo(4.dp.toPixels())
        assertThat(view.paddingRight).isEqualTo(5.dp.toPixels())
        assertThat(view.paddingTop).isEqualTo(6.dp.toPixels())
        assertThat(view.paddingBottom).isEqualTo(7.dp.toPixels())
    }

    @Test
    fun canTranslateRow() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate { Row { } }
        val view = context.applyRemoteViews(rv)

        assertIs<LinearLayout>(view)
        assertThat(view.childCount).isEqualTo(0)
    }

    @Test
    fun canTranslateColumn() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate { Column { } }
        val view = context.applyRemoteViews(rv)

        assertIs<LinearLayout>(view)
        assertThat(view.childCount).isEqualTo(0)
    }

    @Test
    @TargetApi(24)
    fun canTranslateRowWithAlignment() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Row(
                horizontalAlignment = Alignment.End,
                verticalAlignment = Alignment.Bottom
            ) { }
        }
        val view = context.applyRemoteViews(rv)

        assertIs<LinearLayout>(view)
        assertThat(view).hasContentAlignment(Alignment.End)
        assertThat(view).hasContentAlignment(Alignment.Bottom)
    }

    @Test
    @TargetApi(24)
    fun canTranslateColumnWithAlignment() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalAlignment = Alignment.Bottom
            ) { }
        }
        val view = context.applyRemoteViews(rv)

        assertIs<LinearLayout>(view)
        assertThat(view).hasContentAlignment(Alignment.Start)
        assertThat(view).hasContentAlignment(Alignment.Bottom)
    }

    @Test
    @TargetApi(24)
    fun canTranslateRowWithChildren() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
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
        assertThat(view.nonGoneChildCount).isEqualTo(3)
        val (child1, child2, child3) = view.nonGoneChildren.toList()
        assertIs<LinearLayout>(child1)
        assertThat(child1).hasContentAlignment(Alignment.Center)
        assertIs<LinearLayout>(child2)
        assertThat(child2).hasContentAlignment(Alignment.TopCenter)
        assertIs<LinearLayout>(child3)
        assertThat(child3).hasContentAlignment(Alignment.BottomEnd)
    }

    @Test
    @TargetApi(24)
    fun canTranslateColumnWithChildren() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Column {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) { Text("text") }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.Top
                ) { Text("text") }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalAlignment = Alignment.Bottom
                ) { Text("text") }
            }
        }
        val view = context.applyRemoteViews(rv)

        assertIs<LinearLayout>(view)
        assertThat(view.nonGoneChildCount).isEqualTo(3)
        val (child1, child2, child3) = view.nonGoneChildren.toList()
        assertIs<LinearLayout>(child1)
        assertThat(child1).hasContentAlignment(Alignment.Center)
        assertIs<LinearLayout>(child2)
        assertThat(child2).hasContentAlignment(Alignment.TopCenter)
        assertIs<LinearLayout>(child3)
        assertThat(child3).hasContentAlignment(Alignment.BottomEnd)
    }

    @Test
    fun canTranslateRowPaddingModifier() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Row(
                modifier = GlanceModifier.padding(
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
    fun canTranslateColumnPaddingModifier() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Column(
                modifier = GlanceModifier.padding(
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
    fun canTranslateRowPaddingRTL() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslateInRtl {
            Row(
                modifier = GlanceModifier.padding(
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
    fun canTranslateColumnPaddingRTL() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslateInRtl {
            Column(
                modifier = GlanceModifier.padding(
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
    fun canTranslateRowAbsolutePaddingRTL() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslateInRtl {
            Row(
                modifier = GlanceModifier.absolutePadding(
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
    fun canTranslateColumnAbsolutePaddingRTL() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslateInRtl {
            Column(
                modifier = GlanceModifier.absolutePadding(
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
    fun canTranslateLazyColumn_emptyList() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            LazyColumn { }
        }

        assertIs<ListView>(context.applyRemoteViews(rv))
    }

    @Test
    fun canTranslateLazyColumn_withItem() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            LazyColumn {
                item { Text("First") }
                item { Row { Text("Second") } }
            }
        }

        assertIs<ListView>(context.applyRemoteViews(rv))
    }

    @Test
    fun canTranslateLazyColumn_withMultiChildItem() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
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
    fun canTranslateLazyColumn_withMaximumUnreservedItemId() = fakeCoroutineScope.runTest {
        context.runAndTranslate {
            LazyColumn {
                item(ReservedItemIdRangeEnd + 1) { Text("First") }
            }
        }
    }

    @Test
    fun cannotTranslateLazyColumn_failsWithReservedItemId() = fakeCoroutineScope.runTest {
        assertFailsWith<IllegalArgumentException> {
            context.runAndTranslate {
                LazyColumn {
                    item(ReservedItemIdRangeEnd) { Text("First") }
                }
            }
        }
    }

    /* TODO(b/202868171): Restore after viewStub are implemented
    @Test
    fun canTranslateLazyColumn_maximumLists() = fakeCoroutineScope.runTest {
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
    fun cannotTranslateLazyColumn_tooManyLists() = fakeCoroutineScope.runTest {
        assertFailsWith<IllegalArgumentException> {
            runAndTranslate {
                LazyColumn { }
                LazyColumn { }
                LazyColumn { }
                LazyColumn { }
            }
        }
    }
     */

    @Test
    fun cannotTranslateNestedLists() = fakeCoroutineScope.runTest {
        assertFailsWith<IllegalStateException> {
            context.runAndTranslate {
                LazyColumn {
                    item {
                        LazyColumn {
                            item {
                                Text("Crash expected")
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun canTranslateLazyVerticalGrid_emptyList() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            LazyVerticalGrid(gridCells = GridCells.Fixed(3)) { }
        }

        assertIs<GridView>(context.applyRemoteViews(rv))
    }

    @Test
    fun canTranslateLazyVerticalGrid_withItem() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            LazyVerticalGrid(gridCells = GridCells.Fixed(3)) {
                item { Text("First") }
                item { Row { Text("Second") } }
            }
        }

        assertIs<GridView>(context.applyRemoteViews(rv))
    }

    @Test
    fun canTranslateLazyVerticalGrid_withItems() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            LazyVerticalGrid(gridCells = GridCells.Fixed(3)) {
                items(2, { it * 2L }) { index -> Text("Item $index") }
            }
        }

        assertIs<GridView>(context.applyRemoteViews(rv))
    }

    @Test
    fun canTranslateAndroidRemoteViews() = fakeCoroutineScope.runTest {
        val result = context.runAndTranslate {
            val providedViews = RemoteViews(context.packageName, R.layout.text_sample).also {
                it.setTextViewText(R.id.text_view, "Android Remote Views")
            }
            AndroidRemoteViews(providedViews)
        }

        val boxView = assertIs<FrameLayout>(context.applyRemoteViews(result))
        val actual = assertIs<TextView>(boxView.children.single())
        assertThat(actual.id).isEqualTo(R.id.text_view)
        assertThat(actual.text).isEqualTo("Android Remote Views")
    }

    @Test
    fun canTranslateAndroidRemoteViews_Container() = fakeCoroutineScope.runTest {
        val result = context.runAndTranslate {
            val providedViews = RemoteViews(context.packageName, R.layout.raw_container)
            AndroidRemoteViews(
                providedViews,
                R.id.raw_container_view
            ) {
                Text("inner text 1")
                Text("inner text 2")
            }
        }

        val rootLayout = assertIs<FrameLayout>(context.applyRemoteViews(result))
        assertThat(rootLayout.childCount).isEqualTo(1)
        val containerLayout = assertIs<LinearLayout>(rootLayout.getChildAt(0))
        assertThat(containerLayout.orientation).isEqualTo(LinearLayout.VERTICAL)
        assertThat(containerLayout.childCount).isEqualTo(2)
        val boxChild1 = assertIs<FrameLayout>(containerLayout.getChildAt(0))
        val child1 = assertIs<TextView>(boxChild1.getChildAt(0))
        assertThat(child1.text.toString()).isEqualTo("inner text 1")
        val boxChild2 = assertIs<FrameLayout>(containerLayout.getChildAt(1))
        val child2 = assertIs<TextView>(boxChild2.getChildAt(0))
        assertThat(child2.text.toString()).isEqualTo("inner text 2")
    }

    @Test
    fun canTranslateAndroidRemoteViews_Container_BadSetupShouldFail() =
        fakeCoroutineScope.runTest {
            assertFailsWith<IllegalStateException> {
                context.runAndTranslate {
                    val providedViews = RemoteViews(context.packageName, R.layout.raw_container)
                    AndroidRemoteViews(providedViews, View.NO_ID) {
                        Text("inner text 1")
                        Text("inner text 2")
                    }
                }
            }
        }

    @Test
    @Config(maxSdk = 30)
    fun canTranslateCheckbox_pre31_unchecked() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            CheckBox(
                checked = false,
                onCheckedChange = null,
                text = "test",
                style = TextStyle(
                    color = ColorProvider(Color.Red),
                    textDecoration = TextDecoration.Underline
                ),
            )
        }
        val view = context.applyRemoteViews(rv)

        assertIs<LinearLayout>(view)
        val iconView = assertIs<ImageView>(view.getChildAt(0))
        assertThat(iconView.isEnabled).isFalse()

        val textView = assertIs<TextView>(view.getChildAt(1))
        assertThat(textView).hasTextColor(android.graphics.Color.RED)
        val textContent = assertIs<SpannedString>(textView.text)
        assertThat(textContent.toString()).isEqualTo("test")
        assertThat(textContent.getSpans(0, textContent.length, Any::class.java)).hasLength(1)
        textContent.checkHasSingleTypedSpan<UnderlineSpan> { }
    }

    @Test
    @Config(maxSdk = 30)
    fun canTranslateCheckbox_pre31_checked() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            CheckBox(
                checked = true,
                onCheckedChange = null,
                text = "test checked",
                style = TextStyle(textDecoration = TextDecoration.LineThrough),
            )
        }
        val view = context.applyRemoteViews(rv)

        assertIs<LinearLayout>(view)
        val iconView = assertIs<ImageView>(view.getChildAt(0))
        assertThat(iconView.isEnabled).isTrue()

        val textView = assertIs<TextView>(view.getChildAt(1))
        val textContent = assertIs<SpannedString>(textView.text)
        assertThat(textContent.toString()).isEqualTo("test checked")
        assertThat(textContent.getSpans(0, textContent.length, Any::class.java)).hasLength(1)
        textContent.checkHasSingleTypedSpan<StrikethroughSpan> { }
    }

    @Test
    fun canTranslateButton() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Button(
                "Button",
                onClick = actionStartActivity<Activity>(),
                enabled = true
            )
        }

        val button = assertIs<android.widget.Button>(context.applyRemoteViews(rv))
        assertThat(button.text).isEqualTo("Button")
        assertThat(button.isEnabled).isTrue()
        assertThat(button.hasOnClickListeners()).isTrue()
    }

    @Test
    fun canTranslateButton_disabled() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Button(
                "Button",
                onClick = actionStartActivity<Activity>(),
                enabled = false
            )
        }

        val button = assertIs<android.widget.Button>(context.applyRemoteViews(rv))
        assertThat(button.text).isEqualTo("Button")
        assertThat(button.isEnabled).isFalse()
        assertThat(button.hasOnClickListeners()).isFalse()
    }

    @Test
    fun canTranslateCircularProgressIndicator() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            CircularProgressIndicator()
        }

        val progressIndicator = assertIs<android.widget.ProgressBar>(context.applyRemoteViews(rv))
        assertThat(progressIndicator.isIndeterminate()).isTrue()
    }

    @Test
    fun canTranslateLinearProgressIndicator_determinate() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            LinearProgressIndicator(
                progress = 0.5f,
            )
        }

        val progressIndicator = assertIs<android.widget.ProgressBar>(context.applyRemoteViews(rv))
        assertThat(progressIndicator.isIndeterminate()).isFalse()
        assertThat(progressIndicator.getMax()).isEqualTo(100)
        assertThat(progressIndicator.getProgress()).isEqualTo(50)
    }

    @Test
    fun canTranslateLinearProgressIndicator_indeterminate() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            LinearProgressIndicator()
        }

        val progressIndicator = assertIs<android.widget.ProgressBar>(context.applyRemoteViews(rv))
        assertThat(progressIndicator.isIndeterminate()).isTrue()
    }

    @Test
    fun canTranslateBackground_red() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Box(modifier = GlanceModifier.background(Color.Red)) {}
        }

        val view = context.applyRemoteViews(rv)

        assertThat(view).hasBackgroundColor(android.graphics.Color.RED)
    }

    @Test
    fun canTranslateBackground_partialColor() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Box(
                modifier = GlanceModifier.background(Color(red = 0.4f, green = 0.5f, blue = 0.6f))
            ) {}
        }

        val view = context.applyRemoteViews(rv)

        assertThat(view).hasBackgroundColor(android.graphics.Color.argb(255, 102, 128, 153))
    }

    @Test
    fun canTranslateBackground_transparent() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Box(modifier = GlanceModifier.background(Color.Transparent)) {}
        }

        val view = context.applyRemoteViews(rv)

        assertThat(view).hasBackgroundColor(android.graphics.Color.TRANSPARENT)
    }

    @Config(minSdk = 29)
    @Test
    fun canTranslateBackground_resId() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Box(modifier = GlanceModifier.background(R.color.my_color)) {}
        }

        assertThat(lightContext.applyRemoteViews(rv)).hasBackgroundColor("#EEEEEE")
        assertThat(darkContext.applyRemoteViews(rv)).hasBackgroundColor("#111111")
    }

    @Config(sdk = [30])
    @Test
    fun canTranslateBackground_dayNight_light() = fakeCoroutineScope.runTest {
        val rv = lightContext.runAndTranslate() {
            Box(modifier = GlanceModifier.background(day = Color.Red, night = Color.Blue)) {}
        }

        val view = lightContext.applyRemoteViews(rv)

        assertThat(view).hasBackgroundColor(android.graphics.Color.RED)
    }

    @Config(sdk = [30])
    @Test
    fun canTranslateBackground_dayNight_dark() = fakeCoroutineScope.runTest {
        val rv = darkContext.runAndTranslate() {
            Box(modifier = GlanceModifier.background(day = Color.Red, night = Color.Blue)) {}
        }

        val view = darkContext.applyRemoteViews(rv)

        assertThat(view).hasBackgroundColor(android.graphics.Color.BLUE)
    }

    @Test
    fun visibility() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Column {
                Text("first", modifier = GlanceModifier.visibility(Visibility.Invisible))
                Text("second", modifier = GlanceModifier.visibility(Visibility.Gone))
                Text("third")
            }
        }

        val view = context.applyRemoteViews(rv)

        val firstText = checkNotNull(view.findView<TextView> { it.text == "first" })
        val secondText = checkNotNull(view.findView<TextView> { it.text == "second" })
        val thirdText = checkNotNull(view.findView<TextView> { it.text == "third" })

        assertThat(firstText.visibility).isEqualTo(View.INVISIBLE)
        assertThat(secondText.visibility).isEqualTo(View.GONE)
        assertThat(thirdText.visibility).isEqualTo(View.VISIBLE)
    }

    @Test
    fun setAsAppWidgetBackground() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Column(modifier = GlanceModifier.appWidgetBackground()) {
                Text("text1")
            }
        }

        val view = context.applyRemoteViews(rv)

        val column =
            checkNotNull(view.findView<LinearLayout> { it.id == android.R.id.background }) {
                "No LinearLayout with `background` view id"
            }
        assertThat(column.nonGoneChildCount).isEqualTo(1)
    }

    @Test
    fun setAsAppWidgetBackground_multipleTimes_shouldFail() = fakeCoroutineScope.runTest {
        assertFailsWith<IllegalStateException> {
            context.runAndTranslate {
                Column(modifier = GlanceModifier.appWidgetBackground()) {
                    Text("text1", modifier = GlanceModifier.appWidgetBackground())
                }
            }
        }
    }

    @Test
    fun multipleClickable_shouldLogWarning() = fakeCoroutineScope.runTest {
        context.runAndTranslate {
            Text(
                "text1",
                modifier = GlanceModifier.clickable(
                    actionStartActivity(ComponentName("package", "class"))
                ).clickable(
                    actionStartActivity(ComponentName("package", "class2"))
                )
            )
        }

        expectGlanceLog(
            Log.WARN,
            "More than one clickable defined on the same GlanceModifier, " +
                "only the last one will be used."
        )
    }

    @Test
    @Config(minSdk = 31)
    fun canTranslateRowSelectableGroupToHorizontalRadioGroup() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Row(modifier = GlanceModifier.selectableGroup()) {}
        }

        val view = context.applyRemoteViews(rv)
        val group = assertIs<RadioGroup>(view)
        assertThat(group.orientation).isEqualTo(LinearLayout.HORIZONTAL)
    }

    @Test
    @Config(minSdk = 31)
    fun canTranslateColumnSelectableGroupToVerticalRadioGroup() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Column(modifier = GlanceModifier.selectableGroup()) {}
        }

        val view = context.applyRemoteViews(rv)
        val group = assertIs<RadioGroup>(view)
        assertThat(group.orientation).isEqualTo(LinearLayout.VERTICAL)
    }

    @Test
    @Config(maxSdk = 30)
    fun canTranslateRowSelectableGroupToLinearLayout() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Row(modifier = GlanceModifier.selectableGroup()) {}
        }

        val view = context.applyRemoteViews(rv)
        val group = assertIs<LinearLayout>(view)
        assertThat(group.orientation).isEqualTo(LinearLayout.HORIZONTAL)
    }

    @Test
    @Config(maxSdk = 30)
    fun canTranslateColumnSelectableGroupToLinearLayout() = fakeCoroutineScope.runTest {
        val rv = context.runAndTranslate {
            Column(modifier = GlanceModifier.selectableGroup()) {}
        }

        val view = context.applyRemoteViews(rv)
        val group = assertIs<LinearLayout>(view)
        assertThat(group.orientation).isEqualTo(LinearLayout.VERTICAL)
    }

    @Test
    fun cannotTranslateSelectableGroupThatIsNotRowOrColumn() = fakeCoroutineScope.runTest {
        assertFailsWith<Exception> {
            context.runAndTranslate {
                Box(modifier = GlanceModifier.selectableGroup()) {}
            }
        }
    }

    @Test
    fun cannotTranslateSelectableGroupWithMultipleCheckedButtons() = fakeCoroutineScope.runTest {
        assertFailsWith<IllegalStateException> {
            context.runAndTranslate {
                Column(modifier = GlanceModifier.selectableGroup()) {
                    RadioButton(onClick = null, checked = true)
                    RadioButton(onClick = null, checked = true)
                }
            }
        }
    }

    private fun expectGlanceLog(type: Int, message: String) {
        ShadowLog.getLogsForTag(GlanceAppWidgetTag).forEach { logItem ->
            if (logItem.type == type && logItem.msg == message)
                return
        }
        fail("No warning message found")
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

    private fun Dp.toPixels() = toPixels(displayMetrics)
}
