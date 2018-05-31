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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import static org.junit.Assert.assertFalse;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;
import androidx.slice.render.SliceRenderActivity;
import androidx.slice.widget.SliceLiveData;
import androidx.slice.widget.SliceView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for {@link SliceView}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class SliceViewTest {

    private final Context mContext = InstrumentationRegistry.getContext();
    private SliceView mSliceView;

    @Before
    @UiThreadTest
    public void setup() {
        mSliceView = new SliceView(mContext);
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
    }

    @Test
    public void testSetSlice() {
        Uri uri = Uri.parse("content://pkg/slice");
        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        lb.addRow(new ListBuilder.RowBuilder(lb)
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
        lb.addRow(new ListBuilder.RowBuilder(lb)
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
        lb.addRow(new ListBuilder.RowBuilder(lb)
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
        lb.addRow(new ListBuilder.RowBuilder(lb)
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
        lb.addRow(new ListBuilder.RowBuilder(lb)
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
    public void testSetValidActions() {
        Uri uri = Uri.parse("content://pkg/slice");
        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        lb.addRow(new ListBuilder.RowBuilder(lb)
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

    @Test(expected = IllegalArgumentException.class)
    public void testSetInvalidActions() {
        Uri uri = Uri.parse("content://pkg/slice");
        ListBuilder lb = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        lb.addRow(new ListBuilder.RowBuilder(lb)
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
        assertEquals(desired.getIcon() == null, actual.getIcon() == null);
        assertEquals(desired.getImageMode(), actual.getImageMode());
    }
}
