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

import android.app.Activity
import android.os.Build
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.TextView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.core.view.children
import androidx.glance.GlanceModifier
import androidx.glance.action.actionLaunchActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.ReservedItemIdRangeEnd
import androidx.glance.layout.Alignment
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertIs

@SdkSuppress(minSdkVersion = 29)
@MediumTest
class LazyColumnTest {
    @get:Rule
    val mHostRule = AppWidgetHostRule()

    @Test
    fun modifier_modifiesColumn() {
        TestGlanceAppWidget.uiDefinition = {
            LazyColumn(modifier = GlanceModifier.padding(5.dp, 6.dp, 7.dp, 8.dp)) {
                item { Text("1") }
                item { Text("2") }
            }
        }

        mHostRule.startHost()

        waitForListViewChildren { list ->
            fun Dp.toPx() = toPixels(list.context.resources.displayMetrics)
            assertThat(list.paddingStart).isEqualTo(5.dp.toPx())
            assertThat(list.paddingTop).isEqualTo(6.dp.toPx())
            assertThat(list.paddingEnd).isEqualTo(7.dp.toPx())
            assertThat(list.paddingBottom).isEqualTo(8.dp.toPx())
        }
    }

    @FlakyTest(bugId = 206481702)
    @Test
    fun item_withoutItemIds_createsNonStableList() {
        TestGlanceAppWidget.uiDefinition = {
            LazyColumn {
                item { Text("First row") }
                item { Text("Second row") }
            }
        }

        mHostRule.startHost()

        waitForListViewChildren { list ->
            val adapter = list.adapter!!
            assertThat(adapter.hasStableIds()).isFalse()
            assertThat(adapter.count).isEqualTo(2)
            assertThat(adapter.getItemId(0)).isEqualTo(ReservedItemIdRangeEnd)
            assertThat(adapter.getItemId(1)).isEqualTo(ReservedItemIdRangeEnd - 1)
        }
    }

    @Test
    fun item_withItemIds_createsStableList() {
        TestGlanceAppWidget.uiDefinition = {
            LazyColumn {
                item { Text("First row") }
                item(1L) { Text("Second row") }
            }
        }

        mHostRule.startHost()

        waitForListViewChildren { list ->
            val adapter = list.adapter!!
            assertThat(adapter.hasStableIds()).isTrue()
            assertThat(adapter.count).isEqualTo(2)
            assertThat(adapter.getItemId(0)).isEqualTo(ReservedItemIdRangeEnd)
            assertThat(adapter.getItemId(1)).isEqualTo(1L)
        }
    }

    @Test
    fun items_withoutItemIds_createsNonStableList() {
        TestGlanceAppWidget.uiDefinition = {
            LazyColumn {
                items(count = 3) { index -> Text("Row $index") }
            }
        }

        mHostRule.startHost()

        waitForListViewChildren { list ->
            val adapter = list.adapter!!
            assertThat(adapter.count).isEqualTo(3)
            assertThat(adapter.hasStableIds()).isFalse()
            assertThat(adapter.getItemId(0)).isEqualTo(ReservedItemIdRangeEnd)
            assertThat(adapter.getItemId(1)).isEqualTo(ReservedItemIdRangeEnd - 1)
            assertThat(adapter.getItemId(2)).isEqualTo(ReservedItemIdRangeEnd - 2)
        }
    }

    @Test
    fun items_withItemIds_createsStableList() {
        TestGlanceAppWidget.uiDefinition = {
            LazyColumn {
                items(count = 3, itemId = { it * 2L }) { index -> Text("Row $index") }
                item { Text("Row 3") }
            }
        }

        mHostRule.startHost()

        waitForListViewChildren { list ->
            val adapter = list.adapter!!
            assertThat(adapter.count).isEqualTo(4)
            assertThat(adapter.hasStableIds()).isTrue()
            assertThat(adapter.getItemId(0)).isEqualTo(0L)
            assertThat(adapter.getItemId(1)).isEqualTo(2L)
            assertThat(adapter.getItemId(2)).isEqualTo(4L)
            assertThat(adapter.getItemId(3)).isEqualTo(ReservedItemIdRangeEnd)
        }
    }

