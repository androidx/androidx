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
import androidx.app.slice.builders.SliceAction;

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
            "loadgrid", "loadgrid2", "inputrange", "range", "contact2"};

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
            case "/contact2":
                return createContact2(sliceUri);
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
        SliceAction primaryAction = new SliceAction(getBroadcastIntent(ACTION_TOAST,
                "open weather app"), Icon.createWithResource(getContext(), R.drawable.weather_1),
                "Weather is happening!");
        return new ListBuilder(getContext(), sliceUri).addGrid(gb -> gb
                .setPrimaryAction(primaryAction)
                .addCell(cb -> cb
                        .addImage(Icon.createWithResource(getContext(), R.drawable.weather_1),
                                GridBuilder.SMALL_IMAGE)
                        .addText("MON")
                        .addTitleText("69\u00B0"))
                .addCell(cb -> cb
                        .addImage(Icon.createWithResource(getContext(), R.drawable.weather_2),
                                GridBuilder.SMALL_IMAGE)
                        .addText("TUE")
                        .addTitleText("71\u00B0"))
                .addCell(cb -> cb
                        .addImage(Icon.createWithResource(getContext(), R.drawable.weather_3),
                                GridBuilder.SMALL_IMAGE)
                        .addText("WED")
                        .addTitleText("76\u00B0"))
                .addCell(cb -> cb
                        .addImage(Icon.createWithResource(getContext(), R.drawable.weather_4),
                                GridBuilder.SMALL_IMAGE)
                        .addText("THU")
                        .addTitleText("72\u00B0"))
                .addCell(cb -> cb
                        .addImage(Icon.createWithResource(getContext(), R.drawable.weather_1),
                                GridBuilder.SMALL_IMAGE)
                        .addText("FRI")
                        .addTitleText("68\u00B0")))
                .build();
    }

    private Slice createGallery(Uri sliceUri) {
        return new ListBuilder(getContext(), sliceUri)
                .setColor(0xff4285F4)
                .addRow(b -> b
                    .setTitle("Family trip to Hawaii")
                    .setSubtitle("Sep 30, 2017 - Oct 2, 2017"))
                .addAction(new SliceAction(
                        getBroadcastIntent(ACTION_TOAST, "cast photo album"),
                        Icon.createWithResource(getContext(), R.drawable.ic_cast),
                        "Cast photo album"))
                .addAction(new SliceAction(
                        getBroadcastIntent(ACTION_TOAST, "share photo album"),
                        Icon.createWithResource(getContext(), R.drawable.ic_share),
                        "Share photo album"))
                .addGrid(b -> b
                    .addCell(cb -> cb
                        .addImage(Icon.createWithResource(getContext(), R.drawable.slices_1),
                            GridBuilder.LARGE_IMAGE))
                    .addCell(cb -> cb
                        .addImage(Icon.createWithResource(getContext(), R.drawable.slices_2),
                                GridBuilder.LARGE_IMAGE))
                    .addCell(cb -> cb
                        .addImage(Icon.createWithResource(getContext(), R.drawable.slices_3),
                                GridBuilder.LARGE_IMAGE))
                    .addCell(cb -> cb
                        .addImage(Icon.createWithResource(getContext(), R.drawable.slices_4),
                                GridBuilder.LARGE_IMAGE))
                    .addCell(cb -> cb
                        .addImage(Icon.createWithResource(getContext(), R.drawable.slices_2),
                                GridBuilder.LARGE_IMAGE))
                    .addCell(cb -> cb
                        .addImage(Icon.createWithResource(getContext(), R.drawable.slices_3),
                                GridBuilder.LARGE_IMAGE))
                    .addCell(cb -> cb
                        .addImage(Icon.createWithResource(getContext(), R.drawable.slices_4),
                                GridBuilder.LARGE_IMAGE)))
                .build();
    }


    private Slice createContact2(Uri sliceUri) {
        ListBuilder b = new ListBuilder(getContext(), sliceUri);
        ListBuilder.RowBuilder rb = new ListBuilder.RowBuilder(b);
        GridBuilder gb = new GridBuilder(b);
        return b.setColor(0xff3949ab)
                .addRow(rb
                        .setTitle("Mady Pitza")
                        .setSubtitle("Frequently contacted contact")
                        .addEndItem(Icon.createWithResource(getContext(), R.drawable.mady)))
                .addGrid(gb
                        .addCell(new GridBuilder.CellBuilder(gb)
                                .addImage(Icon.createWithResource(getContext(), R.drawable.ic_call),
                                        GridBuilder.ICON_IMAGE)
                                .addText("Call")
                                .setContentIntent(getBroadcastIntent(ACTION_TOAST, "call")))
                        .addCell(new GridBuilder.CellBuilder(gb)
                                .addImage(Icon.createWithResource(getContext(), R.drawable.ic_text),
                                        GridBuilder.ICON_IMAGE)
                                .addText("Text")
                                .setContentIntent(getBroadcastIntent(ACTION_TOAST, "text")))
                        .addCell(new GridBuilder.CellBuilder(gb)
                                .addImage(Icon.createWithResource(getContext(),
                                        R.drawable.ic_video), GridBuilder.ICON_IMAGE)
                                .setContentIntent(getBroadcastIntent(ACTION_TOAST, "video"))
                                .addText("Video"))
                        .addCell(new GridBuilder.CellBuilder(gb)
                                .addImage(Icon.createWithResource(getContext(),
                                        R.drawable.ic_email), GridBuilder.ICON_IMAGE)
                                .addText("Email")
                                .setContentIntent(getBroadcastIntent(ACTION_TOAST, "email"))))
                .build();
    }

    private Slice createContact(Uri sliceUri) {
        final long lastCalled = System.currentTimeMillis() - 20 * DateUtils.MINUTE_IN_MILLIS;
        CharSequence lastCalledString = DateUtils.getRelativeTimeSpanString(lastCalled,
                Calendar.getInstance().getTimeInMillis(),
                DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE);
        SliceAction primaryAction = new SliceAction(getBroadcastIntent(ACTION_TOAST,
                "See contact info"), Icon.createWithResource(getContext(),
                R.drawable.mady), "Mady");
        return new ListBuilder(getContext(), sliceUri)
                .setColor(0xff3949ab)
                .setHeader(b -> b
                        .setTitle("Mady Pitza")
                        .setSummarySubtitle("Called " + lastCalledString)
                        .setPrimaryAction(primaryAction))
                .addRow(b -> b
                        .setTitleItem(Icon.createWithResource(getContext(), R.drawable.ic_call))
                        .setTitle("314-259-2653")
                        .setSubtitle("Call lasted 1 hr 17 min")
                        .addEndItem(lastCalled))
                .addRow(b -> b
                        .setTitleItem(Icon.createWithResource(getContext(), R.drawable.ic_text))
                        .setTitle("You: Coooooool see you then")
                        .addEndItem(System.currentTimeMillis() - 40 * DateUtils.MINUTE_IN_MILLIS))
                .addAction(new SliceAction(getBroadcastIntent(ACTION_TOAST, "call"),
                        Icon.createWithResource(getContext(), R.drawable.ic_call), "Call mady"))
                .addAction(new SliceAction(getBroadcastIntent(ACTION_TOAST, "text"),
                        Icon.createWithResource(getContext(), R.drawable.ic_text), "Text mady"))
                .addAction(new SliceAction(getBroadcastIntent(ACTION_TOAST, "video"),
                        Icon.createWithResource(getContext(), R.drawable.ic_video),
                        "Video call mady"))
                .addAction(new SliceAction(getBroadcastIntent(ACTION_TOAST, "email"),
                        Icon.createWithResource(getContext(), R.drawable.ic_email), "Email mady"))
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
                .addRow(b -> b.setTitle("Create new note"))
                .addAction(new SliceAction(getBroadcastIntent(ACTION_TOAST, "create note"),
                        Icon.createWithResource(getContext(), R.drawable.ic_create),
                        "Create note"))
                .addAction(new SliceAction(getBroadcastIntent(ACTION_TOAST, "voice note"),
                        Icon.createWithResource(getContext(), R.drawable.ic_voice),
                        "Voice note"))
                .addAction(new SliceAction(getIntent("android.media.action.IMAGE_CAPTURE"),
                        Icon.createWithResource(getContext(), R.drawable.ic_camera),
                        "Photo note"))
                .build();
    }

    private Slice createReservationSlice(Uri sliceUri) {
        return new ListBuilder(getContext(), sliceUri)
                .setColor(0xffFF5252)
                .setHeader(b -> b
                    .setTitle("Upcoming trip to Seattle")
                    .setSubtitle("Feb 1 - 19 | 2 guests"))
                .addAction(new SliceAction(
                        getBroadcastIntent(ACTION_TOAST, "show location on map"),
                        Icon.createWithResource(getContext(), R.drawable.ic_location),
                        "Show reservation location"))
                .addAction(new SliceAction(getBroadcastIntent(ACTION_TOAST, "contact host"),
                        Icon.createWithResource(getContext(), R.drawable.ic_text),
                        "Contact host"))
                .addGrid(b -> b
                    .addCell(cb -> cb
                        .addImage(Icon.createWithResource(getContext(), R.drawable.reservation),
                            GridBuilder.LARGE_IMAGE)))
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

        SliceAction primaryAction = new SliceAction(getBroadcastIntent(ACTION_TOAST, "get ride"),
                Icon.createWithResource(getContext(), R.drawable.ic_car), "Get Ride");
        return new ListBuilder(getContext(), sliceUri)
                .setColor(0xff0F9D58)
                .setHeader(b -> b
                    .setTitle("Get ride")
                    .setSubtitle(headerSubtitle)
                    .setSummarySubtitle("Ride to work in 12 min | Ride home in 1 hour 45 min")
                    .setPrimaryAction(primaryAction))
                .addRow(b -> b
                    .setTitle("Work")
                    .setSubtitle(workSubtitle)
                    .addEndItem(new SliceAction(getBroadcastIntent(ACTION_TOAST, "work"),
                            Icon.createWithResource(getContext(), R.drawable.ic_work),
                            "Get ride to work")))
                .addRow(b -> b
                    .setTitle("Home")
                    .setSubtitle(homeSubtitle)
                    .addEndItem(new SliceAction(getBroadcastIntent(ACTION_TOAST, "home"),
                            Icon.createWithResource(getContext(), R.drawable.ic_home),
                            "Get ride home")))
                .build();
    }

    private Slice createCustomToggleSlice(Uri sliceUri) {
        return new ListBuilder(getContext(), sliceUri)
                .setColor(0xffff4081)
                .addRow(b -> b
                    .setTitle("Custom toggle")
                    .setSubtitle("It can support two states")
                    .setPrimaryAction(new SliceAction(getBroadcastIntent(ACTION_TOAST,
                            "star toggled"),
                            Icon.createWithResource(getContext(), R.drawable.toggle_star),
                            "Toggle star", true /* isChecked */)))
                .build();
    }

    private Slice createTwoCustomToggleSlices(Uri sliceUri) {
        return new ListBuilder(getContext(), sliceUri)
                .setColor(0xffff4081)
                .addRow(b -> b
                        .setTitle("2 toggles")
                        .setSubtitle("each supports two states")
                        .addEndItem(new SliceAction(
                                getBroadcastIntent(ACTION_TOAST, "first star toggled"),
                                Icon.createWithResource(getContext(), R.drawable.toggle_star),
                                "Toggle star", true /* isChecked */))
                        .addEndItem(new SliceAction(
                                getBroadcastIntent(ACTION_TOAST, "second star toggled"),
                                Icon.createWithResource(getContext(), R.drawable.toggle_star),
                                "Toggle star", false /* isChecked */)))
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
        SliceAction primaryAction = new SliceAction(getIntent(Settings.ACTION_WIFI_SETTINGS),
                Icon.createWithResource(getContext(), R.drawable.ic_wifi), "Wi-fi Settings");
        return new ListBuilder(getContext(), sliceUri)
                .setColor(0xff4285f4)
                .addRow(b -> b
                    .setTitle("Wi-fi")
                    .setSubtitle(state)
                    .addEndItem(new SliceAction(getBroadcastIntent(ACTION_WIFI_CHANGED, null),
                            "Toggle wifi", finalWifiEnabled))
                    .setPrimaryAction(primaryAction))
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
                return new ListBuilder(getContext(), sliceUri).build();
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
                        .addRow(b -> b
                                .setTitle("My title")
                                .addEndItem(new SliceAction(intent2, "Some action",
                                        false /* isChecked */),
                                        true /* isLoading */))
                        .build();
            } else {
                return new ListBuilder(getContext(), sliceUri).addGrid(gb -> gb
                        .addCell(b -> createCell(b, null, null, null, true))
                        .addCell(b -> createCell(b, "Two stars", null, icon, true))
                        .addCell(b -> createCell(b, null, null, null, true)))
                        .build();
            }
        } else {
            if (isList) {
                return new ListBuilder(getContext(), sliceUri)
                        .addRow(b -> createRow(b, "Slice that has content to load",
                                "Subtitle loaded", icon, intent, false))
                        .addRow(b -> createRow(b, "Loaded row", "Loaded subtitle",
                                icon, intent, false))
                        .addRow(b -> b
                                .setTitle("My title")
                                .addEndItem(new SliceAction(intent2, "Some action",
                                                false /* isChecked */)))
                        .build();
            } else {
                return new ListBuilder(getContext(), sliceUri).addGrid(gb -> gb
                        .addCell(b -> createCell(b, "One star", "meh", icon, false))
                        .addCell(b -> createCell(b, "Two stars", "good", icon, false))
                        .addCell(b -> createCell(b, "Three stars", "best", icon, false)))
                        .build();
            }
        }
    }

    private ListBuilder.RowBuilder createRow(ListBuilder.RowBuilder rb, String title,
            String subtitle, Icon icon, PendingIntent content, boolean isLoading) {
        SliceAction primaryAction = new SliceAction(content, icon, title);
        return rb.setTitle(title, isLoading)
          .setSubtitle(subtitle, isLoading)
          .addEndItem(icon, isLoading)
          .setPrimaryAction(primaryAction);
    }

    private GridBuilder.CellBuilder createCell(GridBuilder.CellBuilder cb, String text1,
            String text2, Icon icon, boolean isLoading) {
        return cb.addText(text1, isLoading).addText(text2, isLoading).addImage(icon,
                GridBuilder.SMALL_IMAGE, isLoading);
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
