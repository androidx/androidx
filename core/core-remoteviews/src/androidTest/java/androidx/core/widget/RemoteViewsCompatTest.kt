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

package androidx.core.widget

import android.Manifest.permission
import android.app.PendingIntent
import android.app.UiAutomation
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Parcel
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnDrawListener
import android.widget.Adapter
import android.widget.ListView
import android.widget.RemoteViews
import android.widget.TextView
import androidx.core.os.BuildCompat
import androidx.core.remoteviews.test.R
import androidx.core.widget.RemoteViewsCompat.RemoteCollectionItems
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertFailsWith
import kotlin.test.fail

@MediumTest
public class RemoteViewsCompatTest {
    private val mUsingBackport = !BuildCompat.isAtLeastS()
    private val mContext = ApplicationProvider.getApplicationContext<Context>()
    private val mPackageName = mContext.packageName
    private val mAppWidgetManager = AppWidgetManager.getInstance(mContext)

    @Rule
    @JvmField
    public val mActivityTestRule: ActivityScenarioRule<AppWidgetHostTestActivity> =
        ActivityScenarioRule(AppWidgetHostTestActivity::class.java)

    private lateinit var mRemoteViews: RemoteViews
    private lateinit var mHostView: AppWidgetHostView
    private var mAppWidgetId = 0

    private val mUiAutomation: UiAutomation
        get() = InstrumentationRegistry.getInstrumentation().uiAutomation

    private val mListView: ListView
        get() = mHostView.getChildAt(0) as ListView

    @Before
    public fun setUp() {

        if (Build.VERSION.SDK_INT < 29) {
            // We need to have at least SDK 29 to use adoptShellPermissionIdentity to host app
            // widgets.
            fail("RemoteViewsCompatWidgetTest can only run on 29+ devices")
        }
        mUiAutomation.adoptShellPermissionIdentity(permission.BIND_APPWIDGET)

        mActivityTestRule.scenario.onActivity { activity ->
            mHostView = activity.bindAppWidget()
        }

        mAppWidgetId = mHostView.appWidgetId
        mRemoteViews = RemoteViews(mPackageName, R.layout.remote_views_list)
        mAppWidgetManager.updateAppWidget(mAppWidgetId, mRemoteViews)

        // Wait until the remote views has been added to the host view.
        observeDrawUntil { mHostView.childCount > 0 }
    }

    @After
    public fun tearDown() {
        mUiAutomation.dropShellPermissionIdentity()
    }

    @Test
    @Ignore("b/190514964")
    public fun testParcelingAndUnparceling() {
        val items = RemoteCollectionItems.Builder()
            .setHasStableIds(true)
            .setViewTypeCount(10)
            .addItem(id = 3, RemoteViews(mPackageName, R.layout.list_view_row))
            .addItem(id = 5, RemoteViews(mPackageName, R.layout.list_view_row2))
            .build()

        val parcel = Parcel.obtain()
        val unparceled = try {
            items.writeToParcel(parcel, /* flags= */ 0)
            parcel.setDataPosition(0)
            RemoteCollectionItems(parcel)
        } finally {
            parcel.recycle()
        }

        assertThat(unparceled.itemCount).isEqualTo(2)
        assertThat(unparceled.getItemId(0)).isEqualTo(3)
        assertThat(unparceled.getItemId(1)).isEqualTo(5)
        assertThat(unparceled.getItemView(0).layoutId).isEqualTo(R.layout.list_view_row)
        assertThat(unparceled.getItemView(1).layoutId).isEqualTo(R.layout.list_view_row2)
        assertThat(unparceled.hasStableIds()).isTrue()
        assertThat(unparceled.viewTypeCount).isEqualTo(10)
    }

    @Test
    @Ignore("b/190514964")
    public fun testBuilder_empty() {
        val items = RemoteCollectionItems.Builder().build()

        assertThat(items.itemCount).isEqualTo(0)
        assertThat(items.viewTypeCount).isEqualTo(1)
        assertThat(items.hasStableIds()).isFalse()
    }

