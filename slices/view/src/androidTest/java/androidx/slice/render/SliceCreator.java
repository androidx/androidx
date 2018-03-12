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

package androidx.slice.render;

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
import android.provider.Settings;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;

import java.util.Arrays;

import androidx.slice.Slice;
import androidx.slice.builders.GridBuilder;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.MessagingSliceBuilder;
import androidx.slice.builders.SliceAction;
import androidx.slice.view.test.R;

/**
 * Examples of using slice template builders.
 */
public class SliceCreator {

    public static final String ACTION_WIFI_CHANGED =
            "com.example.androidx.slice.action.WIFI_CHANGED";
    public static final String ACTION_TOAST =
            "com.example.androidx.slice.action.TOAST";
    public static final String EXTRA_TOAST_MESSAGE = "com.example.androidx.extra.TOAST_MESSAGE";

    public static final String[] URI_PATHS = {"message", "wifi", "wifi2", "note", "ride", "toggle",
            "toggle2", "contact", "gallery", "subscription", "subscription2", "weather",
            "reservation"};

    private final Context mContext;

    public SliceCreator(Context context) {
        mContext = context;
    }

    private Context getContext() {
        return mContext;
    }

    /**
     * @return Uri with the provided path
     */
    public static Uri getUri(String path, Context context) {
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority("androidx.slice.view.test")
                .appendPath(path)
                .build();
    }

    public Slice onBindSlice(Uri sliceUri) {
        String path = sliceUri.getPath();
        switch (path) {
            case "/message":
                return createMessagingSlice(sliceUri);
            case "/wifi":
                return createWifiSlice(sliceUri, false /* customSeeMore */);
            case "/wifi2":
                return createWifiSlice(sliceUri, true /* customSeeMore */);
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
            case "/subscription":
                return createSubSlice(sliceUri, false /* customSeeMore */);
            case "/subscription2":
                return createSubSlice(sliceUri, true /* customSeeMore */);
            case "/weather":
                return createWeather(sliceUri);
            case "/reservation":
                return createReservationSlice(sliceUri);
        }
        throw new IllegalArgumentException("Unknown uri " + sliceUri);
    }

    private Slice createWeather(Uri sliceUri) {
        SliceAction primaryAction = new SliceAction(getBroadcastIntent(ACTION_TOAST,
                "open weather app"), Icon.createWithResource(getContext(), R.drawable.weather_1),
                "Weather is happening!");
        ListBuilder b = new ListBuilder(getContext(), sliceUri);
        GridBuilder gb = new GridBuilder(b);
        gb.setPrimaryAction(primaryAction);
        gb.addCell(new GridBuilder.CellBuilder(gb)
                        .addImage(Icon.createWithResource(getContext(), R.drawable.weather_1),
                                SMALL_IMAGE)
                        .addText("MON")
                        .addTitleText("69\u00B0"))
                .addCell(new GridBuilder.CellBuilder(gb)
                        .addImage(Icon.createWithResource(getContext(), R.drawable.weather_2),
                                SMALL_IMAGE)
                        .addText("TUE")
                        .addTitleText("71\u00B0"))
                .addCell(new GridBuilder.CellBuilder(gb)
                        .addImage(Icon.createWithResource(getContext(), R.drawable.weather_3),
                                SMALL_IMAGE)
                        .addText("WED")
                        .addTitleText("76\u00B0"))
                .addCell(new GridBuilder.CellBuilder(gb)
                        .addImage(Icon.createWithResource(getContext(), R.drawable.weather_4),
                                SMALL_IMAGE)
                        .addText("THU")
                        .addTitleText("72\u00B0"))
                .addCell(new GridBuilder.CellBuilder(gb)
                        .addImage(Icon.createWithResource(getContext(), R.drawable.weather_1),
                                SMALL_IMAGE)
                        .addText("FRI")
                        .addTitleText("68\u00B0"));
        return b.addGrid(gb).build();
    }

