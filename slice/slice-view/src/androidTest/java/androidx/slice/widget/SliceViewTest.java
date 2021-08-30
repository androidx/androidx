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

package androidx.slice.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.graphics.drawable.IconCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.slice.Slice;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;
import androidx.slice.render.SliceRenderActivity;
import androidx.slice.view.test.R;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tests for {@link SliceView}.
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
@SdkSuppress(minSdkVersion = 19)
public class SliceViewTest {

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private SliceView mSliceView;

    @Before
    @UiThreadTest
    public void setup() {
        mContext.setTheme(R.style.AppTheme);
        mSliceView = new SliceView(mContext);
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
    }

    @Test
    public void testSetSlice() {
        Uri uri = Uri.parse("content://pkg/slice");
        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        lb.addRow(new ListBuilder.RowBuilder()
                .setTitle("Title")
                .setSubtitle("Subtitle")
                .setPrimaryAction(getAction("Action")));
        Slice s = lb.build();
        mSliceView.setSlice(s);

        assertEquals(s, mSliceView.getSlice());
    }

    @Test
    public void testSetNullSlice() {
        Uri uri = Uri.parse("content://pkg/slice");
        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        lb.addRow(new ListBuilder.RowBuilder()
                .setTitle("Title")
                .setSubtitle("Subtitle")
                .setPrimaryAction(getAction("Action")));
        Slice s = lb.build();

        mSliceView.setSlice(s);
        assertEquals(s, mSliceView.getSlice());

        mSliceView.setSlice(null);
        assertNull(mSliceView.getSlice());
    }

    @Test
    public void testSetScrollable() {
        Uri uri = Uri.parse("content://pkg/slice");
        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        lb.addRow(new ListBuilder.RowBuilder()
                .setTitle("Title")
                .setSubtitle("Subtitle")
                .setPrimaryAction(getAction("Action")));
        Slice s = lb.build();

        mSliceView.setSlice(s);
        assertTrue(mSliceView.isScrollable());

        mSliceView.setScrollable(false);
        assertFalse(mSliceView.isScrollable());

        mSliceView.setScrollable(true);
        assertTrue(mSliceView.isScrollable());
    }

    @Test
    public void testGetActionsNull() {
        Uri uri = Uri.parse("content://pkg/slice");
        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        lb.addRow(new ListBuilder.RowBuilder()
                .setTitle("Title")
                .setSubtitle("Subtitle")
                .setPrimaryAction(getAction("Action")));
        Slice s = lb.build();
        mSliceView.setSlice(s);

        List<androidx.slice.core.SliceAction> actualActions = mSliceView.getSliceActions();
        assertNull(actualActions);
    }

    @Test
    public void testGetActions() {
        Uri uri = Uri.parse("content://pkg/slice");
        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        lb.addRow(new ListBuilder.RowBuilder()
                .setTitle("Title")
                .setSubtitle("Subtitle")
                .setPrimaryAction(getAction("Action")));

        Bitmap b = Bitmap.createBitmap(50, 25, Bitmap.Config.ARGB_8888);
        new Canvas(b).drawColor(0xffff0000);
        IconCompat icon = IconCompat.createWithBitmap(b);

        SliceAction a1 = new SliceAction(getIntent(""), icon, "action1");
        SliceAction a2 = new SliceAction(getIntent(""), icon, "action2");
        SliceAction a3 = new SliceAction(getIntent(""), icon, "action3");
        SliceAction a4 = new SliceAction(getIntent(""), icon, "action4");

        ArrayList<SliceAction> expectedActions = new ArrayList<>();
        expectedActions.add(a1);
        expectedActions.add(a2);
        expectedActions.add(a3);
        expectedActions.add(a4);

        for (SliceAction action : expectedActions) {
            lb.addAction(action);
        }

        mSliceView.setSlice(lb.build());
        List<androidx.slice.core.SliceAction> actualActions = mSliceView.getSliceActions();
        for (int i = 0; i < expectedActions.size(); i++) {
            assertEquivalent(expectedActions.get(i), actualActions.get(i));
        }
    }