    @Test
    @Ignore("b/190514964")
    public fun testBuilder_viewTypeCountUnspecified() {
        val firstItem = RemoteViews(mPackageName, R.layout.list_view_row)
        val secondItem = RemoteViews(mPackageName, R.layout.list_view_row2)
        val items = RemoteCollectionItems.Builder()
            .setHasStableIds(true)
            .addItem(id = 3, firstItem)
            .addItem(id = 5, secondItem)
            .build()

        assertThat(items.itemCount).isEqualTo(2)
        assertThat(items.getItemId(0)).isEqualTo(3)
        assertThat(items.getItemId(1)).isEqualTo(5)
        assertThat(items.getItemView(0).layoutId).isEqualTo(R.layout.list_view_row)
        assertThat(items.getItemView(1).layoutId).isEqualTo(R.layout.list_view_row2)
        assertThat(items.hasStableIds()).isTrue()
        // The view type count should be derived from the number of different layout ids if
        // unspecified.
        assertThat(items.viewTypeCount).isEqualTo(2)
    }

    @Test
    @Ignore("b/190514964")
    public fun testBuilder_viewTypeCountSpecified() {
        val firstItem = RemoteViews(mPackageName, R.layout.list_view_row)
        val secondItem = RemoteViews(mPackageName, R.layout.list_view_row2)
        val items = RemoteCollectionItems.Builder()
            .addItem(id = 3, firstItem)
            .addItem(id = 5, secondItem)
            .setViewTypeCount(15)
            .build()

        assertThat(items.viewTypeCount).isEqualTo(15)
    }

    @Test
    @Ignore("b/190514964")
    public fun testBuilder_repeatedIdsAndLayouts() {
        val firstItem = RemoteViews(mPackageName, R.layout.list_view_row)
        val secondItem = RemoteViews(mPackageName, R.layout.list_view_row)
        val thirdItem = RemoteViews(mPackageName, R.layout.list_view_row)
        val items = RemoteCollectionItems.Builder()
            .setHasStableIds(false)
            .addItem(id = 42, firstItem)
            .addItem(id = 42, secondItem)
            .addItem(id = 42, thirdItem)
            .build()

        assertThat(items.itemCount).isEqualTo(3)
        assertThat(items.getItemId(0)).isEqualTo(42)
        assertThat(items.getItemId(1)).isEqualTo(42)
        assertThat(items.getItemId(2)).isEqualTo(42)
        assertThat(items.getItemView(0)).isSameInstanceAs(firstItem)
        assertThat(items.getItemView(1)).isSameInstanceAs(secondItem)
        assertThat(items.getItemView(2)).isSameInstanceAs(thirdItem)
        assertThat(items.hasStableIds()).isFalse()
        assertThat(items.viewTypeCount).isEqualTo(1)
    }

    @Test
    @Ignore("b/190514964")
    public fun testBuilder_viewTypeCountLowerThanLayoutCount() {
        assertFailsWith(IllegalArgumentException::class) {
            RemoteCollectionItems.Builder()
                .setHasStableIds(true)
                .setViewTypeCount(1)
                .addItem(3, RemoteViews(mPackageName, R.layout.list_view_row))
                .addItem(5, RemoteViews(mPackageName, R.layout.list_view_row2))
                .build()
        }
    }

    @Test
    @Ignore("b/190514964")
    public fun testServiceIntent_hasSameUriForSameIds() {
        val intent1 = RemoteViewsCompatService.createIntent(mContext, appWidgetId = 1, viewId = 42)
        val intent2 = RemoteViewsCompatService.createIntent(mContext, appWidgetId = 1, viewId = 42)

        assertThat(intent1.data).isEqualTo(intent2.data)
    }

    @Test
    @Ignore("b/190514964")
    public fun testServiceIntent_hasDifferentUriForDifferentWidgetIds() {
        val intent1 = RemoteViewsCompatService.createIntent(mContext, appWidgetId = 1, viewId = 42)
        val intent2 = RemoteViewsCompatService.createIntent(mContext, appWidgetId = 2, viewId = 42)

        assertThat(intent1.data).isNotEqualTo(intent2.data)
    }

    @Test
    @Ignore("b/190514964")
    public fun testServiceIntent_hasDifferentUriForDifferentViewIds() {
        val intent1 = RemoteViewsCompatService.createIntent(mContext, appWidgetId = 1, viewId = 42)
        val intent2 = RemoteViewsCompatService.createIntent(mContext, appWidgetId = 1, viewId = 43)

        assertThat(intent1.data).isNotEqualTo(intent2.data)
    }

