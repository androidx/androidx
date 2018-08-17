/*
 * Copyright 2018 The Android Open Source Project
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

package com.example.androidx.remotecallback.demos;


import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.remotecallback.AppWidgetProviderWithCallbacks;
import androidx.remotecallback.RemoteCallable;
import androidx.remotecallback.RemoteCallback;

/**
 * Sample widget provider that hooks up a +/-/reset button for a counter.
 */
public class RemoteCallbackProvider extends AppWidgetProviderWithCallbacks<RemoteCallbackProvider> {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d("RemoteCallbackProvider", "onUpdate");
        appWidgetManager.updateAppWidget(appWidgetIds,
                createRemoteViews(context, appWidgetIds, 0));
    }

    /**
     * Creates a current view of the widget with actions and text hooked up.
     */
    public RemoteViews createRemoteViews(Context context, int[] ids, int value) {
        Log.d("RemoteCallbackProvider", "createRemoteViews " + value);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                R.layout.app_item);

        remoteViews.setOnClickPendingIntent(R.id.sub,
                createRemoteCallback(context).setValue(context, ids, value - 1).toPendingIntent());
        remoteViews.setOnClickPendingIntent(R.id.add,
                createRemoteCallback(context).setValue(context, ids, value + 1).toPendingIntent());
        remoteViews.setOnClickPendingIntent(R.id.reset,
                createRemoteCallback(context).setValue(context, ids, 0).toPendingIntent());
        remoteViews.setTextViewText(R.id.current, "Value: " + value);
        return remoteViews;
    }

    /**
     * Gets called when a button is pushed.
     */
    @RemoteCallable
    public RemoteCallback setValue(Context context, int[] ids, int value) {
        Log.d("RemoteCallbackProvider", "setValue " + value);
        AppWidgetManager appWidgetManager = (AppWidgetManager) context.getSystemService(
                Context.APPWIDGET_SERVICE);
        appWidgetManager.updateAppWidget(ids, createRemoteViews(context, ids, value));
        return RemoteCallback.LOCAL;
    }
}
