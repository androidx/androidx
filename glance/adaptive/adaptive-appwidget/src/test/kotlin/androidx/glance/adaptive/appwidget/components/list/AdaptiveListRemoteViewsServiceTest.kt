/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.glance.adaptive.appwidget.components.list

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import androidx.glance.adaptive.appwidget.R
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class AdaptiveListRemoteViewsServiceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        AdaptiveListRemoteViewsService.clearForTesting()
    }

    @After
    fun tearDown() {
        AdaptiveListRemoteViewsService.clearForTesting()
    }

    @Test
    fun serviceStore_defensivelyCopiesAndSuppliesFactoryItems() {
        val row0 =
            RemoteViews(
                context.packageName,
                R.layout.glance_adaptive_appwidget_remoteviews_list_item,
            )
        val row1 =
            RemoteViews(
                context.packageName,
                R.layout.glance_adaptive_appwidget_remoteviews_list_checkbox,
            )
        val mutableInput = mutableListOf(row0, row1)
        val key = "widget/special:-42_W2_night"

        AdaptiveListRemoteViewsService.putItems(key, mutableInput)
        mutableInput.clear()

        val intent = AdaptiveListRemoteViewsService.serviceIntent(context, key)
        assertThat(intent.data?.scheme).isEqualTo("glance-adaptive-list")
        assertThat(intent.data?.authority).isEqualTo("list")
        assertThat(intent.data?.lastPathSegment).isEqualTo(key)
        assertThat(
                intent.filterEquals(AdaptiveListRemoteViewsService.serviceIntent(context, "other"))
            )
            .isFalse()

        val service = Robolectric.setupService(AdaptiveListRemoteViewsService::class.java)
        val factory = service.onGetViewFactory(intent)
        factory.onCreate()
        assertThat(factory.count).isEqualTo(2)
        assertThat(factory.viewTypeCount).isEqualTo(LIST_VIEW_TYPE_COUNT)
        assertThat(factory.hasStableIds()).isFalse()
        assertThat(factory.getItemId(1)).isEqualTo(1L)
        assertThat(factory.getViewAt(0)).isSameInstanceAs(row0)
        assertThat(factory.getViewAt(1)).isSameInstanceAs(row1)
        // Out-of-bounds falls back safely to an empty row rather than throwing.
        assertThat(factory.getViewAt(99)).isNotNull()

        AdaptiveListRemoteViewsService.putItems(key, listOf(row1))
        factory.onDataSetChanged()
        assertThat(factory.count).isEqualTo(1)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    @Test
    fun clickReceiver_dispatchesValidActionAndIgnoresInvalidOrCanceled() {
        val receiver = AdaptiveListClickReceiver()
        val broadcastAction = "androidx.glance.adaptive.appwidget.test.CLICK_DISPATCHED"
        val targetIntent = Intent(broadcastAction).setPackage(context.packageName)
        val targetPendingIntent =
            PendingIntent.getBroadcast(
                context,
                42,
                targetIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        // Wrong or missing action is ignored.
        receiver.onReceive(
            context,
            Intent("unexpected.action").putExtra(EXTRA_LIST_PENDING_INTENT, targetPendingIntent),
        )
        assertThat(shadowOf(context as Application).broadcastIntents).isEmpty()

        // Missing or wrong-type Parcelable extra is safely ignored without crashing.
        receiver.onReceive(context, Intent(ACTION_LIST_ITEM_CLICK))
        receiver.onReceive(
            context,
            Intent(ACTION_LIST_ITEM_CLICK).putExtra(EXTRA_LIST_PENDING_INTENT, Uri.EMPTY),
        )
        assertThat(shadowOf(context as Application).broadcastIntents).isEmpty()

        // Foreign creatorPackage is rejected so a hostile host cannot abuse the mutable template.
        val foreignContext =
            object : ContextWrapper(context) {
                override fun getPackageName(): String = "com.example.foreign"
            }
        receiver.onReceive(
            foreignContext,
            Intent(ACTION_LIST_ITEM_CLICK).putExtra(EXTRA_LIST_PENDING_INTENT, targetPendingIntent),
        )
        assertThat(shadowOf(context as Application).broadcastIntents).isEmpty()

        // Valid action dispatches the embedded PendingIntent.
        receiver.onReceive(
            context,
            Intent(ACTION_LIST_ITEM_CLICK).putExtra(EXTRA_LIST_PENDING_INTENT, targetPendingIntent),
        )
        val afterValidCount = shadowOf(context as Application).broadcastIntents.size
        assertThat(shadowOf(context as Application).broadcastIntents.map { it.action })
            .contains(broadcastAction)

        // Canceled PendingIntent is silently ignored without crashing or delivering a broadcast.
        targetPendingIntent.cancel()
        receiver.onReceive(
            context,
            Intent(ACTION_LIST_ITEM_CLICK).putExtra(EXTRA_LIST_PENDING_INTENT, targetPendingIntent),
        )
        assertThat(shadowOf(context as Application).broadcastIntents).hasSize(afterValidCount)

        // Template PendingIntent targets AdaptiveListClickReceiver with FLAG_MUTABLE on API 31+.
        val template = AdaptiveListClickReceiver.pendingIntentTemplate(context)
        assertThat(template.isBroadcast).isTrue()
        assertThat(shadowOf(template).savedIntent.component?.className)
            .isEqualTo(AdaptiveListClickReceiver::class.java.name)
        assertThat(shadowOf(template).savedIntent.action).isEqualTo(ACTION_LIST_ITEM_CLICK)
        assertThat(shadowOf(template).flags and PendingIntent.FLAG_MUTABLE).isNotEqualTo(0)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    @Test
    fun clickActivity_dispatchesActivityAndBroadcastActionsAndAlwaysFinishes() {
        val activityAction = "androidx.glance.adaptive.appwidget.test.OPEN_ACTIVITY"
        val targetActivityPendingIntent =
            PendingIntent.getActivity(
                context,
                11,
                Intent(activityAction).setPackage(context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        // Wrong action and invalid extra are ignored and still finish the NoDisplay activity.
        val wrongActionActivity =
            Robolectric.buildActivity(
                    AdaptiveListClickActivity::class.java,
                    Intent("unexpected.action")
                        .putExtra(EXTRA_LIST_PENDING_INTENT, targetActivityPendingIntent),
                )
                .create()
                .get()
        assertThat(wrongActionActivity.isFinishing).isTrue()
        assertThat(shadowOf(context as Application).nextStartedActivity).isNull()

        val badExtraActivity =
            Robolectric.buildActivity(
                    AdaptiveListClickActivity::class.java,
                    Intent(ACTION_LIST_ITEM_CLICK).putExtra(EXTRA_LIST_PENDING_INTENT, Uri.EMPTY),
                )
                .create()
                .get()
        assertThat(badExtraActivity.isFinishing).isTrue()
        assertThat(shadowOf(context as Application).nextStartedActivity).isNull()

        // Valid activity PendingIntent launches the target activity and finishes.
        val validActivity =
            Robolectric.buildActivity(
                    AdaptiveListClickActivity::class.java,
                    Intent(ACTION_LIST_ITEM_CLICK)
                        .putExtra(EXTRA_LIST_PENDING_INTENT, targetActivityPendingIntent),
                )
                .create()
                .get()
        assertThat(validActivity.isFinishing).isTrue()
        assertThat(shadowOf(context as Application).nextStartedActivity?.action)
            .isEqualTo(activityAction)

        // Mixed-list broadcast PendingIntent is also forwarded and finishes cleanly.
        val broadcastAction = "androidx.glance.adaptive.appwidget.test.MIXED_LIST_CALLBACK"
        val broadcastPendingIntent =
            PendingIntent.getBroadcast(
                context,
                12,
                Intent(broadcastAction).setPackage(context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val callbackActivity =
            Robolectric.buildActivity(
                    AdaptiveListClickActivity::class.java,
                    Intent(ACTION_LIST_ITEM_CLICK)
                        .putExtra(EXTRA_LIST_PENDING_INTENT, broadcastPendingIntent),
                )
                .create()
                .get()
        assertThat(callbackActivity.isFinishing).isTrue()
        assertThat(shadowOf(context as Application).broadcastIntents.map { it.action })
            .contains(broadcastAction)

        // Canceled PendingIntent is ignored without crashing and still finishes.
        targetActivityPendingIntent.cancel()
        val canceledActivity =
            Robolectric.buildActivity(
                    AdaptiveListClickActivity::class.java,
                    Intent(ACTION_LIST_ITEM_CLICK)
                        .putExtra(EXTRA_LIST_PENDING_INTENT, targetActivityPendingIntent),
                )
                .create()
                .get()
        assertThat(canceledActivity.isFinishing).isTrue()
        assertThat(shadowOf(context as Application).nextStartedActivity).isNull()

        // Template PendingIntent targets AdaptiveListClickActivity with FLAG_MUTABLE on API 31+.
        val template = AdaptiveListClickActivity.pendingIntentTemplate(context)
        assertThat(template.isActivity).isTrue()
        assertThat(shadowOf(template).savedIntent.component?.className)
            .isEqualTo(AdaptiveListClickActivity::class.java.name)
        assertThat(shadowOf(template).savedIntent.action).isEqualTo(ACTION_LIST_ITEM_CLICK)
        assertThat(shadowOf(template).flags and PendingIntent.FLAG_MUTABLE).isNotEqualTo(0)
    }

    @Test
    fun serviceStore_evictsLeastRecentlyUsedEntriesAndFactoryRetainsSnapshotOnEviction() {
        val row0 =
            RemoteViews(
                context.packageName,
                R.layout.glance_adaptive_appwidget_remoteviews_list_item,
            )
        val row1 =
            RemoteViews(
                context.packageName,
                R.layout.glance_adaptive_appwidget_remoteviews_list_checkbox,
            )
        AdaptiveListRemoteViewsService.putItems("key_0", listOf(row0, row1))

        val service = Robolectric.setupService(AdaptiveListRemoteViewsService::class.java)
        val factory =
            service.onGetViewFactory(AdaptiveListRemoteViewsService.serviceIntent(context, "key_0"))
        factory.onCreate()
        assertThat(factory.count).isEqualTo(2)

        for (i in 1..64) {
            AdaptiveListRemoteViewsService.putItems("key_$i", listOf(row0))
        }
        assertThat(AdaptiveListRemoteViewsService.getItems("key_0")).isNull()
        assertThat(AdaptiveListRemoteViewsService.getItems("key_1")).hasSize(1)
        assertThat(AdaptiveListRemoteViewsService.getItems("key_64")).hasSize(1)

        // Eviction from LruCache must not blank an already-populated factory on onDataSetChanged().
        factory.onDataSetChanged()
        assertThat(factory.count).isEqualTo(2)
        assertThat(factory.getViewAt(0)).isSameInstanceAs(row0)
        assertThat(factory.getViewAt(1)).isSameInstanceAs(row1)

        // Once the key is re-populated, onDataSetChanged() picks up the new snapshot.
        AdaptiveListRemoteViewsService.putItems("key_0", listOf(row1))
        factory.onDataSetChanged()
        assertThat(factory.count).isEqualTo(1)
        assertThat(factory.getViewAt(0)).isSameInstanceAs(row1)
    }

    @Test
    @Config(sdk = [30])
    fun clickDispatch_preApi34_dispatchesSafelyAndOmitsMutableFlagBelowApi31() {
        val receiver = AdaptiveListClickReceiver()
        val broadcastAction = "androidx.glance.adaptive.appwidget.test.PRE_API_34_CLICK"
        val targetPendingIntent =
            PendingIntent.getBroadcast(
                context,
                7,
                Intent(broadcastAction).setPackage(context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT,
            )

        // Wrong-type Parcelable on < TIRAMISU must not throw ClassCastException.
        receiver.onReceive(
            context,
            Intent(ACTION_LIST_ITEM_CLICK).putExtra(EXTRA_LIST_PENDING_INTENT, Uri.EMPTY),
        )
        assertThat(shadowOf(context as Application).broadcastIntents).isEmpty()

        receiver.onReceive(
            context,
            Intent(ACTION_LIST_ITEM_CLICK).putExtra(EXTRA_LIST_PENDING_INTENT, targetPendingIntent),
        )
        assertThat(shadowOf(context as Application).broadcastIntents.map { it.action })
            .contains(broadcastAction)

        val receiverTemplate = AdaptiveListClickReceiver.pendingIntentTemplate(context)
        assertThat(shadowOf(receiverTemplate).flags and PendingIntent.FLAG_MUTABLE).isEqualTo(0)

        val activityAction = "androidx.glance.adaptive.appwidget.test.PRE_API_34_ACTIVITY"
        val activityPendingIntent =
            PendingIntent.getActivity(
                context,
                8,
                Intent(activityAction).setPackage(context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT,
            )
        val activity =
            Robolectric.buildActivity(
                    AdaptiveListClickActivity::class.java,
                    Intent(ACTION_LIST_ITEM_CLICK)
                        .putExtra(EXTRA_LIST_PENDING_INTENT, activityPendingIntent),
                )
                .create()
                .get()
        assertThat(activity.isFinishing).isTrue()
        assertThat(shadowOf(context as Application).nextStartedActivity?.action)
            .isEqualTo(activityAction)

        val activityTemplate = AdaptiveListClickActivity.pendingIntentTemplate(context)
        assertThat(shadowOf(activityTemplate).flags and PendingIntent.FLAG_MUTABLE).isEqualTo(0)
    }

    @Test
    @Config(sdk = [34])
    fun clickActivity_api34_dispatchesWithForegroundActivityLaunchOptions() {
        val activityAction = "androidx.glance.adaptive.appwidget.test.API_34_ACTIVITY"
        val targetPendingIntent =
            PendingIntent.getActivity(
                context,
                9,
                Intent(activityAction).setPackage(context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val activity =
            Robolectric.buildActivity(
                    AdaptiveListClickActivity::class.java,
                    Intent(ACTION_LIST_ITEM_CLICK)
                        .putExtra(EXTRA_LIST_PENDING_INTENT, targetPendingIntent),
                )
                .create()
                .get()
        assertThat(activity.isFinishing).isTrue()
        assertThat(shadowOf(context as Application).nextStartedActivity?.action)
            .isEqualTo(activityAction)
    }
}
