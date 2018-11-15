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
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceProvider;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;
import androidx.slice.render.SliceRenderActivity;
import androidx.test.InstrumentationRegistry;
import androidx.test.annotation.UiThreadTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

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
@SmallTest
@SdkSuppress(minSdkVersion = 19)
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
    public void testHeightBetweenMinAndMax() {
        Uri uri = Uri.parse("content://pkg/slice");
        SliceViewPolicy p = new SliceViewPolicy();
        mSliceView.setMode(SliceView.MODE_SMALL);
        mSliceView.setSliceViewPolicy(p);

        mSliceView.setSlice(new ListBuilder(mContext, uri, ListBuilder.INFINITY)
                .setHeader(new ListBuilder.HeaderBuilder()
                        .setTitle("Title")
                        .setSummary("Summary")
                        .setSubtitle("Subtitle")
                        .setPrimaryAction(SliceAction.createToggle(getIntent(""), "Switch", true)))
                .build());

        // Test a height between min and max heights, full width because that doesn't matter.
        int width = mContext.getResources().getDisplayMetrics().widthPixels;
        int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 49,
                mContext.getResources().getDisplayMetrics());
        mSliceView.setLayoutParams(new ViewGroup.LayoutParams(width, height));
        mSliceView.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));

        assertEquals(height, p.getMaxSmallHeight());
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
        assertEquals(desired.getIcon() == null, actual.getIcon() == null);
        assertEquals(desired.getImageMode(), actual.getImageMode());
    }
}
