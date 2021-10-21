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

package androidx.core.remoteviews.demos

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import androidx.core.widget.RemoteViewsCompat
import androidx.core.widget.RemoteViewsCompat.RemoteCollectionItems

public class SampleAppWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            val mainRemoteViews = RemoteViews(context.packageName, R.layout.main_layout)

            val itemsBuilder = RemoteCollectionItems.Builder()
            for (i in 0 until 10) {
                val itemRv = RemoteViews(context.packageName, R.layout.item_layout)
                itemRv.setTextViewText(R.id.text, "Row ${i + 1}")
                itemsBuilder.addItem(id = i.toLong(), view = itemRv)
            }
            RemoteViewsCompat.setRemoteAdapter(
                context = context,
                remoteViews = mainRemoteViews,
                appWidgetId = appWidgetId,
                viewId = R.id.list,
                items = itemsBuilder.build()
            )

            appWidgetManager.updateAppWidget(appWidgetId, mainRemoteViews)
        }
    }
}
