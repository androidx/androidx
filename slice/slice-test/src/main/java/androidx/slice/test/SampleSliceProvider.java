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

package androidx.slice.test;

import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;

import static androidx.slice.builders.ListBuilder.ICON_IMAGE;
import static androidx.slice.builders.ListBuilder.INFINITY;
import static androidx.slice.builders.ListBuilder.LARGE_IMAGE;
import static androidx.slice.builders.ListBuilder.RAW_IMAGE_LARGE;
import static androidx.slice.builders.ListBuilder.RAW_IMAGE_SMALL;
import static androidx.slice.builders.ListBuilder.SMALL_IMAGE;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.collection.ArraySet;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceProvider;
import androidx.slice.builders.GridRowBuilder;
import androidx.slice.builders.GridRowBuilder.CellBuilder;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.ListBuilder.HeaderBuilder;
import androidx.slice.builders.ListBuilder.InputRangeBuilder;
import androidx.slice.builders.ListBuilder.RangeBuilder;
import androidx.slice.builders.ListBuilder.RatingBuilder;
import androidx.slice.builders.ListBuilder.RowBuilder;
import androidx.slice.builders.MessagingSliceBuilder;
import androidx.slice.builders.SelectionBuilder;
import androidx.slice.builders.SliceAction;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Examples of using slice template builders.
 */
public class SampleSliceProvider extends SliceProvider {

    private static final String TAG = "SampleSliceProvider";

    private static final boolean TEST_CUSTOM_SEE_MORE = false;

    public static final String ACTION_WIFI_CHANGED =
            "com.example.androidx.slice.action.WIFI_CHANGED";
    public static final String ACTION_TOAST =
            "com.example.androidx.slice.action.TOAST";
    public static final String EXTRA_TOAST_MESSAGE = "com.example.androidx.extra.TOAST_MESSAGE";
    public static final String ACTION_TOAST_RANGE_VALUE =
            "com.example.androidx.slice.action.TOAST_RANGE_VALUE";
    public static final String ACTION_TOAST_DATE_VALUE =
            "com.example.androidx.slice.action.TOAST_DATE_VALUE";
    public static final String ACTION_TOAST_TIME_VALUE =
            "com.example.androidx.slice.action.TOAST_TIME_VALUE";
    public static final String ACTION_PLAY_TTS = "com.example.androidx.slice.action.PLAY_TTS";
    public static int STAR_RATING = 0;
    public static long DATE_MILLIS_VALUE = System.currentTimeMillis();
    public static long TIME_MILLIS_VALUE = System.currentTimeMillis();

    public static final String[] URI_PATHS = {
            "message",
            "wifi",
            "note",
            "grocery",
            "ride",
            "toggle",
            "toggle2",
            "toggletester",
            "contact",
            "contact2",
            "contact3",
            "contact4",
            "singlepicker",
            "picker",
            "gallery",
            "galleryimagesizemovie",
            "galleryimagesizethumbnail",
            "galleryoverlay",
            "indeterminaterange",
            "indeterminaterange2",
            "indeterminaterange3",
            "weather",
            "weatheroverlay",
            "reservation",
            "loadlist",
            "loadgrid",
            "starrating",
            "starrating2",
            "inputrange",
            "richinputrange",
            "range",
            "subscription",
            "singleitems",
            "error",
            "translate",
            "rtlgrid",
            "slices",
            "cat",
            "longtext",
            "loading",
            "selection",
            "notification",
            "tts",
            "textbutton",
            "gridrowsliceaction"
    };

    @SuppressWarnings("deprecation")
    private final Handler mHandler = new Handler();
    private final SparseArray<String> mListSummaries = new SparseArray<>();
    private final SparseArray<String> mGridSummaries = new SparseArray<>();
    long mListLastUpdate;
    long mGridLastUpdate;

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
    public Uri onMapIntentToUri(@NonNull Intent intent) {
        return getUri("wifi", getContext());
    }