    private Slice createGallery(Uri sliceUri) {
        ListBuilder b = new ListBuilder(getContext(), sliceUri);
        GridBuilder gb = new GridBuilder(b);
        PendingIntent pi = getBroadcastIntent(ACTION_TOAST, "see more of your gallery");
        gb.addSeeMoreAction(pi);
        gb.addCell(new GridBuilder.CellBuilder(gb)
                .addImage(Icon.createWithResource(getContext(), R.drawable.slices_1),
                        LARGE_IMAGE))
                .addCell(new GridBuilder.CellBuilder(gb)
                        .addImage(Icon.createWithResource(getContext(), R.drawable.slices_2),
                                LARGE_IMAGE))
                .addCell(new GridBuilder.CellBuilder(gb)
                        .addImage(Icon.createWithResource(getContext(), R.drawable.slices_3),
                                LARGE_IMAGE))
                .addCell(new GridBuilder.CellBuilder(gb)
                        .addImage(Icon.createWithResource(getContext(), R.drawable.slices_4),
                                LARGE_IMAGE))
                .addCell(new GridBuilder.CellBuilder(gb)
                        .addImage(Icon.createWithResource(getContext(), R.drawable.slices_2),
                                LARGE_IMAGE))
                .addCell(new GridBuilder.CellBuilder(gb)
                        .addImage(Icon.createWithResource(getContext(), R.drawable.slices_3),
                                LARGE_IMAGE))
                .addCell(new GridBuilder.CellBuilder(gb)
                        .addImage(Icon.createWithResource(getContext(), R.drawable.slices_4),
                                LARGE_IMAGE));
        return b.addGrid(gb).build();
    }

    private Slice createSubSlice(Uri sliceUri, boolean customSeeMore) {
        ListBuilder b = new ListBuilder(getContext(), sliceUri);
        GridBuilder gb = new GridBuilder(b);
        GridBuilder.CellBuilder cb = new GridBuilder.CellBuilder(gb);
        PendingIntent pi = getBroadcastIntent(ACTION_TOAST, "See cats you follow");
        if (customSeeMore) {
            cb.addImage(Icon.createWithResource(getContext(), R.drawable.ic_right_caret),
                    ICON_IMAGE);
            cb.setContentIntent(pi);
            cb.addText("All cats");
            gb.addSeeMoreCell(cb);
        } else {
            gb.addSeeMoreAction(pi);
        }
        gb.addCell(new GridBuilder.CellBuilder(gb)
                    .addImage(Icon.createWithResource(getContext(), R.drawable.cat_1),
                            SMALL_IMAGE)
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

    private Slice createContact(Uri sliceUri) {
        ListBuilder b = new ListBuilder(getContext(), sliceUri);
        ListBuilder.RowBuilder rb = new ListBuilder.RowBuilder(b);
        GridBuilder gb = new GridBuilder(b);
        return b.setColor(0xff3949ab)
                .addRow(rb
                        .setTitle("Mady Pitza")
                        .setSubtitle("Frequently contacted contact")
                        .addEndItem(Icon.createWithResource(getContext(), R.drawable.mady),
                        SMALL_IMAGE))
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
                            .addImage(Icon.createWithResource(getContext(), R.drawable.ic_video),
                                    ICON_IMAGE)
                            .setContentIntent(getBroadcastIntent(ACTION_TOAST, "video"))
                            .addText("Video"))
                        .addCell(new GridBuilder.CellBuilder(gb)
                            .addImage(Icon.createWithResource(getContext(), R.drawable.ic_email),
                                    ICON_IMAGE)
                            .addText("Email")
                            .setContentIntent(getBroadcastIntent(ACTION_TOAST, "email"))))
                .build();
    }

