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

import static android.app.slice.Slice.HINT_SELECTED;
import static android.app.slice.Slice.HINT_TITLE;

import android.app.PendingIntent;
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
import androidx.app.slice.builders.MessagingSliceBuilder;
import androidx.app.slice.core.SliceHints;

/**
 * Examples of using slice template builders.
 */
public class SampleSliceProvider extends SliceProvider {
    public static final Uri MESSAGE =
            Uri.parse("content://com.example.androidx.slice.demos/message");
    public static final Uri WIFI =
            Uri.parse("content://com.example.androidx.slice.demos/wifi");

    public static final String ACTION_WIFI_CHANGED =
            "com.android.settings.slice.action.WIFI_CHANGED";

    @Override
    public boolean onCreateSliceProvider() {
        return true;
    }

    @NonNull
    @Override
    public Uri onMapIntentToUri(Intent intent) {
        return WIFI;
    }

    @Override
    public Slice onBindSlice(Uri sliceUri) {
        String path = sliceUri.getPath();
        switch (path) {
            case "/message":
                return createMessagingSlice(sliceUri);
            case "/wifi":
                return createSettingsSlice(sliceUri);
        }
        throw new IllegalArgumentException("Unknown uri " + sliceUri);
    }

    private Slice createMessagingSlice(Uri sliceUri) {
        // TODO: Remote input.
        return new MessagingSliceBuilder(sliceUri)
                .startMessage()
                        .addText("yo home \uD83C\uDF55, I emailed you the info")
                        .addTimestamp(System.currentTimeMillis() - 20 * DateUtils.MINUTE_IN_MILLIS)
                        .addSource(Icon.createWithResource(getContext(), R.drawable.mady))
                        .endMessage()
                .startMessage()
                        .addText("just bought my tickets")
                        .addTimestamp(System.currentTimeMillis() - 10 * DateUtils.MINUTE_IN_MILLIS)
                        .endMessage()
                .startMessage()
                        .addText("yay! can't wait for getContext() weekend!\n"
                                + "\uD83D\uDE00")
                        .addTimestamp(System.currentTimeMillis() - 5 * DateUtils.MINUTE_IN_MILLIS)
                        .addSource(Icon.createWithResource(getContext(), R.drawable.mady))
                        .endMessage()
                .build();

    }

    private Slice createSettingsSlice(Uri sliceUri) {
        // TODO: Create a proper template builder for toggles
        // Get wifi state
        String[] toggleHints;
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
        if (wifiEnabled) {
            toggleHints = new String[] {SliceHints.HINT_TOGGLE, HINT_SELECTED};
        } else {
            toggleHints = new String[] {SliceHints.HINT_TOGGLE};
        }
        // Construct the slice
        Slice.Builder b = new Slice.Builder(sliceUri);
        b.addSubSlice(new Slice.Builder(b)
                .addAction(getIntent(Settings.ACTION_WIFI_SETTINGS),
                        new Slice.Builder(b)
                                .addText("Wi-fi", null)
                                .addText(state, null)
                                .addIcon(Icon.createWithResource(getContext(),
                                        R.drawable.ic_settings_wifi), null, SliceHints.HINT_HIDDEN)
                                .addHints(HINT_TITLE)
                                .build(), null)
                .addAction(getBroadcastIntent(ACTION_WIFI_CHANGED),
                        new Slice.Builder(b)
                                .addHints(toggleHints)
                                .build(), null)
                .build());
        return b.build();
    }

    private PendingIntent getIntent(String action) {
        Intent intent = new Intent(action);
        PendingIntent pi = PendingIntent.getActivity(getContext(), 0, intent, 0);
        return pi;
    }

    private PendingIntent getBroadcastIntent(String action) {
        Intent intent = new Intent(action);
        intent.setClass(getContext(), SliceBroadcastReceiver.class);
        return PendingIntent.getBroadcast(getContext(), 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }
}
