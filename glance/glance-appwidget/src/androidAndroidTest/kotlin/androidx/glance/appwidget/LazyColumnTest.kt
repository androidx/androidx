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
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.TextView
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.ReservedItemIdRangeEnd
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.Alignment
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
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

        mHostRule.waitForListViewChildren { list ->
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

        mHostRule.waitForListViewChildren { list ->
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

        mHostRule.waitForListViewChildren { list ->
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

        mHostRule.waitForListViewChildren { list ->
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

        mHostRule.waitForListViewChildren { list ->
            val adapter = list.adapter!!
            assertThat(adapter.count).isEqualTo(4)
            assertThat(adapter.hasStableIds()).isTrue()
            assertThat(adapter.getItemId(0)).isEqualTo(0L)
            assertThat(adapter.getItemId(1)).isEqualTo(2L)
            assertThat(adapter.getItemId(2)).isEqualTo(4L)
            assertThat(adapter.getItemId(3)).isEqualTo(ReservedItemIdRangeEnd - 3)
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

        mHostRule.waitForListViewChildren { list ->
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

        mHostRule.waitForListViewChildren { list ->
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

        mHostRule.waitForListViewChildren { list ->
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

        mHostRule.waitForListViewChildren { list ->
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

        mHostRule.waitForListViewChildren { list ->
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

        mHostRule.waitForListViewChildren { list ->
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

        mHostRule.waitForListViewChildren { list ->
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

        mHostRule.waitForListViewChildren { list ->
            // The adapter may report more layout types than the provider declared, e.g. adding a
            // loading layout
            assertThat(list.adapter.viewTypeCount).isAtLeast(TopLevelLayoutsCount)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun clickable_addsClickHandlers() {
        TestGlanceAppWidget.uiDefinition = {
            LazyColumn {
                item {
                    Text(
                        "Text",
                        modifier = GlanceModifier.clickable(actionStartActivity<Activity>())
                    )
                    Button(
                        "Button",
                        onClick = actionStartActivity<Activity>()
                    )
                }
            }
        }

        mHostRule.startHost()

        mHostRule.waitForListViewChildren { list ->
            val row = list.getUnboxedListItem<FrameLayout>(0)
            val (rowItem0, rowItem1) = row.notGoneChildren.toList()
            // Clickable text items are wrapped in a FrameLayout.
            assertIs<FrameLayout>(rowItem0)
            assertIs<Button>(rowItem1)
            assertThat(rowItem0.hasOnClickListeners()).isTrue()
            assertThat(rowItem1.hasOnClickListeners()).isTrue()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 29, maxSdkVersion = 30)
    fun clickable_backportButton_addsClickHandlers() {
        TestGlanceAppWidget.uiDefinition = {
            LazyColumn {
                item {
                    Text(
                        "Text",
                        modifier = GlanceModifier.clickable(actionStartActivity<Activity>())
                    )
                    Button(
                        "Button",
                        onClick = actionStartActivity<Activity>()
                    )
                }
            }
        }

        mHostRule.startHost()

        mHostRule.waitForListViewChildren { list ->
            val row = list.getUnboxedListItem<FrameLayout>(0)
            val (rowItem0, rowItem1) = row.notGoneChildren.toList()
            // Clickable text items are wrapped in a FrameLayout.
            assertIs<FrameLayout>(rowItem0)
            // backport buttons are implemented using FrameLayout.
            assertIs<FrameLayout>(rowItem1)
            assertThat(rowItem0.hasOnClickListeners()).isTrue()
            assertThat(rowItem1.hasOnClickListeners()).isTrue()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun clickTriggersOnlyOneLambda() = runBlocking {
        val received = MutableStateFlow(-1)
        TestGlanceAppWidget.uiDefinition = {
            LazyColumn {
                items((0..4).toList()) {
                    Button(
                        "$it",
                        onClick = {
                            launch { received.emit(it) }
                        }
                    )
                }
            }
        }

        mHostRule.startHost()

        val buttons = arrayOfNulls<Button>(5)
        mHostRule.waitForListViewChildren { list ->
            for (it in 0..4) {
                val button = list.getUnboxedListItem<Button>(it)
                buttons[it] = button
            }
        }
        (0..4).shuffled().forEach { index ->
            mHostRule.onHostActivity {
                buttons[index]!!.performClick()
            }
            val lastClicked = received.debounce(500.milliseconds).first()
            assertThat(lastClicked).isEqualTo(index)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 29, maxSdkVersion = 30)
    fun clickTriggersOnlyOneLambda_backportButton() = runBlocking {
        val received = MutableStateFlow(-1)
        TestGlanceAppWidget.uiDefinition = {
            LazyColumn {
                items((0..4).toList()) {
                    Button(
                        "$it",
                        onClick = {
                            launch { received.emit(it) }
                        }
                    )
                }
            }
        }

        mHostRule.startHost()

        val buttons = arrayOfNulls<FrameLayout>(5)
        mHostRule.waitForListViewChildren { list ->
            for (it in 0..4) {
                val button = list.getUnboxedListItem<FrameLayout>(it)
                buttons[it] = assertIs<FrameLayout>(button)
            }
        }
        (0..4).shuffled().forEach { index ->
            mHostRule.onHostActivity {
                buttons[index]!!.performClick()
            }
            val lastClicked = received.debounce(500.milliseconds).first()
            assertThat(lastClicked).isEqualTo(index)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 29, maxSdkVersion = 31)
    fun listCanBeUpdated_RemoteViewsService() = runTest {
        val countFlow = MutableStateFlow(0)
        TestGlanceAppWidget.uiDefinition = {
            val count by countFlow.collectAsState()
            LazyColumn {
                items(count) { Text("$it") }
            }
        }

        mHostRule.startHost()
        mHostRule.waitForListViewChildCount(countFlow.value)
        (1..10).forEach { next ->
            countFlow.emit(next)
            mHostRule.waitForListViewChildCount(next)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 32)
    fun listCanBeUpdated_RemoteCollectionItems() = runTest {
        val countFlow = MutableStateFlow(0)
        TestGlanceAppWidget.uiDefinition = {
            val count by countFlow.collectAsState()
            LazyColumn {
                items(count) { Text("$it") }
            }
        }

        mHostRule.startHost()
        mHostRule.waitForListViewChildCount(countFlow.value)
        (1..10).forEach { next ->
            countFlow.emit(next)
            mHostRule.waitForListViewChildCount(next)
        }
    }
}

internal fun AppWidgetHostRule.waitForListViewChildren(action: (list: ListView) -> Unit = {}) {
    onHostView { }

    runAndObserveUntilDraw(condition = "ListView did not load in time") {
        mHostView.let { host ->
            val list = host.findChildByType<ListView>()
            host.childCount > 0 && list?.areItemsFullyLoaded() ?: false
        }
    }

    onUnboxedHostView(action)
}

/**
 * Wait until the first ListView child under the root AppWidgetHostView has [count] children.
 *
 * Suspending version that does not timeout, instead relies on the `runTest` timeout.
 */
internal suspend fun AppWidgetHostRule.waitForListViewChildCount(count: Int) {
    val resume = Channel<Unit>(Channel.CONFLATED)
    fun test() = mHostView.findChildByType<ListView>()?.childCount == count
    val onDrawListener = ViewTreeObserver.OnDrawListener {
        if (test()) resume.trySend(Unit)
    }

    onHostActivity {
        // If test is already true, do not wait for the next draw to resume
        if (test()) resume.trySend(Unit)
        mHostView.viewTreeObserver.addOnDrawListener(onDrawListener)
    }
    try {
        resume.receive()
    } finally {
        onHostActivity {
            mHostView.viewTreeObserver.removeOnDrawListener(onDrawListener)
        }
    }
}

/**
 * Returns a flow that mirrors the original flow, but filters out values that are followed by the
 * newer values within the given timeout.
 */
fun <T> Flow<T>.debounce(timeout: Duration): Flow<T> = channelFlow {
    collectLatest {
        delay(timeout)
        send(it)
    }
}.buffer(0)

internal inline fun <reified T : View> ListView.getUnboxedListItem(position: Int): T {
    val remoteViewFrame = assertIs<FrameLayout>(getChildAt(position))
    // Each list item frame has an explicit focusable = true, see
    // "Glance.AppWidget.Theme.ListChildren" style.
    assertThat(remoteViewFrame.isFocusable).isTrue()

    // Android S- have a RemoteViewsAdapter$RemoteViewsFrameLayout first, Android T+ do not.
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
        return remoteViewFrame.getChildAt(0).getTargetView()
    }
    val frame = assertIs<FrameLayout>(remoteViewFrame.getChildAt(0))
    return frame.getChildAt(0).getTargetView()
}