    @Test
    fun itemContent_nonStableList_translatesChildren() {
        TestGlanceAppWidget.uiDefinition = {
            LazyColumn {
                item { Text("Row 0") }
                item { Text("Row 1") }
                items(2) { index -> Text("Row ${index + 2}") }
            }
        }

        mHostRule.startHost()

        waitForListViewChildren { list ->
            val textView0 = list.getUnboxedListItem<TextView>(0)
            val textView1 = list.getUnboxedListItem<TextView>(1)
            val textView2 = list.getUnboxedListItem<TextView>(2)
            val textView3 = list.getUnboxedListItem<TextView>(3)
            assertThat(textView0.text.toString()).isEqualTo("Row 0")
            assertThat(textView1.text.toString()).isEqualTo("Row 1")
            assertThat(textView2.text.toString()).isEqualTo("Row 2")
            assertThat(textView3.text.toString()).isEqualTo("Row 3")
        }
    }

    @Test
    fun itemContent_stableList_translatesChildren() {
        TestGlanceAppWidget.uiDefinition = {
            LazyColumn {
                item(itemId = 1L) { Text("Row 0") }
                item(itemId = 1L) { Text("Row 1") }
                items(count = 2, itemId = { it + 2L }) { index -> Text("Row ${index + 2}") }
            }
        }

        mHostRule.startHost()

        waitForListViewChildren { list ->
            val textView0 = list.getUnboxedListItem<TextView>(0)
            val textView1 = list.getUnboxedListItem<TextView>(1)
            val textView2 = list.getUnboxedListItem<TextView>(2)
            val textView3 = list.getUnboxedListItem<TextView>(3)
            assertThat(textView0.text.toString()).isEqualTo("Row 0")
            assertThat(textView1.text.toString()).isEqualTo("Row 1")
            assertThat(textView2.text.toString()).isEqualTo("Row 2")
            assertThat(textView3.text.toString()).isEqualTo("Row 3")
        }
    }

    @Test
    fun itemContent_defaultAlignment_doesNotWrapItem() {
        TestGlanceAppWidget.uiDefinition = {
            LazyColumn {
                item {
                    Text("Row item 0")
                }
            }
        }

        mHostRule.startHost()

        waitForListViewChildren { list ->
            list.getUnboxedListItem<TextView>(0)
        }
    }

    @Test
    fun itemContent_startAlignment_doesNotWrapItem() {
        TestGlanceAppWidget.uiDefinition = {
            LazyColumn(horizontalAlignment = Alignment.Start) {
                item {
                    Text("Row item 0")
                }
            }
        }

        mHostRule.startHost()

        waitForListViewChildren { list ->
            list.getUnboxedListItem<TextView>(0)
        }
    }

    @Test
    fun itemContent_centerAlignment_wrapsItemWithGravityCenterContainer() {
        TestGlanceAppWidget.uiDefinition = {
            LazyColumn(horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
                item {
                    Text("Row item 0")
                }
            }
        }

        mHostRule.startHost()

        waitForListViewChildren { list ->
            val listItem = list.getUnboxedListItem<FrameLayout>(0)
            val layoutParams =
                assertIs<FrameLayout.LayoutParams>(listItem.notGoneChildren.first().layoutParams)
            assertThat(layoutParams.gravity).isEqualTo(Gravity.CENTER)
        }
    }

