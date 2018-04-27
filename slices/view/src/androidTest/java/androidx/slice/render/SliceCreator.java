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
import static androidx.slice.builders.ListBuilder.INFINITY;
import static androidx.slice.builders.ListBuilder.LARGE_IMAGE;
import static androidx.slice.builders.ListBuilder.SMALL_IMAGE;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceProvider;
import androidx.slice.builders.GridRowBuilder;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.MessagingSliceBuilder;
import androidx.slice.builders.SliceAction;
import androidx.slice.view.test.R;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Examples of using slice template builders.
 */
public class SliceCreator {

    public static final String ACTION_WIFI_CHANGED =
            "com.example.androidx.slice.action.WIFI_CHANGED";
    public static final String ACTION_TOAST =
            "com.example.androidx.slice.action.TOAST";
    public static final String EXTRA_TOAST_MESSAGE = "com.example.androidx.extra.TOAST_MESSAGE";

    public static final String[] URI_PATHS = {
            "message",
            "wifi",
            "wifi2",
            "note",
            "ride",
            "ride-ttl",
            "toggle",
            "toggle2",
            "contact",
            "gallery",
            "subscription",
            "subscription2",
            "weather",
            "reservation",
            "inputrange",
            "inputrange2",
            "range",
            "permission",
            "empty",
    };

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
                return createRideSlice(sliceUri, false /* showUpdated */);
            case "/ride-ttl":
                return createRideSlice(sliceUri, true /* showUpdated */);
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
            case "/inputrange":
                return createStarRatingInputRange(sliceUri);
            case "/inputrange2":
                return createBasicInputRange(sliceUri);
            case "/range":
                return createDownloadProgressRange(sliceUri);
            case "/permission":
                return createPermissionSlice(sliceUri);
            case "/empty":
                return new ListBuilder(getContext(), sliceUri, INFINITY).build();
        }
        throw new IllegalArgumentException("Unknown uri " + sliceUri);
    }

    private Slice createWeather(Uri sliceUri) {
        SliceAction primaryAction = new SliceAction(getBroadcastIntent(ACTION_TOAST,
                "open weather app"),
                IconCompat.createWithResource(getContext(), R.drawable.weather_1), SMALL_IMAGE,
                "Weather is happening!");
        ListBuilder b = new ListBuilder(getContext(), sliceUri, -TimeUnit.HOURS.toMillis(8));
        GridRowBuilder gb = new GridRowBuilder(b);
        gb.setPrimaryAction(primaryAction);
        gb.addCell(new GridRowBuilder.CellBuilder(gb)
                .addImage(IconCompat.createWithResource(getContext(), R.drawable.weather_1),
                        SMALL_IMAGE)
                .addText("MON")
                .addTitleText("69\u00B0"))
                .addCell(new GridRowBuilder.CellBuilder(gb)
                        .addImage(IconCompat.createWithResource(getContext(), R.drawable.weather_2),
                                SMALL_IMAGE)
                        .addText("TUE")
                        .addTitleText("71\u00B0"))
                .addCell(new GridRowBuilder.CellBuilder(gb)
                        .addImage(IconCompat.createWithResource(getContext(), R.drawable.weather_3),
                                SMALL_IMAGE)
                        .addText("WED")
                        .addTitleText("76\u00B0"))
                .addCell(new GridRowBuilder.CellBuilder(gb)
                        .addImage(IconCompat.createWithResource(getContext(), R.drawable.weather_4),
                                SMALL_IMAGE)
                        .addText("THU")
                        .addTitleText("72\u00B0"))
                .addCell(new GridRowBuilder.CellBuilder(gb)
                        .addImage(IconCompat.createWithResource(getContext(), R.drawable.weather_1),
                                SMALL_IMAGE)
                        .addText("FRI")
                        .addTitleText("68\u00B0"));
        return b.addGridRow(gb).build();
    }

    private Slice createGallery(Uri sliceUri) {
        ListBuilder b = new ListBuilder(getContext(), sliceUri, INFINITY);
        GridRowBuilder gb = new GridRowBuilder(b);
        PendingIntent pi = getBroadcastIntent(ACTION_TOAST, "see more of your gallery");
        gb.setSeeMoreAction(pi);
        gb.addCell(new GridRowBuilder.CellBuilder(gb)
                .addImage(IconCompat.createWithResource(getContext(), R.drawable.slices_1),
                        LARGE_IMAGE))
                .addCell(new GridRowBuilder.CellBuilder(gb)
                        .addImage(IconCompat.createWithResource(getContext(), R.drawable.slices_2),
                                LARGE_IMAGE))
                .addCell(new GridRowBuilder.CellBuilder(gb)
                        .addImage(IconCompat.createWithResource(getContext(), R.drawable.slices_3),
                                LARGE_IMAGE))
                .addCell(new GridRowBuilder.CellBuilder(gb)
                        .addImage(IconCompat.createWithResource(getContext(), R.drawable.slices_4),
                                LARGE_IMAGE))
                .addCell(new GridRowBuilder.CellBuilder(gb)
                        .addImage(IconCompat.createWithResource(getContext(), R.drawable.slices_2),
                                LARGE_IMAGE))
                .addCell(new GridRowBuilder.CellBuilder(gb)
                        .addImage(IconCompat.createWithResource(getContext(), R.drawable.slices_3),
                                LARGE_IMAGE))
                .addCell(new GridRowBuilder.CellBuilder(gb)
                        .addImage(IconCompat.createWithResource(getContext(), R.drawable.slices_4),
                                LARGE_IMAGE));
        return b.addGridRow(gb).build();
    }

    private Slice createSubSlice(Uri sliceUri, boolean customSeeMore) {
        ListBuilder b = new ListBuilder(getContext(), sliceUri, INFINITY);
        GridRowBuilder gb = new GridRowBuilder(b);
        GridRowBuilder.CellBuilder cb = new GridRowBuilder.CellBuilder(gb);
        PendingIntent pi = getBroadcastIntent(ACTION_TOAST, "See cats you follow");
        if (customSeeMore) {
            cb.addImage(IconCompat.createWithResource(getContext(), R.drawable.ic_right_caret),
                    ICON_IMAGE);
            cb.setContentIntent(pi);
            cb.addText("All cats");
            gb.setSeeMoreCell(cb);
        } else {
            gb.setSeeMoreAction(pi);
        }
        gb.addCell(new GridRowBuilder.CellBuilder(gb)
                .addImage(IconCompat.createWithResource(getContext(), R.drawable.cat_1),
                        SMALL_IMAGE)
                .addTitleText("Oreo"))
                .addCell(new GridRowBuilder.CellBuilder(gb)
                        .addImage(IconCompat.createWithResource(getContext(), R.drawable.cat_2),
                                SMALL_IMAGE)
                        .addTitleText("Silver"))
                .addCell(new GridRowBuilder.CellBuilder(gb)
                        .addImage(IconCompat.createWithResource(getContext(), R.drawable.cat_3),
                                SMALL_IMAGE)
                        .addTitleText("Drake"))
                .addCell(new GridRowBuilder.CellBuilder(gb)
                        .addImage(IconCompat.createWithResource(getContext(), R.drawable.cat_5),
                                SMALL_IMAGE)
                        .addTitleText("Olive"))
                .addCell(new GridRowBuilder.CellBuilder(gb)
                        .addImage(IconCompat.createWithResource(getContext(), R.drawable.cat_4),
                                SMALL_IMAGE)
                        .addTitleText("Lady Marmalade"))
                .addCell(new GridRowBuilder.CellBuilder(gb)
                        .addImage(IconCompat.createWithResource(getContext(), R.drawable.cat_6),
                                SMALL_IMAGE)
                        .addTitleText("Grapefruit"));
        return b.addGridRow(gb).build();
    }

    private Slice createContact(Uri sliceUri) {
        ListBuilder b = new ListBuilder(getContext(), sliceUri, INFINITY);
        ListBuilder.RowBuilder rb = new ListBuilder.RowBuilder(b);
        SliceAction primaryAction = new SliceAction(getBroadcastIntent(ACTION_TOAST,
                "See contact info"), IconCompat.createWithResource(getContext(),
                R.drawable.mady), SMALL_IMAGE, "Mady");
        GridRowBuilder gb = new GridRowBuilder(b);
        return b.setAccentColor(0xff3949ab)
                .addRow(rb
                        .setTitle("Mady Pitza")
                        .setSubtitle("Frequently contacted contact")
                        .addEndItem(primaryAction))
                .addGridRow(gb
                        .addCell(new GridRowBuilder.CellBuilder(gb)
                                .addImage(IconCompat.createWithResource(getContext(),
                                        R.drawable.ic_call),
                                        ICON_IMAGE)
                                .addText("Call")
                                .setContentIntent(getBroadcastIntent(ACTION_TOAST, "call")))
                        .addCell(new GridRowBuilder.CellBuilder(gb)
                                .addImage(IconCompat.createWithResource(getContext(),
                                        R.drawable.ic_text),
                                        ICON_IMAGE)
                                .addText("Text")
                                .setContentIntent(getBroadcastIntent(ACTION_TOAST, "text")))
                        .addCell(new GridRowBuilder.CellBuilder(gb)
                                .addImage(IconCompat.createWithResource(getContext(),
                                        R.drawable.ic_video),
                                        ICON_IMAGE)
                                .setContentIntent(getBroadcastIntent(ACTION_TOAST, "video"))
                                .addText("Video"))
                        .addCell(new GridRowBuilder.CellBuilder(gb)
                                .addImage(IconCompat.createWithResource(getContext(),
                                        R.drawable.ic_email),
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
                        .addSource(IconCompat.createWithResource(getContext(), R.drawable.mady)))
                .add(new MessagingSliceBuilder.MessageBuilder(mb)
                        .addText("just bought my tickets")
                        .addTimestamp(System.currentTimeMillis() - 10 * DateUtils.MINUTE_IN_MILLIS))
                .add(new MessagingSliceBuilder.MessageBuilder(mb)
                        .addText("yay! can't wait for getContext() weekend!\n"
                                + "\uD83D\uDE00")
                        .addTimestamp(System.currentTimeMillis() - 5 * DateUtils.MINUTE_IN_MILLIS)
                        .addSource(IconCompat.createWithResource(getContext(), R.drawable.mady)))
                .build();

    }

    private Slice createNoteSlice(Uri sliceUri) {
        // TODO: Remote input.
        ListBuilder lb = new ListBuilder(getContext(), sliceUri, INFINITY);
        return lb.setAccentColor(0xfff4b400)
                .addRow(new ListBuilder.RowBuilder(lb)
                        .setTitle("Create new note")
                        .setSubtitle("with this note taking app")
                        .addEndItem(new SliceAction(getBroadcastIntent(ACTION_TOAST, "create note"),
                                IconCompat.createWithResource(getContext(), R.drawable.ic_create),
                                "Create note"))
                        .addEndItem(new SliceAction(getBroadcastIntent(ACTION_TOAST, "voice note"),
                                IconCompat.createWithResource(getContext(), R.drawable.ic_voice),
                                "Voice note"))
                        .addEndItem(new SliceAction(getIntent("android.media.action.IMAGE_CAPTURE"),
                                IconCompat.createWithResource(getContext(), R.drawable.ic_camera),
                                "Photo note")))
                .build();
    }

    private Slice createRideSlice(Uri sliceUri, boolean showUpdated) {
        final ForegroundColorSpan colorSpan = new ForegroundColorSpan(0xff0F9D58);
        SpannableString headerSubtitle = new SpannableString("Ride in 4 min");
        headerSubtitle.setSpan(colorSpan, 8, headerSubtitle.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString homeSubtitle = new SpannableString("12 miles | 12 min | $9.00");
        homeSubtitle.setSpan(colorSpan, 20, homeSubtitle.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString workSubtitle = new SpannableString("44 miles | 1 hour 45 min | $31.41");
        workSubtitle.setSpan(colorSpan, 27, workSubtitle.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
        SliceAction primaryAction = new SliceAction(getBroadcastIntent(ACTION_TOAST, "get ride"),
                IconCompat.createWithResource(getContext(), R.drawable.ic_car), "Get Ride");
        Long ttl = showUpdated
                ? -TimeUnit.MINUTES.toMillis(2) // negative for testing
                : INFINITY;
        ListBuilder lb = new ListBuilder(getContext(), sliceUri, ttl);
        return lb.setAccentColor(0xff0F9D58)
                .setHeader(new ListBuilder.HeaderBuilder(lb)
                        .setTitle("Get ride")
                        .setSubtitle(headerSubtitle)
                        .setSummary("Ride to work in 12 min | Ride home in 1 hour 45 min")
                        .setPrimaryAction(primaryAction))
                .addRow(new ListBuilder.RowBuilder(lb)
                        .setTitle("Work")
                        .setSubtitle(workSubtitle)
                        .addEndItem(new SliceAction(getBroadcastIntent(ACTION_TOAST, "work"),
                                IconCompat.createWithResource(getContext(), R.drawable.ic_work),
                                "Get ride work")))
                .addRow(new ListBuilder.RowBuilder(lb)
                        .setTitle("Home")
                        .setSubtitle(homeSubtitle)
                        .addEndItem(new SliceAction(getBroadcastIntent(ACTION_TOAST, "home"),
                                IconCompat.createWithResource(getContext(), R.drawable.ic_home),
                                "Get ride home")))
                .build();
    }

    private Slice createCustomToggleSlice(Uri sliceUri) {
        ListBuilder b = new ListBuilder(getContext(), sliceUri, -TimeUnit.HOURS.toMillis(1));
        return b.setAccentColor(0xffff4081)
                .addRow(new ListBuilder.RowBuilder(b)
                        .setTitle("Custom toggle")
                        .addEndItem(
                                new SliceAction(getBroadcastIntent(ACTION_TOAST, "star toggled"),
                                        IconCompat.createWithResource(getContext(),
                                                R.drawable.toggle_star),
                                        "Toggle star", true /* isChecked */)))
                .build();
    }

    private Slice createTwoCustomToggleSlices(Uri sliceUri) {
        ListBuilder lb = new ListBuilder(getContext(), sliceUri, INFINITY);
        return lb.setAccentColor(0xffff4081)
                .addRow(new ListBuilder.RowBuilder(lb)
                        .setTitle("2 toggles")
                        .setSubtitle("each supports two states")
                        .addEndItem(new SliceAction(
                                getBroadcastIntent(ACTION_TOAST, "first star toggled"),
                                IconCompat.createWithResource(getContext(), R.drawable.toggle_star),
                                "Toggle star", true /* isChecked */))
                        .addEndItem(new SliceAction(
                                getBroadcastIntent(ACTION_TOAST, "second star toggled"),
                                IconCompat.createWithResource(getContext(), R.drawable.toggle_star),
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
        ListBuilder lb = new ListBuilder(getContext(), sliceUri, INFINITY);
        SliceAction primaryAction = new SliceAction(getIntent(Settings.ACTION_WIFI_SETTINGS),
                IconCompat.createWithResource(getContext(), R.drawable.ic_wifi), "Wi-fi Settings");
        lb.setAccentColor(0xff4285f4);
        lb.addRow(new ListBuilder.RowBuilder(lb)
                .setTitle("Wi-fi")
                .setTitleItem(IconCompat.createWithResource(getContext(), R.drawable.ic_wifi),
                        ICON_IMAGE)
                .setSubtitle(state)
                .addEndItem(new SliceAction(getBroadcastIntent(ACTION_WIFI_CHANGED, null),
                        "Toggle wifi", finalWifiEnabled))
                .setPrimaryAction(primaryAction));

        // Add keywords
        String[] keywords = new String[]{"internet", "wifi", "data", "network"};
        lb.setKeywords(Arrays.asList(keywords));

        // Add fake wifi networks
        int[] wifiIcons = new int[]{R.drawable.ic_wifi_full, R.drawable.ic_wifi_low,
                R.drawable.ic_wifi_fair};
        for (int i = 0; i < 10; i++) {
            final int iconId = wifiIcons[i % wifiIcons.length];
            IconCompat icon = IconCompat.createWithResource(getContext(), iconId);
            final String networkName = "Network" + i;
            ListBuilder.RowBuilder rb = new ListBuilder.RowBuilder(lb)
                    .setTitleItem(icon, ICON_IMAGE)
                    .setTitle("Network" + networkName);
            boolean locked = i % 3 == 0;
            if (locked) {
                rb.addEndItem(IconCompat.createWithResource(getContext(), R.drawable.ic_lock),
                        ICON_IMAGE);
            }
            String message = locked ? "Open wifi password dialog" : "Connect to " + networkName;
            rb.setPrimaryAction(new SliceAction(getBroadcastIntent(ACTION_TOAST, message), icon,
                    message));
            lb.addRow(rb);
        }
        if (customSeeMore) {
            lb.setSeeMoreRow(new ListBuilder.RowBuilder(lb)
                    .setTitle("See all available networks")
                    .addEndItem(
                            IconCompat.createWithResource(getContext(), R.drawable.ic_right_caret),
                            ICON_IMAGE)
                    .setPrimaryAction(primaryAction));
        } else {
            lb.setSeeMoreAction(primaryAction.getAction());
        }
        return lb.build();
    }

    private Slice createReservationSlice(Uri sliceUri) {
        ListBuilder lb = new ListBuilder(getContext(), sliceUri, INFINITY);
        GridRowBuilder gb1 = new GridRowBuilder(lb);
        gb1.addCell(new GridRowBuilder.CellBuilder(gb1)
                .addImage(IconCompat.createWithResource(getContext(), R.drawable.reservation),
                        LARGE_IMAGE)
                .setContentDescription("Image of your reservation in Seattle"));
        GridRowBuilder gb2 = new GridRowBuilder(lb);
        gb2.addCell(new GridRowBuilder.CellBuilder(gb2)
                .addTitleText("Check In")
                .addText("12:00 PM, Feb 1"))
                .addCell(new GridRowBuilder.CellBuilder(gb2)
                        .addTitleText("Check Out")
                        .addText("11:00 AM, Feb 19"));
        return lb.setAccentColor(0xffFF5252)
                .setHeader(new ListBuilder.HeaderBuilder(lb)
                        .setTitle("Upcoming trip to Seattle")
                        .setSubtitle("Feb 1 - 19 | 2 guests"))
                .addAction(new SliceAction(
                        getBroadcastIntent(ACTION_TOAST, "show location on map"),
                        IconCompat.createWithResource(getContext(), R.drawable.ic_location),
                        "Show reservation location"))
                .addAction(new SliceAction(getBroadcastIntent(ACTION_TOAST, "contact host"),
                        IconCompat.createWithResource(getContext(), R.drawable.ic_text),
                        "Contact host"))
                .addGridRow(gb1)
                .addGridRow(gb2)
                .build();
    }

    private Slice createBasicInputRange(Uri sliceUri) {
        IconCompat icon = IconCompat.createWithResource(getContext(), R.drawable.ic_star_on);
        SliceAction primaryAction =
                new SliceAction(getBroadcastIntent(ACTION_TOAST, "open star rating"),
                        icon, "Rate");
        ListBuilder lb = new ListBuilder(getContext(), sliceUri, INFINITY);
        return lb.setAccentColor(0xff4285f4)
                .addInputRange(new ListBuilder.InputRangeBuilder(lb)
                        .setTitle("Alarm volume")
                        .setSubtitle("Adjust your volume")
                        .setInputAction(getBroadcastIntent(ACTION_TOAST, "volume changed"))
                        .setValue(80)
                        .setPrimaryAction(primaryAction)
                        .setContentDescription("Slider for alarm volume"))
                .build();
    }

    private Slice createStarRatingInputRange(Uri sliceUri) {
        IconCompat icon = IconCompat.createWithResource(getContext(), R.drawable.ic_star_on);
        SliceAction primaryAction =
                new SliceAction(getBroadcastIntent(ACTION_TOAST, "open star rating"), icon,
                        "Rate");
        ListBuilder lb = new ListBuilder(getContext(), sliceUri, INFINITY);
        return lb.setAccentColor(0xffff4081)
                .addInputRange(new ListBuilder.InputRangeBuilder(lb)
                        .setTitle("Star rating")
                        .setSubtitle("Pick a rating from 0 to 5")
                        .setThumb(icon)
                        .setMin(5)
                        .setInputAction(getBroadcastIntent(ACTION_TOAST, "range changed"))
                        .setMax(10)
                        .setValue(7)
                        .setPrimaryAction(primaryAction)
                        .setContentDescription("Slider for star ratings"))
                .build();
    }

    private Slice createDownloadProgressRange(Uri sliceUri) {
        IconCompat icon = IconCompat.createWithResource(getContext(), R.drawable.ic_star_on);
        SliceAction primaryAction =
                new SliceAction(
                        getBroadcastIntent(ACTION_TOAST, "open download"), icon,
                        "Download");
        ListBuilder lb = new ListBuilder(getContext(), sliceUri, INFINITY);
        return lb.setAccentColor(0xffff4081)
                .addRange(new ListBuilder.RangeBuilder(lb)
                        .setTitle("Download progress")
                        .setSubtitle("Download is happening")
                        .setMax(100)
                        .setValue(75)
                        .setPrimaryAction(primaryAction))
                .build();
    }

    private Slice createPermissionSlice(Uri uri) {
        return SliceProvider.createPermissionSlice(getContext(), uri,
                getContext().getPackageName());
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
