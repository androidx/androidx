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
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlin.test.assertNotNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.TimeoutCancellationException
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
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.Test

@MediumTest
@SdkSuppress(minSdkVersion = 29)
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
            runBlocking {
                adapter.waitForItemIdAtPosition(0, ReservedItemIdRangeEnd)
                adapter.waitForItemIdAtPosition(1, ReservedItemIdRangeEnd - 1)
            }
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
            runBlocking {
                adapter.waitForItemIdAtPosition(0, ReservedItemIdRangeEnd)
                adapter.waitForItemIdAtPosition(1, 1L)
            }
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
            runBlocking {
                adapter.waitForItemIdAtPosition(0, ReservedItemIdRangeEnd)
                adapter.waitForItemIdAtPosition(1, ReservedItemIdRangeEnd - 1)
                adapter.waitForItemIdAtPosition(2, ReservedItemIdRangeEnd - 2)
            }
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
            runBlocking {
                adapter.waitForItemIdAtPosition(0, 0L)
                adapter.waitForItemIdAtPosition(1, 2L)
                adapter.waitForItemIdAtPosition(2, 4L)
                adapter.waitForItemIdAtPosition(3, ReservedItemIdRangeEnd - 3)
            }
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
            val textView0 =
                list.getViewFromUnboxedListItem<TextView>(itemPosition = 0, viewPosition = 0)
            val textView1 =
                list.getViewFromUnboxedListItem<TextView>(itemPosition = 1, viewPosition = 0)
            val textView2 =
                list.getViewFromUnboxedListItem<TextView>(itemPosition = 2, viewPosition = 0)
            val textView3 =
                list.getViewFromUnboxedListItem<TextView>(itemPosition = 3, viewPosition = 0)
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
            val textView0 =
                list.getViewFromUnboxedListItem<TextView>(itemPosition = 0, viewPosition = 0)
            val textView1 =
                list.getViewFromUnboxedListItem<TextView>(itemPosition = 1, viewPosition = 0)
            val textView2 =
                list.getViewFromUnboxedListItem<TextView>(itemPosition = 2, viewPosition = 0)
            val textView3 =
                list.getViewFromUnboxedListItem<TextView>(itemPosition = 3, viewPosition = 0)
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
            list.getViewFromUnboxedListItem<TextView>(itemPosition = 0, viewPosition = 0)
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
            list.getViewFromUnboxedListItem<TextView>(itemPosition = 0, viewPosition = 0)
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
    fun adapter_emptyList() {
        TestGlanceAppWidget.uiDefinition = {
            LazyColumn { }
        }

        mHostRule.startHost()

        mHostRule.waitForListView { list ->
            assertThat(list.childCount).isEqualTo(0)
            assertThat(list.adapter.count).isEqualTo(0)
            assertThat(list.adapter.viewTypeCount).isAtLeast(1)
            assertThat(list.adapter.hasStableIds()).isFalse()
        }
    }

    @Test
    fun adapter_itemContentChangesOnClick_appliedCorrectly() {
        TestGlanceAppWidget.uiDefinition = {
            var count by remember { mutableStateOf(1) }
            LazyColumn {
                item {
                    Text(
                        text = "Row item 0, count $count",
                        modifier = GlanceModifier.clickable {
                            count++
                        })
                }
            }
        }

        mHostRule.startHost()

        mHostRule.waitForListViewChildren { list ->
            val row = list.getUnboxedListItem<FrameLayout>(0)
            val rowItem0 = row.notGoneChildren.first()
            rowItem0.performClick()
        }

        mHostRule.waitForListViewChildWithText(text = "Row item 0, count 2") {}
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
                val button =
                    list.getViewFromUnboxedListItem<Button>(itemPosition = it, viewPosition = 0)
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
                val button = list.getViewFromUnboxedListItem<FrameLayout>(
                    itemPosition = it,
                    viewPosition = 0
                )
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

/**
 * Wait until the ListView is loaded and has an adapter (irrespective of whether it has children or
 * not). Use waitForListViewChildren if the list is expected to have children.
 */
internal fun AppWidgetHostRule.waitForListView(action: (list: ListView) -> Unit = {}) {
    onHostView { }

    runAndObserveUntilDraw(condition = "ListView did not load in time") {
        mHostView.let { host ->
            val list = host.findChildByType<ListView>()
            host.childCount > 0 && list != null && list.adapter != null
        }
    }

    onUnboxedHostView(action)
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

internal fun AppWidgetHostRule.waitForListViewChildWithText(
    text: String,
    action: (list: ListView) -> Unit = {}
) {
    onHostView { }

    runAndObserveUntilDraw(condition = "List child with text '$text' not load in time") {
        mHostView.let { host ->
            val list = host.findChildByType<ListView>()
            host.childCount > 0 && list?.isItemLoaded(text) ?: false
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
    // Get adapter item at position
    val remoteViewFrame = assertIs<FrameLayout>(getChildAt(position))

    // Find Glance's root view for each item
    val rootView = assertNotNull(remoteViewFrame.findViewById(R.id.rootView)) as ViewGroup
    // The RemoteViews created in translateComposition for holding an item
    return rootView.getChildAt(0).getTargetView()
}

private suspend fun ListAdapter.waitForItemIdAtPosition(
    position: Int,
    expectedItemId: Long
) {
    var actualItemId = getItemId(position)
    try {
        withTimeout(600) {
            while (actualItemId != expectedItemId) {
                Log.i(
                    "LazyColumnTest", "ItemId at $position was expected to be " +
                        "$expectedItemId, but was $actualItemId. Waiting for 200 ms."
                )
                delay(200) // Wait before retrying
                actualItemId = getItemId(position)
            }
        }
    } catch (e: TimeoutCancellationException) {
        throw AssertionError(
            "ItemId at $position was expected to be $expectedItemId, but was $actualItemId"
        )
    }
}

internal inline fun <reified T : View> ListView.getViewFromUnboxedListItem(
    itemPosition: Int,
    viewPosition: Int
): T {
    // Box added during normalization to allow aligning item contents per the alignment set on
    // LazyColumn
    val alignmentView = assertIs<FrameLayout>(getUnboxedListItem(itemPosition))
    return alignmentView.getChildAt(viewPosition).getTargetView()
}
