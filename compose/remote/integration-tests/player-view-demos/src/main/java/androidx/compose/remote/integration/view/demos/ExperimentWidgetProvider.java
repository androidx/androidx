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
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

import androidx.annotation.RequiresApi;
import androidx.compose.remote.core.RemoteComposeBuffer;
import androidx.compose.remote.core.WireBuffer;

import org.jspecify.annotations.NonNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

/** This provide a widget provider for Adnvance Widget Demo */
@SuppressLint("RestrictedApiAndroidX")
public class ExperimentWidgetProvider extends AppWidgetProvider {
    // Action for manual widget update
    // private static final String WIDGET_UPDATE_ACTION = "com.example.mywidget.WIDGET_UPDATE";
    public static int @NonNull [] sAppWidgetIds;

    @Override
    public void onUpdate(@NonNull Context context,
            @NonNull AppWidgetManager appWidgetManager,
            int @NonNull [] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        sAppWidgetIds = appWidgetIds;
        RemoteViews widget = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            widget = loadWidgetFromRemoteComposeContext();
        }
        for (int id : appWidgetIds) {
            appWidgetManager.updateAppWidget(id, widget);
        }
    }

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        super.onReceive(context, intent);

        // Handle custom update action
        /* Used in building two way communication with the widget
        if (WIDGET_UPDATE_ACTION.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, ExperimentWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

            // Update all widgets
        }

         */
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @SuppressLint("RestrictedApiAndroidX")
    private RemoteViews loadWidgetFromRemoteComposeContext() {
        try {
            RemoteComposeBuffer doc = ExperimentRecyclerActivity.getCurrentDoc();

            WireBuffer buffer = doc.getBuffer();
            int bufferSize = buffer.size();
            byte[] bytes = new byte[bufferSize];
            ByteArrayInputStream b = new ByteArrayInputStream(buffer.getBuffer(), 0, bufferSize);

            b.read(bytes);

            RemoteViews.DrawInstructions.Builder r =
                    new RemoteViews.DrawInstructions.Builder(List.of(bytes));

            return new RemoteViews(r.build());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