    private Slice createMessagingSlice(Uri sliceUri) {
        // TODO: Remote input.
        MessagingSliceBuilder mb = new MessagingSliceBuilder(getContext(), sliceUri);
        return mb
                .add(new MessagingSliceBuilder.MessageBuilder(mb)
                        .addText("yo home \uD83C\uDF55, I emailed you the info")
                        .addTimestamp(System.currentTimeMillis() - 20 * DateUtils.MINUTE_IN_MILLIS)
                        .addSource(Icon.createWithResource(getContext(), R.drawable.mady)))
                .add(new MessagingSliceBuilder.MessageBuilder(mb)
                        .addText("just bought my tickets")
                        .addTimestamp(System.currentTimeMillis() - 10 * DateUtils.MINUTE_IN_MILLIS))
                .add(new MessagingSliceBuilder.MessageBuilder(mb)
                        .addText("yay! can't wait for getContext() weekend!\n"
                                + "\uD83D\uDE00")
                        .addTimestamp(System.currentTimeMillis() - 5 * DateUtils.MINUTE_IN_MILLIS)
                        .addSource(Icon.createWithResource(getContext(), R.drawable.mady)))
                .build();

    }

    private Slice createNoteSlice(Uri sliceUri) {
        // TODO: Remote input.
        ListBuilder lb = new ListBuilder(getContext(), sliceUri);
        return lb.setColor(0xfff4b400)
                .addRow(new ListBuilder.RowBuilder(lb)
                    .setTitle("Create new note")
                    .setSubtitle("with this note taking app")
                    .addEndItem(new SliceAction(getBroadcastIntent(ACTION_TOAST, "create note"),
                            Icon.createWithResource(getContext(), R.drawable.ic_create),
                            "Create note"))
                    .addEndItem(new SliceAction(getBroadcastIntent(ACTION_TOAST, "voice note"),
                            Icon.createWithResource(getContext(), R.drawable.ic_voice),
                            "Voice note"))
                    .addEndItem(new SliceAction(getIntent("android.media.action.IMAGE_CAPTURE"),
                            Icon.createWithResource(getContext(), R.drawable.ic_camera),
                            "Photo note")))
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
        ListBuilder lb = new ListBuilder(getContext(), sliceUri);
        return lb.setColor(0xff0F9D58)
                .setHeader(new ListBuilder.HeaderBuilder(lb)
                        .setTitle("Get ride")
                        .setSubtitle(headerSubtitle)
                        .setSummarySubtitle("Ride to work in 12 min | Ride home in 1 hour 45 min")
                        .setPrimaryAction(primaryAction))
                .addRow(new ListBuilder.RowBuilder(lb)
                        .setTitle("Work")
                        .setSubtitle(workSubtitle)
                        .addEndItem(new SliceAction(getBroadcastIntent(ACTION_TOAST, "work"),
                                Icon.createWithResource(getContext(), R.drawable.ic_work),
                                "Get ride work")))
                .addRow(new ListBuilder.RowBuilder(lb)
                        .setTitle("Home")
                        .setSubtitle(homeSubtitle)
                        .addEndItem(new SliceAction(getBroadcastIntent(ACTION_TOAST, "home"),
                                Icon.createWithResource(getContext(), R.drawable.ic_home),
                                "Get ride home")))
                .build();
    }

    private Slice createCustomToggleSlice(Uri sliceUri) {
        ListBuilder b = new ListBuilder(getContext(), sliceUri);
        return b.setColor(0xffff4081)
                .addRow(new ListBuilder.RowBuilder(b)
                    .setTitle("Custom toggle")
                    .setSubtitle("It can support two states")
                    .addEndItem(new SliceAction(getBroadcastIntent(ACTION_TOAST, "star toggled"),
                            Icon.createWithResource(getContext(), R.drawable.toggle_star),
                            "Toggle star", true /* isChecked */)))
                .build();
    }

