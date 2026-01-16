/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.compose.remote.integration.view.demos;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

import androidx.annotation.RequiresApi;
import androidx.compose.remote.core.RemoteComposeBuffer;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.integration.view.demos.examples.RcTickerKt;

import org.jspecify.annotations.NonNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/** This provide a widget provider for Adnvance Widget Demo */
@SuppressLint("RestrictedApiAndroidX")
public class TickerWidgetProvider extends AppWidgetProvider {
    // Action for manual widget update
    private static final String WIDGET_UPDATE_ACTION = "android.appwidget.action.APPWIDGET_UPDATE";
    public static int sId = 567;
    public static int @NonNull [] sAppWidgetIds = new int[0];

    @SuppressLint("RestrictedApiAndroidX")
    @Override
    public void onUpdate(@NonNull Context context,
            @NonNull AppWidgetManager appWidgetManager,
            int @NonNull [] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        Utils.log(" >>>>> appWidgetIds " + Arrays.toString(appWidgetIds));

        sAppWidgetIds = appWidgetIds;
        RemoteViews widget = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            widget = loadWidgetFromRemoteComposeContext(context);
            Intent intent1 = new Intent(context, TickerWidgetProvider.class);
            intent1.setAction(WIDGET_UPDATE_ACTION);
            PendingIntent pendingIntent1 =
                    PendingIntent.getBroadcast(context, 0, intent1, PendingIntent.FLAG_MUTABLE);

            widget.setOnClickPendingIntent(sId, pendingIntent1);


        }
        for (int id : appWidgetIds) {
            appWidgetManager.updateAppWidget(id, widget);
        }
    }

    @SuppressLint("RestrictedApiAndroidX")
    private void setupWidgets(@NonNull Context context, int @NonNull [] appWidgetIds) {
        RemoteViews widget = null;
        Utils.log(">>>>> ====================  ");

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            widget = loadWidgetFromRemoteComposeContext(context);
            Intent intent1 = new Intent(context, TickerWidgetProvider.class);
            intent1.setAction(WIDGET_UPDATE_ACTION);
            PendingIntent pendingIntent1 =
                    PendingIntent.getBroadcast(context, 0, intent1, PendingIntent.FLAG_MUTABLE);

            widget.setOnClickPendingIntent(sId, pendingIntent1);


        }
        for (int id : appWidgetIds) {
            appWidgetManager.updateAppWidget(id, widget);
        }
    }


    @SuppressLint("RestrictedApiAndroidX")
    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        super.onReceive(context, intent);

        Utils.log(">>>>>>>>> TickerWidgetProvider.onReceive " + intent.getAction());
        if (WIDGET_UPDATE_ACTION.equals(intent.getAction())) {
            setupWidgets(context, sAppWidgetIds);
        }

    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @SuppressLint("RestrictedApiAndroidX")
    private RemoteViews loadWidgetFromRemoteComposeContext(@NonNull Context context) {
        try {
            Utils.log(">>>>> ====================  ");

            RemoteComposeBuffer doc = RcTickerKt.RcTicker(context).getWriter().getBuffer();

            WireBuffer buffer = doc.getBuffer();
            int bufferSize = buffer.size();
            byte[] bytes = new byte[bufferSize];
            ByteArrayInputStream b = new ByteArrayInputStream(buffer.getBuffer(), 0, bufferSize);

            int n = b.read(bytes);
            if (n < 0) {
                throw new IOException("read failed");
            }
            RemoteViews.DrawInstructions.Builder r =
                    new RemoteViews.DrawInstructions.Builder(List.of(bytes));


            return new RemoteViews(r.build());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
