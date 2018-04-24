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

package androidx.slice;

import static android.app.slice.Slice.HINT_TITLE;

import static androidx.slice.SliceMetadata.LOADED_ALL;
import static androidx.slice.SliceMetadata.LOADED_NONE;
import static androidx.slice.SliceMetadata.LOADED_PARTIAL;
import static androidx.slice.builders.ListBuilder.ICON_IMAGE;
import static androidx.slice.core.SliceHints.HINT_KEYWORDS;
import static androidx.slice.core.SliceHints.INFINITY;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.core.graphics.drawable.IconCompat;
import androidx.core.util.Pair;
import androidx.slice.builders.GridRowBuilder;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;
import androidx.slice.core.SliceActionImpl;
import androidx.slice.core.SliceHints;
import androidx.slice.render.SliceRenderActivity;
import androidx.slice.widget.EventInfo;
import androidx.slice.widget.SliceLiveData;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Tests for {@link SliceMetadata}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class SliceMetadataTest {

    private final Context mContext = InstrumentationRegistry.getContext();

    @Before
    public void setup() {
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
    }

    @Test
    public void testGetSliceActionsNull() {
        Uri uri = Uri.parse("content://pkg/slice");

        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        lb.addRow(new ListBuilder.RowBuilder(lb).setTitle("Text"));

        SliceMetadata sliceMetadata = SliceMetadata.from(mContext, lb.build());
        assertNull(sliceMetadata.getSliceActions());
    }

    @Test
    public void testGetSliceActions() {
        Uri uri = Uri.parse("content://pkg/slice");
        PendingIntent pi = getIntent("");
        Bitmap b = Bitmap.createBitmap(50, 25, Bitmap.Config.ARGB_8888);
        new Canvas(b).drawColor(0xffff0000);
        IconCompat icon = IconCompat.createWithBitmap(b);

        SliceAction action1 = new SliceAction(pi, icon, "action1");
        SliceAction action2 = new SliceAction(pi, icon, "action2");
        SliceAction action3 = new SliceAction(pi, icon, "action3");

        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        lb.addRow(new ListBuilder.RowBuilder(lb).setTitle("Text"))
                .addAction(action1)
                .addAction(action2)
                .addAction(action3);

        ArrayList<SliceAction> expectedActions = new ArrayList<>();
        expectedActions.add(action1);
        expectedActions.add(action2);
        expectedActions.add(action3);

        SliceMetadata sliceMetadata = SliceMetadata.from(mContext, lb.build());
        List<SliceItem> actions = sliceMetadata.getSliceActions();

        for (int i = 0; i < expectedActions.size(); i++) {
            assertEquivalent(expectedActions.get(i), new SliceActionImpl(actions.get(i)));
        }
    }

    @Test
    public void testGetPrimaryActionForGrid() {
        Uri uri = Uri.parse("content://pkg/slice");
        PendingIntent pi = getIntent("");
        Bitmap b = Bitmap.createBitmap(50, 25, Bitmap.Config.ARGB_8888);
        new Canvas(b).drawColor(0xffff0000);
        IconCompat icon = IconCompat.createWithBitmap(b);

        SliceAction primaryAction = new SliceAction(pi, icon, "action");

        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        GridRowBuilder grb = new GridRowBuilder(lb);
        grb.setPrimaryAction(primaryAction);
        grb.addCell(new GridRowBuilder.CellBuilder(grb).addText("some text"));
        grb.addCell(new GridRowBuilder.CellBuilder(grb).addText("some text"));
        grb.addCell(new GridRowBuilder.CellBuilder(grb).addText("some text"));
        lb.addGridRow(grb);

        Slice gridSlice = lb.build();
        SliceMetadata gridInfo = SliceMetadata.from(mContext, gridSlice);
        assertEquivalent(primaryAction, gridInfo.getPrimaryAction());
    }

    @Test
    public void testGetPrimaryActionForRow() {
        Uri uri = Uri.parse("content://pkg/slice");
        PendingIntent pi = getIntent("");
        Bitmap b = Bitmap.createBitmap(50, 25, Bitmap.Config.ARGB_8888);
        new Canvas(b).drawColor(0xffff0000);
        IconCompat icon = IconCompat.createWithBitmap(b);

        SliceAction primaryAction = new SliceAction(pi, icon, "action");
        SliceAction endAction = new SliceAction(pi, "toogle action", false);

        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        ListBuilder.RowBuilder rb = new ListBuilder.RowBuilder(lb)
                .setTitle("a title")
                .addEndItem(endAction)
                .setPrimaryAction(primaryAction);
        lb.addRow(rb);

        Slice rowSlice = lb.build();
        SliceMetadata rowInfo = SliceMetadata.from(mContext, rowSlice);
        assertEquivalent(primaryAction, rowInfo.getPrimaryAction());
    }

    @Test
    public void testGetPrimaryActionForHeader() {
        Uri uri = Uri.parse("content://pkg/slice");
        PendingIntent pi = getIntent("");
        Bitmap b = Bitmap.createBitmap(50, 25, Bitmap.Config.ARGB_8888);
        new Canvas(b).drawColor(0xffff0000);
        IconCompat icon = IconCompat.createWithBitmap(b);

        SliceAction primaryAction = new SliceAction(pi, icon, "action");
        SliceAction sliceAction = new SliceAction(pi, "another action", true);

        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        lb.addAction(sliceAction);
        ListBuilder.HeaderBuilder hb = new ListBuilder.HeaderBuilder(lb);
        hb.setTitle("header title");
        hb.setPrimaryAction(primaryAction);
        lb.setHeader(hb);

        Slice headerSlice = lb.build();
        SliceMetadata headerInfo = SliceMetadata.from(mContext, headerSlice);
        assertEquivalent(primaryAction, headerInfo.getPrimaryAction());
    }

    @Test
    public void testGetPrimaryActionNull() {
        Uri uri = Uri.parse("content://pkg/slice");
        PendingIntent pi = getIntent("");
        Bitmap b = Bitmap.createBitmap(50, 25, Bitmap.Config.ARGB_8888);
        new Canvas(b).drawColor(0xffff0000);
        IconCompat icon = IconCompat.createWithBitmap(b);

        SliceAction endAction1 = new SliceAction(pi, icon, "action");
        SliceAction endAction2 = new SliceAction(pi, "toogle action", false);

        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        ListBuilder.RowBuilder rb = new ListBuilder.RowBuilder(lb)
                .setTitle("a title")
                .addEndItem(endAction1)
                .addEndItem(endAction2);
        lb.addRow(rb);

        Slice rowSlice = lb.build();
        SliceMetadata rowInfo = SliceMetadata.from(mContext, rowSlice);
        assertNull(rowInfo.getPrimaryAction());
    }

    @Test
    public void testGetHeaderTypeGrid() {
        Uri uri = Uri.parse("content://pkg/slice");

        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        GridRowBuilder grb = new GridRowBuilder(lb);
        grb.addCell(new GridRowBuilder.CellBuilder(grb).addText("some text"));
        grb.addCell(new GridRowBuilder.CellBuilder(grb).addText("some text"));
        grb.addCell(new GridRowBuilder.CellBuilder(grb).addText("some text"));
        lb.addGridRow(grb);

        Slice gridSlice = lb.build();
        SliceMetadata gridInfo = SliceMetadata.from(mContext, gridSlice);
        assertEquals(EventInfo.ROW_TYPE_GRID, gridInfo.getHeaderType());
    }

    @Test
    public void testGetHeaderTypeRow() {
        Uri uri = Uri.parse("content://pkg/slice");

        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        lb.addRow(new ListBuilder.RowBuilder(lb).setTitle("a title"));

        Slice rowSlice = lb.build();
        SliceMetadata rowInfo = SliceMetadata.from(mContext, rowSlice);
        assertEquals(EventInfo.ROW_TYPE_LIST, rowInfo.getHeaderType());
    }

    @Test
    public void testGetHeaderTypeToggle() {
        Uri uri = Uri.parse("content://pkg/slice");
        PendingIntent pi = getIntent("");

        SliceAction toggleAction = new SliceAction(pi, "toggle", false /* isChecked */);
        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        lb.addRow(new ListBuilder.RowBuilder(lb)
                .setTitle("another title")
                .addEndItem(toggleAction));

        Slice toggleSlice = lb.build();
        SliceMetadata toggleInfo = SliceMetadata.from(mContext, toggleSlice);
        assertEquals(EventInfo.ROW_TYPE_TOGGLE, toggleInfo.getHeaderType());
    }

    @Test
    public void testGetHeaderTypeSlider() {
        Uri uri = Uri.parse("content://pkg/slice");
        PendingIntent pi = getIntent("");

        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        lb.addInputRange(new ListBuilder.InputRangeBuilder(lb)
                .setTitle("another title")
                .setValue(5)
                .setMax(10)
                .setInputAction(pi));

        Slice sliderSlice = lb.build();
        SliceMetadata sliderInfo = SliceMetadata.from(mContext, sliderSlice);
        assertEquals(EventInfo.ROW_TYPE_SLIDER, sliderInfo.getHeaderType());
    }

    @Test
    public void testGetHeaderTypeProgress() {
        Uri uri = Uri.parse("content://pkg/slice");

        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        lb.addRange(new ListBuilder.RangeBuilder(lb)
                .setTitle("another title")
                .setValue(5)
                .setMax(10));

        Slice sliderSlice = lb.build();
        SliceMetadata progressInfo = SliceMetadata.from(mContext, sliderSlice);
        assertEquals(EventInfo.ROW_TYPE_PROGRESS, progressInfo.getHeaderType());
    }

    @Test
    public void testGetHeaderTypeHeader() {
        Uri uri = Uri.parse("content://pkg/slice");

        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        ListBuilder.HeaderBuilder hb = new ListBuilder.HeaderBuilder(lb);
        hb.setTitle("header title");
        lb.setHeader(hb);

        Slice headerSlice = lb.build();
        SliceMetadata headerInfo = SliceMetadata.from(mContext, headerSlice);
        assertEquals(EventInfo.ROW_TYPE_LIST, headerInfo.getHeaderType());
    }

    @Test
    public void testHasLargeModeFullGrid() {
        // If a grid is "full" with two text and an image there is a different small / large state.
        Uri uri = Uri.parse("content://pkg/slice");
        Bitmap b = Bitmap.createBitmap(50, 25, Bitmap.Config.ARGB_8888);
        new Canvas(b).drawColor(0xffff0000);
        IconCompat icon = IconCompat.createWithBitmap(b);

        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        GridRowBuilder grb = new GridRowBuilder(lb);
        grb.addCell(new GridRowBuilder.CellBuilder(grb)
                .addText("some text")
                .addText("more text")
                .addImage(icon, ICON_IMAGE));
        grb.addCell(new GridRowBuilder.CellBuilder(grb)
                .addText("some text")
                .addText("more text")
                .addImage(icon, ICON_IMAGE));
        grb.addCell(new GridRowBuilder.CellBuilder(grb)
                .addText("some text")
                .addText("more text")
                .addImage(icon, ICON_IMAGE));
        lb.addGridRow(grb);

        Slice gridSlice = lb.build();
        SliceMetadata gridInfo = SliceMetadata.from(mContext, gridSlice);
        assertTrue(gridInfo.hasLargeMode());
    }

    @Test
    public void testHasLargeModeGrid() {
        // If a grid is not "full" either no image or an image and one text item, small / large
        // is the same.
        Uri uri = Uri.parse("content://pkg/slice");

        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        GridRowBuilder grb = new GridRowBuilder(lb);
        grb.addCell(new GridRowBuilder.CellBuilder(grb).addText("some text"));
        grb.addCell(new GridRowBuilder.CellBuilder(grb).addText("some text"));
        grb.addCell(new GridRowBuilder.CellBuilder(grb).addText("some text"));
        lb.addGridRow(grb);

        Slice gridSlice = lb.build();
        SliceMetadata gridInfo = SliceMetadata.from(mContext, gridSlice);
        assertTrue(!gridInfo.hasLargeMode());
    }

    @Test
    public void testGetLargeModeMultipleRows() {
        Uri uri = Uri.parse("content://pkg/slice");

        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        lb.addRow(new ListBuilder.RowBuilder(lb).setTitle("a title"));
        lb.addRow(new ListBuilder.RowBuilder(lb).setTitle("another row another title"));

        Slice rowSlice = lb.build();
        SliceMetadata rowInfo = SliceMetadata.from(mContext, rowSlice);
        assertTrue(rowInfo.hasLargeMode());
    }

    @Test
    public void testGetLargeModeSingleRow() {
        Uri uri = Uri.parse("content://pkg/slice");

        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        lb.addRow(new ListBuilder.RowBuilder(lb).setTitle("a title"));

        Slice rowSlice = lb.build();
        SliceMetadata rowInfo = SliceMetadata.from(mContext, rowSlice);
        assertTrue(!rowInfo.hasLargeMode());
    }

    @Test
    public void testGetTogglesNone() {
        Uri uri = Uri.parse("content://pkg/slice");

        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        lb.addRow(new ListBuilder.RowBuilder(lb).setTitle("a title"));

        Slice rowSlice = lb.build();
        SliceMetadata rowInfo = SliceMetadata.from(mContext, rowSlice);
        assertTrue(rowInfo.getToggles().isEmpty());
    }

    @Test
    public void testGetTogglesSingle() {
        Uri uri = Uri.parse("content://pkg/slice");
        PendingIntent pi = getIntent("");

        SliceAction toggleAction = new SliceAction(pi, "toggle", false /* isChecked */);
        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        lb.addRow(new ListBuilder.RowBuilder(lb)
                .setTitle("another title")
                .addEndItem(toggleAction));

        Slice toggleSlice = lb.build();
        SliceMetadata toggleInfo = SliceMetadata.from(mContext, toggleSlice);
        List<androidx.slice.core.SliceAction> actualToggles = toggleInfo.getToggles();
        assertEquals(1, actualToggles.size());
        assertEquivalent(toggleAction, actualToggles.get(0));
    }

    @Test
    public void testGetTogglesMultiple() {
        Uri uri = Uri.parse("content://pkg/slice");
        PendingIntent pi = getIntent("");
        Bitmap b = Bitmap.createBitmap(50, 25, Bitmap.Config.ARGB_8888);
        new Canvas(b).drawColor(0xffff0000);
        IconCompat icon = IconCompat.createWithBitmap(b);

        SliceAction toggleAction = new SliceAction(pi, icon, "toggle", false /* isChecked */);
        SliceAction toggleAction2 = new SliceAction(pi, icon, "toggle2", true /* isChecked */);

        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        lb.addRow(new ListBuilder.RowBuilder(lb)
                .setTitle("another title")
                .addEndItem(toggleAction)
                .addEndItem(toggleAction2));

        Slice toggleSlice = lb.build();
        SliceMetadata toggleInfo = SliceMetadata.from(mContext, toggleSlice);
        List<androidx.slice.core.SliceAction> actualToggles = toggleInfo.getToggles();

        List<SliceAction> expectedToggles = new ArrayList<>();
        expectedToggles.add(toggleAction);
        expectedToggles.add(toggleAction2);

        for (int i = 0; i < expectedToggles.size(); i++) {
            assertEquivalent(expectedToggles.get(i), actualToggles.get(i));
        }
    }

    @Test
    public void testGetRangeNull() {
        Uri uri = Uri.parse("content://pkg/slice");

        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        lb.addRow(new ListBuilder.RowBuilder(lb).setTitle("a title"));

        Slice rowSlice = lb.build();
        SliceMetadata rowInfo = SliceMetadata.from(mContext, rowSlice);
        assertNull(rowInfo.getRange());
    }

    @Test
    public void testGetRangeForSlider() {
        Uri uri = Uri.parse("content://pkg/slice");
        PendingIntent pi = getIntent("");

        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        lb.addInputRange(new ListBuilder.InputRangeBuilder(lb)
                .setTitle("another title")
                .setValue(7)
                .setMin(5)
                .setMax(10)
                .setInputAction(pi));

        Slice sliderSlice = lb.build();
        SliceMetadata sliderInfo = SliceMetadata.from(mContext, sliderSlice);

        Pair<Integer, Integer> values = sliderInfo.getRange();
        assertEquals(5, (int) values.first);
        assertEquals(10, (int) values.second);

        int currentValue = sliderInfo.getRangeValue();
        assertEquals(7, currentValue);
    }

    @Test
    public void testGetInputRangeAction() {
        Uri uri = Uri.parse("content://pkg/slice");
        PendingIntent expectedIntent = getIntent("rangeintent");

        Bitmap b = Bitmap.createBitmap(50, 25, Bitmap.Config.ARGB_8888);
        new Canvas(b).drawColor(0xffff0000);
        IconCompat icon = IconCompat.createWithBitmap(b);
        SliceAction primaryAction = new SliceAction(getIntent(""), icon, "action");

        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        lb.addInputRange(new ListBuilder.InputRangeBuilder(lb)
                .setTitle("another title")
                .setValue(7)
                .setMin(5)
                .setMax(10)
                .setPrimaryAction(primaryAction)
                .setInputAction(expectedIntent));
        Slice sliderSlice = lb.build();

        SliceMetadata sliderInfo = SliceMetadata.from(mContext, sliderSlice);
        assertEquals(expectedIntent, sliderInfo.getInputRangeAction());
        assertEquivalent(primaryAction, sliderInfo.getPrimaryAction());
    }

    @Test
    public void testGetRangeForProgress() {
        Uri uri = Uri.parse("content://pkg/slice");

        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        lb.addRange(new ListBuilder.RangeBuilder(lb)
                .setTitle("another title")
                .setValue(5)
                .setMax(10));

        Slice sliderSlice = lb.build();
        SliceMetadata progressInfo = SliceMetadata.from(mContext, sliderSlice);
        Pair<Integer, Integer> values = progressInfo.getRange();
        assertEquals(0, (int) values.first);
        assertEquals(10, (int) values.second);
    }

    @Test
    public void testKeywords() {
        Uri uri = Uri.parse("content://pkg/slice");
        Slice keywordSlice = new Slice.Builder(uri)
                .addHints(HINT_KEYWORDS)
                .addText("keyword1", null)
                .addText("keyword2", null)
                .addText("keyword3", null).build();
        Slice slice = new Slice.Builder(uri)
                .addText("Some text", null, HINT_TITLE)
                .addText("Some other text", null)
                .addSubSlice(keywordSlice)
                .build();

        SliceMetadata SliceMetadata1 = SliceMetadata.from(mContext, slice);
        List<String> sliceKeywords = SliceMetadata1.getSliceKeywords();
        String[] expectedList = new String[] {"keyword1", "keyword2", "keyword3"};
        assertArrayEquals(expectedList, sliceKeywords.toArray());

        // Make sure it doesn't find keywords that aren't there
        Slice slice2 = new Slice.Builder(uri)
                .addText("Some text", null, HINT_TITLE)
                .addText("Some other text", null).build();

        SliceMetadata SliceMetadata2 = SliceMetadata.from(mContext, slice2);
        List<String> slice2Keywords = SliceMetadata2.getSliceKeywords();
        assertNull(slice2Keywords);

        // Make sure empty list if specified to have no keywords
        Slice noKeywordSlice = new Slice.Builder(uri).addHints(HINT_KEYWORDS).build();
        Slice slice3 = new Slice.Builder(uri)
                .addText("Some text", null, HINT_TITLE)
                .addSubSlice(noKeywordSlice)
                .build();

        SliceMetadata sliceMetadata3 = SliceMetadata.from(mContext, slice3);
        List<String> slice3Keywords = sliceMetadata3.getSliceKeywords();
        assertTrue(slice3Keywords.isEmpty());
    }

    @Test
    public void testGetLoadingState() {
        Uri uri = Uri.parse("content://pkg/slice");
        Slice s1 = new ListBuilder(mContext, uri, INFINITY).build();
        SliceMetadata SliceMetadata1 = SliceMetadata.from(mContext, s1);
        int actualState1 = SliceMetadata1.getLoadingState();
        assertEquals(LOADED_NONE, actualState1);

        ListBuilder lb = new ListBuilder(mContext, uri, INFINITY);
        Slice s2 = lb.addRow(new ListBuilder.RowBuilder(lb)
                .setTitle(null, true /* isLoading */))
                .build();
        SliceMetadata SliceMetadata2 = SliceMetadata.from(mContext, s2);
        int actualState2 = SliceMetadata2.getLoadingState();
        assertEquals(LOADED_PARTIAL, actualState2);

        ListBuilder lb2 = new ListBuilder(mContext, uri, INFINITY);
        Slice s3 = lb2.addRow(new ListBuilder.RowBuilder(lb2)
                .setTitle("Title", false /* isLoading */))
                .build();
        SliceMetadata SliceMetadata3 = SliceMetadata.from(mContext, s3);
        int actualState3 = SliceMetadata3.getLoadingState();
        assertEquals(LOADED_ALL, actualState3);
    }

    @Test
    public void testGetExpiry() {
        Uri uri = Uri.parse("content://pkg/slice");
        long timestamp = System.currentTimeMillis();
        long ttl = TimeUnit.DAYS.toMillis(1);
        Slice ttlSlice = new Slice.Builder(uri)
                .addText("Some text", null)
                .addLong(timestamp, null)
                .addLong(timestamp, null, SliceHints.HINT_LAST_UPDATED)
                .addLong(ttl, null, SliceHints.HINT_TTL)
                .build();

        SliceMetadata si1 = SliceMetadata.from(mContext, ttlSlice);
        long retrievedTtl = si1.getExpiry();
        assertEquals(ttl, retrievedTtl);

        Slice noTtlSlice = new Slice.Builder(uri)
                .addText("Some text", null)
                .addLong(timestamp, null).build();
        SliceMetadata si2 = SliceMetadata.from(mContext, noTtlSlice);
        long retrievedTtl2 = si2.getExpiry();
        assertEquals(0, retrievedTtl2);
    }

    @Test
    public void testGetLastUpdated() {
        Uri uri = Uri.parse("content://pkg/slice");
        long timestamp = System.currentTimeMillis();
        long ttl = TimeUnit.DAYS.toMillis(1);
        Slice ttlSlice = new Slice.Builder(uri)
                .addText("Some text", null)
                .addLong(timestamp - 20, null)
                .addLong(timestamp, null, SliceHints.HINT_LAST_UPDATED)
                .addLong(ttl, null, SliceHints.HINT_TTL)
                .build();

        SliceMetadata si1 = SliceMetadata.from(mContext, ttlSlice);
        long retrievedLastUpdated = si1.getLastUpdatedTime();
        assertEquals(timestamp, retrievedLastUpdated);

        Slice noTtlSlice = new Slice.Builder(uri)
                .addText("Some text", null)
                .addLong(timestamp, null).build();

        SliceMetadata si2 = SliceMetadata.from(mContext, noTtlSlice);
        long retrievedLastUpdated2 = si2.getLastUpdatedTime();
        assertEquals(0, retrievedLastUpdated2);
    }

    @Test
    public void testIsPermissionSlice() {
        Uri uri = Uri.parse("content://pkg/slice");
        Slice permissionSlice =
                SliceProvider.createPermissionSlice(mContext, uri, mContext.getPackageName());

        SliceMetadata metadata = SliceMetadata.from(mContext, permissionSlice);
        assertEquals(true, metadata.isPermissionSlice());
    }

    private PendingIntent getIntent(String action) {
        Intent intent = new Intent(action);
        intent.setClassName(mContext.getPackageName(), SliceRenderActivity.class.getName());
        return PendingIntent.getActivity(mContext, 0, intent, 0);
    }

    private void assertEquivalent(androidx.slice.core.SliceAction desired,
            androidx.slice.core.SliceAction actual) {
        assertEquals(desired.getTitle(), actual.getTitle());
        assertEquals(desired.getContentDescription(), actual.getContentDescription());
        assertEquals(desired.isToggle(), actual.isToggle());
        assertEquals(desired.isDefaultToggle(), actual.isDefaultToggle());
        assertEquals(desired.isChecked(), actual.isChecked());
        assertEquals(desired.getPriority(), actual.getPriority());
        assertEquals(desired.getIcon() == null, actual.getIcon() == null);
        assertEquals(desired.getImageMode(), actual.getImageMode());
    }
}
