/*
 * Copyright 2025 The Android Open Source Project
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

import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.RemoteViews;

import androidx.compose.remote.core.RemoteComposeBuffer;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.integration.view.demos.examples.RcTickerKt;
import androidx.compose.remote.player.core.RemoteDocument;
import androidx.compose.remote.player.view.RemoteComposePlayer;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * This is used to do long run test (typically over night) This insures we are not doing anything
 * that degrades the performance of the device over time.
 */
@SuppressWarnings("RestrictedApiAndroidX")
public class TickerActivity extends Activity {
    static RemoteComposeBuffer sDoc;

    public static @Nullable RemoteComposeBuffer getDoc() {
        return sDoc;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sDoc = RcTickerKt.RcTicker(this.getApplicationContext()).getWriter().getBuffer();
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        // Hide the system bars.
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        // Configure the system bars to behave in a way that allows for immersive content.
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        FrameLayout frameLayout = new FrameLayout(getApplicationContext());
        byte[] buffer = sDoc.getBuffer().cloneBytes();
        setup(buffer);
        RemoteDocument rcd = new RemoteDocument(buffer);
        RemoteComposePlayer player = new RemoteComposePlayer(getApplicationContext());
        player.setDocument(rcd);
        frameLayout.setBackgroundColor(Color.BLACK);
        int widthPixels = Resources.getSystem().getDisplayMetrics().widthPixels;
        int heightPixels = Resources.getSystem().getDisplayMetrics().heightPixels;
        int w = Math.min(widthPixels, 1200);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(w, heightPixels);
        params.gravity = Gravity.CENTER;
        params.leftMargin = 0;
        params.rightMargin = 100;
        player.setLayoutParams(params);
        player.setScaleX((widthPixels - 200) / (float) w);
        player.addIdActionListener((id, metadata) -> {
            Utils.log(" \"" + metadata + "\"");
            setup(buffer);
        });
        frameLayout.addView(player);

        setContentView(frameLayout);
    }

    void setup(byte @NonNull [] bytes) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        Utils.log("update widget");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            RemoteViews.DrawInstructions drawInstruction = new RemoteViews.DrawInstructions.Builder(
                    List.of(bytes)).build();
            RemoteViews remoteViews = new RemoteViews(drawInstruction);
            Intent intent = new Intent(this, TickerActivity.class);
            intent.setAction("android.appwidget.action.APPWIDGET_UPDATE");

            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                    PendingIntent.FLAG_MUTABLE);
            int id = 567;
            remoteViews.setOnClickPendingIntent(id, pendingIntent);
            appWidgetManager.updateAppWidget(TickerWidgetProvider.sAppWidgetIds, remoteViews);
            System.out.println("update widget ");
        } else {
            System.out.println("VERSION.SDK_INT < VANILLA_ICE_CREAM");
        }
    }

}
