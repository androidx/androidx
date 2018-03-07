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

import static androidx.slice.builders.ListBuilder.ICON_IMAGE;
import static androidx.slice.builders.ListBuilder.LARGE_IMAGE;
import static androidx.slice.builders.ListBuilder.SMALL_IMAGE;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.util.SparseArray;

import java.util.Calendar;

import androidx.annotation.NonNull;
import androidx.slice.Slice;
import androidx.slice.SliceProvider;
import androidx.slice.builders.GridBuilder;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.MessagingSliceBuilder;
import androidx.slice.builders.SliceAction;

/**
 * Examples of using slice template builders.
 */
public class SampleSliceProvider extends SliceProvider {

    private static final boolean TEST_CUSTOM_SEE_MORE = false;

    public static final String ACTION_WIFI_CHANGED =
            "com.example.androidx.slice.action.WIFI_CHANGED";
    public static final String ACTION_TOAST =
            "com.example.androidx.slice.action.TOAST";
    public static final String EXTRA_TOAST_MESSAGE = "com.example.androidx.extra.TOAST_MESSAGE";
    public static final String ACTION_TOAST_RANGE_VALUE =
            "com.example.androidx.slice.action.TOAST_RANGE_VALUE";

    public static final String[] URI_PATHS = {"message", "wifi", "note", "ride", "toggle",
            "toggle2", "contact", "gallery", "weather", "reservation", "loadlist", "loadgrid",
            "inputrange", "range", "contact2", "subscription"};

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
        if (!path.equals("/loadlist")) {
            mListSummaries.clear();
            mListLastUpdate = 0;
        }
        if (!path.equals("/loadgrid")) {
            mGridSummaries.clear();
            mGridLastUpdate = 0;
        }
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
                return createLoadingListSlice(sliceUri);
            case "/loadgrid":
                return createLoadingGridSlice(sliceUri);
            case "/inputrange":
                return createStarRatingInputRange(sliceUri);
            case "/range":
                return createDownloadProgressRange(sliceUri);
            case "/subscription":
                return createCatSlice(sliceUri, false /* customSeeMore */);
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
                                SMALL_IMAGE)
                        .addText("MON")
                        .addTitleText("69\u00B0"))
                .addCell(cb -> cb
                        .addImage(Icon.createWithResource(getContext(), R.drawable.weather_2),
                                SMALL_IMAGE)
                        .addText("TUE")
                        .addTitleText("71\u00B0"))
                .addCell(cb -> cb
                        .addImage(Icon.createWithResource(getContext(), R.drawable.weather_3),
                                SMALL_IMAGE)
                        .addText("WED")
                        .addTitleText("76\u00B0"))
                .addCell(cb -> cb
                        .addImage(Icon.createWithResource(getContext(), R.drawable.weather_4),
                                SMALL_IMAGE)
                        .addText("THU")
                        .addTitleText("72\u00B0"))
                .addCell(cb -> cb
                        .addImage(Icon.createWithResource(getContext(), R.drawable.weather_1),
                                SMALL_IMAGE)
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
                            LARGE_IMAGE))
                    .addCell(cb -> cb
                        .addImage(Icon.createWithResource(getContext(), R.drawable.slices_2),
                                LARGE_IMAGE))
                    .addCell(cb -> cb
                        .addImage(Icon.createWithResource(getContext(), R.drawable.slices_3),
                                LARGE_IMAGE))
                    .addCell(cb -> cb
                        .addImage(Icon.createWithResource(getContext(), R.drawable.slices_4),
                                LARGE_IMAGE))
                    .addCell(cb -> cb
                        .addImage(Icon.createWithResource(getContext(), R.drawable.slices_2),
                                LARGE_IMAGE))
                    .addCell(cb -> cb
                        .addImage(Icon.createWithResource(getContext(), R.drawable.slices_3),
                                LARGE_IMAGE))
                    .addCell(cb -> cb
                        .addImage(Icon.createWithResource(getContext(), R.drawable.slices_4),
                                LARGE_IMAGE))
                    .addSeeMoreAction(getBroadcastIntent(ACTION_TOAST, "see your gallery")))
                .build();
    }

    private Slice createCatSlice(Uri sliceUri, boolean customSeeMore) {
        ListBuilder b = new ListBuilder(getContext(), sliceUri);
        GridBuilder gb = new GridBuilder(b);
        PendingIntent pi = getBroadcastIntent(ACTION_TOAST, "See cats you follow");
        if (customSeeMore) {
            GridBuilder.CellBuilder cb = new GridBuilder.CellBuilder(gb);
            cb.addImage(Icon.createWithResource(getContext(), R.drawable.ic_right_caret),
                    ICON_IMAGE);
            cb.setContentIntent(pi);
            cb.addTitleText("All cats");
            gb.addSeeMoreCell(cb);
        } else {
            gb.addSeeMoreAction(pi);
        }
        gb.addCell(new GridBuilder.CellBuilder(gb)
                    .addImage(Icon.createWithResource(getContext(), R.drawable.cat_1), SMALL_IMAGE)
                    .addTitleText("Oreo"))
                .addCell(new GridBuilder.CellBuilder(gb)
                        .addImage(Icon.createWithResource(getContext(), R.drawable.cat_2),
                                SMALL_IMAGE)
                        .addTitleText("Silver"))
                .addCell(new GridBuilder.CellBuilder(gb)
                        .addImage(Icon.createWithResource(getContext(), R.drawable.cat_3),
                                SMALL_IMAGE)
                        .addTitleText("Drake"))
                .addCell(new GridBuilder.CellBuilder(gb)
                        .addImage(Icon.createWithResource(getContext(), R.drawable.cat_5),
                                SMALL_IMAGE)
                        .addTitleText("Olive"))
                .addCell(new GridBuilder.CellBuilder(gb)
                        .addImage(Icon.createWithResource(getContext(), R.drawable.cat_4),
                                SMALL_IMAGE)
                        .addTitleText("Lady Marmalade"))
                .addCell(new GridBuilder.CellBuilder(gb)
                        .addImage(Icon.createWithResource(getContext(), R.drawable.cat_6),
                                SMALL_IMAGE)
                        .addTitleText("Grapefruit"));
        return b.addGrid(gb).build();
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
                                        ICON_IMAGE)
                                .addText("Call")
                                .setContentIntent(getBroadcastIntent(ACTION_TOAST, "call")))
                        .addCell(new GridBuilder.CellBuilder(gb)
                                .addImage(Icon.createWithResource(getContext(), R.drawable.ic_text),
                                        ICON_IMAGE)
                                .addText("Text")
                                .setContentIntent(getBroadcastIntent(ACTION_TOAST, "text")))
                        .addCell(new GridBuilder.CellBuilder(gb)
                                .addImage(Icon.createWithResource(getContext(),
                                        R.drawable.ic_video), ICON_IMAGE)
                                .setContentIntent(getBroadcastIntent(ACTION_TOAST, "video"))
                                .addText("Video"))
                        .addCell(new GridBuilder.CellBuilder(gb)
                                .addImage(Icon.createWithResource(getContext(),
                                        R.drawable.ic_email), ICON_IMAGE)
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
                            LARGE_IMAGE)))
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

        // Set the first row as a toggle
        boolean finalWifiEnabled = wifiEnabled;
        SliceAction primaryAction = new SliceAction(getIntent(Settings.ACTION_WIFI_SETTINGS),
                Icon.createWithResource(getContext(), R.drawable.ic_wifi), "Wi-fi Settings");
        ListBuilder lb = new ListBuilder(getContext(), sliceUri)
                .setColor(0xff4285f4)
                .addRow(b -> b
                    .setTitle("Wi-fi")
                    .setSubtitle(state)
                    .addEndItem(new SliceAction(getBroadcastIntent(ACTION_WIFI_CHANGED, null),
                            "Toggle wifi", finalWifiEnabled))
                    .setPrimaryAction(primaryAction));

        // Add fake wifi networks
        int[] wifiIcons = new int[] {R.drawable.ic_wifi_full, R.drawable.ic_wifi_low,
                R.drawable.ic_wifi_fair};
        for (int i = 0; i < 10; i++) {
            final int iconId = wifiIcons[i % wifiIcons.length];
            Icon icon = Icon.createWithResource(getContext(), iconId);
            final String networkName = "Network" + i;
            ListBuilder.RowBuilder rb = new ListBuilder.RowBuilder(lb);
            rb.setTitleItem(icon, ICON_IMAGE).setTitle("Network" + networkName);
            boolean locked = i % 3 == 0;
            if (locked) {
                rb.addEndItem(Icon.createWithResource(getContext(), R.drawable.ic_lock));
            }
            String message = locked ? "Open wifi password dialog" : "Connect to " + networkName;
            rb.setPrimaryAction(new SliceAction(getBroadcastIntent(ACTION_TOAST, message), icon,
                    message));
            lb.addRow(rb);
        }

        // Add see more intent
        if (TEST_CUSTOM_SEE_MORE) {
            lb.addSeeMoreRow(rb -> rb
                    .setTitle("See all available networks")
                    .addEndItem(Icon.createWithResource(getContext(), R.drawable.ic_right_caret))
                    .setPrimaryAction(primaryAction));
        } else {
            lb.addSeeMoreAction(primaryAction.getAction());
        }
        return lb.build();
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
    private SparseArray<String> mListSummaries = new SparseArray<>();
    private long mListLastUpdate;
    private SparseArray<String> mGridSummaries = new SparseArray<>();
    private long mGridLastUpdate;

    private void update(long delay, SparseArray<String> summaries, int id, String s, Uri uri,
            Runnable r) {
        mHandler.postDelayed(() -> {
            summaries.put(id, s);
            getContext().getContentResolver().notifyChange(uri, null);
            r.run();
        }, delay);
    }

    private Slice createLoadingListSlice(Uri sliceUri) {
        boolean updating = mListLastUpdate == 0
                || mListLastUpdate < (System.currentTimeMillis() - 10 * System.currentTimeMillis());
        if (updating) {
            Runnable r = () -> mListLastUpdate = System.currentTimeMillis();
            update(1000, mListSummaries, 0, "44 miles | 1 hour 45 min | $31.41", sliceUri, r);
            update(1500, mListSummaries, 1, "12 miles | 12 min | $9.00", sliceUri, r);
            update(1700, mListSummaries, 2, "5 miles | 10 min | $8.00", sliceUri, r);
        }
        Slice s = new ListBuilder(getContext(), sliceUri)
                .addRow(b -> b
                        .setTitle("Work")
                        .setSubtitle(mListSummaries.get(0, ""), updating)
                        .addEndItem(Icon.createWithResource(getContext(), R.drawable.ic_work),
                                ICON_IMAGE))
                .addRow(b -> b
                        .setTitle("Home")
                        .setSubtitle(mListSummaries.get(1, ""), updating)
                        .addEndItem(
                                Icon.createWithResource(getContext(), R.drawable.ic_home),
                                ICON_IMAGE))
                .addRow(b -> b
                        .setTitle("School")
                        .setSubtitle(mListSummaries.get(2, ""), updating)
                        .addEndItem(Icon.createWithResource(getContext(), R.drawable.ic_school),
                                ICON_IMAGE))
                .build();
        return s;
    }

    private Slice createLoadingGridSlice(Uri sliceUri) {
        boolean updating = mGridLastUpdate == 0
                || mGridLastUpdate < (System.currentTimeMillis() - 10 * System.currentTimeMillis());
        if (updating) {
            Runnable r = () -> mGridLastUpdate = System.currentTimeMillis();
            update(2000, mGridSummaries, 0, "Heavy traffic in your area", sliceUri, r);
            update(3500, mGridSummaries, 1, "Typical conditions with delays up to 28 min",
                    sliceUri, r);
            update(3000, mGridSummaries, 2, "41 min", sliceUri, r);
            update(1500, mGridSummaries, 3, "33 min", sliceUri, r);
            update(1000, mGridSummaries, 4, "12 min", sliceUri, r);
        }
        Slice s = new ListBuilder(getContext(), sliceUri)
                .setHeader(hb -> hb
                        .setTitle(mGridSummaries.get(0, ""), updating)
                        .setSubtitle(mGridSummaries.get(1, ""), updating))
                .addGrid(gb -> gb
                    .addCell(cb -> cb
                            .addImage(Icon.createWithResource(getContext(), R.drawable.ic_home),
                                    ICON_IMAGE)
                            .addTitleText("Home")
                            .addText(mGridSummaries.get(2, ""), updating))
                    .addCell(cb -> cb
                            .addImage(Icon.createWithResource(getContext(), R.drawable.ic_work),
                                    ICON_IMAGE)
                            .addTitleText("Work")
                            .addText(mGridSummaries.get(3, ""), updating))
                    .addCell(cb -> cb
                            .addImage(Icon.createWithResource(getContext(), R.drawable.ic_school),
                                    ICON_IMAGE)
                            .addTitleText("School")
                            .addText(mGridSummaries.get(4, ""), updating)))
                    .build();
        return s;
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