    @Test
    @Ignore("b/190514964")
    public fun testSetRemoteAdapter_emptyCollection() {
        val items = RemoteCollectionItems.Builder().build()

        RemoteViewsCompat.setRemoteAdapter(
            mContext,
            mRemoteViews,
            mAppWidgetId,
            R.id.list_view,
            items
        )
        mAppWidgetManager.updateAppWidget(mAppWidgetId, mRemoteViews)

        observeDrawUntil { mListView.adapter != null }

        assertThat(mListView.childCount).isEqualTo(0)
        assertThat(mListView.adapter.count).isEqualTo(0)
        assertThat(mListView.adapter.viewTypeCount).isAtLeast(1)
        assertThat(mListView.adapter.hasStableIds()).isFalse()
    }

    @Test
    @Ignore("b/190514964")
    public fun testSetRemoteAdapter_withItems() {
        val items = RemoteCollectionItems.Builder()
            .setHasStableIds(true)
            .addItem(id = 10, createTextRow("Hello"))
            .addItem(id = 11, createTextRow("World"))
            .build()

        RemoteViewsCompat.setRemoteAdapter(
            mContext,
            mRemoteViews,
            mAppWidgetId,
            R.id.list_view,
            items
        )
        mAppWidgetManager.updateAppWidget(mAppWidgetId, mRemoteViews)

        observeDrawUntil { mListView.adapter != null && mListView.childCount == 2 }

        val adapter = mListView.adapter
        assertThat(adapter.count).isEqualTo(2)
        assertThat(adapter.getItemViewType(1)).isEqualTo(adapter.getItemViewType(0))
        assertThat(adapter.getItemId(0)).isEqualTo(10)
        assertThat(adapter.getItemId(1)).isEqualTo(11)

        assertThat(mListView.adapter.hasStableIds()).isTrue()
        assertThat(mListView.childCount).isEqualTo(2)
        assertThat(getListChildAt<TextView>(0).text.toString()).isEqualTo("Hello")
        assertThat(getListChildAt<TextView>(1).text.toString()).isEqualTo("World")
    }