    private Slice createTwoCustomToggleSlices(Uri sliceUri) {
        ListBuilder lb = new ListBuilder(getContext(), sliceUri);
        return lb.setColor(0xffff4081)
                .addRow(new ListBuilder.RowBuilder(lb)
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

    private Slice createWifiSlice(Uri sliceUri, boolean customSeeMore) {
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
        ListBuilder lb = new ListBuilder(getContext(), sliceUri);
        SliceAction primaryAction = new SliceAction(getIntent(Settings.ACTION_WIFI_SETTINGS),
                Icon.createWithResource(getContext(), R.drawable.ic_wifi), "Wi-fi Settings");
        lb.setColor(0xff4285f4);
        lb.addRow(new ListBuilder.RowBuilder(lb)
                .setTitle("Wi-fi")
                .setTitleItem(Icon.createWithResource(getContext(), R.drawable.ic_wifi), ICON_IMAGE)
                .setSubtitle(state)
                .addEndItem(new SliceAction(getBroadcastIntent(ACTION_WIFI_CHANGED, null),
                        "Toggle wifi", finalWifiEnabled))
                .setPrimaryAction(primaryAction));

        // Add keywords
        String[] keywords = new String[] {"internet", "wifi", "data", "network"};
        lb.setKeywords(Arrays.asList(keywords));

        // Add fake wifi networks
        int[] wifiIcons = new int[] {R.drawable.ic_wifi_full, R.drawable.ic_wifi_low,
                R.drawable.ic_wifi_fair};
        for (int i = 0; i < 10; i++) {
            final int iconId = wifiIcons[i % wifiIcons.length];
            Icon icon = Icon.createWithResource(getContext(), iconId);
            final String networkName = "Network" + i;
            ListBuilder.RowBuilder rb = new ListBuilder.RowBuilder(lb)
                    .setTitleItem(icon, ICON_IMAGE)
                    .setTitle("Network" + networkName);
            boolean locked = i % 3 == 0;
            if (locked) {
                rb.addEndItem(Icon.createWithResource(getContext(), R.drawable.ic_lock));
            }
            String message = locked ? "Open wifi password dialog" : "Connect to " + networkName;
            rb.setPrimaryAction(new SliceAction(getBroadcastIntent(ACTION_TOAST, message), icon,
                    message));
            lb.addRow(rb);
        }
        if (customSeeMore) {
            lb.addSeeMoreRow(new ListBuilder.RowBuilder(lb)
                    .setTitle("See all available networks")
                    .addEndItem(Icon.createWithResource(getContext(), R.drawable.ic_right_caret))
                    .setPrimaryAction(primaryAction));
        } else {
            lb.addSeeMoreAction(primaryAction.getAction());
        }
        return lb.build();
    }

    private Slice createReservationSlice(Uri sliceUri) {
        ListBuilder lb = new ListBuilder(getContext(), sliceUri);
        GridBuilder gb1 = new GridBuilder(lb);
        gb1.addCell(new GridBuilder.CellBuilder(gb1)
                .addImage(Icon.createWithResource(getContext(), R.drawable.reservation),
                        LARGE_IMAGE)
                .setContentDescription("Image of your reservation in Seattle"));
        GridBuilder gb2 = new GridBuilder(lb);
        gb2.addCell(new GridBuilder.CellBuilder(gb2)
                    .addTitleText("Check In")
                    .addText("12:00 PM, Feb 1"))
                .addCell(new GridBuilder.CellBuilder(gb2)
                    .addTitleText("Check Out")
                    .addText("11:00 AM, Feb 19"));
        return lb.setColor(0xffFF5252)
                .setHeader(new ListBuilder.HeaderBuilder(lb)
                        .setTitle("Upcoming trip to Seattle")
                        .setSubtitle("Feb 1 - 19 | 2 guests"))
                .addAction(new SliceAction(
                        getBroadcastIntent(ACTION_TOAST, "show location on map"),
                        Icon.createWithResource(getContext(), R.drawable.ic_location),
                        "Show reservation location"))
                .addAction(new SliceAction(getBroadcastIntent(ACTION_TOAST, "contact host"),
                        Icon.createWithResource(getContext(), R.drawable.ic_text),
                        "Contact host"))
                .addGrid(gb1)
                .addGrid(gb2)
                .build();
    }

    private PendingIntent getIntent(String action) {
        Intent intent = new Intent(action);
        intent.setClassName(getContext().getPackageName(), SliceRenderActivity.class.getName());
        PendingIntent pi = PendingIntent.getActivity(getContext(), 0, intent, 0);
        return pi;
    }

    private PendingIntent getBroadcastIntent(String action, String message) {
        Intent intent = new Intent(action);
        intent.setClassName(getContext().getPackageName(), SliceRenderActivity.class.getName());
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