    @Test
    fun itemContent_endAlignment_wrapsItemWithGravityEndContainer() {
        TestGlanceAppWidget.uiDefinition = {
            LazyColumn(horizontalAlignment = Alignment.Horizontal.End) {
                item {
                    Text("Row item 0")
                }
            }
        }

        mHostRule.startHost()

        waitForListViewChildren { list ->
            val listItem = list.getUnboxedListItem<FrameLayout>(0)
            val layoutParams =
                assertIs<FrameLayout.LayoutParams>(listItem.notGoneChildren.first().layoutParams)
            assertThat(layoutParams.gravity).isEqualTo(Gravity.END + Gravity.CENTER_VERTICAL)
        }
    }

    @Test
    fun itemContent_multipleViews() {
        TestGlanceAppWidget.uiDefinition = {
            LazyColumn {
                item {
                    Text("Row item 0")
                    Text("Row item 1")
                }
            }
        }

        mHostRule.startHost()

        waitForListViewChildren { list ->
            val row = list.getUnboxedListItem<FrameLayout>(0)
            val (rowItem0, rowItem1) = row.notGoneChildren.toList()
            assertIs<TextView>(rowItem0)
            assertIs<TextView>(rowItem1)
            assertThat(rowItem0.text.toString()).isEqualTo("Row item 0")
            assertThat(rowItem1.text.toString()).isEqualTo("Row item 1")
        }
    }

    @Test
    fun adapter_setsViewTypeCount() {
        TestGlanceAppWidget.uiDefinition = {
            LazyColumn {
                item { Text("Item") }
            }
        }

        mHostRule.startHost()

        waitForListViewChildren { list ->
            // The adapter may report more layout types than the provider declared, e.g. adding a
            // loading layout
            assertThat(list.adapter.viewTypeCount).isAtLeast(TopLevelLayoutsCount)
        }
    }

    @Test
    fun clickable_addsClickHandlers() {
        TestGlanceAppWidget.uiDefinition = {
            LazyColumn {
                item {
                    Text(
                        "Text",
                        modifier = GlanceModifier.clickable(actionLaunchActivity<Activity>())
                    )
                    Button(
                        "Button",
                        onClick = actionLaunchActivity<Activity>()
                    )
                }
            }
        }

        mHostRule.startHost()

        waitForListViewChildren { list ->
            val row = list.getUnboxedListItem<FrameLayout>(0)
            val (rowItem0, rowItem1) = row.notGoneChildren.toList()
            assertIs<TextView>(rowItem0)
            assertIs<Button>(rowItem1)
            assertThat(rowItem0.hasOnClickListeners()).isTrue()
            assertThat(rowItem1.hasOnClickListeners()).isTrue()
        }
    }

    private fun waitForListViewChildren(action: (list: ListView) -> Unit = {}) {
        mHostRule.onHostView { }

        mHostRule.runAndObserveUntilDraw(condition = "ListView did not load in time") {
            mHostRule.mHostView.let { host ->
                val list = host.findChildByType<ListView>()
                host.childCount > 0 &&
                    list?.let { it.childCount > 0 && it.adapter != null } ?: false
            }
        }

        // b/206630723: Change in Ie0923d5de57d328b2cdd78219f0049bf38cb9bed to work around KT-49573
        mHostRule.onUnboxedHostView2(action)
    }

    private inline fun <reified T : View> ListView.getUnboxedListItem(position: Int): T {
        val remoteViewFrame = assertIs<FrameLayout>(getChildAt(position))
        // Android S- have a RemoteViewsAdapter$RemoteViewsFrameLayout first, Android T+ do not.
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
            return remoteViewFrame.getChildAt(0).getTargetView()
        }
        val frame = assertIs<FrameLayout>(remoteViewFrame.getChildAt(0))
        return frame.getChildAt(0).getTargetView()
    }
}

// b/206630723 Change in Ie0923d5de57d328b2cdd78219f0049bf38cb9bed to work around KT-49573
inline fun AppWidgetHostRule.onUnboxedHostView2(crossinline block: (ListView) -> Unit) {
    onHostActivity {
        val boxingView = assertIs<ViewGroup>(mHostView.getChildAt(0))
        block(boxingView.children.single().getTargetView())
    }
}
