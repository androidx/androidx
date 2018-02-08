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

import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;

import java.util.Calendar;

import androidx.app.slice.Slice;
import androidx.app.slice.SliceProvider;
import androidx.app.slice.builders.GridBuilder;
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
    public static final String ACTION_TOAST_RANGE_VALUE =
            "com.example.androidx.slice.action.TOAST_RANGE_VALUE";

    public static final int LOADING_DELAY_MS = 4000;

    public static final String[] URI_PATHS = {"message", "wifi", "note", "ride", "toggle",
            "toggle2", "contact", "gallery", "weather", "reservation", "loadlist", "loadlist2",
            "loadgrid", "loadgrid2", "inputrange", "range"};

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
            // TODO: add list / grid slices with 'see more' options
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
            case "/toggle2":
                return createTwoCustomToggleSlices(sliceUri);
            case "/contact":
                return createContact(sliceUri);
            case "/gallery":
                return createGallery(sliceUri);
            case "/weather":
                return createWeather(sliceUri);
            case "/reservation":
                return createReservationSlice(sliceUri);
            case "/loadlist":
                return createLoadingSlice(sliceUri, false /* loadAll */, true /* isList */);
            case "/loadlist2":
                return createLoadingSlice(sliceUri, true /* loadAll */, true /* isList */);
            case "/loadgrid":
                return createLoadingSlice(sliceUri, false /* loadAll */, false /* isList */);
            case "/loadgrid2":
                return createLoadingSlice(sliceUri, true /* loadAll */, false /* isList */);
            case "/inputrange":
                return createStarRatingInputRange(sliceUri);
            case "/range":
                return createDownloadProgressRange(sliceUri);
        }
        throw new IllegalArgumentException("Unknown uri " + sliceUri);
    }

    private Slice createWeather(Uri sliceUri) {
        return new GridBuilder(getContext(), sliceUri)
                .addCell(cb -> cb
                        .addLargeImage(Icon.createWithResource(getContext(), R.drawable.weather_1))
                        .addText("MON")
                        .addTitleText("69\u00B0"))
                .addCell(cb -> cb
                        .addLargeImage(Icon.createWithResource(getContext(), R.drawable.weather_2))
                        .addText("TUE")
                        .addTitleText("71\u00B0"))
                .addCell(cb -> cb
                        .addLargeImage(Icon.createWithResource(getContext(), R.drawable.weather_3))
                        .addText("WED")
                        .addTitleText("76\u00B0"))
                .addCell(cb -> cb
                        .addLargeImage(Icon.createWithResource(getContext(), R.drawable.weather_4))
                        .addText("THU")
                        .addTitleText("72\u00B0"))
                .addCell(cb -> cb
                        .addLargeImage(Icon.createWithResource(getContext(), R.drawable.weather_1))
                        .addText("FRI")
                        .addTitleText("68\u00B0"))
                .build();
    }

    private Slice createGallery(Uri sliceUri) {
        return new ListBuilder(getContext(), sliceUri)
                .setColor(0xff4285F4)
                .addRow(b -> b
                    .setTitle("Family trip to Hawaii")
                    .setSubtitle("Sep 30, 2017 - Oct 2, 2017")
                    .addEndItem(Icon.createWithResource(getContext(), R.drawable.ic_cast),
                            getBroadcastIntent(ACTION_TOAST, "cast photo album"))
                    .addEndItem(Icon.createWithResource(getContext(), R.drawable.ic_share),
                            getBroadcastIntent(ACTION_TOAST, "share photo album")))
                .addGrid(b -> b
                    .addCell(cb -> cb
                        .addLargeImage(Icon.createWithResource(getContext(), R.drawable.slices_1)))
                    .addCell(cb -> cb
                        .addLargeImage(Icon.createWithResource(getContext(), R.drawable.slices_2)))
                    .addCell(cb -> cb
                        .addLargeImage(Icon.createWithResource(getContext(), R.drawable.slices_3)))
                    .addCell(cb -> cb
                        .addLargeImage(Icon.createWithResource(getContext(), R.drawable.slices_4))))
                .build();
    }

    private Slice createContact(Uri sliceUri) {
        final long lastCalled = System.currentTimeMillis() - 20 * DateUtils.MINUTE_IN_MILLIS;
        CharSequence lastCalledString = DateUtils.getRelativeTimeSpanString(lastCalled,
                Calendar.getInstance().getTimeInMillis(),
                DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE);
        return new ListBuilder(getContext(), sliceUri)
                .setColor(0xff3949ab)
                .setHeader(b -> b
                        .setTitle("Mady Pitza")
                        .setSummarySubtitle("Called " + lastCalledString)
                        .setContentIntent(getBroadcastIntent(ACTION_TOAST, "See contact info")))
                .addRow(b -> b
                        .setTitleItem(Icon.createWithResource(getContext(), R.drawable.ic_call))
                        .setTitle("314-259-2653")
                        .setSubtitle("Call lasted 1 hr 17 min")
                        .addEndItem(lastCalled))
                .addRow(b -> b
                        .setTitleItem(Icon.createWithResource(getContext(), R.drawable.ic_text))
                        .setTitle("You: Coooooool see you then")
                        .addEndItem(System.currentTimeMillis() - 40 * DateUtils.MINUTE_IN_MILLIS))
                .addGrid(b -> b
                        .addCell(cb -> cb
                            .addImage(Icon.createWithResource(getContext(), R.drawable.ic_call))
                            .addText("Call")
                            .setContentIntent(getBroadcastIntent(ACTION_TOAST, "call")))
                        .addCell(cb -> cb
                            .addImage(Icon.createWithResource(getContext(), R.drawable.ic_text))
                            .addText("Text")
                            .setContentIntent(getBroadcastIntent(ACTION_TOAST, "text")))
                        .addCell(cb ->cb
                            .addImage(Icon.createWithResource(getContext(), R.drawable.ic_video))
                            .setContentIntent(getBroadcastIntent(ACTION_TOAST, "video"))
                            .addText("Video"))
                        .addCell(cb -> cb
                            .addImage(Icon.createWithResource(getContext(), R.drawable.ic_email))
                            .addText("Email")
                            .setContentIntent(getBroadcastIntent(ACTION_TOAST, "email"))))
                .build();
    }

    private Slice createMessagingSlice(Uri sliceUri) {
        // TODO: Remote input.
        return new MessagingSliceBuilder(getContext(), sliceUri)
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
        // TODO: Remote input.
        return new ListBuilder(getContext(), sliceUri)
                .setColor(0xfff4b400)
                .addRow(b -> b
                    .setTitle("Create new note")
                    .setSubtitle("with this note taking app")
                    .addEndItem(Icon.createWithResource(getContext(), R.drawable.ic_create),
                            getBroadcastIntent(ACTION_TOAST, "create note"))
                    .addEndItem(Icon.createWithResource(getContext(), R.drawable.ic_voice),
                            getBroadcastIntent(ACTION_TOAST, "voice note"))
                    .addEndItem(Icon.createWithResource(getContext(), R.drawable.ic_camera),
                            getIntent("android.media.action.IMAGE_CAPTURE")))
                .build();
    }

    private Slice createReservationSlice(Uri sliceUri) {
        return new ListBuilder(getContext(), sliceUri)
                .setColor(0xffFF5252)
                .addRow(b -> b
                    .setTitle("Upcoming trip to Seattle")
                    .setSubtitle("Feb 1 - 19 | 2 guests")
                    .addEndItem(Icon.createWithResource(getContext(), R.drawable.ic_location),
                            getBroadcastIntent(ACTION_TOAST, "show reservation location on map"))
                    .addEndItem(Icon.createWithResource(getContext(), R.drawable.ic_text),
                            getBroadcastIntent(ACTION_TOAST, "contact host")))
                .addGrid(b -> b
                    .addCell(cb -> cb
                        .addLargeImage(
                                Icon.createWithResource(getContext(), R.drawable.reservation))))
                .addGrid(b -> b
                    .addCell(cb -> cb
                        .addTitleText("Check In")
                        .addText("12:00 PM, Feb 1"))
                    .addCell(cb -> cb
                        .addTitleText("Check Out")
                        .addText("11:00 AM, Feb 19")))
                .build();
    }

    private Slice createRideSlice(Uri sliceUri) {
        final ForegroundColorSpan colorSpan = new ForegroundColorSpan(0xff0F9D58);
        SpannableString headerSubtitle = new SpannableString("Ride in 4 min");
        headerSubtitle.setSpan(colorSpan, 8, headerSubtitle.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString homeSubtitle = new SpannableString("12 miles | 12 min | $9.00");
        homeSubtitle.setSpan(colorSpan, 20, homeSubtitle.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString workSubtitle = new SpannableString("44 miles | 1 hour 45 min | $31.41");
        workSubtitle.setSpan(colorSpan, 27, workSubtitle.length(), SPAN_EXCLUSIVE_EXCLUSIVE);

        return new ListBuilder(getContext(), sliceUri)
                .setColor(0xff0F9D58)
                .setHeader(b -> b
                    .setTitle("Get ride")
                    .setSubtitle(headerSubtitle)
                    .setSummarySubtitle("Ride to work in 12 min | Ride home in 1 hour 45 min")
                    .setContentIntent(getBroadcastIntent(ACTION_TOAST, "get ride")))
                .addRow(b -> b
                    .setTitle("Work")
                    .setSubtitle(workSubtitle)
                    .addEndItem(Icon.createWithResource(getContext(), R.drawable.ic_work),
                            getBroadcastIntent(ACTION_TOAST, "work")))
                .addRow(b -> b
                    .setTitle("Home")
                    .setSubtitle(homeSubtitle)
                    .addEndItem(Icon.createWithResource(getContext(), R.drawable.ic_home),
                            getBroadcastIntent(ACTION_TOAST, "home")))
                .build();
    }

    private Slice createCustomToggleSlice(Uri sliceUri) {
        return new ListBuilder(getContext(), sliceUri)
                .setColor(0xffff4081)
                .addRow(b -> b
                    .setTitle("Custom toggle")
                    .setSubtitle("It can support two states")
                    .addToggle(getBroadcastIntent(ACTION_TOAST, "star toggled"),
                            true /* isChecked */,
                            Icon.createWithResource(getContext(), R.drawable.toggle_star)))
                .build();
    }

    private Slice createTwoCustomToggleSlices(Uri sliceUri) {
        return new ListBuilder(getContext(), sliceUri)
                .setColor(0xffff4081)
                .addRow(b -> b
                        .setTitle("2 toggles")
                        .setSubtitle("each supports two states")
                        .addToggle(getBroadcastIntent(ACTION_TOAST, "first star toggled"),
                                true /* isChecked */,
                                Icon.createWithResource(getContext(), R.drawable.toggle_star))
                        .addToggle(getBroadcastIntent(ACTION_TOAST, "second star toggled"),
                                false /* isChecked */,
                                Icon.createWithResource(getContext(), R.drawable.toggle_star)))
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
        return new ListBuilder(getContext(), sliceUri)
                .setColor(0xff4285f4)
                .addRow(b -> b
                    .setTitle("Wi-fi")
                    .setTitleItem(Icon.createWithResource(getContext(), R.drawable.ic_wifi))
                    .setSubtitle(state)
                    .addToggle(getBroadcastIntent(ACTION_WIFI_CHANGED, null), finalWifiEnabled)
                    .setContentIntent(getIntent(Settings.ACTION_WIFI_SETTINGS)))
                .build();
    }

    private Slice createStarRatingInputRange(Uri sliceUri) {
        return new ListBuilder(getContext(), sliceUri)
                .setColor(0xffff4081)
                .addInputRange(c -> c
                        .setTitle("Star rating")
                        .setThumb(Icon.createWithResource(getContext(), R.drawable.ic_star_on))
                        .setAction(getBroadcastIntent(ACTION_TOAST_RANGE_VALUE, null))
                        .setMax(5)
                        .setValue(3))
                .build();
    }

    private Slice createDownloadProgressRange(Uri sliceUri) {
        return new ListBuilder(getContext(), sliceUri)
                .setColor(0xffff4081)
                .addRange(c -> c
                        .setTitle("Download progress")
                        .setMax(100)
                        .setValue(75))
                .build();
    }

    private Handler mHandler = new Handler();
    private Runnable mLoader;
    private boolean mLoaded = false;

    private Slice createLoadingSlice(Uri sliceUri, boolean loadAll, boolean isList) {
        if (!mLoaded || mLoader != null) {
            // Need to load content or we're still loading so just return partial
            if (!mLoaded) {
                mLoader = () -> {
                    // Note that we've loaded things
                    mLoader = null;
                    mLoaded = true;
                    // Notify to update the slice
                    getContext().getContentResolver().notifyChange(sliceUri, null);
                };
                mHandler.postDelayed(mLoader, LOADING_DELAY_MS);
            }
            if (loadAll) {
                return isList
                        ? new ListBuilder(getContext(), sliceUri).build()
                        : new GridBuilder(getContext(), sliceUri).build();
            }
            return createPartialSlice(sliceUri, true, isList);
        } else {
            mLoaded = false;
            return createPartialSlice(sliceUri, false, isList);
        }
    }

    private Slice createPartialSlice(Uri sliceUri, boolean isPartial, boolean isList) {
        Icon icon = Icon.createWithResource(getContext(), R.drawable.ic_star_on);
        PendingIntent intent = getBroadcastIntent(ACTION_TOAST, "star tapped");
        PendingIntent intent2 = getBroadcastIntent(ACTION_TOAST, "toggle tapped");
        if (isPartial) {
            if (isList) {
                return new ListBuilder(getContext(), sliceUri)
                        .addRow(b -> createRow(b, "Slice that has content to load",
                                "Temporary subtitle", icon, intent, true))
                        .addRow(b -> createRow(b, null, null, null, intent, true))
                        .addRow(b -> b.setTitle("My title").addToggle(intent2, false, true))
                        .build();
            } else {
                return new GridBuilder(getContext(), sliceUri)
                        .addCell(b -> createCell(b, null, null, null, true))
                        .addCell(b -> createCell(b, "Two stars", null, icon, true))
                        .addCell(b -> createCell(b, null, null, null, true))
                        .build();
            }
        } else {
            if (isList) {
                return new ListBuilder(getContext(), sliceUri)
                        .addRow(b -> createRow(b, "Slice that has content to load",
                                "Subtitle loaded", icon, intent, false))
                        .addRow(b -> createRow(b, "Loaded row", "Loaded subtitle",
                                icon, intent, false))
                        .addRow(b -> b.setTitle("My title").addToggle(intent2, false))
                        .build();
            } else {
                return new GridBuilder(getContext(), sliceUri)
                        .addCell(b -> createCell(b, "One star", "meh", icon, false))
                        .addCell(b -> createCell(b, "Two stars", "good", icon, false))
                        .addCell(b -> createCell(b, "Three stars", "best", icon, false))
                        .build();
            }
        }
    }

    private ListBuilder.RowBuilder createRow(ListBuilder.RowBuilder rb, String title,
            String subtitle, Icon icon, PendingIntent content, boolean isLoading) {
        return rb.setTitle(title, isLoading)
          .setSubtitle(subtitle, isLoading)
          .addEndItem(icon, isLoading)
          .setContentIntent(content);
    }

    private GridBuilder.CellBuilder createCell(GridBuilder.CellBuilder cb, String text1,
            String text2, Icon icon, boolean isLoading) {
        return cb.addText(text1, isLoading).addText(text2, isLoading).addImage(icon, isLoading);
    }

    private PendingIntent getIntent(String action) {
        Intent intent = new Intent(action);
        PendingIntent pi = PendingIntent.getActivity(getContext(), 0, intent, 0);
        return pi;
    }

    private PendingIntent getBroadcastIntent(String action, String message) {
        Intent intent = new Intent(action);
        intent.setClass(getContext(), SliceBroadcastReceiver.class);
        // Ensure a new PendingIntent is created for each message.
        int requestCode = 0;
        if (message != null) {
            intent.putExtra(EXTRA_TOAST_MESSAGE, message);
            requestCode = message.hashCode();
        }
        return PendingIntent.getBroadcast(getContext(), requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
