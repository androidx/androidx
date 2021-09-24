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

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.glance.Modifier
import androidx.glance.appwidget.layout.LazyColumn
import androidx.glance.appwidget.layout.ReservedItemIdRangeEnd
import androidx.glance.layout.Text
import androidx.glance.layout.padding
import androidx.glance.unit.Dp
import androidx.glance.unit.dp
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
            LazyColumn(modifier = Modifier.padding(5.dp, 6.dp, 7.dp, 8.dp)) {
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

    @FlakyTest
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

    @FlakyTest
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

    @FlakyTest
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
            val textView1 = list.getListChildAt<TextView>(0)
            val textView2 = list.getListChildAt<TextView>(1)
            val textView3 = list.getListChildAt<TextView>(2)
            val textView4 = list.getListChildAt<TextView>(3)
            assertThat(textView1.text.toString()).isEqualTo("Row 0")
            assertThat(textView2.text.toString()).isEqualTo("Row 1")
            assertThat(textView3.text.toString()).isEqualTo("Row 2")
            assertThat(textView4.text.toString()).isEqualTo("Row 3")
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
            val textView1 = list.getListChildAt<TextView>(0)
            val textView2 = list.getListChildAt<TextView>(1)
            val textView3 = list.getListChildAt<TextView>(2)
            val textView4 = list.getListChildAt<TextView>(3)
            assertThat(textView1.text.toString()).isEqualTo("Row 0")
            assertThat(textView2.text.toString()).isEqualTo("Row 1")
            assertThat(textView3.text.toString()).isEqualTo("Row 2")
            assertThat(textView4.text.toString()).isEqualTo("Row 3")
        }
    }

    @FlakyTest
    @Test
    fun itemContent_emptyItem() {
        TestGlanceAppWidget.uiDefinition = {
            LazyColumn {
                item { }
            }
        }

        mHostRule.startHost()

        waitForListViewChildren { list ->
            val row = list.getListChildAt<RelativeLayout>(0)
            assertThat(row.childCount).isEqualTo(0)
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
            val row = list.getListChildAt<RelativeLayout>(0)
            val rowItem0 = assertIs<TextView>(row.getChildAt(0))
            val rowItem1 = assertIs<TextView>(row.getChildAt(1))
            assertThat(rowItem0.text.toString()).isEqualTo("Row item 0")
            assertThat(rowItem1.text.toString()).isEqualTo("Row item 1")
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

        mHostRule.onHostView {
            action(mHostRule.mHostView.findChildByType<ListView>()!!)
        }
    }

    private inline fun <reified V : View> ListView.getListChildAt(position: Int): V {
        return assertIs<V>(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                this.getChildAt(position)
            } else {
                // Pre-S, a RemoteViewsAdapter is used which adds an extra wrapper FrameLayout
                (this.getChildAt(position) as ViewGroup).getChildAt(0)
            }
        )
    }
}