    @Test
    @Ignore("b/190514964")
    public fun testSetRemoteAdapter_clickListener() {
        val action = "my-action"
        val receiver = TestBroadcastReceiver()
        mContext.registerReceiver(receiver, IntentFilter(action))
        val pendingIntent = PendingIntent.getBroadcast(
            mContext,
            0,
            Intent(action).setPackage(mPackageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        mRemoteViews.setPendingIntentTemplate(R.id.list_view, pendingIntent)

        val item2 = RemoteViews(mPackageName, R.layout.list_view_row2)
        item2.setTextViewText(R.id.text, "Clickable")
        item2.setOnClickFillInIntent(R.id.text, Intent().putExtra("my-extra", 42))

        val items = RemoteCollectionItems.Builder()
            .setHasStableIds(true)
            .addItem(id = 10, createTextRow("Hello"))
            .addItem(id = 11, createTextRow("World"))
            .addItem(id = 12, item2)
            .build()
        RemoteViewsCompat.setRemoteAdapter(
            mContext,
            mRemoteViews,
            mAppWidgetId,
            R.id.list_view,
            items
        )
        mAppWidgetManager.updateAppWidget(mAppWidgetId, mRemoteViews)
        observeDrawUntil { mListView.adapter != null && mListView.childCount == 3 }

        val adapter: Adapter = mListView.adapter
        assertThat(adapter.count).isEqualTo(3)
        assertThat(adapter.getItemViewType(0)).isEqualTo(adapter.getItemViewType(1))
        assertThat(adapter.getItemViewType(0)).isNotEqualTo(adapter.getItemViewType(2))
        assertThat(adapter.getItemId(0)).isEqualTo(10)
        assertThat(adapter.getItemId(1)).isEqualTo(11)
        assertThat(adapter.getItemId(2)).isEqualTo(12)
        assertThat(adapter.hasStableIds()).isTrue()

        assertThat(mListView.childCount).isEqualTo(3)
        val textView2 = getListChildAt<ViewGroup>(2).getChildAt(0) as TextView
        assertThat(getListChildAt<TextView>(0).text.toString()).isEqualTo("Hello")
        assertThat(getListChildAt<TextView>(1).text.toString()).isEqualTo("World")
        assertThat(textView2.text.toString()).isEqualTo("Clickable")

        // View being clicked should launch the intent.
        val receiverIntent = receiver.runAndAwaitIntentReceived {
            textView2.performClick()
        }
        assertThat(receiverIntent.getIntExtra("my-extra", 0)).isEqualTo(42)
        mContext.unregisterReceiver(receiver)
    }

    @Test
    @Ignore("b/190514964")
    public fun testSetRemoteAdapter_multipleCalls() {
        var items = RemoteCollectionItems.Builder()
            .setHasStableIds(true)
            .addItem(id = 10, createTextRow("Hello"))
            .addItem(id = 11, createTextRow("World"))
            .build()
        RemoteViewsCompat.setRemoteAdapter(
            mContext,
            mRemoteViews,
            mAppWidgetId,
            R.id.list_view,
            items
        )
        mAppWidgetManager.updateAppWidget(mAppWidgetId, mRemoteViews)
        observeDrawUntil { mListView.adapter != null && mListView.childCount == 2 }

        items = RemoteCollectionItems.Builder()
            .setHasStableIds(true)
            .addItem(id = 20, createTextRow("Bonjour"))
            .addItem(id = 21, createTextRow("le"))
            .addItem(id = 22, createTextRow("monde"))
            .build()
        RemoteViewsCompat.setRemoteAdapter(
            mContext,
            mRemoteViews,
            mAppWidgetId,
            R.id.list_view,
            items
        )
        mAppWidgetManager.updateAppWidget(mAppWidgetId, mRemoteViews)
        observeDrawUntil { mListView.childCount == 3 }

        val adapter: Adapter = mListView.adapter
        assertThat(adapter.count).isEqualTo(3)
        assertThat(adapter.getItemId(0)).isEqualTo(20)
        assertThat(adapter.getItemId(1)).isEqualTo(21)
        assertThat(adapter.getItemId(2)).isEqualTo(22)

        assertThat(mListView.childCount).isEqualTo(3)
        assertThat(getListChildAt<TextView>(0).text.toString()).isEqualTo("Bonjour")
        assertThat(getListChildAt<TextView>(1).text.toString()).isEqualTo("le")
        assertThat(getListChildAt<TextView>(2).text.toString()).isEqualTo("monde")
    }

    private fun createTextRow(text: String): RemoteViews {
        return RemoteViews(mPackageName, R.layout.list_view_row)
            .also { it.setTextViewText(R.id.text, text) }
    }

    private fun observeDrawUntil(test: () -> Boolean) {
        val latch = CountDownLatch(1)
        val onDrawListener = OnDrawListener {
            if (test()) latch.countDown()
        }

        mActivityTestRule.scenario.onActivity {
            mHostView.viewTreeObserver.addOnDrawListener(onDrawListener)
        }

        val countedDown = latch.await(5, TimeUnit.SECONDS)

        mActivityTestRule.scenario.onActivity {
            mHostView.viewTreeObserver.removeOnDrawListener(onDrawListener)
        }

        if (!countedDown && !test()) {
            fail("Expected condition to be met within 5 seconds")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <V : View> getListChildAt(position: Int): V {
        return if (mUsingBackport) {
            // When using RemoteViewsAdapter, an extra wrapper FrameLayout is added.
            (mListView.getChildAt(position) as ViewGroup).getChildAt(0) as V
        } else {
            mListView.getChildAt(position) as V
        }
    }

    private inner class TestBroadcastReceiver : BroadcastReceiver() {
        private lateinit var mCountDownLatch: CountDownLatch

        private var mIntent: Intent? = null

        override fun onReceive(context: Context, intent: Intent) {
            mIntent = intent
            mCountDownLatch.countDown()
        }

        fun runAndAwaitIntentReceived(runnable: () -> Unit): Intent {
            mCountDownLatch = CountDownLatch(1)

            mActivityTestRule.scenario.onActivity { runnable() }

            mCountDownLatch.await(5, TimeUnit.SECONDS)

            return mIntent ?: fail("Expected intent to be received within five seconds")
        }
    }
}