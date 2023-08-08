/*
 * Copyright 2023 The Android Open Source Project
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

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.glance.session.GlanceSessionManager
import kotlinx.coroutines.runBlocking

/**
 * [RemoteViewsService] to be connected to for a remote adapter that returns RemoteViews for lazy
 * lists / grids.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GlanceRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        check(appWidgetId != -1) { "No app widget id was present in the intent" }

        val viewId = intent.getIntExtra(EXTRA_VIEW_ID, -1)
        check(viewId != -1) { "No view id was present in the intent" }

        val sizeInfo = intent.getStringExtra(EXTRA_SIZE_INFO)
        check(!sizeInfo.isNullOrEmpty()) { "No size info was present in the intent" }

        return GlanceRemoteViewsFactory(this, appWidgetId, viewId, sizeInfo)
    }

    companion object {
        const val EXTRA_VIEW_ID = "androidx.glance.widget.extra.view_id"
        const val EXTRA_SIZE_INFO = "androidx.glance.widget.extra.size_info"

        // An in-memory store containing items to be returned via the adapter when requested.
        private val InMemoryStore = RemoteCollectionItemsInMemoryStore()

        // Adds items to the store for later use by the list adapter to display the items.
        internal fun saveItems(
            appWidgetId: Int,
            viewId: Int,
            sizeInfo: String,
            remoteCollectionItems: RemoteCollectionItems
        ) {
            synchronized(InMemoryStore) {
                InMemoryStore.save(appWidgetId, viewId, sizeInfo, remoteCollectionItems)
            }
        }

        // Returns items in the store for the requested view in appwidget for the specified size.
        private fun getItems(
            appWidgetId: Int,
            viewId: Int,
            sizeInfo: String
        ): RemoteCollectionItems {
            return synchronized(InMemoryStore) {
                InMemoryStore.getItems(appWidgetId, viewId, sizeInfo)
            }
        }

        // Removes items in the store for the requested view in appwidget for the specified size.
        private fun removeItems(appWidgetId: Int, viewId: Int, sizeInfo: String) {
            synchronized(InMemoryStore) {
                InMemoryStore.removeItems(appWidgetId, viewId, sizeInfo)
            }
        }
    }

    /**
     * A RemoteViewsFactory that holds items in memory and provides it to the host when requested.
     *
     * <p>Starts glance session if needed to reload items in memory e.g. when app process was killed
     * and user scrolled on an existing list / grid view.
     */
    internal class GlanceRemoteViewsFactory(
        private val context: Context,
        private val appWidgetId: Int,
        private val viewId: Int,
        private val size: String
    ) : RemoteViewsFactory {
        override fun onCreate() {
            // OnDataSetChanged is always called even onCreate, so we don't need to load data here.
        }

        override fun onDataSetChanged() = loadData()

        private fun loadData() {
            runBlocking {
                val glanceId = AppWidgetId(appWidgetId)
                // If session is already running, data must have already been loaded into the store
                // during composition.
                if (!GlanceSessionManager.isSessionRunning(context, glanceId.toSessionKey())) {
                    startSessionAndWaitUntilReady(glanceId)
                }
            }
        }

        private suspend fun startSessionAndWaitUntilReady(glanceId: AppWidgetId) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val providerInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
            if (providerInfo?.provider != null) {
                val receiverClass = Class.forName(providerInfo.provider.className)
                val glanceAppWidget =
                    (receiverClass.getDeclaredConstructor()
                        .newInstance() as GlanceAppWidgetReceiver).glanceAppWidget
                AppWidgetSession(glanceAppWidget, glanceId)
                    .also { GlanceSessionManager.startSession(context, it) }
                    .waitForReady()
            }
        }

        override fun onDestroy() {
            removeItems(appWidgetId, viewId, size)
        }

        private fun items() = getItems(appWidgetId, viewId, size)

        override fun getCount(): Int {
            return items().itemCount
        }

        override fun getViewAt(position: Int): RemoteViews {
            return try {
                items().getItemView(position)
            } catch (e: ArrayIndexOutOfBoundsException) {
                // RemoteViewsAdapter may sometimes request an index that is out of bounds. Return
                // an error view in this case. See b/242730601, b/254682488 for more details.
                RemoteViews(context.packageName, R.layout.glance_invalid_list_item)
            }
        }

        override fun getLoadingView() = null

        override fun getViewTypeCount(): Int = items().viewTypeCount

        override fun getItemId(position: Int): Long =
            try {
                items().getItemId(position)
            } catch (e: ArrayIndexOutOfBoundsException) {
                -1
            }

        override fun hasStableIds(): Boolean = items().hasStableIds()
    }
}

