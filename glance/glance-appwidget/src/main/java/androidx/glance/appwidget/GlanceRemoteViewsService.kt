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
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.glance.session.GlanceSessionManager
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.runBlocking

/**
 * [RemoteViewsService] to be connected to for a remote adapter that returns RemoteViews for lazy
 * lists / grids.
 */
open class GlanceRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent?): RemoteViewsFactory {
        requireNotNull(intent) { "Intent is null" }
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        check(appWidgetId != -1) { "No app widget id was present in the intent" }

        val viewId = intent.getIntExtra(EXTRA_VIEW_ID, -1)
        check(viewId != -1) { "No view id was present in the intent" }

        val sizeInfo = intent.getStringExtra(EXTRA_SIZE_INFO)
        check(!sizeInfo.isNullOrEmpty()) { "No size info was present in the intent" }

        return GlanceRemoteViewsFactory(this, appWidgetId, viewId, sizeInfo)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    internal companion object {
        internal const val EXTRA_VIEW_ID = "androidx.glance.widget.extra.view_id"
        internal const val EXTRA_SIZE_INFO = "androidx.glance.widget.extra.size_info"
        internal const val TAG = "GlanceRemoteViewService"

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
            synchronized(InMemoryStore) { InMemoryStore.removeItems(appWidgetId, viewId, sizeInfo) }
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
                try {
                    startSessionIfNeededAndWaitUntilReady(glanceId)
                } catch (e: ClosedSendChannelException) {
                    // This catch should no longer be necessary.
                    // Because we use SessionManager.runWithLock, we are guaranteed that the session
                    // we create won't be closed by concurrent calls to SessionManager. Currently,
                    // the only way a session would be closed is if there is an error in the
                    // composition that happens between the call to `startSession` and
                    // `waitForReady()` In that case, the composition error will be logged by
                    // GlanceAppWidget.onCompositionError, but could still cause
                    // ClosedSendChannelException. This is pretty unlikely, however keeping this
                    // here to avoid crashes in that scenario.
                    Log.e(TAG, "Error when trying to start session for list items", e)
                }
            }
        }

        private suspend fun startSessionIfNeededAndWaitUntilReady(glanceId: AppWidgetId) {
            val job =
                getGlanceAppWidget()?.let { widget ->
                    GlanceSessionManager.runWithLock {
                        if (isSessionRunning(context, glanceId.toSessionKey())) {
                            // If session is already running, data must have already been loaded
                            // into
                            // the store during composition.
                            return@runWithLock null
                        }
                        startSession(context, AppWidgetSession(widget, glanceId))
                        val session = getSession(glanceId.toSessionKey()) as AppWidgetSession
                        session.waitForReady()
                    }
                } ?: UnmanagedSessionReceiver.getSession(appWidgetId)?.waitForReady()
            // The following join() may throw CancellationException if the session is closed before
            // it is ready. This will have the effect of cancelling the runBlocking scope.
            job?.join()
        }

        private fun getGlanceAppWidget(): GlanceAppWidget? {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val providerInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
            return providerInfo?.provider?.className?.let { className ->
                val receiverClass = Class.forName(className)
                (receiverClass.getDeclaredConstructor().newInstance() as GlanceAppWidgetReceiver)
                    .glanceAppWidget
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

/** An in-memory store holding [RemoteCollectionItems] for each sized lazy view in appWidgets. */
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

    /** Returns the collection items corresponding to the requested view in appwidget and size. */
    fun getItems(appWidgetId: Int, viewId: Int, sizeInfo: String): RemoteCollectionItems {
        return items[key(appWidgetId, viewId, sizeInfo)] ?: RemoteCollectionItems.Empty
    }

    /** Removes the collection items corresponding to the requested view in appwidget and size. */
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
internal fun RemoteViews.setRemoteAdapter(
    translationContext: TranslationContext,
    viewId: Int,
    sizeInfo: String,
    items: RemoteCollectionItems
) {
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
        CollectionItemsApi31Impl.setRemoteAdapter(this, viewId, items)
    } else {
        val context = translationContext.context
        val appWidgetId = translationContext.appWidgetId
        val intent =
            Intent()
                .setComponent(translationContext.glanceComponents.remoteViewsService)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                .putExtra(GlanceRemoteViewsService.EXTRA_VIEW_ID, viewId)
                .putExtra(GlanceRemoteViewsService.EXTRA_SIZE_INFO, sizeInfo)
                .apply {
                    // Set a data Uri to disambiguate Intents for different widget/view ids.
                    data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
                }
        check(context.packageManager.resolveService(intent, 0) != null) {
            "${intent.component} could not be resolved, check the app manifest."
        }
        setRemoteAdapter(viewId, intent)
        GlanceRemoteViewsService.saveItems(appWidgetId, viewId, sizeInfo, items)
        AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(appWidgetId, viewId)
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private object CollectionItemsApi31Impl {
    fun setRemoteAdapter(remoteViews: RemoteViews, viewId: Int, items: RemoteCollectionItems) {
        remoteViews.setRemoteAdapter(viewId, toPlatformCollectionItems(items))
    }

    fun toPlatformCollectionItems(items: RemoteCollectionItems): RemoteViews.RemoteCollectionItems {
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