    @Test
    public void testSortSliceActions() {
        Bitmap b = Bitmap.createBitmap(50, 25, Bitmap.Config.ARGB_8888);
        new Canvas(b).drawColor(0xffff0000);
        IconCompat icon = IconCompat.createWithBitmap(b);

        SliceAction a1 = new SliceAction(getIntent(""), icon, "action1").setPriority(0);
        SliceAction a2 = new SliceAction(getIntent(""), icon, "action2").setPriority(1);
        SliceAction a3 = new SliceAction(getIntent(""), icon, "action3").setPriority(2);
        SliceAction a4 = new SliceAction(getIntent(""), icon, "action4").setPriority(10);
        SliceAction a5 = new SliceAction(getIntent(""), icon, "action5").setPriority(-1);

        ArrayList<SliceAction> actions = new ArrayList<>();
        actions.add(a2);
        actions.add(a3);
        actions.add(a5);
        actions.add(a1);
        actions.add(a4);

        ArrayList<SliceAction> expectedActions = new ArrayList<>();
        expectedActions.add(a1);
        expectedActions.add(a2);
        expectedActions.add(a3);
        expectedActions.add(a4);
        expectedActions.add(a5);

        Collections.sort(actions, SliceView.SLICE_ACTION_PRIORITY_COMPARATOR);

        for (int i = 0; i < expectedActions.size(); i++) {
            assertEquivalent(expectedActions.get(i), actions.get(i));
        }
    }

    @Test
    public void testSetValidActions() {
        Uri uri = Uri.parse("content://pkg/slice");
        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        lb.addRow(new ListBuilder.RowBuilder()
                .setTitle("Title")
                .setSubtitle("Subtitle")
                .setPrimaryAction(getAction("Action")));

        Bitmap b = Bitmap.createBitmap(50, 25, Bitmap.Config.ARGB_8888);
        new Canvas(b).drawColor(0xffff0000);
        IconCompat icon = IconCompat.createWithBitmap(b);

        SliceAction a1 = new SliceAction(getIntent(""), icon, "action1");
        SliceAction a2 = new SliceAction(getIntent(""), icon, "action2");
        SliceAction a3 = new SliceAction(getIntent(""), icon, "action3");
        SliceAction a4 = new SliceAction(getIntent(""), icon, "action4");
        lb.addAction(a1);
        lb.addAction(a2);
        lb.addAction(a3);
        lb.addAction(a4);

        mSliceView.setSlice(lb.build());
        List<androidx.slice.core.SliceAction> originalActions = mSliceView.getSliceActions();

        List<androidx.slice.core.SliceAction> actionsToSet = new ArrayList<>();
        actionsToSet.add(originalActions.get(1));
        actionsToSet.add(originalActions.get(3));

        mSliceView.setSliceActions(actionsToSet);

        List<androidx.slice.core.SliceAction> actualActions = mSliceView.getSliceActions();

        for (int i = 0; i < actionsToSet.size(); i++) {
            assertEquivalent(actionsToSet.get(i), actualActions.get(i));
        }
    }

    @Test
    public void testSetNullActionsOnHeader() {
        Uri uri = Uri.parse("content://pkg/slice");
        PendingIntent pi = getIntent("");
        Bitmap b = Bitmap.createBitmap(50, 25, Bitmap.Config.ARGB_8888);
        new Canvas(b).drawColor(0xffff0000);
        IconCompat icon = IconCompat.createWithBitmap(b);

        SliceAction action1 = SliceAction.create(pi, icon, ListBuilder.SMALL_IMAGE, "action1");
        SliceAction action2 = SliceAction.createDeeplink(pi, icon,
                ListBuilder.SMALL_IMAGE, "action2");
        SliceAction action3 = SliceAction.create(pi, icon, ListBuilder.SMALL_IMAGE, "action3");

        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        lb.setHeader(new ListBuilder.HeaderBuilder()
                .setTitle("Text")
                .setPrimaryAction(getAction("Action")))
                .addAction(action1)
                .addAction(action2)
                .addAction(action3);

        ArrayList<SliceAction> expectedActions = new ArrayList<>();
        expectedActions.add(action1);
        expectedActions.add(action2);
        expectedActions.add(action3);

        mSliceView.setSlice(lb.build());

        List<androidx.slice.core.SliceAction> actualActions = mSliceView.getSliceActions();
        for (int i = 0; i < expectedActions.size(); i++) {
            assertEquivalent(expectedActions.get(i), actualActions.get(i));
        }

        mSliceView.setSliceActions(null);
        assertNull(mSliceView.getSliceActions());
    }