/**
 * An in-memory store holding [RemoteCollectionItems] for each sized lazy view in appWidgets.
 */
private class RemoteCollectionItemsInMemoryStore {
    private val items = mutableMapOf<String, RemoteCollectionItems>()

    fun save(
        appWidgetId: Int,
        viewId: Int,
        sizeInfo: String,
        remoteCollectionItems: RemoteCollectionItems
    ) {
        items[key(appWidgetId, viewId, sizeInfo)] = remoteCollectionItems
    }

    /**
     * Returns the collection items corresponding to the requested view in appwidget and size.
     */
    fun getItems(appWidgetId: Int, viewId: Int, sizeInfo: String): RemoteCollectionItems {
        return items[key(appWidgetId, viewId, sizeInfo)] ?: RemoteCollectionItems.Empty
    }

    /**
     * Removes the collection items corresponding to the requested view in appwidget and size.
     */
    fun removeItems(appWidgetId: Int, viewId: Int, sizeInfo: String) {
        items.remove(key(appWidgetId, viewId, sizeInfo))
    }

    // A unique key for RemoteCollectionItems in the store. Including size info allows us to compose
    // for different sizes and maintain separate collection items for each size.
    private fun key(appWidgetId: Int, viewId: Int, sizeInfo: String): String {
        return "$appWidgetId-$viewId-$sizeInfo"
    }
}

/**
 * Sets remote views adapter.
 *
 * <p>For SDKs higher than S, passes the items in the adapter. For S and below SDKs, connects to a
 * GlanceRemoteViewsService using an intent.
 */
@Suppress("DEPRECATION")
@DoNotInline
internal fun RemoteViews.setRemoteAdapter(
    context: Context,
    appWidgetId: Int,
    viewId: Int,
    sizeInfo: String,
    items: RemoteCollectionItems
) {
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
        CollectionItemsApi31Impl.setRemoteAdapter(this, viewId, items)
    } else {
        val intent = Intent(context, GlanceRemoteViewsService::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            .putExtra(GlanceRemoteViewsService.EXTRA_VIEW_ID, viewId)
            .putExtra(GlanceRemoteViewsService.EXTRA_SIZE_INFO, sizeInfo)
            .apply {
                // Set a data Uri to disambiguate Intents for different widget/view ids.
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
        check(context.packageManager.resolveService(intent, 0) != null) {
            "GlanceRemoteViewsService could not be resolved, check the app manifest."
        }
        setRemoteAdapter(viewId, intent)
        GlanceRemoteViewsService.saveItems(
            appWidgetId,
            viewId,
            sizeInfo,
            items
        )
        AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(appWidgetId, viewId)
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private object CollectionItemsApi31Impl {
    @DoNotInline
    fun setRemoteAdapter(remoteViews: RemoteViews, viewId: Int, items: RemoteCollectionItems) {
        remoteViews.setRemoteAdapter(viewId, toPlatformCollectionItems(items))
    }

    @DoNotInline
    fun toPlatformCollectionItems(items: RemoteCollectionItems):
        RemoteViews.RemoteCollectionItems {
        return RemoteViews.RemoteCollectionItems.Builder()
            .setHasStableIds(items.hasStableIds())
            .setViewTypeCount(items.viewTypeCount)
            .also { builder ->
                repeat(items.itemCount) { index ->
                    builder.addItem(items.getItemId(index), items.getItemView(index))
                }
            }
            .build()
    }
}