    @Override
    public Slice onBindSlice(@NonNull Uri sliceUri) {
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
            case "/longtext":
                return createLongText(sliceUri);
            case "/message":
                return createMessagingSlice(sliceUri);
            case "/wifi":
                return createWifiSlice(sliceUri);
            case "/note":
                return createNoteSlice(sliceUri);
            case "/grocery":
                return createInteractiveNote(sliceUri);
            case "/indeterminaterange":
                return createIndeterminateProgressRange(sliceUri);
            case "/indeterminaterange2":
                return createIndeterminateProgressRange2(sliceUri);
            case "/indeterminaterange3":
                return createIndeterminateProgressRange3(sliceUri);
            case "/ride":
                return createRideSlice(sliceUri);
            case "/toggle":
                return createCustomToggleSlice(sliceUri);
            case "/toggle2":
                return createTwoCustomToggleSlices(sliceUri);
            case "/toggletester":
                return createToggleTesterSlice(sliceUri);
            case "/contact":
                return createContact(sliceUri);
            case "/contact2":
                return createContact2(sliceUri);
            case "/contact3":
                return createContact3(sliceUri);
            case "/contact4":
                return createContact4(sliceUri);
            case "/singlepicker":
                return createSinglePicker(sliceUri);
            case "/picker":
                return createPicker(sliceUri);
            case "/gallery":
                return createGallery(sliceUri);
            case "/galleryimagesizemovie":
                return createGalleryImageSizeMovie(sliceUri);
            case "/galleryimagesizethumbnail":
                return createGalleryImageSizeThumbnails(sliceUri);
            case "/galleryoverlay":
                return createGalleryOverlay(sliceUri);
            case "/weather":
                return createWeather(sliceUri);
            case "/weatheroverlay":
                return createWeatherOverlay(sliceUri);
            case "/reservation":
                return createReservationSlice(sliceUri);
            case "/loadlist":
                return createLoadingListSlice(sliceUri);
            case "/loadgrid":
                return createLoadingGridSlice(sliceUri);
            case "/starrating":
                return createStarRating(sliceUri);
            case "/starrating2":
                return createStarRating2(sliceUri);
            case "/inputrange":
                return createInputRange(sliceUri);
            case "/richinputrange":
                return createRichInputRange(sliceUri);
            case "/range":
                return createDownloadProgressRange(sliceUri);
            case "/subscription":
                return createCatSlice(sliceUri, false /* customSeeMore */);
            case "/singleitems":
                return createSingleSlice(sliceUri);
            case "/error":
                return createErrorSlice(sliceUri);
            case "/translate":
                return createTranslationSlice(sliceUri);
            case "/rtlgrid":
                return createRtlGridSlice(sliceUri);
            case "/slices":
                return createFoodOptionsSlice(sliceUri);
            case "/cat":
                return createBigPicSlice(sliceUri);
            case "/loading":
                return createLoadingSlice(sliceUri);
            case "/selection":
                return createSelectionSlice(sliceUri);
            case "/notification":
                return createNotificationSlice(sliceUri);
            case "/tts":
                return createTtsSlice(sliceUri);
            case "/textbutton":
                return createTextButtonSlice(sliceUri);
            case "/gridrowsliceaction":
                return createGridRowSliceAction(sliceUri);
        }
        Log.w(TAG, String.format("Unknown uri: %s", sliceUri));
        return null;
    }

    private Slice createWeather(Uri sliceUri) {
        SliceAction primaryAction = SliceAction.create(getBroadcastIntent(ACTION_TOAST,
                "open weather app"),
                IconCompat.createWithResource(getContext(), R.drawable.weather_1), SMALL_IMAGE,
                "Weather is happening!");
        ListBuilder lb = new ListBuilder(getContext(), sliceUri, INFINITY);
        lb.setHeader(new HeaderBuilder()
                .setTitle("Mountain View Weather")
                .setSubtitle("High 69\u00B0, Low 62\u00B0")
                .setPrimaryAction(primaryAction));
        return lb.addGridRow(new GridRowBuilder()
                .addCell(new CellBuilder()
                        .addImage(IconCompat.createWithResource(getContext(),
                                R.drawable.weather_1),
                                SMALL_IMAGE)
                        .addText("MON")
                        .addTitleText("69\u00B0"))
                .addCell(new CellBuilder()
                        .addImage(IconCompat.createWithResource(getContext(),
                                R.drawable.weather_2),
                                SMALL_IMAGE)
                        .addText("TUE")
                        .addTitleText("71\u00B0"))
                .addCell(new CellBuilder()
                        .addImage(IconCompat.createWithResource(getContext(),
                                R.drawable.weather_3),
                                SMALL_IMAGE)
                        .addText("WED")
                        .addTitleText("76\u00B0"))
                .addCell(new CellBuilder()
                        .addImage(IconCompat.createWithResource(getContext(),
                                R.drawable.weather_4),
                                SMALL_IMAGE)
                        .addText("THU")
                        .addTitleText("72\u00B0"))
                .addCell(new CellBuilder()
                        .addImage(IconCompat.createWithResource(getContext(),
                                R.drawable.weather_1),
                                SMALL_IMAGE)
                        .addText("FRI")
                        .addTitleText("68\u00B0")))
                .build();
    }

    private Slice createWeatherOverlay(Uri sliceUri) {
        SliceAction primaryAction = SliceAction.create(getBroadcastIntent(ACTION_TOAST,
                "open weather app"),
                IconCompat.createWithResource(getContext(), R.drawable.weather_1), SMALL_IMAGE,
                "Weather is happening!");
        ListBuilder lb = new ListBuilder(getContext(), sliceUri, INFINITY);
        lb.setHeader(new HeaderBuilder()
                .setTitle("Mountain View Weather")
                .setSubtitle("High 69\u00B0, Low 62\u00B0")
                .setPrimaryAction(primaryAction));
        return lb.addGridRow(new GridRowBuilder()
                .addCell(new CellBuilder()
                        .addImage(IconCompat.createWithResource(getContext(),
                                R.drawable.weather_1),
                                LARGE_IMAGE)
                        .addOverlayText("MON 69\u00B0"))
                .addCell(new CellBuilder()
                        .addImage(IconCompat.createWithResource(getContext(),
                                R.drawable.weather_2),
                                SMALL_IMAGE)
                        .addOverlayText("TUE 71\u00B0"))
                .addCell(new CellBuilder()
                        .addImage(IconCompat.createWithResource(getContext(),
                                R.drawable.weather_3),
                                LARGE_IMAGE))
                .addCell(new CellBuilder()
                        .addImage(IconCompat.createWithResource(getContext(),
                                R.drawable.weather_4),
                                SMALL_IMAGE))
                .addCell(new CellBuilder()
                        .addImage(IconCompat.createWithResource(getContext(),
                                R.drawable.weather_1),
                                LARGE_IMAGE)
                        .addOverlayText("FRI 68\u00B0")))
                .build();
    }

    private Slice createSinglePicker(Uri sliceUri) {
        ListBuilder lb = new ListBuilder(getContext(), sliceUri, INFINITY);
        lb.addRow(new RowBuilder()
                .setTitle("Title:" + DateFormat.getTimeInstance(DateFormat.SHORT,
                        Locale.ENGLISH).format(new Date(TIME_MILLIS_VALUE)))
                .setPrimaryAction(SliceAction.createTimePicker(
                        getBroadcastIntent(ACTION_TOAST_TIME_VALUE, null),
                        "PA:" + DateFormat.getTimeInstance(DateFormat.SHORT,
                                Locale.ENGLISH).format(new Date(TIME_MILLIS_VALUE)),
                        TIME_MILLIS_VALUE
                )));
        return lb.build();
    }

    private Slice createPicker(Uri sliceUri) {
        SliceAction primaryAction = SliceAction.create(
                getBroadcastIntent(ACTION_TOAST, "set picked"),
                IconCompat.createWithResource(getContext(), R.drawable.slices_1),
                LARGE_IMAGE,
                "set picked");
        ListBuilder lb = new ListBuilder(getContext(), sliceUri, INFINITY);
        lb.addRow(new RowBuilder()
                .setTitle("Date and Time Picker")
                .setPrimaryAction(primaryAction))
                .addGridRow(new GridRowBuilder()
                        .addCell(new CellBuilder().addTitleText("Date Picker").setSliceAction(
                                SliceAction.createDatePicker(
                                        getBroadcastIntent(ACTION_TOAST_DATE_VALUE, null),
                                        DateFormat.getDateInstance(DateFormat.FULL,
                                                Locale.ENGLISH).format(new Date(DATE_MILLIS_VALUE)),
                                        DATE_MILLIS_VALUE
                                ))
                        )
                        .addCell(new CellBuilder().addTitleText("Time Picker").setSliceAction(
                                SliceAction.createTimePicker(
                                        getBroadcastIntent(ACTION_TOAST_TIME_VALUE, null),
                                        DateFormat.getTimeInstance(DateFormat.SHORT,
                                                Locale.ENGLISH).format(new Date(TIME_MILLIS_VALUE)),
                                        TIME_MILLIS_VALUE
                                ))
                        ));
        return lb.build();
    }

    private Slice createGridRowSliceAction(Uri sliceUri) {
        SliceAction primaryAction = SliceAction.create(
                getBroadcastIntent(ACTION_TOAST, "PrimaryAction"),
                IconCompat.createWithResource(getContext(), R.drawable.slices_1),
                LARGE_IMAGE,
                "PrimaryAction");
        ListBuilder lb = new ListBuilder(getContext(), sliceUri, INFINITY);
        lb.addRow(new RowBuilder()
                .setTitle("Grid Row SliceAction Example")
                .setPrimaryAction(primaryAction))
                .addGridRow(new GridRowBuilder()
                        .addCell(new CellBuilder()
                                .addImage(IconCompat.createWithResource(getContext(),
                                        R.drawable.weather_1), SMALL_IMAGE)
                                .addText("pendingIntent")
                                .addTitleText("from ContentIntent")
                                .setContentIntent(getBroadcastIntent(ACTION_TOAST, "ContentIntent"))
                        )
                        .addCell(new CellBuilder()
                                .addImage(IconCompat.createWithResource(getContext(),
                                        R.drawable.weather_1), SMALL_IMAGE)
                                .addText("pendingIntent")
                                .addTitleText("from SliceAction")
                                .setSliceAction(
                                    SliceAction.create(
                                            getBroadcastIntent(ACTION_TOAST, "SliceAction"),
                                            IconCompat.createWithResource(getContext(),
                                                    R.drawable.weather_1),
                                            SMALL_IMAGE, "SliceAction"
                                ))
                        )
                )
                .addGridRow(new GridRowBuilder()
                        .addCell(new CellBuilder()
                                .addTitleText("Toggle 1")
                                .setSliceAction(SliceAction.createToggle(
                                        getBroadcastIntent(ACTION_TOAST, "Toggled 1"),
                                        "Toggle Title", true /* isChecked */)))
                        .addCell(new CellBuilder()
                                .addTitleText("Toggle 2")
                                .setSliceAction(SliceAction.createToggle(
                                        getBroadcastIntent(ACTION_TOAST, "Toggled 2"),
                                        "Toggle Title", false /* isChecked */)))
                        .addCell(new CellBuilder().addTitleText("Time Picker").setSliceAction(
                                SliceAction.createTimePicker(
                                        getBroadcastIntent(ACTION_TOAST_TIME_VALUE, null),
                                        DateFormat.getTimeInstance(DateFormat.SHORT,
                                                Locale.ENGLISH).format(new Date(TIME_MILLIS_VALUE)),
                                        TIME_MILLIS_VALUE
                                ))));
        return lb.build();
    }

    private Slice createGallery(Uri sliceUri) {
        SliceAction primaryAction = SliceAction.create(
                getBroadcastIntent(ACTION_TOAST, "open photo album"),
                IconCompat.createWithResource(getContext(), R.drawable.slices_1),
                LARGE_IMAGE,
                "Open photo album");
        ListBuilder lb = new ListBuilder(getContext(), sliceUri, INFINITY)
                .setAccentColor(0xff4285F4);
        lb.addRow(new RowBuilder()
                .setTitle("Family trip to Hawaii")
                .setSubtitle("Sep 30, 2017 - Oct 2, 2017")
                .setPrimaryAction(primaryAction))
                .addAction(SliceAction.create(
                        getBroadcastIntent(ACTION_TOAST, "cast photo album"),
                        IconCompat.createWithResource(getContext(), R.drawable.ic_cast), ICON_IMAGE,
                        "Cast photo album"))
                .addAction(SliceAction.create(
                        getBroadcastIntent(ACTION_TOAST, "share photo album"),
                        IconCompat.createWithResource(getContext(), R.drawable.ic_share),
                        ICON_IMAGE, "Share photo album"));
        int[] galleryResId = new int[] {R.drawable.slices_1, R.drawable.slices_2,
                R.drawable.slices_3, R.drawable.slices_4};
        int imageCount = 7;
        GridRowBuilder grb = new GridRowBuilder();
        for (int i = 0; i < imageCount; i++) {
            IconCompat ic = IconCompat.createWithResource(getContext(),
                    galleryResId[i % galleryResId.length]);
            grb.addCell(new CellBuilder().addImage(ic, LARGE_IMAGE));
        }
        grb.setPrimaryAction(primaryAction)
                .setSeeMoreAction(getBroadcastIntent(ACTION_TOAST, "see your gallery"))
                .setContentDescription("Images from your trip to Hawaii");
        return lb.addGridRow(grb).build();
    }

    private Slice createGalleryImageSizeMovie(Uri sliceUri) {
        SliceAction primaryAction = SliceAction.create(
                getBroadcastIntent(ACTION_TOAST, "open movie list"),
                IconCompat.createWithResource(getContext(), R.drawable.slices_1),
                LARGE_IMAGE,
                "Open movie list");
        ListBuilder lb = new ListBuilder(getContext(), sliceUri, INFINITY)
                .setAccentColor(0xff4285F4);
        lb.addRow(new RowBuilder()
                .setTitle("These movies near you")
                .setSubtitle("Top rated Movies")
                .setPrimaryAction(primaryAction))
                .addAction(SliceAction.create(
                        getBroadcastIntent(ACTION_TOAST, ""),
                        IconCompat.createWithResource(getContext(), R.drawable.ic_cast), ICON_IMAGE,
                        "Share movie list"));
        GridRowBuilder grb = new GridRowBuilder();
        grb.addCell(new CellBuilder().addImage(IconCompat.createWithResource(getContext(),
                R.drawable.portrait), RAW_IMAGE_LARGE)
                .addTitleText("MovieName the movie"));
        grb.addCell(new CellBuilder().addImage(IconCompat.createWithResource(getContext(),
                R.drawable.portrait), RAW_IMAGE_LARGE)
                .addTitleText("MovieName the movie2"));
        grb.addCell(new CellBuilder().addImage(IconCompat.createWithResource(getContext(),
                R.drawable.portrait), RAW_IMAGE_LARGE)
                .addTitleText("Amazing movie"));
        grb.addCell(new CellBuilder().addImage(IconCompat.createWithResource(getContext(),
                R.drawable.portrait), RAW_IMAGE_LARGE)
                .addTitleText("Cool movie").addText("The Sequel"));
        return lb.addGridRow(grb).build();
    }


    private Slice createGalleryImageSizeThumbnails(Uri sliceUri) {
        SliceAction primaryAction = SliceAction.create(
                getBroadcastIntent(ACTION_TOAST, "open video list"),
                IconCompat.createWithResource(getContext(), R.drawable.slices_1),
                LARGE_IMAGE,
                "Open video list");
        ListBuilder lb = new ListBuilder(getContext(), sliceUri, INFINITY)
                .setAccentColor(0xff4285F4);
        lb.addRow(new RowBuilder()
                .setTitle("Recommended videos")
                .setSubtitle("Top rated Videos")
                .setPrimaryAction(primaryAction))
                .addAction(SliceAction.create(
                        getBroadcastIntent(ACTION_TOAST, ""),
                        IconCompat.createWithResource(getContext(), R.drawable.ic_cast), ICON_IMAGE,
                        "Share videos"));
        GridRowBuilder grb = new GridRowBuilder();
        grb.addCell(new CellBuilder().addImage(IconCompat.createWithResource(getContext(),
                R.drawable.landscape), RAW_IMAGE_LARGE)
                .addTitleText("You won't believe this movie"));
        grb.addCell(new CellBuilder().addImage(IconCompat.createWithResource(getContext(),
                R.drawable.landscape), RAW_IMAGE_LARGE)
                .addTitleText("Amazing movie"));
        grb.addCell(new CellBuilder().addImage(IconCompat.createWithResource(getContext(),
                R.drawable.landscape), RAW_IMAGE_LARGE)
                .addTitleText("kittens wow"));
        return lb.addGridRow(grb).build();
    }

    private Slice createGalleryOverlay(Uri sliceUri) {
        SliceAction primaryAction = SliceAction.create(
                getBroadcastIntent(ACTION_TOAST, "open photo album"),
                IconCompat.createWithResource(getContext(), R.drawable.slices_1),
                LARGE_IMAGE,
                "Open photo album");
        ListBuilder lb = new ListBuilder(getContext(), sliceUri, INFINITY)
                .setAccentColor(0xff4285F4);
        lb.addRow(new RowBuilder()
                .setTitle("Family trip to Hawaii")
                .setSubtitle("Sep 30, 2017 - Oct 2, 2017")
                .setPrimaryAction(primaryAction))
                .addAction(SliceAction.create(
                        getBroadcastIntent(ACTION_TOAST, "cast photo album"),
                        IconCompat.createWithResource(getContext(), R.drawable.ic_cast), ICON_IMAGE,
                        "Cast photo album"))
                .addAction(SliceAction.create(
                        getBroadcastIntent(ACTION_TOAST, "share photo album"),
                        IconCompat.createWithResource(getContext(), R.drawable.ic_share),
                        ICON_IMAGE, "Share photo album"));
        int[] galleryResId = new int[]{R.drawable.slices_1, R.drawable.slices_2,
                R.drawable.slices_3, R.drawable.slices_4};
        int imageCount = 7;
        GridRowBuilder grb = new GridRowBuilder();
        for (int i = 0; i < imageCount; i++) {
            IconCompat ic = IconCompat.createWithResource(getContext(),
                    galleryResId[i % galleryResId.length]);
            grb.addCell(new CellBuilder().addImage(ic, LARGE_IMAGE).addOverlayText("image " + i));
        }
        grb.setPrimaryAction(primaryAction)
                .setSeeMoreAction(getBroadcastIntent(ACTION_TOAST, "see your gallery"))
                .setContentDescription("Images from your trip to Hawaii");
        return lb.addGridRow(grb).build();
    }

    private Slice createFoodOptionsSlice(Uri sliceUri) {
        int[] pizzaResId = new int[]{R.drawable.pizza3, R.drawable.pizza2, R.drawable.pizza1,
                R.drawable.pizza4};
        String[] titles = new String[]{"Sung's Pizza", "Slice of Life", "Ideal Triangles",
                "Meeting place"};
        String[] subtitles = new String[]{"5 stars", "5 stars", "4 stars",
                "4 stars"};

        int count = 4; // How many things show in the grid
        StringBuilder summary = new StringBuilder();
        String subtitle = count + " nearby restaurants";
        for (int i = 0; i < count; i++) {
            summary.append(titles[i]);
            if (i != count - 1) {
                summary.append(", ");
            }
        }
        SliceAction primaryAction = SliceAction.create(
                getBroadcastIntent(ACTION_TOAST, "open nearby pizza places"),
                IconCompat.createWithResource(getContext(), R.drawable.pizza3),
                LARGE_IMAGE,
                "Nearby Pizza");
        ListBuilder lb = new ListBuilder(getContext(), sliceUri, INFINITY)
                .setAccentColor(0xff4285F4);
        lb.setHeader(new HeaderBuilder().setTitle("Pizza near you")
                .setSubtitle(subtitle)
                .setSummary(summary.toString())
                .setPrimaryAction(primaryAction));
        GridRowBuilder grb = new GridRowBuilder();

        for (int i = 0; i < count; i++) {
            final int index = i;
            grb.addCell(new CellBuilder()
                    .addImage(IconCompat.createWithResource(
                            getContext(), pizzaResId[index]), LARGE_IMAGE)
                    .addTitleText(titles[index])
                    .addText(subtitles[index]));
        }
        lb.addGridRow(grb);
        return lb.build();
    }

    private Slice createBigPicSlice(Uri sliceUri) {
        ListBuilder b = new ListBuilder(getContext(), sliceUri, INFINITY);
        b.setHeader(new HeaderBuilder().setTitle("This is a nice cat"));
        GridRowBuilder gb = new GridRowBuilder();
        PendingIntent pi = getBroadcastIntent(ACTION_TOAST, "Cats you follow");
        IconCompat ic = IconCompat.createWithResource(getContext(), R.drawable.cat);
        SliceAction primaryAction = SliceAction.create(pi, ic, LARGE_IMAGE, "Cats you follow");
        gb.setPrimaryAction(primaryAction);
        gb.addCell(new GridRowBuilder.CellBuilder()
                .addImage(ic, RAW_IMAGE_LARGE));
        b.addGridRow(gb);
        return b.build();
    }

    private Slice createCatSlice(Uri sliceUri, boolean customSeeMore) {
        ListBuilder b = new ListBuilder(getContext(), sliceUri, INFINITY);
        GridRowBuilder gb = new GridRowBuilder();
        PendingIntent pi = getBroadcastIntent(ACTION_TOAST, "Cats you follow");
        SliceAction primaryAction = SliceAction.create(pi,
                IconCompat.createWithResource(getContext(), R.drawable.cat_1),
                SMALL_IMAGE, "Cats you follow");
        b.setHeader(new HeaderBuilder().setTitle("Cats you follow")
                .setPrimaryAction(primaryAction));
        if (customSeeMore) {
            GridRowBuilder.CellBuilder cb = new GridRowBuilder.CellBuilder();
            cb.addImage(IconCompat.createWithResource(getContext(), R.drawable.ic_right_caret),
                    ICON_IMAGE);
            cb.setContentIntent(pi);
            cb.addTitleText("All cats");
            gb.setSeeMoreCell(cb);
        } else {
            gb.setSeeMoreAction(pi);
        }
        gb.addCell(new GridRowBuilder.CellBuilder()
                .addImage(IconCompat.createWithResource(getContext(), R.drawable.cat_1),
                        SMALL_IMAGE)
                .addTitleText("Oreo"))
                .addCell(new GridRowBuilder.CellBuilder()
                        .addImage(IconCompat.createWithResource(getContext(), R.drawable.cat_2),
                                SMALL_IMAGE)
                        .addTitleText("Silver"))
                .addCell(new GridRowBuilder.CellBuilder()
                        .addImage(IconCompat.createWithResource(getContext(), R.drawable.cat_3),
                                SMALL_IMAGE)
                        .addTitleText("Drake"))
                .addCell(new GridRowBuilder.CellBuilder()
                        .addImage(IconCompat.createWithResource(getContext(), R.drawable.cat_5),
                                SMALL_IMAGE)
                        .addTitleText("Olive"))
                .addCell(new GridRowBuilder.CellBuilder()
                        .addImage(IconCompat.createWithResource(getContext(), R.drawable.cat_4),
                                SMALL_IMAGE)
                        .addTitleText("Lady Marmalade"))
                .addCell(new GridRowBuilder.CellBuilder()
                        .addImage(IconCompat.createWithResource(getContext(), R.drawable.cat_6),
                                SMALL_IMAGE)
                        .addTitleText("Grapefruit"));
        return b.addGridRow(gb).build();
    }

    private Slice createContact2(Uri sliceUri) {
        ListBuilder b = new ListBuilder(getContext(), sliceUri, INFINITY);
        ListBuilder.RowBuilder rb = new ListBuilder.RowBuilder();
        GridRowBuilder gb = new GridRowBuilder();
        IconCompat ic = IconCompat.createWithResource(getContext(), R.drawable.mady);
        SliceAction sliceAction = SliceAction.create(getBroadcastIntent(ACTION_TOAST,
                "View contact"), ic, SMALL_IMAGE, "View contact");
        return b.setAccentColor(0xff3949ab)
                .addRow(rb
                        .setTitle("Mady Pitza")
                        .setSubtitle("Frequently contacted contact")
                        .setPrimaryAction(sliceAction)
                        .addEndItem(ic, SMALL_IMAGE))
                .addGridRow(gb
                        .addCell(new GridRowBuilder.CellBuilder()
                                .addImage(IconCompat.createWithResource(getContext(),
                                        R.drawable.ic_call),
                                        ICON_IMAGE)
                                .addText("Call")
                                .setContentIntent(getBroadcastIntent(ACTION_TOAST, "call")))
                        .addCell(new GridRowBuilder.CellBuilder()
                                .addImage(IconCompat.createWithResource(getContext(),
                                        R.drawable.ic_text),
                                        ICON_IMAGE)
                                .addText("Text")
                                .setContentIntent(getBroadcastIntent(ACTION_TOAST, "text")))
                        .addCell(new GridRowBuilder.CellBuilder()
                                .addImage(IconCompat.createWithResource(getContext(),
                                        R.drawable.ic_video), ICON_IMAGE)
                                .setContentIntent(getBroadcastIntent(ACTION_TOAST, "video"))
                                .addText("Video"))
                        .addCell(new GridRowBuilder.CellBuilder()
                                .addImage(IconCompat.createWithResource(getContext(),
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
        SliceAction primaryAction = SliceAction.create(getBroadcastIntent(ACTION_TOAST,
                "See contact info"), IconCompat.createWithResource(getContext(),
                R.drawable.mady), SMALL_IMAGE, "Mady");

        return new ListBuilder(getContext(), sliceUri, INFINITY)
                .setAccentColor(0xff3949ab)
                .setHeader(new HeaderBuilder()
                        .setTitle("Mady Pitza")
                        .setSummary("Called " + lastCalledString)
                        .setPrimaryAction(primaryAction))
                .addRow(new RowBuilder()
                        .setTitleItem(
                                IconCompat.createWithResource(getContext(), R.drawable.ic_call),
                                ICON_IMAGE)
                        .setTitle("314-259-2653")
                        .setSubtitle("Call lasted 1 hr 17 min")
                        .addEndItem(lastCalled))
                .addRow(new RowBuilder()
                        .setTitleItem(
                                IconCompat.createWithResource(getContext(), R.drawable.ic_text),
                                ICON_IMAGE)
                        .setTitle("You: Coooooool see you then")
                        .addEndItem(System.currentTimeMillis() - 40 * DateUtils.MINUTE_IN_MILLIS))
                .addAction(SliceAction.create(getBroadcastIntent(ACTION_TOAST, "call"),
                        IconCompat.createWithResource(getContext(), R.drawable.ic_call),
                        ICON_IMAGE, "Call mady"))
                .addAction(SliceAction.create(getBroadcastIntent(ACTION_TOAST, "text"),
                        IconCompat.createWithResource(getContext(), R.drawable.ic_text),
                        ICON_IMAGE, "Text mady"))
                .addAction(SliceAction.create(getBroadcastIntent(ACTION_TOAST, "video"),
                        IconCompat.createWithResource(getContext(), R.drawable.ic_video),
                        ICON_IMAGE, "Video call mady"))
                .addAction(SliceAction.create(getBroadcastIntent(ACTION_TOAST, "email"),
                        IconCompat.createWithResource(getContext(), R.drawable.ic_email),
                        ICON_IMAGE, "Email mady"))
                .build();
    }

    private Slice createContact3(Uri sliceUri) {
        SliceAction sendEmail = SliceAction.create(getBroadcastIntent(ACTION_TOAST, "send email"),
                IconCompat.createWithResource(getContext(), R.drawable.ic_email), ICON_IMAGE,
                "send contact email");
        return new ListBuilder(getContext(), sliceUri, INFINITY).addRow(new RowBuilder()
                .setTitle("Mady")
                .setTitleItem(
                        IconCompat.createWithResource(getContext(), R.drawable.ic_call), ICON_IMAGE)
                .setPrimaryAction(sendEmail))
                .build();
    }

    private Slice createContact4(Uri sliceUri) {
        SliceAction sendEmail = SliceAction.create(getBroadcastIntent(ACTION_TOAST, "send email"),
                IconCompat.createWithResource(getContext(), R.drawable.ic_email), ICON_IMAGE,
                "send contact email");
        SliceAction sendNote = SliceAction.create(getBroadcastIntent(ACTION_TOAST, "send note"),
                IconCompat.createWithResource(getContext(), R.drawable.ic_note), ICON_IMAGE,
                "send contact note");
        return new ListBuilder(getContext(), sliceUri, INFINITY).addRow(new RowBuilder()
                .setTitle("Mady")
                .setTitleItem(
                        IconCompat.createWithResource(getContext(), R.drawable.ic_call), ICON_IMAGE)
                .addEndItem(sendNote, false)
                .setPrimaryAction(sendEmail))
                .build();
    }

    private Slice createLongText(Uri sliceUri) {
        ListBuilder lb = new ListBuilder(getContext(), sliceUri, INFINITY);
        SliceAction action = SliceAction.create(getBroadcastIntent(ACTION_TOAST, "Open story"),
                IconCompat.createWithResource(getContext(), R.drawable.pizza1), SMALL_IMAGE,
                "Best Pizza");
        lb.setHeader(new HeaderBuilder()
                .setTitle("Best new pizza places in San Francisco")
                .setSubtitle("June 3rd, 2019 - Pizza Review")
                .setPrimaryAction(action));
        lb.addRow(new RowBuilder()
                .setSubtitle("San Francisco doesn't have a lot of variety in terms of pizza, "
                        + "you're either getting something fancy and tasty or something awful, "
                        + "there are very few mid-tier pizza options."));
        return lb.build();
    }

    private Slice createMessagingSlice(Uri sliceUri) {
        // TODO: Remote input.
        MessagingSliceBuilder b = new MessagingSliceBuilder(getContext(), sliceUri);
        return b.add(new MessagingSliceBuilder.MessageBuilder(b)
                        .addText("yo home \uD83C\uDF55, I emailed you the info")
                        .addTimestamp(System.currentTimeMillis() - 20 * DateUtils.MINUTE_IN_MILLIS)
                        .addSource(IconCompat.createWithResource(getContext(), R.drawable.mady)))
                .add(new MessagingSliceBuilder.MessageBuilder(b)
                        .addText("just bought my tickets")
                        .addTimestamp(System.currentTimeMillis() - 10 * DateUtils.MINUTE_IN_MILLIS))
                .add(new MessagingSliceBuilder.MessageBuilder(b)
                        .addText("yay! can't wait for getContext() weekend!\n"
                                + "\uD83D\uDE00")
                        .addTimestamp(System.currentTimeMillis() - 5 * DateUtils.MINUTE_IN_MILLIS)
                        .addSource(IconCompat.createWithResource(getContext(), R.drawable.mady)))
                .build();

    }

    private Slice createNoteSlice(Uri sliceUri) {
        // TODO: Remote input.
        SliceAction createNote = SliceAction.create(getBroadcastIntent(ACTION_TOAST, "create note"),
                IconCompat.createWithResource(getContext(), R.drawable.ic_create), ICON_IMAGE,
                "Create note");
        return new ListBuilder(getContext(), sliceUri, INFINITY)
                .setAccentColor(0xfff4b400)
                .addRow(new RowBuilder()
                        .setTitle("Create new note")
                        .setPrimaryAction(createNote)
                .addEndItem(createNote)
                .addEndItem(SliceAction.create(getBroadcastIntent(ACTION_TOAST, "voice note"),
                        IconCompat.createWithResource(getContext(), R.drawable.ic_voice),
                        ICON_IMAGE, "Voice note"))
                .addEndItem(SliceAction.create(getIntent("android.media.action.IMAGE_CAPTURE"),
                        IconCompat.createWithResource(getContext(), R.drawable.ic_camera),
                        ICON_IMAGE, "Photo note")))
                .build();
    }

    public static ArrayList<String> sGroceryList = new ArrayList<>();
    public static final String ACTION_ITEM_CHECKED = "com.example.androidx.ACTION_ITEM_CHECKED";
    public static final String EXTRA_ITEM_INDEX = "com.example.androidx.extra.ITEM_INDEX";
    public static final String[] GROCERY_LIST = {"Mozzarella", "Tomatoes", "Garlic", "Parmesan",
            "Green olives", "Green peppers", "Pineapple"};

    private PendingIntent getGroceryIntent(int i) {
        Intent intent = new Intent(ACTION_ITEM_CHECKED);
        intent.setClass(getContext(), SliceBroadcastReceiver.class);
        intent.putExtra(EXTRA_ITEM_INDEX, i);
        return PendingIntent.getBroadcast(getContext(), i, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private Slice createInteractiveNote(Uri sliceUri) {
        if (sGroceryList.size() == 0) {
            sGroceryList.addAll(Arrays.asList(GROCERY_LIST));
        }
        ListBuilder lb = new ListBuilder(getContext(), sliceUri, INFINITY)
                .setAccentColor(0xfff4b400);
        SliceAction action = SliceAction.create(getBroadcastIntent(ACTION_TOAST,
                "Open grocery note"),
                IconCompat.createWithResource(getContext(), R.drawable.ic_note), ICON_IMAGE,
                "Grocery list");
        lb.setHeader(new HeaderBuilder().setTitle("Grocery list")
                .setSubtitle("Shared with 2 others")
                .setPrimaryAction(action));

        for (int i = 0; i < sGroceryList.size(); i++) {
            ListBuilder.RowBuilder rb = new ListBuilder.RowBuilder();
            rb.setTitle(sGroceryList.get(i));

            SliceAction checkBox = SliceAction.createToggle(getGroceryIntent(i),
                    IconCompat.createWithResource(getContext(), R.drawable.toggle_check),
                     "Check", false /* unchecked */);
            rb.setTitleItem(checkBox);
            lb.addRow(rb);
        }
        return lb.build();
    }

    private Slice createReservationSlice(Uri sliceUri) {
        SliceAction sliceAction = SliceAction.create(
                getBroadcastIntent(ACTION_TOAST, "View reservation"),
                IconCompat.createWithResource(getContext(),
                R.drawable.reservation), LARGE_IMAGE, "View reservation");
        return new ListBuilder(getContext(), sliceUri, INFINITY)
                .setAccentColor(0xffFF5252)
                .setHeader(new HeaderBuilder()
                        .setTitle("Upcoming trip to Seattle")
                        .setSubtitle("Feb 1 - 19 | 2 guests")
                        .setPrimaryAction(sliceAction))
                .addAction(SliceAction.create(
                        getBroadcastIntent(ACTION_TOAST, "show location on map"),
                        IconCompat.createWithResource(getContext(), R.drawable.ic_location),
                        ICON_IMAGE, "Show reservation location"))
                .addAction(SliceAction.create(getBroadcastIntent(ACTION_TOAST, "contact host"),
                        IconCompat.createWithResource(getContext(), R.drawable.ic_text), ICON_IMAGE,
                        "Contact host"))
                .addGridRow(new GridRowBuilder()
                        .addCell(new CellBuilder()
                                .addImage(IconCompat.createWithResource(getContext(),
                                        R.drawable.reservation),
                                        LARGE_IMAGE)
                                .setContentDescription("Image of your reservation in Seattle")))
                .addGridRow(new GridRowBuilder()
                        .addCell(new CellBuilder()
                                .addTitleText("Check In")
                                .addText("12:00 PM, Feb 1"))
                        .addCell(new CellBuilder()
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

        SliceAction primaryAction = SliceAction.create(getBroadcastIntent(ACTION_TOAST, "get ride"),
                IconCompat.createWithResource(getContext(), R.drawable.ic_car),
                ICON_IMAGE, "Get Ride");
        return new ListBuilder(getContext(), sliceUri, TimeUnit.SECONDS.toMillis(10))
                .setAccentColor(0xff0F9D58)
                .setHeader(new HeaderBuilder()
                        .setTitle("Get ride")
                        .setSubtitle(headerSubtitle)
                        .setSummary("Ride to work in 12 min | Ride home in 1 hour 45 min")
                        .setPrimaryAction(primaryAction))
                .addRow(new RowBuilder()
                        .setTitle("Work")
                        .setSubtitle(workSubtitle)
                        .addEndItem(SliceAction.create(getBroadcastIntent(ACTION_TOAST, "work"),
                                IconCompat.createWithResource(getContext(), R.drawable.ic_work),
                                ICON_IMAGE, "Get ride to work")))
                .addRow(new RowBuilder()
                        .setTitle("Home")
                        .setSubtitle(homeSubtitle)
                        .addEndItem(SliceAction.create(getBroadcastIntent(ACTION_TOAST, "home"),
                                IconCompat.createWithResource(getContext(), R.drawable.ic_home),
                                ICON_IMAGE, "Get ride home")))
                .build();
    }

    private Slice createCustomToggleSlice(Uri sliceUri) {
        return new ListBuilder(getContext(), sliceUri, INFINITY)
                .setAccentColor(0xffff4081)
                .addRow(new RowBuilder()
                        .setTitle("Custom toggle")
                        .setSubtitle("It can support two states")
                        .setPrimaryAction(SliceAction.createToggle(getBroadcastIntent(ACTION_TOAST,
                                "star toggled"),
                                IconCompat.createWithResource(getContext(), R.drawable.toggle_star),
                                "Toggle star", true /* isChecked */)))
                .build();
    }

    private Slice createTwoCustomToggleSlices(Uri sliceUri) {
        return new ListBuilder(getContext(), sliceUri, INFINITY)
                .setAccentColor(0xffff4081)
                .addRow(new RowBuilder()
                        .setTitle("2 toggles")
                        .setSubtitle("each supports two states")
                        .setPrimaryAction(SliceAction.create(
                                getBroadcastIntent(ACTION_TOAST, "open toggle app"),
                                IconCompat.createWithResource(getContext(), R.drawable.ic_star_on),
                                ICON_IMAGE, "Toggles"))
                        .addEndItem(SliceAction.createToggle(
                                getBroadcastIntent(ACTION_TOAST, "first star toggled"),
                                IconCompat.createWithResource(getContext(), R.drawable.toggle_star),
                                "Toggle star", true /* isChecked */))
                        .addEndItem(SliceAction.createToggle(
                                getBroadcastIntent(ACTION_TOAST, "second star toggled"),
                                IconCompat.createWithResource(getContext(), R.drawable.toggle_star),
                                "Toggle star", false /* isChecked */)))
                .build();
    }

    private Slice createWifiSlice(Uri sliceUri) {
        // Get wifi state
        WifiManager wifiManager = (WifiManager) getContext()
                .getApplicationContext().getSystemService(Context.WIFI_SERVICE);
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
        SliceAction primaryAction = SliceAction.create(getIntent(Settings.ACTION_WIFI_SETTINGS),
                IconCompat.createWithResource(getContext(), R.drawable.ic_wifi),
                ICON_IMAGE, "Wi-fi Settings");
        String toggleCDString = wifiEnabled ? "Turn wifi off" : "Turn wifi on";
        String sliceCDString = wifiEnabled ? "Wifi connected to " + state
                : "Wifi disconnected, 10 networks available";
        ListBuilder lb = new ListBuilder(getContext(), sliceUri, INFINITY)
                .setAccentColor(0xff4285f4)
                .setHeader(new HeaderBuilder()
                        .setTitle("Wi-fi")
                        .setSubtitle(state)
                        .setContentDescription(sliceCDString)
                        .setPrimaryAction(primaryAction))
                .addAction(SliceAction.createToggle(getBroadcastIntent(ACTION_WIFI_CHANGED, null),
                        toggleCDString, finalWifiEnabled));

        // Add fake wifi networks
        int[] wifiIcons = new int[]{R.drawable.ic_wifi_full, R.drawable.ic_wifi_low,
                R.drawable.ic_wifi_fair};
        for (int i = 0; i < 10; i++) {
            final int iconId = wifiIcons[i % wifiIcons.length];
            IconCompat icon = IconCompat.createWithResource(getContext(), iconId);
            final String networkName = "Network" + i;
            ListBuilder.RowBuilder rb = new ListBuilder.RowBuilder();
            rb.setTitleItem(icon, ICON_IMAGE).setTitle(networkName);
            boolean locked = i % 3 == 0;
            if (locked) {
                rb.addEndItem(IconCompat.createWithResource(getContext(), R.drawable.ic_lock),
                        ICON_IMAGE);
                rb.setContentDescription("Connect to " + networkName + ", password needed");
            } else {
                rb.setContentDescription("Connect to " + networkName);
            }
            String message = locked ? "Open wifi password dialog" : "Connect to " + networkName;
            rb.setPrimaryAction(SliceAction.create(
                    getBroadcastIntent(ACTION_TOAST, message), icon, ICON_IMAGE, message));
            lb.addRow(rb);
        }

        // Add keywords
        String[] keywords = new String[]{"internet", "wifi", "data", "network"};
        lb.setKeywords(new ArraySet<>(Arrays.asList(keywords)));

        // Add see more intent
        if (TEST_CUSTOM_SEE_MORE) {
            lb.setSeeMoreRow(new RowBuilder()
                    .setTitle("See all available networks")
                    .addEndItem(
                            IconCompat.createWithResource(getContext(), R.drawable.ic_right_caret),
                            SMALL_IMAGE)
                    .setPrimaryAction(primaryAction));
        } else {
            lb.setSeeMoreAction(primaryAction.getAction());
        }
        return lb.build();
    }

    private Slice createInputRange(Uri sliceUri) {
        IconCompat icon = IconCompat.createWithResource(getContext(), R.drawable.ic_star_on);
        SliceAction primaryAction = SliceAction.create(
                getBroadcastIntent(ACTION_TOAST, "open input rating"), icon, ICON_IMAGE, "Rate");
        String subtitle = "Rated " + STAR_RATING;
        return new ListBuilder(getContext(), sliceUri, INFINITY)
                .setAccentColor(0xffff4081)
                .addInputRange(new InputRangeBuilder()
                        .setTitle("Input rating")
                        .setSubtitle(subtitle)
                        .setMin(0)
                        .setThumb(icon)
                        .setInputAction(getBroadcastIntent(ACTION_TOAST_RANGE_VALUE, null))
                        .setMax(100)
                        .setValue(STAR_RATING)
                        .setPrimaryAction(primaryAction)
                        .setContentDescription("Slider for input ratings"))
                .build();
    }

    private Slice createRichInputRange(Uri sliceUri) {
        IconCompat thumbIcon = IconCompat.createWithResource(getContext(), R.drawable.ic_star_on);
        IconCompat titleIcon = IconCompat.createWithResource(getContext(), R.drawable.ic_car);
        IconCompat checkBoxIcon = IconCompat.createWithResource(getContext(),
                R.drawable.toggle_check);
        SliceAction primaryAction = SliceAction.create(
                getBroadcastIntent(ACTION_TOAST, "open rich star rating"), thumbIcon, ICON_IMAGE,
                "Rate");
        return new ListBuilder(getContext(), sliceUri, INFINITY)
                .setAccentColor(0xff4285f4)
                .setHeader(new HeaderBuilder()
                        .setTitle("Rich InputRangeBuilder demo"))
                .addInputRange(new InputRangeBuilder()
                        .setTitleItem(titleIcon, ListBuilder.ICON_IMAGE)
                        .addEndItem(SliceAction.createToggle(
                                getBroadcastIntent(ACTION_TOAST, "click checkbox"), checkBoxIcon,
                                "checkbox", false))
                        .setTitle("Rich input rating")
                        .setMin(0)
                        .setThumb(thumbIcon)
                        .setInputAction(getBroadcastIntent(ACTION_TOAST_RANGE_VALUE, null))
                        .setMax(100)
                        .setValue(STAR_RATING)
                        .setPrimaryAction(primaryAction)
                        .setContentDescription("Slider for star ratings"))
                .build();
    }

    private Slice createDownloadProgressRange(Uri sliceUri) {
        IconCompat icon = IconCompat.createWithResource(getContext(), R.drawable.ic_star_on);
        SliceAction primaryAction = SliceAction.create(
                getBroadcastIntent(ACTION_TOAST, "open download"), icon, ICON_IMAGE, "Download");
        int progress = PROGRESS.next();
        if (progress != 100) {
            mHandler.postDelayed(() -> getContext().getContentResolver().notifyChange(sliceUri,
                    null), 500);
        }
        return new ListBuilder(getContext(), sliceUri, INFINITY)
                .setAccentColor(0xffff4081)
                .addRange(new RangeBuilder()
                        .setTitle("Download progress")
                        .setSubtitle("Download is happening")
                        .setMax(100)
                        .setValue(progress)
                        .setPrimaryAction(primaryAction))
                .build();
    }

    private Slice createIndeterminateProgressRange(Uri sliceUri) {
        IconCompat icon = IconCompat.createWithResource(getContext(), R.drawable.ic_star_on);
        SliceAction primaryAction = SliceAction.create(
                getBroadcastIntent(ACTION_TOAST, "open download"), icon, ICON_IMAGE,
                "Download");
        return new ListBuilder(getContext(), sliceUri, INFINITY)
                .setAccentColor(0xff4285f4)
                .addRange(new RangeBuilder()
                        .setMode(ListBuilder.RANGE_MODE_INDETERMINATE)
                        .setTitle("Indeterminate progress")
                        .setSubtitle("Download is happening")
                        .setPrimaryAction(primaryAction))
                .build();
    }

    private Slice createIndeterminateProgressRange2(Uri sliceUri) {
        IconCompat icon = IconCompat.createWithResource(getContext(), R.drawable.ic_star_on);
        SliceAction primaryAction = SliceAction.create(
                getBroadcastIntent(ACTION_TOAST, "open download"), icon, ICON_IMAGE,
                "Download");
        return new ListBuilder(getContext(), sliceUri, INFINITY)
                .setAccentColor(0xff4285f4)
                .addRange(new RangeBuilder()
                        .setTitleItem(icon, ListBuilder.ICON_IMAGE)
                        .setMode(ListBuilder.RANGE_MODE_INDETERMINATE)
                        .setTitle("Indeterminate progress")
                        .setPrimaryAction(primaryAction))
                .build();
    }

    private Slice createIndeterminateProgressRange3(Uri sliceUri) {
        IconCompat thumbIcon = IconCompat.createWithResource(getContext(), R.drawable.ic_star_on);
        IconCompat titleIcon = IconCompat.createWithResource(getContext(), R.drawable.ic_car);
        IconCompat checkBoxIcon = IconCompat.createWithResource(getContext(),
                R.drawable.toggle_check);
        SliceAction primaryAction = SliceAction.create(
                getBroadcastIntent(ACTION_TOAST, "open rich star rating"), thumbIcon, ICON_IMAGE,
                "Rate");
        IconCompat icon = IconCompat.createWithResource(getContext(), R.drawable.ic_star_on);
        SliceAction progressPrimaryAction = SliceAction.create(
                getBroadcastIntent(ACTION_TOAST, "open download"), icon, ICON_IMAGE,
                "Download");
        return new ListBuilder(getContext(), sliceUri, INFINITY)
                .setAccentColor(0xff4285f4)
                .addInputRange(new InputRangeBuilder()
                        .setTitleItem(titleIcon, ListBuilder.ICON_IMAGE)
                        .addEndItem(SliceAction.createToggle(
                                getBroadcastIntent(ACTION_TOAST, "click checkbox"), checkBoxIcon,
                                "checkbox", false))
                        .setTitle("Rich star rating")
                        .setMin(0)
                        .setThumb(thumbIcon)
                        .setInputAction(getBroadcastIntent(ACTION_TOAST_RANGE_VALUE, null))
                        .setMax(100)
                        .setValue(STAR_RATING)
                        .setPrimaryAction(primaryAction)
                        .setContentDescription("Slider for star ratings"))
                .addRange(new RangeBuilder()
                        .setTitleItem(icon, ListBuilder.ICON_IMAGE)
                        .setMode(ListBuilder.RANGE_MODE_INDETERMINATE)
                        .setTitle("Indeterminate progress")
                        .setPrimaryAction(progressPrimaryAction))
                .build();
    }

    private Slice createStarRating(Uri sliceUri) {
        IconCompat icon = IconCompat.createWithResource(getContext(), R.drawable.ic_star_on);
        SliceAction primaryAction = SliceAction.create(
                getBroadcastIntent(ACTION_TOAST, "open input rating"), icon, ICON_IMAGE, "Rate");
        String subtitle = "Rated " + STAR_RATING;
        return new ListBuilder(getContext(), sliceUri, INFINITY)
                .setAccentColor(0xFFE7711B) // GOLD
                .addRow(new RowBuilder()
                        .setTitle("Star rating")
                        .setSubtitle(subtitle)
                        .setPrimaryAction(primaryAction))
                .addRating(new RatingBuilder()
                        .setMin(0)
                        .setInputAction(getBroadcastIntent(ACTION_TOAST_RANGE_VALUE, null))
                        .setMax(5)
                        .setValue(STAR_RATING)
                        .setContentDescription("Slider for star ratings"))
                .build();
    }

    private Slice createStarRating2(Uri sliceUri) {
        IconCompat icon = IconCompat.createWithResource(getContext(), R.drawable.ic_star_on);
        SliceAction primaryAction = SliceAction.create(
                getBroadcastIntent(ACTION_TOAST, "open input rating"), icon, ICON_IMAGE, "Rate");
        String subtitle = "Rated " + STAR_RATING;
        return new ListBuilder(getContext(), sliceUri, INFINITY)
                .setAccentColor(0xFFE7711B) // GOLD
                .addRating(new RatingBuilder()
                        .setTitle("Star rating")
                        .setSubtitle(subtitle)
                        .setPrimaryAction(primaryAction)
                        .setMin(0)
                        .setInputAction(getBroadcastIntent(ACTION_TOAST_RANGE_VALUE, null))
                        .setMax(5)
                        .setValue(STAR_RATING)
                        .setContentDescription("Slider for star ratings"))
                .build();
    }

    private Slice createToggleTesterSlice(Uri uri) {
        IconCompat star = IconCompat.createWithResource(getContext(), R.drawable.toggle_star);
        IconCompat icon = IconCompat.createWithResource(getContext(), R.drawable.ic_star_on);

        SliceAction primaryAction = SliceAction.create(
                getBroadcastIntent(ACTION_TOAST, "primary action"), icon,
                ICON_IMAGE, "Primary action");
        SliceAction toggleAction = SliceAction.createToggle(
                getBroadcastIntent(ACTION_TOAST, "star note"), star, "Star note", false);
        SliceAction toggleAction2 = SliceAction.createToggle(
                getBroadcastIntent(ACTION_TOAST, "star note 2"), star, "Star note 2", true);
        SliceAction toggleAction3 = SliceAction.createToggle(
                getBroadcastIntent(ACTION_TOAST, "star note 3"), star, "Star note 3", false);

        ListBuilder lb = new ListBuilder(getContext(), uri, INFINITY);

        // Primary action toggle
        ListBuilder.RowBuilder primaryToggle = new ListBuilder.RowBuilder();
        primaryToggle.setTitle("Primary action is a toggle")
                .setPrimaryAction(toggleAction);

        // End toggle + normal primary action
        ListBuilder.RowBuilder endToggle = new ListBuilder.RowBuilder();
        endToggle.setTitle("Only end toggles")
                .setSubtitle("Normal primary action")
                .setPrimaryAction(primaryAction)
                .addEndItem(toggleAction)
                .addEndItem(toggleAction2);

        // Start toggle + normal primary
        ListBuilder.RowBuilder startToggle = new ListBuilder.RowBuilder();
        startToggle.setTitle("One start toggle")
                .setTitleItem(toggleAction)
                .setSubtitle("Normal primary action")
                .setPrimaryAction(primaryAction);

        // Start + end toggles + normal primary action
        ListBuilder.RowBuilder someToggles = new ListBuilder.RowBuilder();
        someToggles.setTitleItem(toggleAction)
                .setPrimaryAction(primaryAction)
                .setTitle("Start & end toggles")
                .setSubtitle("Normal primary action")
                .addEndItem(toggleAction2)
                .addEndItem(toggleAction3);

        // Start toggle ONLY
        ListBuilder.RowBuilder startToggleOnly = new ListBuilder.RowBuilder();
        startToggleOnly.setTitle("Start action is a toggle")
                .setSubtitle("No other actions")
                .setTitleItem(toggleAction);

        // End toggle ONLY
        ListBuilder.RowBuilder endToggleOnly = new ListBuilder.RowBuilder();
        endToggleOnly.setTitle("End action is a toggle")
                .setSubtitle("No other actions")
                .addEndItem(toggleAction);

        // All toggles: end item should be ignored / replaced with primary action
        ListBuilder.RowBuilder muchToggles = new ListBuilder.RowBuilder();
        muchToggles.setTitleItem(toggleAction)
                .setTitle("All toggles")
                .setSubtitle("Even the primary action")
                .setPrimaryAction(toggleAction2)
                .addEndItem(toggleAction3);

        lb.addRow(primaryToggle);
        lb.addRow(endToggleOnly);
        lb.addRow(endToggle);
        lb.addRow(startToggleOnly);
        lb.addRow(startToggle);
        lb.addRow(someToggles);
        lb.addRow(muchToggles);
        return lb.build();
    }

    private Slice createRtlGridSlice(Uri uri) {
        ListBuilder lb = new ListBuilder(getContext(), uri, INFINITY);
        SliceAction action = SliceAction.create(getBroadcastIntent(ACTION_TOAST,
                "Open language practice"),
                IconCompat.createWithResource(getContext(), R.drawable.ic_speak), ICON_IMAGE,
                "Language practice");
        lb.setHeader(new HeaderBuilder()
                .setTitle("Language practice")
                .setSubtitle("Which image doesn't match the word?")
                .setPrimaryAction(action)
                .setLayoutDirection(View.LAYOUT_DIRECTION_LTR));
        lb.addGridRow(new GridRowBuilder()
                .addCell(new CellBuilder()
                        .addImage(IconCompat.createWithResource(getContext(), R.drawable.cake),
                                SMALL_IMAGE)
                        .addText("1")
                        .addTitleText("كيكة")
                        .setContentIntent(getBroadcastIntent(ACTION_TOAST, "Wrong answer")))
                .addCell(new CellBuilder()
                        .addImage(IconCompat.createWithResource(getContext(), R.drawable.cheese),
                                SMALL_IMAGE)
                        .addText("2")
                        .addTitleText("جبن")
                        .setContentIntent(getBroadcastIntent(ACTION_TOAST, "Wrong answer")))
                .addCell(new CellBuilder()
                        .addImage(IconCompat.createWithResource(getContext(), R.drawable.pizza),
                                SMALL_IMAGE)
                        .addText("3")
                        .addTitleText("تفاحة")
                        .setContentIntent(getBroadcastIntent(ACTION_TOAST,
                                "Correct! This is pizza not an apple!")))
                .setLayoutDirection(View.LAYOUT_DIRECTION_RTL));
        return lb.build();
    }

    private Slice createTranslationSlice(Uri uri) {
        ListBuilder lb = new ListBuilder(getContext(), uri, INFINITY);
        SliceAction action = SliceAction.create(
                getBroadcastIntent(ACTION_TOAST, "View translations"),
                IconCompat.createWithResource(getContext(), R.drawable.ic_speak), ICON_IMAGE,
                "Translations");
        return lb.setLayoutDirection(View.LAYOUT_DIRECTION_LTR)
                .setHeader(new HeaderBuilder()
                        .setTitle("How to say hello")
                        .setSummary("Hello, bonjour, שלום ,مرحبا")
                        .setPrimaryAction(action))
                .addRow(new RowBuilder()
                        .setTitle("Hello")
                        .setSubtitle("English \u00b7 heˈlō")
                        .addEndItem(getSpeakWordAction("Hello")))
                .addRow(new RowBuilder()
                        .setTitle("Bonjour")
                        .setSubtitle("French")
                        .addEndItem(getSpeakWordAction("Bonjour")))
                .addRow(new RowBuilder()
                        .setTitle("שלום")
                        .setSubtitle("Hebrew")
                        .addEndItem(getSpeakWordAction("שלום"))
                        .setLayoutDirection(View.LAYOUT_DIRECTION_RTL))
                .addRow(new RowBuilder()
                        .setTitle("مرحبا")
                        .setSubtitle("Arabic \u00b7 marhabaan")
                        .addEndItem(getSpeakWordAction("مرحبا"))
                        .setLayoutDirection(View.LAYOUT_DIRECTION_RTL))
                .build();
    }

    private SliceAction getSpeakWordAction(String word) {
        return SliceAction.create(getBroadcastIntent(ACTION_TOAST, word),
                IconCompat.createWithResource(getContext(), R.drawable.ic_speak), ICON_IMAGE,
                "Hear " + word);
    }

    private Slice createSingleSlice(Uri uri) {
        IconCompat ic2 = IconCompat.createWithResource(getContext(), R.drawable.ic_create);
        IconCompat image = IconCompat.createWithResource(getContext(), R.drawable.cat_3);
        IconCompat toggle = IconCompat.createWithResource(getContext(), R.drawable.toggle_star);
        SliceAction toggleAction = SliceAction.createToggle(
                getBroadcastIntent(ACTION_TOAST, "toggle action"), toggle, "toggle", false);
        SliceAction simpleAction = SliceAction.create(
                getBroadcastIntent(ACTION_TOAST, "icon action"), ic2, ICON_IMAGE, "icon");
        ListBuilder lb = new ListBuilder(getContext(), uri, INFINITY);
        return lb.addRow(new ListBuilder.RowBuilder()
                        .setTitle("Single title")
                        .setPrimaryAction(simpleAction))
                .addRow(new ListBuilder.RowBuilder()
                        .setSubtitle("Single subtitle"))
                 //Time stamps
                .addRow(new ListBuilder.RowBuilder()
                        .setTitleItem(System.currentTimeMillis()))
                .addRow(new ListBuilder.RowBuilder()
                        .addEndItem(System.currentTimeMillis()))
                // Toggle actions
                .addRow(new ListBuilder.RowBuilder()
                        .setTitleItem(toggleAction))
                .addRow(new ListBuilder.RowBuilder()
                        .addEndItem(toggleAction))
                // Icon actions
                .addRow(new ListBuilder.RowBuilder()
                        .setTitleItem(simpleAction))
                .addRow(new ListBuilder.RowBuilder()
                        .addEndItem(simpleAction))
                // Images
                .addRow(new ListBuilder.RowBuilder()
                        .setTitleItem(image, SMALL_IMAGE))
                .addRow(new ListBuilder.RowBuilder()
                        .addEndItem(image, SMALL_IMAGE))
                .build();
    }

    private Slice createErrorSlice(Uri uri) {
        IconCompat ic2 = IconCompat.createWithResource(getContext(), R.drawable.ic_error);
        SliceAction simpleAction = SliceAction.create(
                getBroadcastIntent(ACTION_TOAST, "error slice tapped"), ic2, ICON_IMAGE, "icon");
        ListBuilder lb = new ListBuilder(getContext(), uri, INFINITY);
        return lb.setIsError(true)
                .addRow(new RowBuilder()
                        .setTitle("Slice representing an error")
                        .setSubtitle("This is not the slice you're looking for")
                        .addEndItem(ic2, ICON_IMAGE)
                        .setPrimaryAction(simpleAction)).build();
    }

    private Slice createLoadingSlice(Uri uri) {
        return new ListBuilder(getContext(), uri, INFINITY)
                .setHeader(new HeaderBuilder().setTitle("Some loading title", true)).build();
    }

    private void update(long delay, final SparseArray<String> summaries, final int id,
            final String s, final Uri uri, final Runnable r) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                summaries.put(id, s);
                getContext().getContentResolver().notifyChange(uri, null);
                r.run();
            }
        }, delay);
    }

    private Slice createLoadingListSlice(Uri sliceUri) {
        boolean updating = mListLastUpdate == 0
                || mListLastUpdate < (System.currentTimeMillis() - 10 * System.currentTimeMillis());
        if (updating) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    mListLastUpdate = System.currentTimeMillis();
                }
            };
            update(1000, mListSummaries, 0, "44 miles | 1 hour 45 min | $31.41", sliceUri, r);
            update(1500, mListSummaries, 1, "12 miles | 12 min | $9.00", sliceUri, r);
            update(1700, mListSummaries, 2, "5 miles | 10 min | $8.00", sliceUri, r);
        }
        CharSequence work = mListSummaries.get(0, "");
        CharSequence home = mListSummaries.get(1, "");
        CharSequence school = mListSummaries.get(2, "");
        SliceAction action = SliceAction.create(
                getBroadcastIntent(ACTION_TOAST, "View traffic info"),
                IconCompat.createWithResource(getContext(), R.drawable.ic_car),
                ICON_IMAGE, "Traffic info");
        Slice s = new ListBuilder(getContext(), sliceUri, -TimeUnit.MINUTES.toMillis(50))
                .addRow(new RowBuilder()
                        .setTitle("Work")
                        .setPrimaryAction(action)
                        .setSubtitle(work,
                                updating || TextUtils.isEmpty(work))
                        .addEndItem(IconCompat.createWithResource(getContext(), R.drawable.ic_work),
                                ICON_IMAGE))
                .addRow(new RowBuilder()
                        .setTitle("Home")
                        .setSubtitle(mListSummaries.get(1, ""),
                                updating || TextUtils.isEmpty(home))
                        .addEndItem(
                                IconCompat.createWithResource(getContext(), R.drawable.ic_home),
                                ICON_IMAGE))
                .addRow(new RowBuilder()
                        .setTitle("School")
                        .setSubtitle(mListSummaries.get(2, ""),
                                updating || TextUtils.isEmpty(school))
                        .addEndItem(
                                IconCompat.createWithResource(getContext(), R.drawable.ic_school),
                                ICON_IMAGE))
                .build();
        return s;
    }

    // TODO: Should test large image grids
    private Slice createLoadingGridSlice(Uri sliceUri) {
        boolean updating = mGridLastUpdate == 0
                || mGridLastUpdate < (System.currentTimeMillis() - 10 * System.currentTimeMillis());
        if (updating) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    mGridLastUpdate = System.currentTimeMillis();
                }
            };
            update(2000, mGridSummaries, 0, "Heavy traffic in your area", sliceUri, r);
            update(3500, mGridSummaries, 1, "Typical conditions with delays up to 28 min",
                    sliceUri, r);
            update(3000, mGridSummaries, 2, "41 min", sliceUri, r);
            update(1500, mGridSummaries, 3, "33 min", sliceUri, r);
            update(1000, mGridSummaries, 4, "12 min", sliceUri, r);
        }
        CharSequence title = mGridSummaries.get(0, "");
        CharSequence subtitle = mGridSummaries.get(1, "");
        CharSequence home = mGridSummaries.get(2, "");
        CharSequence work = mGridSummaries.get(3, "");
        CharSequence school = mGridSummaries.get(4, "");
        SliceAction action = SliceAction.create(
                getBroadcastIntent(ACTION_TOAST, "View traffic info"),
                IconCompat.createWithResource(getContext(), R.drawable.ic_car),
                ICON_IMAGE, "Traffic info");
        Slice s = new ListBuilder(getContext(), sliceUri, INFINITY)
                .setHeader(new HeaderBuilder()
                        .setTitle(title,
                                updating || TextUtils.isEmpty(title))
                        .setSubtitle(subtitle,
                                updating || TextUtils.isEmpty(subtitle))
                        .setPrimaryAction(action))
                .addGridRow(new GridRowBuilder()
                        .addCell(new CellBuilder()
                                .addImage(IconCompat.createWithResource(getContext(),
                                        R.drawable.ic_home),
                                        ICON_IMAGE)
                                .addTitleText("Home")
                                .addText(home,
                                        updating || TextUtils.isEmpty(home)))
                        .addCell(new CellBuilder()
                                .addImage(IconCompat.createWithResource(getContext(),
                                        R.drawable.ic_work),
                                        ICON_IMAGE)
                                .addTitleText("Work")
                                .addText(work,
                                        updating || TextUtils.isEmpty(work)))
                        .addCell(new CellBuilder()
                                .addImage(IconCompat.createWithResource(getContext(),
                                        R.drawable.ic_school),
                                        ICON_IMAGE)
                                .addTitleText("School")
                                .addText(school,
                                        updating || TextUtils.isEmpty(school))))
                .build();
        return s;
    }

    private Slice createSelectionSlice(Uri sliceUri) {
        return new ListBuilder(getContext(), sliceUri, INFINITY)
                .addSelection(new SelectionBuilder()
                        .setTitle("Pick a card")
                        .setSubtitle("Any card")
                        .addOption("index", "Index")
                        .addOption("business", "Business")
                        .addOption("playing", "Playing")
                        .setSelectedOption("business")
                        // TODO: Update this intent once view-side selection is supported.
                        .setPrimaryAction(SliceAction.create(
                                getBroadcastIntent(ACTION_TOAST,
                                        "open card type selection"),
                                IconCompat.createWithResource(getContext(), R.drawable.ic_note),
                                ICON_IMAGE,
                                "Select card type"))
                        .setInputAction(getBroadcastIntent(ACTION_TOAST,
                                "handle card type selection"))
                        .setContentDescription("selection for card type"))
                .build();
    }

    private Slice createNotificationSlice(Uri sliceUri) {
        final IconCompat icon = IconCompat.createWithResource(getContext(), R.drawable.message);
        final SliceAction action = SliceAction.create(getBroadcastIntent(ACTION_TOAST,
                "View notifications"), icon, ICON_IMAGE, "Notifications");
        SliceAction toggleAction = SliceAction.createToggle(
                getBroadcastIntent(ACTION_TOAST, "toggle action"), "toggle", false);
        return new ListBuilder(getContext(), sliceUri, INFINITY)
                .addRow(new RowBuilder()
                        .setTitleItem(icon, RAW_IMAGE_SMALL)
                        .setTitle("Manage Message Notifications")
                        .setSubtitle("7 notification channels. Tap to manage all.")
                        .setPrimaryAction(action))
                .addRow(new RowBuilder()
                        .setTitle("Chat heads active")
                        .setSubtitle("~6 per week")
                        .setPrimaryAction(action)
                        .addEndItem(toggleAction))
                .addRow(new RowBuilder()
                        .setTitle("Chats and calls")
                        .setSubtitle("~2 per week")
                        .setPrimaryAction(action)
                        .addEndItem(toggleAction))
                .addRow(new RowBuilder()
                        .setTitle("Other")
                        .setSubtitle("~2 per week")
                        .setPrimaryAction(action)
                        .addEndItem(toggleAction))
                .build();
    }

    private Slice createTextButtonSlice(Uri sliceUri) {
        SliceAction primaryAction = SliceAction.create(
                getBroadcastIntent(ACTION_TOAST, "Test text button"),
                IconCompat.createWithResource(getContext(), R.drawable.message),
                LARGE_IMAGE,
                "Text button");
        //The drawable passed into SliceAction could be a random one, since it will not be
        // actually used for text button.
        ListBuilder slice = new ListBuilder(getContext(), sliceUri, INFINITY)
                .addRow(new RowBuilder()
                        .setTitle("Text Button")
                        .setSubtitle("Click")
                        .setPrimaryAction(primaryAction))
                .addAction(
                        SliceAction.create(
                                getBroadcastIntent(ACTION_TOAST, "clicked button1"),
                                IconCompat.createWithResource(getContext(), R.drawable.ic_cast),
                                ListBuilder.ACTION_WITH_LABEL,
                                "Button1"))
                .addAction(
                        SliceAction.create(
                                getBroadcastIntent(ACTION_TOAST, "clicked button2"),
                                IconCompat.createWithResource(getContext(), R.drawable.ic_cast),
                                ListBuilder.ACTION_WITH_LABEL,
                                "Button2"));

        return slice.build();
    }

    private Slice createTtsSlice(Uri sliceUri) {
        ListBuilder slice = new ListBuilder(getContext(), sliceUri, INFINITY)
                .addRow(
                        new RowBuilder().setPrimaryAction(
                                SliceAction.create(
                                        getBroadcastIntent(ACTION_PLAY_TTS, null),
                                        IconCompat.createWithResource(getContext(),
                                                R.drawable.message),
                                        ICON_IMAGE, "TTS"
                                )
                        ).setTitle("Text to speech").setSubtitle("Play"));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            SetHostExtraApi21Impl.setHostExtra(slice, "tts", "hello world");
        }

        return slice.build();
    }

    @RequiresApi(21)
    private static class SetHostExtraApi21Impl {
        private SetHostExtraApi21Impl() {}
        static void setHostExtra(ListBuilder listBuilder, String key, String value) {
            PersistableBundle extras = new PersistableBundle();
            extras.putString(key, value);
            // Attach additional information for host. Depending on the host apps, this
            // information might or might not be used.
            // In this case, SliceBrowser is customized to play TTS when binding the slice.
            listBuilder.setHostExtras(extras);
        }
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

    static final class RandomProgressGenerator {
        private int mProgress = 0;
        private final int mEnd = 100;
        private final int mInterval = 5;

        int next() {
            if (mProgress >= mEnd) return mEnd;
            mProgress += new Random().nextInt(mInterval);
            return Math.min(mEnd, mProgress);
        }
    }

    private static final RandomProgressGenerator PROGRESS = new RandomProgressGenerator();
}