    @Test
    public void testSetNullActionsOnRow() {
        Uri uri = Uri.parse("content://pkg/slice");
        PendingIntent pi = getIntent("");
        Bitmap b = Bitmap.createBitmap(50, 25, Bitmap.Config.ARGB_8888);
        new Canvas(b).drawColor(0xffff0000);
        IconCompat icon = IconCompat.createWithBitmap(b);

        SliceAction action1 = SliceAction.create(pi, icon, ListBuilder.SMALL_IMAGE, "action1");
        SliceAction action2 = SliceAction.createDeeplink(pi, icon,
                ListBuilder.SMALL_IMAGE, "action2");
        SliceAction action3 = SliceAction.create(pi, icon, ListBuilder.SMALL_IMAGE, "action3");

        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        lb.addRow(new ListBuilder.RowBuilder()
                .setTitle("Text")
                .setPrimaryAction(getAction("Action"))
                .addEndItem(action1)
                .addEndItem(action2)
                .addEndItem(action3));

        ArrayList<SliceAction> expectedActions = new ArrayList<>();
        expectedActions.add(action1);
        expectedActions.add(action2);
        expectedActions.add(action3);

        mSliceView.setSlice(lb.build());

        List<androidx.slice.core.SliceAction> actualActions = mSliceView.getSliceActions();
        for (int i = 0; i < expectedActions.size(); i++) {
            assertEquivalent(expectedActions.get(i), actualActions.get(i));
        }

        mSliceView.setSliceActions(null);
        assertNull(mSliceView.getSliceActions());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetInvalidActions() {
        Uri uri = Uri.parse("content://pkg/slice");
        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        lb.addRow(new ListBuilder.RowBuilder()
                .setTitle("Title")
                .setSubtitle("Subtitle")
                .setPrimaryAction(getAction("Action")));

        Bitmap b = Bitmap.createBitmap(50, 25, Bitmap.Config.ARGB_8888);
        new Canvas(b).drawColor(0xffff0000);
        IconCompat icon = IconCompat.createWithBitmap(b);

        SliceAction a1 = new SliceAction(getIntent(""), icon, "action1");
        SliceAction a2 = new SliceAction(getIntent(""), icon, "action2");
        SliceAction a3 = new SliceAction(getIntent(""), icon, "action3");
        SliceAction a4 = new SliceAction(getIntent(""), icon, "action4");
        lb.addAction(a1);
        lb.addAction(a2);
        lb.addAction(a3);
        lb.addAction(a4);

        mSliceView.setSlice(lb.build());

        List<androidx.slice.core.SliceAction> actionsToSet = new ArrayList<>();
        actionsToSet.add(new SliceAction(getIntent(""), icon, "action1"));
        actionsToSet.add(new SliceAction(getIntent(""), icon, "action2"));

        mSliceView.setSliceActions(actionsToSet);
    }

    @Test(expected = IllegalStateException.class)
    public void testSetActionsNullSlice() {
        mSliceView.setSlice(null);

        Bitmap b = Bitmap.createBitmap(50, 25, Bitmap.Config.ARGB_8888);
        new Canvas(b).drawColor(0xffff0000);
        IconCompat icon = IconCompat.createWithBitmap(b);

        List<androidx.slice.core.SliceAction> actionsToSet = new ArrayList<>();
        actionsToSet.add(new SliceAction(getIntent(""), icon, "action1"));
        actionsToSet.add(new SliceAction(getIntent(""), icon, "action2"));

        mSliceView.setSliceActions(actionsToSet);
    }

    @Test
    public void testHeightMin() {
        Uri uri = Uri.parse("content://pkg/slice");
        SliceViewPolicy p = new SliceViewPolicy();
        mSliceView.setMode(SliceView.MODE_SMALL);
        mSliceView.setSliceViewPolicy(p);

        mSliceView.setSlice(new ListBuilder(mContext, uri, ListBuilder.INFINITY)
                .addInputRange(new ListBuilder.InputRangeBuilder()
                        .setTitle("Title")
                        .setSubtitle("Subtitle")
                        .setPrimaryAction(SliceAction.createToggle(getIntent(""), "Switch", true))
                        .setMin(0)
                        .setMax(5)
                        .setInputAction(getIntent("")))
                .build());

        // Test a height between min and max heights, full width because that doesn't matter.
        int width = mContext.getResources().getDisplayMetrics().widthPixels;
        int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                mContext.getResources().getDisplayMetrics());
        mSliceView.setLayoutParams(new ViewGroup.LayoutParams(width, height));
        mSliceView.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));

        assertEquals(height, mSliceView.getMeasuredHeight());
        assertEquals(height, p.getMaxSmallHeight());
    }

    @Test
    public void testHeightBetweenMinAndMax() {
        Uri uri = Uri.parse("content://pkg/slice");
        SliceViewPolicy p = new SliceViewPolicy();
        mSliceView.setMode(SliceView.MODE_SMALL);
        mSliceView.setSliceViewPolicy(p);

        mSliceView.setSlice(new ListBuilder(mContext, uri, ListBuilder.INFINITY)
                .addInputRange(new ListBuilder.InputRangeBuilder()
                        .setTitle("Title")
                        .setSubtitle("Subtitle")
                        .setPrimaryAction(SliceAction.createToggle(getIntent(""), "Switch", true))
                        .setMin(0)
                        .setMax(5)
                        .setInputAction(getIntent("")))
                .build());

        // Test a height between min and max heights, full width because that doesn't matter.
        int width = mContext.getResources().getDisplayMetrics().widthPixels;
        int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 49,
                mContext.getResources().getDisplayMetrics());
        mSliceView.setLayoutParams(new ViewGroup.LayoutParams(width, height));
        mSliceView.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));

        assertEquals(height, mSliceView.getMeasuredHeight());
        assertEquals(height, p.getMaxSmallHeight());
    }

    @Test
    public void testDefaultHideTitleItems() {
        Uri uri = Uri.parse("content://pkg/slice");
        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        lb.addRow(new ListBuilder.RowBuilder()
                .setTitleItem(getAction("Action"))
                .setTitle("Title")
                .setSubtitle("Subtitle")
                .setPrimaryAction(getAction("Action")));
        Slice s = lb.build();

        mSliceView.setSlice(s);

        RowContent row = (RowContent) mSliceView.mListContent.getRowItems().get(0);
        assertFalse(row.hasTitleItems());
        assertNull(row.getStartItem());
    }

    @Test
    public void testShowTitleItems() {
        Uri uri = Uri.parse("content://pkg/slice");
        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        lb.addRow(new ListBuilder.RowBuilder()
                .setTitleItem(getAction("Action"))
                .setTitle("Title")
                .setSubtitle("Subtitle")
                .setPrimaryAction(getAction("Action")));
        Slice s = lb.build();

        mSliceView.setSlice(s);
        mSliceView.setShowTitleItems(true);

        RowContent row = (RowContent) mSliceView.mListContent.getRowItems().get(0);
        assertTrue(row.hasTitleItems());
        assertNotNull(row.getStartItem());

        mSliceView.setShowTitleItems(false);

        assertFalse(row.hasTitleItems());
        assertNull(row.getStartItem());
    }

    @Test
    public void testHideHeaderDividerWhenOnlyOneRow() {
        Uri uri = Uri.parse("content://pkg/slice");
        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        lb.addRow(new ListBuilder.RowBuilder()
                .setTitle("Title")
                .setSubtitle("Subtitle")
                .setPrimaryAction(getAction("Action")));
        Slice s = lb.build();

        mSliceView.setSlice(s);
        mSliceView.setShowHeaderDivider(true);

        assertFalse(mSliceView.mListContent.getHeader().hasBottomDivider());
    }

    @Test
    public void testShowHeaderDivider() {
        Uri uri = Uri.parse("content://pkg/slice");
        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        lb.addRow(new ListBuilder.RowBuilder()
                .setTitle("Title")
                .setSubtitle("Subtitle")
                .setPrimaryAction(getAction("Action")));
        lb.addRow(new ListBuilder.RowBuilder()
                .setTitle("Title")
                .setSubtitle("Subtitle")
                .setPrimaryAction(getAction("Action")));
        Slice s = lb.build();

        mSliceView.setSlice(s);
        mSliceView.setShowHeaderDivider(true);

        assertTrue(mSliceView.mListContent.getHeader().hasBottomDivider());
    }

    @Test
    public void testDefaultHideActionDividers() {
        Uri uri = Uri.parse("content://pkg/slice");
        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        lb.addRow(new ListBuilder.RowBuilder()
                .setTitle("Title")
                .setSubtitle("Subtitle")
                .setPrimaryAction(getAction("Action")));
        Slice s = lb.build();

        mSliceView.setSlice(s);

        RowContent row = (RowContent) mSliceView.mListContent.getRowItems().get(0);
        assertFalse(row.hasActionDivider());
    }

    @Test
    public void testShowActionDividers() {
        Uri uri = Uri.parse("content://pkg/slice");
        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        lb.addRow(new ListBuilder.RowBuilder()
                .setTitle("Title")
                .setSubtitle("Subtitle")
                .setPrimaryAction(getAction("Action")));
        Slice s = lb.build();

        mSliceView.setSlice(s);
        mSliceView.setShowActionDividers(true);

        RowContent row = (RowContent) mSliceView.mListContent.getRowItems().get(0);
        assertTrue(row.hasActionDivider());
    }

    @Test
    public void testSetRowStyleFactory() {
        // Create a slice with an unchecked and a checked SliceAction.
        Uri uri = Uri.parse("content://pkg/slice");
        Slice s = new ListBuilder(mContext, uri, ListBuilder.INFINITY)
                .addRow(new ListBuilder.RowBuilder()
                        .setTitle("Header"))
                .addRow(new ListBuilder.RowBuilder()
                        .setTitle("Unchecked")
                        .setSubtitle("Unchecked Subtitle")
                        .setTitleItem(
                                SliceAction.createToggle(getIntent("Check"), "checkbox", false)))
                .addRow(new ListBuilder.RowBuilder()
                        .setTitle("Checked")
                        .setSubtitle("Checked Subtitle")
                        .setTitleItem(
                                SliceAction.createToggle(getIntent("Uncheck"), "checkbox", true)))
                .build();

        // Use an alternative style for checked items.
        mSliceView.setRowStyleFactory(sliceItem -> {
            androidx.slice.core.SliceAction action = SliceMetadata.from(
                    mContext, sliceItem.getSlice()).getPrimaryAction();
            if (action != null && action.isToggle() && action.isChecked()) {
                return R.style.CheckedSliceRowStyle;
            }
            // Use the default style otherwise.
            return 0;
        });

        mSliceView.setSlice(s);

        // Lay out the SliceView to initialize the row views.
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mSliceView.measure(View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.UNSPECIFIED));
                mSliceView.layout(0, 0, 500, 1000);
            }
        });

        // Expected colors for checked items.
        int checkedTitleColor = mContext.getResources().getColor(
                R.color.checkedItemTitleColor);
        int checkedSubtitleColor = mContext.getResources().getColor(
                R.color.checkedItemSubtitleColor);

        // Expected colors for unchecked items (the default theme colors).
        int themeTitleColor = getThemeColor(android.R.attr.textColorPrimary);
        int themeSubtitleColor = getThemeColor(android.R.attr.textColorSecondary);

        RecyclerView recyclerView = (RecyclerView) find(mSliceView, RecyclerView.class);
        assertNotNull(recyclerView);

        // The checked item has the checked row style.
        TextView checkedTitleView = recyclerView.findViewHolderForAdapterPosition(2)
                .itemView.findViewById(android.R.id.title);
        assertEquals("Checked", checkedTitleView.getText());
        assertEquals(checkedTitleColor, checkedTitleView.getCurrentTextColor());

        TextView checkedSubtitleView = recyclerView.findViewHolderForAdapterPosition(2)
                .itemView.findViewById(android.R.id.summary);
        assertEquals("Checked Subtitle", checkedSubtitleView.getText());
        assertEquals(checkedSubtitleColor, checkedSubtitleView.getCurrentTextColor());

        // The unchecked item has the default style.
        TextView uncheckedTitleView = recyclerView.findViewHolderForAdapterPosition(1)
                .itemView.findViewById(android.R.id.title);
        assertEquals("Unchecked", uncheckedTitleView.getText());
        assertEquals(themeTitleColor, uncheckedTitleView.getCurrentTextColor());

        TextView uncheckedSubtitleView = recyclerView.findViewHolderForAdapterPosition(1)
                .itemView.findViewById(android.R.id.summary);
        assertEquals("Unchecked Subtitle", uncheckedSubtitleView.getText());
        assertEquals(themeSubtitleColor, uncheckedSubtitleView.getCurrentTextColor());
    }

    @Test
    @UiThreadTest
    public void testHeaderRowHidden_onlyTwoRowsFit_showsSeeMoreRow() {
        // Create a slice with 3 content rows.
        Uri uri = Uri.parse("content://pkg/slice");
        Slice s = new ListBuilder(mContext, uri, ListBuilder.INFINITY)
                .addRow(new ListBuilder.RowBuilder()
                        .setTitle("Header")
                        .setPrimaryAction(getAction("Action")))
                .addRow(new ListBuilder.RowBuilder().setTitle("0"))
                .addRow(new ListBuilder.RowBuilder().setTitle("1"))
                .addRow(new ListBuilder.RowBuilder().setTitle("2"))
                .setSeeMoreRow(new ListBuilder.RowBuilder().setTitle("See more"))
                .build();

        // This slice view has hideHeaderRow set to true.
        View inflatedView = LayoutInflater.from(mContext).inflate(
                R.layout.hide_header_row_slice_view, /* root= */ null);
        SliceView sliceView = inflatedView.findViewById(R.id.sliceTestSliceView);
        sliceView.setScrollable(false);
        sliceView.setSlice(s);

        // Lay out the slice such that exactly 2 rows fit.
        int rowHeightPixels = mContext.getResources().getDimensionPixelSize(
                R.dimen.abc_slice_action_row_height);
        sliceView.measure(
                View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(2 * rowHeightPixels, View.MeasureSpec.EXACTLY));
        sliceView.layout(0, 0, 2 * rowHeightPixels, 1000);

        RecyclerView recyclerView = (RecyclerView) find(sliceView, RecyclerView.class);
        assertEquals(2, recyclerView.getAdapter().getItemCount());
        TextView view0 = recyclerView.findViewHolderForAdapterPosition(0)
                 .itemView.findViewById(android.R.id.title);
        TextView view1 = recyclerView.findViewHolderForAdapterPosition(1)
                 .itemView.findViewById(android.R.id.title);
        assertEquals("0", view0.getText());
        assertEquals("See more", view1.getText());
    }

    @Test
    @UiThreadTest
    public void testHeaderRowHidden_onlyOneRowFits_showsHeaderRow() {
        // Create a slice with 2 content rows.
        Uri uri = Uri.parse("content://pkg/slice");
        Slice s = new ListBuilder(mContext, uri, ListBuilder.INFINITY)
                .addRow(new ListBuilder.RowBuilder()
                        .setTitle("Header")
                        .setPrimaryAction(getAction("Action")))
                .addRow(new ListBuilder.RowBuilder().setTitle("0"))
                .addRow(new ListBuilder.RowBuilder().setTitle("1"))
                .setSeeMoreRow(new ListBuilder.RowBuilder().setTitle("See more"))
                .build();

        // This slice view has hideHeaderRow set to true.
        View inflatedView = LayoutInflater.from(mContext).inflate(
                R.layout.hide_header_row_slice_view, /* root= */ null);
        SliceView sliceView = inflatedView.findViewById(R.id.sliceTestSliceView);
        sliceView.setScrollable(false);
        sliceView.setSlice(s);

        // Lay out the slice such that exactly 1 row fits.
        int rowHeightPixels = mContext.getResources().getDimensionPixelSize(
                R.dimen.abc_slice_action_row_height);
        sliceView.measure(
                View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(rowHeightPixels, View.MeasureSpec.EXACTLY));
        sliceView.layout(0, 0, rowHeightPixels, 1000);

        // It doesn't make sense to display only "See more". It also doesn't make sense
        // to only show a single content row without "See more. Hence, we show the
        // header row.
        RecyclerView recyclerView = (RecyclerView) find(sliceView, RecyclerView.class);
        assertEquals(1, recyclerView.getAdapter().getItemCount());
        TextView view0 = recyclerView.findViewHolderForAdapterPosition(0)
                 .itemView.findViewById(android.R.id.title);
        assertEquals("Header", view0.getText());
    }

    private int getThemeColor(int colorRes) {
        TypedArray arr = mContext.getTheme().obtainStyledAttributes(new int[] {colorRes});
        assertTrue(arr.hasValue(0));
        int themeColor = arr.getColor(0, -1);
        arr.recycle();
        return themeColor;
    }

    private View find(View v, Class<?> desiredClass) {
        if (desiredClass.isInstance(v)) {
            return v;
        }
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                v = find(vg.getChildAt(i), desiredClass);
                if (v != null) {
                    return v;
                }
            }
        }
        return null;
    }

    private SliceAction getAction(String actionName) {
        Bitmap b = Bitmap.createBitmap(50, 25, Bitmap.Config.ARGB_8888);
        new Canvas(b).drawColor(0xffff0000);
        IconCompat icon = IconCompat.createWithBitmap(b);
        return new SliceAction(getIntent(""), icon, actionName);
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
        assertEquals(desired.getKey(), actual.getKey());
        assertEquals(desired.getIcon() == null, actual.getIcon() == null);
        assertEquals(desired.getImageMode(), actual.getImageMode());
    }
}
