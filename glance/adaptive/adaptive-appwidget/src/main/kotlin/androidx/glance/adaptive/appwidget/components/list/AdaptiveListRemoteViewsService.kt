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

import android.app.Activity
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.BadParcelableException
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.LruCache
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.annotation.RequiresApi
import androidx.glance.adaptive.appwidget.R

/**
 * Distinct item layout count across scrollable `ListView` items: divider, compact (inert/tappable),
 * focused (inert/tappable), checkbox (inert/tappable), and detail-card (inert/tappable).
 */
internal const val LIST_VIEW_TYPE_COUNT: Int = 9

internal const val ACTION_LIST_ITEM_CLICK: String =
    "androidx.glance.adaptive.appwidget.action.LIST_ITEM_CLICK"
internal const val EXTRA_LIST_PENDING_INTENT: String =
    "androidx.glance.adaptive.appwidget.extra.LIST_PENDING_INTENT"

/**
 * Upper bound on cached pre-API 32 list snapshots retained in memory across placed widgets,
 * multi-size mappings, and theme/configuration changes; older keys are evicted via LRU.
 */
private const val MAX_STORED_LISTS = 64

/**
 * [RemoteViewsService] supplying list item [RemoteViews] for scrollable `ItemList` widgets on API
 * 24–31 (before [RemoteViews.RemoteCollectionItems] Parcelable support stabilized in API 32).
 *
 * Declared in `AndroidManifest.xml` with `android:exported="true"` and guarded by
 * `android:permission="android.permission.BIND_REMOTEVIEWS"` (required by the platform widget
 * host); kept `internal` (compiling to JVM `public` for platform instantiation) so it does not
 * widen the `LIBRARY_GROUP` API surface.
 */
internal class AdaptiveListRemoteViewsService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val listKey = intent.getStringExtra(EXTRA_LIST_KEY) ?: ""
        return ListFactory(applicationContext ?: this, listKey)
    }

    private class ListFactory(
        private val context: Context,
        private val listKey: String,
    ) : RemoteViewsFactory {
        @Volatile private var items: List<RemoteViews> = emptyList()

        override fun onCreate() {
            getItems(listKey)?.let { items = it }
        }

        override fun onDataSetChanged() {
            // Retain the last populated snapshot if this key was evicted from the LRU cache.
            getItems(listKey)?.let { items = it }
        }

        override fun onDestroy() {}

        override fun getCount(): Int = items.size

        override fun getViewAt(position: Int): RemoteViews =
            items.getOrNull(position)
                ?: RemoteViews(
                    context.packageName,
                    R.layout.glance_adaptive_appwidget_remoteviews_list_item,
                )

        override fun getLoadingView(): RemoteViews? = null

        override fun getViewTypeCount(): Int = LIST_VIEW_TYPE_COUNT

        override fun getItemId(position: Int): Long = position.toLong()

        // Positional indices are not stable across insertions or removals above existing rows.
        override fun hasStableIds(): Boolean = false
    }

    internal companion object {
        private const val EXTRA_LIST_KEY = "androidx.glance.adaptive.appwidget.extra.LIST_KEY"
        private const val URI_SCHEME = "glance-adaptive-list"
        private const val URI_AUTHORITY = "list"
        // TODO(b/580007927): In-memory storage blanks pre-API 32 scrollable lists after process
        //  death until appWidgetId is plumbed into renderers to back items on disk.
        private val listStore = LruCache<String, List<RemoteViews>>(MAX_STORED_LISTS)

        internal fun getItems(key: String): List<RemoteViews>? =
            synchronized(listStore) { listStore.get(key) }

        fun putItems(key: String, items: List<RemoteViews>) {
            val copy = List(items.size) { i -> items[i] }
            synchronized(listStore) { listStore.put(key, copy) }
        }

        internal fun clearForTesting() {
            synchronized(listStore) { listStore.evictAll() }
        }

        fun serviceIntent(context: Context, key: String): Intent =
            Intent(context, AdaptiveListRemoteViewsService::class.java).apply {
                // Keep a distinct, properly encoded URI so Intent.filterEquals differentiates
                // separate list keys (including widgetName, night mode, density, and fontScale).
                data =
                    Uri.Builder()
                        .scheme(URI_SCHEME)
                        .authority(URI_AUTHORITY)
                        .appendPath(key)
                        .build()
                putExtra(EXTRA_LIST_KEY, key)
            }
    }
}

