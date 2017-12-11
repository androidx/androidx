/*
 * Copyright 2017 The Android Open Source Project
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

package com.example.androidx.slice.demos;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;

import androidx.app.slice.Slice;
import androidx.app.slice.SliceProvider;
import androidx.app.slice.builders.ListBuilder;
import androidx.app.slice.builders.MessagingSliceBuilder;

/**
 * Examples of using slice template builders.
 */
public class SampleSliceProvider extends SliceProvider {

    public static final String ACTION_WIFI_CHANGED =
            "com.example.androidx.slice.action.WIFI_CHANGED";
    public static final String ACTION_TOAST =
            "com.example.androidx.slice.action.TOAST";
    public static final String EXTRA_TOAST_MESSAGE = "com.example.androidx.extra.TOAST_MESSAGE";

    public static final String[] URI_PATHS = { "message", "wifi", "note", "ride", "toggle"};

    /**
     * @return Uri with the provided path
     */
    public static Uri getUri(String path, Context context) {
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(context.getPackageName())
                .appendPath(path)
                .build();
    }

    @Override
    public boolean onCreateSliceProvider() {
        return true;
    }

    @NonNull
    @Override
    public Uri onMapIntentToUri(Intent intent) {
        return getUri("wifi", getContext());
    }

    @Override
    public Slice onBindSlice(Uri sliceUri) {
        String path = sliceUri.getPath();
        switch (path) {
            case "/message":
                return createMessagingSlice(sliceUri);
            case "/wifi":
                return createWifiSlice(sliceUri);
            case "/note":
                return createNoteSlice(sliceUri);
            case "/ride":
                return createRideSlice(sliceUri);
            case "/toggle":
                return createCustomToggleSlice(sliceUri);
        }
        throw new IllegalArgumentException("Unknown uri " + sliceUri);
    }

    private Slice createMessagingSlice(Uri sliceUri) {
        // TODO: Remote input.
        return new MessagingSliceBuilder(sliceUri)
                .add(b -> b
                        .addText("yo home \uD83C\uDF55, I emailed you the info")
                        .addTimestamp(System.currentTimeMillis() - 20 * DateUtils.MINUTE_IN_MILLIS)
                        .addSource(Icon.createWithResource(getContext(), R.drawable.mady)))
                .add(b -> b
                        .addText("just bought my tickets")
                        .addTimestamp(System.currentTimeMillis() - 10 * DateUtils.MINUTE_IN_MILLIS))
                .add(b -> b
                        .addText("yay! can't wait for getContext() weekend!\n"
                                + "\uD83D\uDE00")
                        .addTimestamp(System.currentTimeMillis() - 5 * DateUtils.MINUTE_IN_MILLIS)
                        .addSource(Icon.createWithResource(getContext(), R.drawable.mady)))
                .build();

    }

    private Slice createNoteSlice(Uri sliceUri) {
        return new ListBuilder(sliceUri)
                .setColor(0xfff4b400)
                .add(b -> b
                    .setTitle("Create new note")
                    .setSubtitle("with this note taking app")
                    .addEndItem(getBroadcastIntent(ACTION_TOAST, "create note"),
                            Icon.createWithResource(getContext(), R.drawable.ic_create))
                    .addEndItem(getBroadcastIntent(ACTION_TOAST, "voice note"),
                            Icon.createWithResource(getContext(), R.drawable.ic_voice))
                    .addEndItem(getIntent("android.media.action.IMAGE_CAPTURE"),
                            Icon.createWithResource(getContext(), R.drawable.ic_camera)))
                .build();
    }

    private Slice createRideSlice(Uri sliceUri) {
        return new ListBuilder(sliceUri)
                .setColor(0xff1b5e20)
                .addSummaryRow(b -> b
                    .setTitle("Get ride")
                    .setSubtitle("Multiple cars 4 minutes away")
                    .addEndItem(getBroadcastIntent(ACTION_TOAST, "home"),
                            Icon.createWithResource(getContext(), R.drawable.ic_home))
                    .addEndItem(getBroadcastIntent(ACTION_TOAST, "work"),
                            Icon.createWithResource(getContext(), R.drawable.ic_work)))
                .add(b -> b
                    .setContentIntent(getBroadcastIntent(ACTION_TOAST, "work"))
                    .setTitle("Work")
                    .setSubtitle("2 min")
                    .addEndItem(Icon.createWithResource(getContext(), R.drawable.ic_work)))
                .add(b -> b
                    .setContentIntent(getBroadcastIntent(ACTION_TOAST, "home"))
                    .setTitle("Home")
                    .setSubtitle("2 hours 33 min via 101")
                    .addEndItem(Icon.createWithResource(getContext(), R.drawable.ic_home)))
                .add(b -> b
                    .setContentIntent(getBroadcastIntent(ACTION_TOAST, "book ride"))
                    .setTitle("Book ride")
                    .addEndItem(Icon.createWithResource(getContext(), R.drawable.ic_car)))
                .build();
    }

    private Slice createCustomToggleSlice(Uri sliceUri) {
        return new ListBuilder(sliceUri)
                .setColor(0xffff4081)
                .add(b -> b
                    .setTitle("Custom toggle")
                    .setSubtitle("It can support two states")
                    .addToggle(getBroadcastIntent(ACTION_TOAST, "start toggled"),
                            Icon.createWithResource(getContext(), R.drawable.toggle_star),
                            true /* isChecked */))
                .build();
    }

    private Slice createWifiSlice(Uri sliceUri) {
        // Get wifi state
        WifiManager wifiManager = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
        int wifiState = wifiManager.getWifiState();
        boolean wifiEnabled = false;
        String state;
        switch (wifiState) {
            case WifiManager.WIFI_STATE_DISABLED:
            case WifiManager.WIFI_STATE_DISABLING:
                state = "disconnected";
                break;
            case WifiManager.WIFI_STATE_ENABLED:
            case WifiManager.WIFI_STATE_ENABLING:
                state = wifiManager.getConnectionInfo().getSSID();
                wifiEnabled = true;
                break;
            case WifiManager.WIFI_STATE_UNKNOWN:
            default:
                state = ""; // just don't show anything?
                break;
        }
        boolean finalWifiEnabled = wifiEnabled;
        return new ListBuilder(sliceUri)
                .setColor(0xff4285f4)
                .add(b -> b
                    .setTitle("Wi-fi")
                    .setTitleItem(Icon.createWithResource(getContext(), R.drawable.ic_wifi))
                    .setSubtitle(state)
                    .addToggle(getBroadcastIntent(ACTION_WIFI_CHANGED, null), finalWifiEnabled)
                    .setContentIntent(getIntent(Settings.ACTION_WIFI_SETTINGS)))
            .build();
    }

    private PendingIntent getIntent(String action) {
        Intent intent = new Intent(action);
        PendingIntent pi = PendingIntent.getActivity(getContext(), 0, intent, 0);
        return pi;
    }

    private PendingIntent getBroadcastIntent(String action, String message) {
        Intent intent = new Intent(action);
        if (message != null) {
            intent.putExtra(EXTRA_TOAST_MESSAGE, message);
        }
        intent.setClass(getContext(), SliceBroadcastReceiver.class);
        return PendingIntent.getBroadcast(getContext(), 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }
}
