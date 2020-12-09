/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.car.app;

import static androidx.car.app.model.CarIcon.BACK;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.text.SpannableString;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarText;
import androidx.car.app.model.DateTimeWithZone;
import androidx.car.app.model.DistanceSpan;
import androidx.car.app.model.GridItem;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Pane;
import androidx.car.app.model.Row;
import androidx.car.app.model.SectionedItemList;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** A grab bag of utility methods intended only for tests. */
public class TestUtils {
    /** Helper functions in here only. */
    private TestUtils() {
    }

    /**
     * Returns a {@link DateTimeWithZone} instance from a date string and a time zone id.
     *
     * @param dateTimeString The string in ISO format, for example "2020-04-14T15:57:00".
     * @param zoneIdString   An Olson DB time zone identifier, for example "US/Pacific".
     */
    public static DateTimeWithZone createDateTimeWithZone(
            String dateTimeString, String zoneIdString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        TimeZone timeZone = TimeZone.getTimeZone(zoneIdString);
        dateFormat.setTimeZone(timeZone);
        Date date;
        try {
            date = dateFormat.parse(dateTimeString);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Failed to parse string: " + dateTimeString, e);
        }
        if (date == null) {
            throw new IllegalArgumentException("Failed to parse string: " + dateTimeString);
        }
        return DateTimeWithZone.create(date.getTime(), timeZone);
    }

    /** Returns a default {@link Action} instance. */
    public static Action createAction(@Nullable String title, @Nullable CarIcon icon) {
        return Action.builder().setTitle(title).setIcon(icon).setOnClickListener(() -> {
        }).build();
    }

    /** Returns an {@link ItemList} with the given number of rows and selectable state. */
    public static ItemList createItemList(int rowCount, boolean isSelectable) {
        return createItemListWithDistanceSpan(rowCount, isSelectable, null);
    }

    /**
     * Returns an {@link ItemList} with the given selectable state and number of rows populated with
     * the given {@link DistanceSpan}.
     */
    public static ItemList createItemListWithDistanceSpan(
            int rowCount, boolean isSelectable, @Nullable DistanceSpan distanceSpan) {
        ItemList.Builder builder = ItemList.builder();
        for (int i = 0; i < rowCount; ++i) {
            Row.Builder rowBuilder = Row.builder();
            if (distanceSpan != null) {
                SpannableString title = new SpannableString("  title " + i);
                title.setSpan(distanceSpan, /* start= */ 0, /* end= */ 1, /* flags= */ 0);
                rowBuilder.setTitle(title);
            } else {
                rowBuilder.setTitle("title " + i);
            }
            builder.addItem(rowBuilder.build());
        }

        if (isSelectable) {
            builder.setOnSelectedListener(index -> {
            });
        }

        return builder.build();
    }

    /** Returns a {@link Pane} with the given number of rows and actions */
    public static Pane createPane(int rowCount, int actionCount) {
        Pane.Builder builder = Pane.builder();
        for (int i = 0; i < rowCount; ++i) {
            builder.addRow(Row.builder().setTitle("title " + i).build());
        }

        List<Action> actions = new ArrayList<>();
        for (int i = 0; i < actionCount; i++) {
            actions.add(createAction("action " + i, null));
        }
        builder.setActions(actions);

        return builder.build();
    }

    /** Returns a list of {@link SectionedItemList} with the given parameters. */
    public static List<SectionedItemList> createSections(
            int sectionCount, int rowCountPerSection, boolean isSelectable) {
        List<SectionedItemList> sections = new ArrayList<>();

        for (int i = 0; i < sectionCount; i++) {
            sections.add(
                    SectionedItemList.create(
                            createItemList(rowCountPerSection, isSelectable),
                            CarText.create("Section " + i)));
        }

        return sections;
    }

    /** Returns an {@link ItemList} consisting of {@link GridItem}s */
    public static ItemList getGridItemList(int itemCount) {
        ItemList.Builder builder = ItemList.builder();
        while (itemCount-- > 0) {
            builder.addItem(GridItem.builder().setTitle("Title").setImage(BACK).build());
        }
        return builder.build();
    }

    @RequiresApi(26)
    public static void assertDateTimeWithZoneEquals(
            ZonedDateTime zonedDateTime, DateTimeWithZone dateTimeWithZone) {
        assertThat(dateTimeWithZone.getZoneShortName())
                .isEqualTo(zonedDateTime.getZone().getDisplayName(TextStyle.SHORT,
                        Locale.getDefault()));
        assertThat(dateTimeWithZone.getZoneOffsetSeconds())
                .isEqualTo(dateTimeWithZone.getZoneOffsetSeconds());
        assertThat(dateTimeWithZone.getTimeSinceEpochMillis())
                .isEqualTo(dateTimeWithZone.getTimeSinceEpochMillis());
    }

    public static Context getMockContextWithPermission(@NonNull String permission) {
        Context mockContext = mock(Context.class);
        PackageManager packageManager = mock(PackageManager.class);
        when(mockContext.getPackageManager()).thenReturn(packageManager);
        when(packageManager.checkPermission(eq(permission), any())).thenReturn(
                PackageManager.PERMISSION_GRANTED);
        return mockContext;
    }
}