/**
 * Trampoline [Activity] that dispatches `ListView` item fill-in intents for lists containing
 * activity-launching actions (`LaunchAction` / `DeeplinkAction`).
 *
 * Launching the template as an [Activity] grants foreground activity-launch privileges on Android
 * 14+ (avoiding Background Activity Launch blocks that reject activity starts from a
 * [BroadcastReceiver]) and lets the widget host run native widget-to-app launch transitions.
 * Passing [ActivityOptions] on API 34+ delegates this foreground activity's launch privileges to
 * the embedded [PendingIntent] after verifying [PendingIntent.getCreatorPackage].
 */
internal class AdaptiveListClickActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val pendingIntent = extractOwnListClickPendingIntent(this, intent) ?: return
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    AdaptiveListClickApi34Impl.sendWithBalOptions(this, pendingIntent)
                } else {
                    pendingIntent.send()
                }
            } catch (_: PendingIntent.CanceledException) {
                // Ignore clicks whose target PendingIntent was canceled before delivery.
            }
        } finally {
            finish()
        }
    }

    internal companion object {
        fun pendingIntentTemplate(context: Context): PendingIntent {
            val intent =
                Intent(context, AdaptiveListClickActivity::class.java).apply {
                    action = ACTION_LIST_ITEM_CLICK
                }
            return PendingIntent.getActivity(context, 0, intent, templatePendingIntentFlags())
        }
    }
}

/**
 * Dispatches `ListView` item fill-in intents for callback-only lists (no activity-launching items)
 * without starting a trampoline [Activity].
 *
 * Android's `RemoteViews` rejects direct `setOnClickPendingIntent` / `fromPendingIntent` bindings
 * on collection adapter items across all API levels; callback-only lists bind
 * `setPendingIntentTemplate` to this receiver so background callbacks (such as checklist toggles)
 * run silently with no activity flicker.
 */
internal class AdaptiveListClickReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingIntent = extractOwnListClickPendingIntent(context, intent) ?: return
        try {
            pendingIntent.send()
        } catch (_: PendingIntent.CanceledException) {
            // Ignore clicks whose target PendingIntent was canceled before delivery.
        }
    }

    internal companion object {
        fun pendingIntentTemplate(context: Context): PendingIntent {
            val intent =
                Intent(context, AdaptiveListClickReceiver::class.java).apply {
                    action = ACTION_LIST_ITEM_CLICK
                }
            return PendingIntent.getBroadcast(context, 0, intent, templatePendingIntentFlags())
        }
    }
}

internal fun extractOwnListClickPendingIntent(context: Context, intent: Intent): PendingIntent? {
    if (intent.action != ACTION_LIST_ITEM_CLICK) return null
    val pendingIntent =
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(EXTRA_LIST_PENDING_INTENT, PendingIntent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<Parcelable>(EXTRA_LIST_PENDING_INTENT) as? PendingIntent
            }
        } catch (_: BadParcelableException) {
            return null
        } ?: return null
    // pendingIntentTemplate is FLAG_MUTABLE so hosts can merge fill-in extras; verify the
    // embedded PendingIntent was created by this app before forwarding it.
    return pendingIntent.takeIf { it.creatorPackage == context.packageName }
}

private fun templatePendingIntentFlags(): Int =
    PendingIntent.FLAG_UPDATE_CURRENT or
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private object AdaptiveListClickApi34Impl {
    fun sendWithBalOptions(context: Context, pendingIntent: PendingIntent) {
        val options = ActivityOptions.makeBasic()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            AdaptiveListClickApi36Impl.allowBackgroundStart(options)
        } else {
            @Suppress("DEPRECATION")
            options.pendingIntentBackgroundActivityStartMode =
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
        }
        pendingIntent.send(context, 0, null, null, null, null, options.toBundle())
    }
}

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
private object AdaptiveListClickApi36Impl {
    fun allowBackgroundStart(options: ActivityOptions) {
        options.pendingIntentBackgroundActivityStartMode =
            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
    }
}
